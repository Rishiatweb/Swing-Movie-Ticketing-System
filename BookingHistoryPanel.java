package movieticketbookingsystem;

// Imports... (ensure db DAOs are imported)
import movieticketbookingsystem.db.BookingDAO;
import movieticketbookingsystem.db.MovieDAO;
import movieticketbookingsystem.db.ShowtimeDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class BookingHistoryPanel extends JPanel {

    // Fields (mainApp, DAOs, UI components)
    private MovieTicketBookingSystem mainApp;
    private JTable bookingTable;
    private DefaultTableModel tableModel;
    private JButton cancelButton;
    private JButton backButton;
    private int currentUserId = -1;

    private BookingDAO bookingDAO;
    private MovieDAO movieDAO;
    private ShowtimeDAO showtimeDAO;

    // Formatters/Constants remain the same
    private static final DateTimeFormatter TABLE_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy 'at' hh:mm a");
    private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    static { currencyFormatter.setMaximumFractionDigits(0); }


    public BookingHistoryPanel(MovieTicketBookingSystem mainApp) {
        this.mainApp = mainApp;
        // Instantiate DAOs
        this.bookingDAO = new BookingDAO();
        this.movieDAO = new MovieDAO();
        this.showtimeDAO = new ShowtimeDAO();
        setupUI();
    }

    private void setupUI() {
        // --- UI Layout Code (Identical to previous version) ---
         setLayout(new BorderLayout(10, 10)); setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
         JLabel titleLabel = new JLabel("My Bookings", SwingConstants.CENTER); titleLabel.setFont(new Font("Arial", Font.BOLD, 20)); add(titleLabel, BorderLayout.NORTH);
         String[] columnNames = {"Booking ID", "Movie", "Showtime", "Seats", "Snacks", "Total Cost", "Status"}; tableModel = new DefaultTableModel(columnNames, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } }; bookingTable = new JTable(tableModel); bookingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); bookingTable.setFillsViewportHeight(true);
         TableColumnModel cm = bookingTable.getColumnModel(); cm.getColumn(0).setMinWidth(0); cm.getColumn(0).setMaxWidth(0); cm.getColumn(0).setWidth(0); cm.getColumn(0).setPreferredWidth(0); // Hide ID column
         bookingTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) { updateCancelButtonState(); } });
         JScrollPane scrollPane = new JScrollPane(bookingTable); add(scrollPane, BorderLayout.CENTER);
         JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10)); cancelButton = new JButton("Cancel Selected Booking"); cancelButton.setFont(new Font("Arial", Font.BOLD, 14)); cancelButton.setEnabled(false); cancelButton.addActionListener(this::handleCancelBooking); backButton = new JButton("Back"); backButton.setFont(new Font("Arial", Font.PLAIN, 14)); backButton.addActionListener(e -> mainApp.showPanel(MovieTicketBookingSystem.MOVIE_SELECTION_PANEL)); buttonPanel.add(cancelButton); buttonPanel.add(backButton); add(buttonPanel, BorderLayout.SOUTH);
        // --- End UI Layout ---
    }

    // Use DAO to load bookings
    public void loadBookings(int userId) {
        this.currentUserId = userId;
        tableModel.setRowCount(0);
        cancelButton.setEnabled(false);
        if (userId == -1) return;

        // Use BookingDAO to get bookings
        List<Booking> userBookings = bookingDAO.getBookingsByUserId(userId);

        for (Booking booking : userBookings) {
            // Use other DAOs to get related info using stored integer IDs
            Optional<Showtime> showtimeOpt = showtimeDAO.getShowtimeByIntId(booking.getShowtimeId());
            Optional<Movie> movieOpt = showtimeOpt.flatMap(st -> movieDAO.getMovieByIntId(st.getMovieId()));

            String movieTitle = movieOpt.map(Movie::getTitle).orElse("N/A");
            String showtimeStr = showtimeOpt.map(st -> st.getShowDateTime().format(TABLE_DATE_TIME_FORMAT)).orElse("N/A");
            String seatsStr = String.join(", ", booking.getBookedSeats());
            String snacksStr = formatSnacks(booking.getSelectedSnacksWithQuantities());
            String costStr = currencyFormatter.format(booking.getTotalCost());
            String status = booking.getStatus();

            tableModel.addRow(new Object[]{
                    booking.getId(), // Store MongoDB ObjectId String in the hidden column now
                    movieTitle, showtimeStr, seatsStr, snacksStr, costStr, status
            });
        }
    }

    // formatSnacks and updateCancelButtonState remain the same
    private String formatSnacks(Map<String, Integer> snacks) { /* Same */ if (snacks == null || snacks.isEmpty()) return "None"; return snacks.entrySet().stream().map(entry -> entry.getKey() + " x" + entry.getValue()).collect(Collectors.joining(", ")); }
    private void updateCancelButtonState() { /* Same */ int selectedRow = bookingTable.getSelectedRow(); if (selectedRow >= 0) { String status = (String) tableModel.getValueAt(selectedRow, 6); cancelButton.setEnabled("confirmed".equalsIgnoreCase(status)); } else { cancelButton.setEnabled(false); } }

    // Use DAO to cancel booking
    private void handleCancelBooking(ActionEvent e) {
        int selectedRow = bookingTable.getSelectedRow();
        if (selectedRow < 0) return;

        // Get MongoDB ObjectId String from hidden column 0
        String bookingMongoId = (String) tableModel.getValueAt(selectedRow, 0);
        if (bookingMongoId == null) {
             JOptionPane.showMessageDialog(this, "Cannot identify selected booking.", "Error", JOptionPane.ERROR_MESSAGE);
             return;
        }

        // Fetch full booking details using ObjectId to get seats and showtimeId
        Optional<Booking> bookingOpt = bookingDAO.getBookingByObjectId(bookingMongoId);

        if (!bookingOpt.isPresent()) {
            JOptionPane.showMessageDialog(this, "Booking details not found.", "Error", JOptionPane.ERROR_MESSAGE);
            loadBookings(this.currentUserId); return;
        }
        Booking booking = bookingOpt.get();

        Optional<Showtime> showtimeOpt = showtimeDAO.getShowtimeByIntId(booking.getShowtimeId());

        if (!showtimeOpt.isPresent() || !"confirmed".equalsIgnoreCase(booking.getStatus())) {
            JOptionPane.showMessageDialog(this, "Cannot cancel this booking (invalid showtime or status).", "Cancellation Error", JOptionPane.WARNING_MESSAGE);
            loadBookings(this.currentUserId); return;
        }

        Showtime showtime = showtimeOpt.get();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime showDateTime = showtime.getShowDateTime();
        Duration timeUntilShow = Duration.between(now, showDateTime);
        long hoursUntilShow = timeUntilShow.toHours();

        // Cancellation Fee Logic (same as before)
        double cancellationFeePercentage = 0.10; if (now.isAfter(showDateTime)) { JOptionPane.showMessageDialog(this, "Cannot cancel a booking after the show has started.", "Cancellation Failed", JOptionPane.ERROR_MESSAGE); return; } else if (hoursUntilShow < 6) { cancellationFeePercentage = 0.50; } else if (hoursUntilShow < 24) { cancellationFeePercentage = 0.25; }
        int totalCost = booking.getTotalCost(); int cancellationFee = (int) (totalCost * cancellationFeePercentage); int refundAmount = totalCost - cancellationFee;

        // Confirmation Dialog (same logic)
        String message = String.format(Locale.US, "<html>Are you sure...</html>" /* Full message */, showDateTime.format(TABLE_DATE_TIME_FORMAT), hoursUntilShow < 0 ? 0 : hoursUntilShow, cancellationFeePercentage * 100, currencyFormatter.format(cancellationFee), currencyFormatter.format(refundAmount) );
        int choice = JOptionPane.showConfirmDialog(this, message, "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            // Perform cancellation using DAO Transaction
            boolean cancelled = bookingDAO.cancelBookingTransaction(
                                    bookingMongoId, // Pass MongoDB ID
                                    booking.getBookedSeats(),
                                    showtime.getShowtimeId()
                                );

            if (cancelled) {
                JOptionPane.showMessageDialog(this, "Booking Cancelled. Refund amount: " + currencyFormatter.format(refundAmount), "Cancellation Successful", JOptionPane.INFORMATION_MESSAGE);
            } else {
                 JOptionPane.showMessageDialog(this, "Cancellation failed. Booking might have already been cancelled or an error occurred.", "Cancellation Error", JOptionPane.ERROR_MESSAGE);
            }
             loadBookings(this.currentUserId); // Refresh the table regardless
        }
    }
}