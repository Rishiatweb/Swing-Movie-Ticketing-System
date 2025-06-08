package movieticketbookingsystem.db;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.ClientSession; // Import ClientSession
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import movieticketbookingsystem.SeatSelectionPanel; // For constants
import org.bson.Document;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions; // Import IndexOptions
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult; // Keep if needed elsewhere, not strictly needed here

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SeatDAO {

    private final MongoCollection<Document> seatsCollection;

    public SeatDAO() {
        MongoDatabase database = MongoConnector.getDatabase();
        this.seatsCollection = database.getCollection("seats");
        // Ensure compound unique index
         try {
             seatsCollection.createIndex(Indexes.compoundIndex(
                     Indexes.ascending("showtime_id"),
                     Indexes.ascending("seat_number")
             ), new IndexOptions().unique(true));
        } catch (Exception e) {
             System.err.println("Index creation for seats might have failed: " + e.getMessage());
        }
    }

    public Map<String, String> getSeatStatusForShowtime(int showtimeId) {
        Map<String, String> seatStatusMap = new HashMap<>();
        Document query = new Document("showtime_id", showtimeId);
        try (MongoCursor<Document> cursor = seatsCollection.find(query).iterator()) {
            while (cursor.hasNext()) {
                Document seatDoc = cursor.next();
                seatStatusMap.put(seatDoc.getString("seat_number"), seatDoc.getString("status"));
            }
        } catch (Exception e) {
            System.err.println("Error fetching seat status for showtime " + showtimeId + ": " + e.getMessage());
            e.printStackTrace(); // Log stack trace for debugging
        }
        return seatStatusMap;
    }

     public boolean initializeSeatsForShowtime(int showtimeId, int totalSeats) {
         // 1. Delete existing seats first
         try {
             DeleteResult deleteResult = seatsCollection.deleteMany(Filters.eq("showtime_id", showtimeId));
             System.out.println("Deleted " + deleteResult.getDeletedCount() + " existing seats for showtime " + showtimeId);
         } catch (Exception e) {
              System.err.println("Error deleting existing seats for showtime " + showtimeId + ": " + e.getMessage());
              return false;
         }

         // 2. Prepare new seat documents
         List<Document> newSeatDocs = new ArrayList<>();
         int maxSeats = Math.min(totalSeats, SeatSelectionPanel.TOTAL_POSITIONS);
         for (int i = 0; i < maxSeats; i++) {
             String seatNumber = SeatSelectionPanel.generateSeatNumber(i, SeatSelectionPanel.COLS);
             if (!"Err".equals(seatNumber)) {
                 newSeatDocs.add(new Document("showtime_id", showtimeId)
                                     .append("seat_number", seatNumber)
                                     .append("status", "available"));
             }
         }

         // 3. Bulk insert new seats
         if (!newSeatDocs.isEmpty()) {
             try {
                 seatsCollection.insertMany(newSeatDocs);
                 System.out.println("Inserted " + newSeatDocs.size() + " new seats for showtime " + showtimeId);
                 return true;
             } catch (Exception e) {
                 System.err.println("Error bulk inserting seats for showtime " + showtimeId + ": " + e.getMessage());
                 return false;
             }
         }
         return true; // No seats needed, so successful
     }

    // Must be called within a ClientSession transaction
    public boolean updateSeatStatusBulk(ClientSession session, int showtimeId, List<String> seatNumbers, String newStatus) {
        if (seatNumbers == null || seatNumbers.isEmpty()) return true;

        List<WriteModel<Document>> updates = seatNumbers.stream()
            .map(seatNumber -> new UpdateOneModel<Document>(
                Filters.and(
                    Filters.eq("showtime_id", showtimeId),
                    Filters.eq("seat_number", seatNumber)
                    // Add filter for current status if needed for optimistic locking, e.g.:
                    // Filters.eq("status", newStatus.equals("booked") ? "available" : "booked")
                ),
                Updates.set("status", newStatus)
            ))
            .collect(Collectors.toList());

        try {
            BulkWriteResult result = seatsCollection.bulkWrite(session, updates);
            System.out.println("Seat status update ["+newStatus+"] result: Matched=" + result.getMatchedCount() + ", Modified=" + result.getModifiedCount());
            // If optimistic locking is used (checking current status), modified count might be less than seatNumbers.size() if a seat was already taken.
            // For this implementation, we check availability first, so modifiedCount *should* match.
            return result.wasAcknowledged() && result.getModifiedCount() == seatNumbers.size();
        } catch (MongoBulkWriteException e) {
             System.err.println("Bulk write error updating seat status: " + e.getWriteErrors());
             throw e; // Re-throw to handle in transaction
        } catch (Exception e) {
             System.err.println("Error updating seat status: " + e.getMessage());
              throw new RuntimeException("Failed to update seat status", e);
        }
    }

     // Must be called within a ClientSession transaction
     public boolean checkSeatsAvailability(ClientSession session, int showtimeId, List<String> seatNumbers) {
          if (seatNumbers == null || seatNumbers.isEmpty()) return true;
          try {
               long unavailableCount = seatsCollection.countDocuments(session,
                   Filters.and(
                       Filters.eq("showtime_id", showtimeId),
                       Filters.in("seat_number", seatNumbers),
                       Filters.ne("status", "available")
                   )
               );
               return unavailableCount == 0;
          } catch (Exception e) {
              System.err.println("Error checking seat availability: " + e.getMessage());
              return false; // Assume unavailable on error
          }
     }
}