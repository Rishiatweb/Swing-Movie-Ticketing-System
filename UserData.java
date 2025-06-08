package movieticketbookingsystem;

public class UserData {
    private String id; // To hold MongoDB ObjectId as String
    private int userId; // Keep simple int ID for linking if needed
    private String username;
    // No password stored after login

    // Constructor potentially used by DAO
    public UserData(String id, int userId, String username) {
        this.id = id;
        this.userId = userId;
        this.username = username;
    }

    // Constructor if creating before DB insert (ID will be null)
     public UserData(int userId, String username) {
         this(null, userId, username);
     }


    // Getters
    public String getId() { return id; }
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public void setId(String id) { this.id = id; } // Setter needed by DAO
}