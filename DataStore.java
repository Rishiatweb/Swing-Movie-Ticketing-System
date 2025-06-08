package movieticketbookingsystem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Simple In-Memory Data Store Simulation
public class DataStore {

    private static List<UserData> users = new ArrayList<>();
    private static List<Movie> movies = new ArrayList<>();
    private static List<Showtime> showtimes = new ArrayList<>();
    private static List<Booking> bookings = new ArrayList<>();

    // Static initializer block to populate sample data
    static {
        // Sample Users
        users.add(new UserData("admin", "password"));
        users.add(new UserData("user", "user"));

        // Sample Movies
        Movie matrix = new Movie("The Matrix", "A computer hacker learns...", "Cineplex A");
        Movie inception = new Movie("Inception", "A thief who enters dreams...", "Cineplex B");
        Movie interstellar = new Movie("Interstellar", "Explorers travel through space...", "IMAX Theatre");
        movies.add(matrix);
        movies.add(inception);
        movies.add(interstellar);

        // Sample Showtimes (Today and Tomorrow)
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalTime time10AM = LocalTime.of(10, 0);
        LocalTime time1PM = LocalTime.of(13, 0);
        LocalTime time4PM = LocalTime.of(16, 0);
        LocalTime time7PM = LocalTime.of(19, 0);

        // Matrix Showtimes
        showtimes.add(new Showtime(matrix.getMovieId(), LocalDateTime.of(today, time1PM), 64));
        showtimes.add(new Showtime(matrix.getMovieId(), LocalDateTime.of(today, time7PM), 64));
        showtimes.add(new Showtime(matrix.getMovieId(), LocalDateTime.of(tomorrow, time4PM), 64));

        // Inception Showtimes
        showtimes.add(new Showtime(inception.getMovieId(), LocalDateTime.of(today, time10AM), 64));
        showtimes.add(new Showtime(inception.getMovieId(), LocalDateTime.of(today, time4PM), 64));
        showtimes.add(new Showtime(inception.getMovieId(), LocalDateTime.of(tomorrow, time1PM), 64));
        showtimes.add(new Showtime(inception.getMovieId(), LocalDateTime.of(tomorrow, time7PM), 64));

        // Interstellar Showtimes
        showtimes.add(new Showtime(interstellar.getMovieId(), LocalDateTime.of(today, time7PM), 64));
        showtimes.add(new Showtime(interstellar.getMovieId(), LocalDateTime.of(tomorrow, time10AM), 64));
        showtimes.add(new Showtime(interstellar.getMovieId(), LocalDateTime.of(tomorrow, time7PM), 64));
    }

    // --- Accessor Methods ---

    public static Optional<UserData> findUser(String username) {
        return users.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst();
    }

    public static Movie getMovieById(int movieId) {
         return movies.stream()
                      .filter(m -> m.getMovieId() == movieId)
                      .findFirst()
                      .orElse(null); // Or throw exception
    }

     public static Showtime getShowtimeById(int showtimeId) {
         return showtimes.stream()
                         .filter(s -> s.getShowtimeId() == showtimeId)
                         .findFirst()
                         .orElse(null);
    }

    public static List<Showtime> getShowtimesByDate(LocalDate date) {
        return showtimes.stream()
                        .filter(s -> s.getShowDateTime().toLocalDate().equals(date))
                        .collect(Collectors.toList());
    }

     // Find showtimes for a specific movie on a specific date/time
    public static Optional<Showtime> findShowtime(int movieId, LocalDateTime dateTime) {
         return showtimes.stream()
                         .filter(s -> s.getMovieId() == movieId && s.getShowDateTime().equals(dateTime))
                         .findFirst();
    }


    public static void addBooking(Booking booking) {
        bookings.add(booking);
    }

    public static List<Booking> getBookingsByUserId(int userId) {
        return bookings.stream()
                       .filter(b -> b.getUserId() == userId)
                       .sorted((b1, b2) -> b2.getBookingTimestamp().compareTo(b1.getBookingTimestamp())) // Show newest first
                       .collect(Collectors.toList());
    }

     public static Booking getBookingById(int bookingId) {
         return bookings.stream()
                        .filter(b -> b.getBookingId() == bookingId)
                        .findFirst()
                        .orElse(null);
    }

    // Method to get all movies (for display)
    public static List<Movie> getAllMovies() {
        return new ArrayList<>(movies); // Return a copy
    }
}