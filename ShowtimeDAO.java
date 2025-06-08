package movieticketbookingsystem.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import movieticketbookingsystem.Showtime;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Indexes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime; // Added LocalTime
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class ShowtimeDAO {

    private final MongoCollection<Document> showtimesCollection;
    private final SeatDAO seatDAO; // Required for initialization

    public ShowtimeDAO() {
        MongoDatabase database = MongoConnector.getDatabase();
        this.showtimesCollection = database.getCollection("showtimes");
        this.seatDAO = new SeatDAO();
        // Create indexes
        try {
             showtimesCollection.createIndex(Indexes.ascending("show_datetime"));
             showtimesCollection.createIndex(Indexes.ascending("movie_id"));
        } catch (Exception e) {
             System.err.println("Index creation for showtimes might have failed: " + e.getMessage());
        }
        // Insert sample data if collection is empty
        if (showtimesCollection.countDocuments() == 0) {
             LocalDateTime today = LocalDate.now().atStartOfDay(); // Use LocalDate for clarity
             LocalDateTime tomorrow = today.plusDays(1);
             // Use LocalTime constants if defined elsewhere, or create them
             LocalTime t1000 = LocalTime.of(10, 0);
             LocalTime t1300 = LocalTime.of(13, 0);
             LocalTime t1600 = LocalTime.of(16, 0);
             LocalTime t1900 = LocalTime.of(19, 0);

             // Arguments: showtimeId, movieId, dateTime, totalSeats
             insertSampleShowtime(1, 1, today.with(t1300), 50); // Matrix Today 1 PM
             insertSampleShowtime(2, 1, today.with(t1900), 50); // Matrix Today 7 PM
             insertSampleShowtime(3, 2, today.with(t1000), 60); // Inception Today 10 AM
             insertSampleShowtime(4, 2, tomorrow.with(t1300), 60);// Inception Tomorrow 1 PM
             insertSampleShowtime(5, 3, tomorrow.with(t1900), 64);// Interstellar Tomorrow 7 PM
        }
    }

    public List<Showtime> getShowtimesByDate(LocalDate date) {
        List<Showtime> showtimes = new ArrayList<>();
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        Date startDate = Date.from(startOfDay.toInstant(ZoneOffset.UTC));
        Date endDate = Date.from(endOfDay.toInstant(ZoneOffset.UTC));

        Document query = new Document("show_datetime", new Document("$gte", startDate).append("$lt", endDate));

        try (MongoCursor<Document> cursor = showtimesCollection.find(query).sort(Sorts.ascending("show_datetime")).iterator()) {
            while (cursor.hasNext()) {
                showtimes.add(mapDocumentToShowtime(cursor.next()));
            }
        } catch (Exception e) {
            System.err.println("Error fetching showtimes by date " + date + ": " + e.getMessage());
            e.printStackTrace();
        }
        return showtimes;
    }

     public Optional<Showtime> findShowtime(int movieId, LocalDateTime dateTime) {
        Date exactDate = Date.from(dateTime.toInstant(ZoneOffset.UTC));
        Document query = new Document("movie_id", movieId).append("show_datetime", exactDate);
         try {
            Document showtimeDoc = showtimesCollection.find(query).first();
            if (showtimeDoc != null) {
                return Optional.of(mapDocumentToShowtime(showtimeDoc));
            }
        } catch (Exception e) {
            System.err.println("Error finding specific showtime for movie " + movieId + " at " + dateTime + ": " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
     }

    public Optional<Showtime> getShowtimeByIntId(int showtimeId) {
         try {
            Document showtimeDoc = showtimesCollection.find(Filters.eq("showtime_id", showtimeId)).first();
            if (showtimeDoc != null) {
                return Optional.of(mapDocumentToShowtime(showtimeDoc));
            }
        } catch (Exception e) {
            System.err.println("Error fetching showtime by int ID " + showtimeId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Showtime> getShowtimeByObjectId(String idString) {
         try {
             ObjectId objectId = new ObjectId(idString);
             Document showtimeDoc = showtimesCollection.find(Filters.eq("_id", objectId)).first();
             if (showtimeDoc != null) {
                 return Optional.of(mapDocumentToShowtime(showtimeDoc));
             }
         } catch (IllegalArgumentException e) { System.err.println("Invalid ObjectId format: " + idString); }
         catch (Exception e) { System.err.println("Error fetching showtime by ObjectId " + idString + ": " + e.getMessage()); e.printStackTrace(); }
         return Optional.empty();
     }

    // Calls SeatDAO to initialize seats
    public boolean initializeSeatsForShowtime(int showtimeId, int totalSeats) {
        return seatDAO.initializeSeatsForShowtime(showtimeId, totalSeats);
    }

    private Showtime mapDocumentToShowtime(Document doc) {
        ObjectId objectId = doc.getObjectId("_id");
        Date showDate = doc.getDate("show_datetime");
        LocalDateTime showDateTime = (showDate != null) ? LocalDateTime.ofInstant(showDate.toInstant(), ZoneOffset.UTC) : null;

        return new Showtime(
                objectId != null ? objectId.toHexString() : null,
                doc.getInteger("showtime_id", -1),
                doc.getInteger("movie_id", -1),
                showDateTime,
                doc.getInteger("total_seats", 0)
        );
    }

    private void insertSampleShowtime(int showtimeId, int movieId, LocalDateTime showDateTime, int totalSeats) {
        if (showtimesCollection.countDocuments(Filters.eq("showtime_id", showtimeId)) == 0) {
            Date bsonDate = Date.from(showDateTime.toInstant(ZoneOffset.UTC));
            Document newShowtime = new Document("showtime_id", showtimeId)
                                      .append("movie_id", movieId)
                                      .append("show_datetime", bsonDate)
                                      .append("total_seats", totalSeats);
            try {
                showtimesCollection.insertOne(newShowtime);
                System.out.println("Inserted sample showtime ID: " + showtimeId);
                // IMPORTANT: Initialize seats immediately
                initializeSeatsForShowtime(showtimeId, totalSeats);
            } catch (Exception e) {
                 System.err.println("Failed to insert/initialize sample showtime ID " + showtimeId + ": " + e.getMessage());
            }
        }
    }
}