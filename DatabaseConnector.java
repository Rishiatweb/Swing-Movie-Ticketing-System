package movieticketbookingsystem.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {

    // --- IMPORTANT: Replace with your actual database details ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/movie_ticket_db?useSSL=false&serverTimezone=UTC"; // Added timezone
    private static final String DB_USER = "movieappuser"; // Use the user you created (or root)
    private static final String DB_PASSWORD = "your_password"; // Use the password you set
    // --- ---

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("FATAL ERROR: MySQL JDBC Driver not found!");
            throw new RuntimeException("MySQL JDBC Driver not found!", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // Test method (optional)
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            System.out.println("Database connection successful!");
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}