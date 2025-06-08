package movieticketbookingsystem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Booking {
    private String id; // MongoDB ObjectId as String
    private int bookingId; // Keep simple int ID
    private int userId;
    private int showtimeId;
    private LocalDateTime bookingTimestamp;
    private List<String> bookedSeats;
    private Map<String, Integer> selectedSnacksWithQuantities;
    private int seatCost;
    private int snackCost;
    private int totalCost;
    private String status;

    // Constructor used by DAO
    public Booking(String id, int bookingId, int userId, int showtimeId, LocalDateTime bookingTimestamp,
                   List<String> bookedSeats, Map<String, Integer> snacks,
                   int seatCost, int snackCost, int totalCost, String status) {
        this.id = id;
        this.bookingId = bookingId;
        this.userId = userId;
        this.showtimeId = showtimeId;
        this.bookingTimestamp = bookingTimestamp;
        this.bookedSeats = bookedSeats;
        this.selectedSnacksWithQuantities = snacks;
        this.seatCost = seatCost;
        this.snackCost = snackCost;
        this.totalCost = totalCost;
        this.status = status;
    }

    // Getters... (keep all getters)
    public String getId() { return id; }
    public int getBookingId() { return bookingId; }
    public int getUserId() { return userId; }
    public int getShowtimeId() { return showtimeId; }
    public LocalDateTime getBookingTimestamp() { return bookingTimestamp; }
    public List<String> getBookedSeats() { return bookedSeats; }
    public Map<String, Integer> getSelectedSnacksWithQuantities() { return selectedSnacksWithQuantities; }
    public int getSeatCost() { return seatCost; }
    public int getSnackCost() { return snackCost; }
    public int getTotalCost() { return totalCost; }
    public String getStatus() { return status; }
    public void setId(String id) { this.id = id; }

    // Optional: Setter for status if needed outside DAO transactions (unlikely)
    // public void setStatus(String status) { this.status = status; }
}