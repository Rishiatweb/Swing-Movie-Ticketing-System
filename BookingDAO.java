package movieticketbookingsystem.db;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.TransactionOptions;
import com.mongodb.ReadPreference;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes; // Correct import
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult; // Import UpdateResult
import com.mongodb.bulk.BulkWriteResult;
import movieticketbookingsystem.Booking;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*; // List, Map, ArrayList, HashMap, Date
import com.mongodb.client.MongoClient;

public class BookingDAO {

    private final MongoCollection<Document> bookingsCollection;
    private final SeatDAO seatDAO;

    public BookingDAO() {
        MongoDatabase database = MongoConnector.getDatabase(); // Get DB to get collection
        this.bookingsCollection = database.getCollection("bookings");
        this.seatDAO = new SeatDAO();
        // Index creation logic (remains the same)
        try {
             bookingsCollection.createIndex(Indexes.ascending("user_id"));
             bookingsCollection.createIndex(Indexes.ascending("showtime_id"));
        } catch (Exception e) { /* ... error handling ... */ }
    }

    public String addBookingTransaction(int userId, int showtimeId, List<String> seats,
                                        Map<String, Integer> snacks, Map<String, Integer> snackPrices,
                                        int seatCost, int snackCost, int totalCost) {

        // --- Start session using MongoClient from Connector ---
        try (ClientSession clientSession = MongoConnector.getMongoClient().startSession()) { // <-- CORRECTED LINE
            final String[] bookingMongoId = {null};

            TransactionOptions txnOptions = TransactionOptions.builder()
                    .readPreference(ReadPreference.primary())
                    .readConcern(ReadConcern.LOCAL)
                    .writeConcern(WriteConcern.MAJORITY).build();

            // Transaction logic remains the same inside withTransaction
            clientSession.withTransaction(() -> {
                // 1. Check seat availability
                if (!seatDAO.checkSeatsAvailability(clientSession, showtimeId, seats)) {
                    throw new RuntimeException("Seat Conflict...");
                }
                // 2. Prepare Booking Document (same logic)
                 List<Document> snacksDocList = new ArrayList<>(); if (snacks != null) { snacks.forEach((name, qty) -> snacksDocList.add(new Document("snack_name", name).append("quantity", qty).append("price_per_item", snackPrices.getOrDefault(name, 0)))); }
                 long maxId = 0; Document maxDoc = bookingsCollection.find(clientSession).sort(Sorts.descending("booking_id")).limit(1).projection(new Document("booking_id",1)).first(); if (maxDoc != null) maxId = maxDoc.getInteger("booking_id", 0); int nextBookingId = (int)maxId + 1;
                 Document newBooking = new Document("booking_id", nextBookingId).append("user_id", userId).append("showtime_id", showtimeId).append("booking_timestamp", Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))).append("booked_seats", seats).append("snacks", snacksDocList).append("seat_cost", seatCost).append("snack_cost", snackCost).append("total_cost", totalCost).append("status", "confirmed");

                // 3. Insert Booking Document (same logic)
                 InsertOneResult insertResult = bookingsCollection.insertOne(clientSession, newBooking); if (!insertResult.wasAcknowledged() || insertResult.getInsertedId() == null) { throw new RuntimeException("Booking insertion failed."); } bookingMongoId[0] = insertResult.getInsertedId().asObjectId().getValue().toHexString();

                // 4. Update Seat Status (same logic)
                 if (!seatDAO.updateSeatStatusBulk(clientSession, showtimeId, seats, "booked")) { throw new RuntimeException("Failed to update seat status..."); }

                System.out.println("Transaction successful for booking " + bookingMongoId[0]);
                return "Committed";
            }, txnOptions);

            return bookingMongoId[0]; // Return ID if successful

        } catch (Exception e) { // Catch broader exceptions during session/transaction
            System.err.println("Booking transaction failed: " + e.getMessage());
            e.printStackTrace();
            return null; // Indicate failure
        }
    }

    // cancelBookingTransaction needs the same fix for starting the session
    public boolean cancelBookingTransaction(String bookingMongoId, int showtimeId, List<String> seatsToRelease) {
         // --- Start session using MongoClient from Connector ---
         try (ClientSession clientSession = MongoConnector.getMongoClient().startSession()) { // <-- CORRECTED LINE

            TransactionOptions txnOptions = TransactionOptions.builder()
                    .readPreference(ReadPreference.primary())
                    .readConcern(ReadConcern.LOCAL)
                    .writeConcern(WriteConcern.MAJORITY).build();

            // Transaction logic remains the same inside withTransaction
            clientSession.withTransaction(() -> {
                ObjectId objectId; try { objectId = new ObjectId(bookingMongoId); } catch (IllegalArgumentException e) { throw new RuntimeException("Invalid Booking ID format."); }
                 UpdateResult updateResult = bookingsCollection.updateOne(clientSession, Filters.and(Filters.eq("_id", objectId), Filters.eq("status", "confirmed")), Updates.set("status", "cancelled"));
                 if (updateResult.getMatchedCount() == 0) { throw new RuntimeException("Booking not found or not 'confirmed'."); }
                 if (!seatDAO.updateSeatStatusBulk(clientSession, showtimeId, seatsToRelease, "available")) { throw new RuntimeException("Failed to release booked seats."); }
                 System.out.println("Transaction successful for cancelling booking " + bookingMongoId); return "Committed";
             }, txnOptions);

             return true; // Transaction succeeded

         } catch (Exception e) { // Catch broader exceptions
             System.err.println("Booking cancellation transaction failed for ID " + bookingMongoId + ": " + e.getMessage());
             e.printStackTrace();
             return false; // Indicate failure
         }
    }


    // getBookingsByUserId, getBookingByObjectId, getBookingByIntId, mapDocumentToBooking methods remain the same
     public List<Booking> getBookingsByUserId(int userId) { /* Same */ List<Booking> bookings = new ArrayList<>(); Document query = new Document("user_id", userId); try (MongoCursor<Document> cursor = bookingsCollection.find(query).sort(Sorts.descending("booking_timestamp")).iterator()) { while (cursor.hasNext()) { bookings.add(mapDocumentToBooking(cursor.next())); } } catch (Exception e) { System.err.println("Error fetching bookings for user " + userId + ": " + e.getMessage()); e.printStackTrace(); } return bookings; }
     public Optional<Booking> getBookingByObjectId(String idString) { /* Same */ try { ObjectId oid = new ObjectId(idString); Document doc = bookingsCollection.find(Filters.eq("_id", oid)).first(); if (doc != null) return Optional.of(mapDocumentToBooking(doc)); } catch (IllegalArgumentException e) { System.err.println("Invalid ObjectId: " + idString); } catch (Exception e) { System.err.println("Error fetching booking by ObjectId " + idString + ": " + e.getMessage()); e.printStackTrace(); } return Optional.empty(); }
     public Optional<Booking> getBookingByIntId(int bookingId) { /* Same */ try { Document doc = bookingsCollection.find(Filters.eq("booking_id", bookingId)).first(); if (doc != null) return Optional.of(mapDocumentToBooking(doc)); } catch (Exception e) { System.err.println("Error fetching booking by int ID " + bookingId + ": " + e.getMessage()); e.printStackTrace(); } return Optional.empty(); }
     private Booking mapDocumentToBooking(Document doc) { /* Same */ ObjectId oid = doc.getObjectId("_id"); Date tsDate = doc.getDate("booking_timestamp"); LocalDateTime ts = (tsDate != null) ? LocalDateTime.ofInstant(tsDate.toInstant(), ZoneOffset.UTC) : null; List<String> seats = doc.getList("booked_seats", String.class, new ArrayList<>()); Map<String, Integer> snacks = new HashMap<>(); List<Document> snacksDocs = doc.getList("snacks", Document.class, new ArrayList<>()); snacksDocs.forEach(sd -> snacks.put(sd.getString("snack_name"), sd.getInteger("quantity"))); return new Booking(oid != null ? oid.toHexString() : null, doc.getInteger("booking_id", -1), doc.getInteger("user_id", -1), doc.getInteger("showtime_id", -1), ts, seats, snacks, doc.getInteger("seat_cost", 0), doc.getInteger("snack_cost", 0), doc.getInteger("total_cost", 0), doc.getString("status")); }

}