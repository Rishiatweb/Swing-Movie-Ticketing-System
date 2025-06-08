package movieticketbookingsystem.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import movieticketbookingsystem.Movie;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovieDAO {

    private final MongoCollection<Document> moviesCollection;

    public MovieDAO() {
        MongoDatabase database = MongoConnector.getDatabase();
        this.moviesCollection = database.getCollection("movies");
        // Insert sample data if collection is empty
         if (moviesCollection.countDocuments() == 0) {
            insertSampleMovie(1, "The Matrix", "A computer hacker learns...", "Cineplex A");
            insertSampleMovie(2, "Inception", "A thief who enters dreams...", "Cineplex B");
            insertSampleMovie(3, "Interstellar", "Explorers travel through space...", "IMAX Theatre");
         }
    }

    public List<Movie> getAllMovies() {
        List<Movie> movies = new ArrayList<>();
        try (MongoCursor<Document> cursor = moviesCollection.find().sort(Sorts.ascending("title")).iterator()) {
            while (cursor.hasNext()) {
                movies.add(mapDocumentToMovie(cursor.next()));
            }
        } catch (Exception e) {
            System.err.println("Error fetching all movies: " + e.getMessage());
            e.printStackTrace();
        }
        return movies;
    }

    public Optional<Movie> getMovieByIntId(int movieId) {
         try {
            Document movieDoc = moviesCollection.find(Filters.eq("movie_id", movieId)).first();
            if (movieDoc != null) {
                return Optional.of(mapDocumentToMovie(movieDoc));
            }
        } catch (Exception e) {
            System.err.println("Error fetching movie by int ID " + movieId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

     public Optional<Movie> getMovieByObjectId(String idString) {
         try {
             ObjectId objectId = new ObjectId(idString);
             Document movieDoc = moviesCollection.find(Filters.eq("_id", objectId)).first();
             if (movieDoc != null) {
                 return Optional.of(mapDocumentToMovie(movieDoc));
             }
         } catch (IllegalArgumentException e) {
              System.err.println("Invalid ObjectId format: " + idString);
         } catch (Exception e) {
             System.err.println("Error fetching movie by ObjectId " + idString + ": " + e.getMessage());
             e.printStackTrace();
         }
         return Optional.empty();
     }

    private Movie mapDocumentToMovie(Document doc) {
        ObjectId objectId = doc.getObjectId("_id");
        return new Movie(
                objectId != null ? objectId.toHexString() : null,
                doc.getInteger("movie_id", -1),
                doc.getString("title"),
                doc.getString("description"),
                doc.getString("theatre")
        );
    }

    private void insertSampleMovie(int movieId, String title, String description, String theatre) {
         if (moviesCollection.countDocuments(Filters.eq("movie_id", movieId)) == 0) {
              Document newMovie = new Document("movie_id", movieId)
                                     .append("title", title)
                                     .append("description", description)
                                     .append("theatre", theatre);
              try {
                  moviesCollection.insertOne(newMovie);
                  System.out.println("Inserted sample movie: " + title);
              } catch (Exception e) {
                  System.err.println("Failed to insert sample movie " + title + ": " + e.getMessage());
              }
         }
    }
}