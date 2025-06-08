package movieticketbookingsystem;

import movieticketbookingsystem.db.DatabaseConnector; // Keep this import if you used it before for testing
import movieticketbookingsystem.db.MongoConnector; // Import Mongo Connector
import movieticketbookingsystem.db.MovieDAO; // May need this if fetching movie directly here

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional; // Import Optional

public class MovieTicketBookingSystem extends JFrame {

    // Panel identifiers and fields (same as before)
    public static final String LOGIN_PANEL = "LoginPanel"; public static final String MOVIE_SELECTION_PANEL = "MovieSelectionPanel"; public static final String SEAT_SELECTION_PANEL = "SeatSelectionPanel"; public static final String CONFIRMATION_PANEL = "ConfirmationPanel"; public static final String BOOKING_HISTORY_PANEL = "BookingHistoryPanel";
    private CardLayout cardLayout; private JPanel mainPanel;
    private LoginPanel loginPanel; private MovieSelectionPanel movieSelectionPanel; private SeatSelectionPanel seatSelectionPanel; private ConfirmationPanel confirmationPanel; private BookingHistoryPanel bookingHistoryPanel;
    private Movie selectedMovie; // Still useful for confirmation panel title
    private Showtime selectedShowtime;
    private BookingDetails bookingDetails;
    private int loggedInUserId = -1;
    private JMenuBar menuBar; private JMenu userMenu; private JMenuItem myBookingsItem; private JMenuItem logoutItem;

    // BookingDetails inner class (same)
    public static class BookingDetails { List<String> bookedSeats; Map<String, Integer> selectedSnacksWithQuantities; int seatCost; int snackCost; int totalCost; BookingDetails(List<String> bookedSeats, Map<String, Integer> selectedSnacksWithQuantities, int seatCost, int snackCost, int totalCost) { this.bookedSeats = bookedSeats; this.selectedSnacksWithQuantities = selectedSnacksWithQuantities; this.seatCost = seatCost; this.snackCost = snackCost; this.totalCost = totalCost; } }


    public MovieTicketBookingSystem() {
        // --- Constructor UI Setup (same) ---
        setTitle("Movie Ticket Booking System"); setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Change close operation for shutdown hook
         setMinimumSize(new Dimension(800, 650)); setLocationRelativeTo(null);
         cardLayout = new CardLayout(); mainPanel = new JPanel(cardLayout);
         loginPanel = new LoginPanel(this); movieSelectionPanel = new MovieSelectionPanel(this); seatSelectionPanel = new SeatSelectionPanel(this); confirmationPanel = new ConfirmationPanel(this); bookingHistoryPanel = new BookingHistoryPanel(this);
         mainPanel.add(loginPanel, LOGIN_PANEL); mainPanel.add(movieSelectionPanel, MOVIE_SELECTION_PANEL); mainPanel.add(seatSelectionPanel, SEAT_SELECTION_PANEL); mainPanel.add(confirmationPanel, CONFIRMATION_PANEL); mainPanel.add(bookingHistoryPanel, BOOKING_HISTORY_PANEL);
         add(mainPanel); createMenuBar(); setJMenuBar(menuBar); menuBar.setVisible(false);
        // --- End UI Setup ---

        // Add Shutdown Hook to close MongoDB connection
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Application shutting down...");
                MongoConnector.close(); // Close MongoDB client
                dispose(); // Dispose the JFrame
                System.exit(0); // Ensure application exits
            }
        });

        showPanel(LOGIN_PANEL);
        SwingUtilities.invokeLater(() -> { setVisible(true); });
    }

    // createMenuBar, handleLogout, showPanel, clearBookingState (same logic)
    private void createMenuBar() { /* Same */ menuBar = new JMenuBar(); userMenu = new JMenu("User"); myBookingsItem = new JMenuItem("My Bookings"); logoutItem = new JMenuItem("Logout"); myBookingsItem.addActionListener(e -> showPanel(BOOKING_HISTORY_PANEL)); logoutItem.addActionListener(e -> handleLogout()); userMenu.add(myBookingsItem); userMenu.addSeparator(); userMenu.add(logoutItem); menuBar.add(userMenu); }
    private void handleLogout() { /* Same */ setLoggedInUser(-1, null); showPanel(LOGIN_PANEL); }
    public void showPanel(String panelName) { /* Same logic as before, calls setupPanel/loadBookings appropriately */ if (SEAT_SELECTION_PANEL.equals(panelName) && selectedShowtime != null) { seatSelectionPanel.setupPanel(selectedShowtime); } else if (CONFIRMATION_PANEL.equals(panelName) && selectedMovie != null && bookingDetails != null) { confirmationPanel.showConfirmation(selectedMovie, bookingDetails); } else if (BOOKING_HISTORY_PANEL.equals(panelName) && loggedInUserId != -1) { bookingHistoryPanel.loadBookings(loggedInUserId); } else if (LOGIN_PANEL.equals(panelName)) { loginPanel.resetFields(); clearBookingState(); } else if (MOVIE_SELECTION_PANEL.equals(panelName)) { clearBookingState(); if(movieSelectionPanel!=null) movieSelectionPanel.resetSelections(); } menuBar.setVisible(loggedInUserId != -1 && !LOGIN_PANEL.equals(panelName)); cardLayout.show(mainPanel, panelName); mainPanel.revalidate(); mainPanel.repaint(); }
    private void clearBookingState() { /* Same */ bookingDetails = null; selectedMovie = null; selectedShowtime = null; }

    // setLoggedInUser, getLoggedInUserId (same)
    public void setLoggedInUser(int userId, String username) { /* Same */ this.loggedInUserId = userId; if (userId != -1 && username != null) { userMenu.setText("User: " + username); menuBar.setVisible(true); } else { userMenu.setText("User"); menuBar.setVisible(false); } }
    public int getLoggedInUserId() { return loggedInUserId; }

    // setSelectedShowtime needs MovieDAO now potentially
    public void setSelectedShowtime(Showtime showtime) {
        this.selectedShowtime = showtime;
        if (showtime != null) {
            // Fetch movie details when showtime is selected
            MovieDAO movieDAO = new MovieDAO(); // Instantiate DAO
            Optional<Movie> movieOpt = movieDAO.getMovieByIntId(showtime.getMovieId());
            this.selectedMovie = movieOpt.orElse(null); // Store movie object if found
        } else {
            this.selectedMovie = null;
        }
    }
    public Showtime getSelectedShowtime() { return selectedShowtime; }
    public Movie getSelectedMovie() { return selectedMovie; } // Keep getter

    // setBookingDetails (same - just stores temp data)
    public void setBookingDetails(List<String> seats, Map<String, Integer> snacks, int seatCost, int snackCost, int totalCost) {
        this.bookingDetails = new BookingDetails(seats, snacks, seatCost, snackCost, totalCost);
    }
    public BookingDetails getBookingDetails() { return bookingDetails; }

    // main method (same)
    public static void main(String[] args) { try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) { System.err.println("Couldn't set L&F."); } SwingUtilities.invokeLater(MovieTicketBookingSystem::new); }
}