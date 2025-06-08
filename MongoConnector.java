package movieticketbookingsystem.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;

public class MongoConnector {

    // --- Configuration ---
    // Default connection string for local MongoDB without auth
    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    // Alternative if using Atlas or specific auth:
    // private static final String CONNECTION_STRING = "mongodb+srv://<username>:<password>@<cluster-url>/<database_name>?retryWrites=true&w=majority";
    private static final String DATABASE_NAME = "movie_ticket_db_mongo"; // Database name
    // --- ---

    private static MongoClient mongoClient = null;
    private static MongoDatabase database = null;

    private MongoConnector() {} // Private constructor

    private static void initialize() {
        if (mongoClient == null) {
            try {
                 ConnectionString connectionString = new ConnectionString(CONNECTION_STRING);
                 MongoClientSettings settings = MongoClientSettings.builder()
                         .applyConnectionString(connectionString)
                         .serverApi(ServerApi.builder()
                                 .version(ServerApiVersion.V1) // Use Stable API
                                 .build())
                         .build();

                 mongoClient = MongoClients.create(settings);
                 database = mongoClient.getDatabase(DATABASE_NAME);
                 database.runCommand(new org.bson.Document("ping", 1)); // Verify connection
                System.out.println("Successfully connected to MongoDB database: " + DATABASE_NAME);

            } catch (Exception e) {
                System.err.println("FATAL ERROR: Failed to connect to MongoDB!");
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize MongoDB connection", e);
            }
        }
    }

    public static MongoDatabase getDatabase() {
        if (database == null) {
            initialize();
        }
        return database;
    }

    public static void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                mongoClient = null;
                database = null;
                System.out.println("MongoDB connection closed.");
            } catch (Exception e) {
                System.err.println("Error closing MongoDB connection: " + e.getMessage());
            }
        }
    }
    public static MongoClient getMongoClient() {
        if (mongoClient == null) {
            initialize();
        }
        return mongoClient;
    }
}