package movieticketbookingsystem;

public class Movie {
    private String id; // MongoDB ObjectId as String
    private int movieId; // Keep simple int ID
    private String title;
    private String description;
    private String theatre;

    // Constructor used by DAO
    public Movie(String id, int movieId, String title, String description, String theatre) {
        this.id = id;
        this.movieId = movieId;
        this.title = title;
        this.description = description;
        this.theatre = theatre;
    }
     // Constructor if creating before DB insert
     public Movie(int movieId, String title, String description, String theatre) {
         this(null, movieId, title, description, theatre);
     }

    // Getters
    public String getId() { return id; }
    public int getMovieId() { return movieId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTheatre() { return theatre; }
    public void setId(String id) { this.id = id; }

    @Override
    public String toString() {
        return title;
    }
}