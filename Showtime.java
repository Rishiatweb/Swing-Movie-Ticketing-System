package movieticketbookingsystem;

import java.time.LocalDateTime;

public class Showtime {
    private String id; // MongoDB ObjectId as String
    private int showtimeId; // Keep simple int ID
    private int movieId;
    private LocalDateTime showDateTime;
    private int totalSeats;
    // Seat status managed in DB 'seats' collection

    // Constructor used by DAO
    public Showtime(String id, int showtimeId, int movieId, LocalDateTime showDateTime, int totalSeats) {
        this.id = id;
        this.showtimeId = showtimeId;
        this.movieId = movieId;
        this.showDateTime = showDateTime;
        this.totalSeats = totalSeats;
    }
     // Constructor if creating before DB insert
    public Showtime(int showtimeId, int movieId, LocalDateTime showDateTime, int totalSeats) {
         this(null, showtimeId, movieId, showDateTime, totalSeats);
     }

    // Getters
    public String getId() { return id; }
    public int getShowtimeId() { return showtimeId; }
    public int getMovieId() { return movieId; }
    public LocalDateTime getShowDateTime() { return showDateTime; }
    public int getTotalSeats() { return totalSeats; }
    public void setId(String id) { this.id = id; }
}