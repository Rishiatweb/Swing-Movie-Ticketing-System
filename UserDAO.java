package movieticketbookingsystem.db;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import movieticketbookingsystem.UserData;
import org.bson.Document;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.IndexOptions;

import java.util.Optional;

public class UserDAO {

    private final MongoCollection<Document> usersCollection;

    public UserDAO() {
        MongoDatabase database = MongoConnector.getDatabase();
        this.usersCollection = database.getCollection("users");
        // Ensure unique index on username
        try {
             usersCollection.createIndex(Indexes.ascending("username"), new IndexOptions().unique(true));
        } catch (Exception e) {
             System.err.println("Index creation for 'username' might have failed (may already exist): " + e.getMessage());
        }
         // Insert sample data if collection is empty (HASH PASSWORDS!)
        if (usersCollection.countDocuments() == 0) {
             insertSampleUser("admin", "password"); // Replace with HASHED password
             insertSampleUser("user", "user");     // Replace with HASHED password
        }
    }

    public Optional<UserData> findUserAndVerifyPassword(String username, String plainPassword) {
        try {
            Document userDoc = usersCollection.find(Filters.eq("username", username)).first();
            if (userDoc != null) {
                String storedHash = userDoc.getString("password_hash");
                // --- !!! WARNING: INSECURE - Replace with bcrypt check !!! ---
                if (storedHash != null && storedHash.equals(plainPassword)) {
                    String objectIdString = userDoc.getObjectId("_id").toHexString();
                    int userId = userDoc.getInteger("user_id", -1); // Get int ID
                    String dbUsername = userDoc.getString("username");
                    UserData user = new UserData(objectIdString, userId, dbUsername);
                    return Optional.of(user);
                }
                // --- !!! END OF BCRYPT REPLACEMENT AREA !!! ---
            }
        } catch (Exception e) {
            System.err.println("Error finding user '" + username + "': " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // --- Method to insert sample user if they don't exist ---
    private void insertSampleUser(String username, String plainPassword) {
         // TODO: HASH Password here before storing using BCrypt!
         String passwordToStore = plainPassword; // INSECURE! Use hashed password.

         if (usersCollection.countDocuments(Filters.eq("username", username)) == 0) {
              // Find max user_id and increment (simplistic counter)
              long maxId = 0;
              Document maxDoc = usersCollection.find().sort(new Document("user_id", -1)).limit(1).projection(new Document("user_id", 1)).first();
              if (maxDoc != null) maxId = maxDoc.getInteger("user_id", 0);
              int nextUserId = (int)maxId + 1;

              Document newUser = new Document("user_id", nextUserId)
                                 .append("username", username)
                                 .append("password_hash", passwordToStore);
              try {
                  usersCollection.insertOne(newUser);
                  System.out.println("Inserted sample user: " + username);
              } catch (Exception e) {
                   System.err.println("Failed to insert sample user " + username + ": " + e.getMessage());
              }
         }
    }
}