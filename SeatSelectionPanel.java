package movieticketbookingsystem;

// Core Java Imports
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat; // For currency
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // For sorting seats
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

// Swing Imports
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

// DB and DAO Imports (assuming they are in the db package)
import movieticketbookingsystem.db.BookingDAO;
import movieticketbookingsystem.db.MovieDAO;
import movieticketbookingsystem.db.SeatDAO;
import movieticketbookingsystem.db.ShowtimeDAO; // Might need if re-init seats

public class SeatSelectionPanel extends JPanel {

    // --- Core Fields ---
    private MovieTicketBookingSystem mainApp;
    private Showtime currentShowtime; // Store the current showtime object
    private JPanel seatGridPanel;
    private List<JToggleButton> seatButtons;
    private Map<String, Integer> selectedSeatsWithPrices; // SeatNumber -> Price

    // --- DAOs ---
    private SeatDAO seatDAO;
    private MovieDAO movieDAO;
    private BookingDAO bookingDAO;

    // --- Constants ---
    private final Color AVAILABLE_COLOR = new Color(34, 139, 34);
    private final Color SELECTED_COLOR = new Color(255, 165, 0);
    private final Color OCCUPIED_COLOR = new Color(200, 0, 0);
    private final Color PLACEHOLDER_COLOR = new Color(240, 240, 240);
    public static final int ROWS = 8; // Made public static
    public static final int COLS = 8; // Made public static
    public static final int TOTAL_POSITIONS = ROWS * COLS;
    private static final int PRICE_ROW_AB = 300;
    private static final int PRICE_ROW_CD = 250;
    private static final int PRICE_ROW_EF = 200;
    private static final int PRICE_ROW_GH = 150;
    private static final Map<String, Integer> SNACK_PRICES = new HashMap<>();
    static {
        SNACK_PRICES.put("Popcorn", 120);
        SNACK_PRICES.put("Sandwich", 180);
        SNACK_PRICES.put("Nachos", 150);
        SNACK_PRICES.put("Soft Drink", 80);
    }
    private static final DateTimeFormatter SHOWTIME_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM dd 'at' hh:mm a");
    private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    static { currencyFormatter.setMaximumFractionDigits(0); }


    // --- UI Elements ---
    private Map<String, JSpinner> snackSpinners;
    private JLabel seatCostLabel;
    private JLabel snackCostLabel;
    private JLabel totalCostLabel;
    private JLabel titleLabel;

    // --- State ---
    private int currentSeatCost = 0;
    private int currentSnackCost = 0;
    private final ChangeListener snackChangeListener = this::handleSnackQuantityChange;


    // --- Constructor ---
    public SeatSelectionPanel(MovieTicketBookingSystem mainApp) {
        this.mainApp = mainApp;
        // Instantiate MongoDB DAOs
        this.seatDAO = new SeatDAO();
        this.movieDAO = new MovieDAO();
        this.bookingDAO = new BookingDAO();
        setupUI(); // Keep UI setup separate
    }

    // --- UI Setup Method ---
    private void setupUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        seatButtons = new ArrayList<>();
        selectedSeatsWithPrices = new HashMap<>();
        snackSpinners = new HashMap<>();

        // Title Label (North)
        titleLabel = new JLabel("Select Your Seats", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        // Main Content Panel (Center) - Holds Screen and Seat Grid
        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 10));
        JPanel screenPanel = createScreenPanel(); // Helper for Screen UI
        mainContentPanel.add(screenPanel, BorderLayout.NORTH);

        seatGridPanel = new JPanel(); // Grid layout set in setupPanel
        JPanel gridContainer = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Centers grid
        gridContainer.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0)); // Padding
        gridContainer.add(seatGridPanel);
        JScrollPane seatScrollPane = new JScrollPane(gridContainer); // Make grid scrollable
        mainContentPanel.add(seatScrollPane, BorderLayout.CENTER);
        add(mainContentPanel, BorderLayout.CENTER);

        // Snacks Panel (East)
        JPanel snacksPanel = createSnacksPanel(); // Helper for Snacks UI
        add(snacksPanel, BorderLayout.EAST);

        // Bottom Panel (South) - Holds Costs, Legend, Buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));

        // Costs Panel (Inside Bottom, North)
        JPanel costsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        seatCostLabel = new JLabel("Seat Cost: ₹0");
        snackCostLabel = new JLabel("Snack Cost: ₹0");
        totalCostLabel = new JLabel("Total Cost: ₹0");
        seatCostLabel.setFont(new Font("Arial", Font.BOLD, 12));
        snackCostLabel.setFont(new Font("Arial", Font.BOLD, 12));
        totalCostLabel.setFont(new Font("Arial", Font.BOLD, 14));
        costsPanel.add(seatCostLabel);
        costsPanel.add(snackCostLabel);
        costsPanel.add(totalCostLabel);
        bottomPanel.add(costsPanel, BorderLayout.NORTH);

        // Action Panel (Inside Bottom, South) - Holds Legend and Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5)); // Legend
        legendPanel.add(createLegendLabel("Available", AVAILABLE_COLOR));
        legendPanel.add(createLegendLabel("Selected", SELECTED_COLOR));
        legendPanel.add(createLegendLabel("Occupied", OCCUPIED_COLOR));

        JButton backButton = new JButton("Back to Movies"); // Buttons
        backButton.setFont(new Font("Arial", Font.PLAIN, 14));
        backButton.addActionListener(e -> mainApp.showPanel(MovieTicketBookingSystem.MOVIE_SELECTION_PANEL));

        JButton confirmButton = new JButton("Proceed to Payment");
        confirmButton.setFont(new Font("Arial", Font.BOLD, 14));
        confirmButton.setBackground(new Color(0, 150, 0));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.addActionListener(this::confirmSelection);

        actionPanel.add(backButton);
        actionPanel.add(legendPanel); // Put legend between buttons
        actionPanel.add(confirmButton);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // --- UI Helper Methods ---
    private JPanel createScreenPanel() {
        JPanel screenPanel = new JPanel();
        screenPanel.setBackground(Color.BLACK);
        Border loweredetched = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border raisedetched = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        Border compound = BorderFactory.createCompoundBorder(raisedetched, loweredetched);
        screenPanel.setBorder(BorderFactory.createCompoundBorder(compound,
                BorderFactory.createEmptyBorder(5, 5, 5, 5))); // Padding inside
        JLabel screenLabel = new JLabel("S C R E E N");
        screenLabel.setFont(new Font("Arial", Font.BOLD, 14));
        screenLabel.setForeground(Color.WHITE);
        screenPanel.add(screenLabel);
        return screenPanel;
    }

    private JPanel createSnacksPanel() {
        JPanel snacksOuterPanel = new JPanel(new BorderLayout());
        snacksOuterPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10)); // Padding

        JPanel snacksPanel = new JPanel();
        snacksPanel.setLayout(new GridBagLayout()); // GridBag for alignment
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE; // Stack vertically
        gbc.anchor = GridBagConstraints.WEST; gbc.insets = new Insets(2, 5, 2, 5);

        snacksPanel.setBorder(BorderFactory.createTitledBorder(
                                BorderFactory.createEtchedBorder(), "Add Snacks",
                                TitledBorder.CENTER, TitledBorder.TOP,
                                new Font("Arial", Font.BOLD, 12), Color.BLUE));

        snackSpinners.clear();
        for (Map.Entry<String, Integer> entry : SNACK_PRICES.entrySet()) {
            String snackName = entry.getKey(); int price = entry.getValue();
            JLabel snackLabel = new JLabel(snackName + " (₹" + price + "):");
            snackLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.8;
            snacksPanel.add(snackLabel, gbc);

            SpinnerModel spinnerModel = new SpinnerNumberModel(0, 0, 10, 1); // Min 0, Max 10
            JSpinner spinner = new JSpinner(spinnerModel);
            spinner.setPreferredSize(new Dimension(50, 20)); // Compact size
            spinner.setFont(new Font("Arial", Font.PLAIN, 12));
            spinner.addChangeListener(snackChangeListener); // Add listener
            snackSpinners.put(snackName, spinner);

            gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.2;
            snacksPanel.add(spinner, gbc);
        }
        // Filler to push items up
        gbc.gridx = 0; gbc.gridwidth = 2; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.VERTICAL;
        snacksPanel.add(Box.createVerticalGlue(), gbc);

        snacksOuterPanel.add(snacksPanel, BorderLayout.NORTH);
        return snacksOuterPanel;
    }

    private JLabel createLegendLabel(String text, Color color) {
        JLabel label = new JLabel("■ " + text);
        label.setForeground(color);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        return label;
    }


    // --- Setup Panel with Data ---
    public void setupPanel(Showtime showtime) {
        this.currentShowtime = showtime;
        // Reset state
        selectedSeatsWithPrices.clear();
        seatButtons.clear();
        seatGridPanel.removeAll(); // Clear previous grid components
        currentSeatCost = 0;
        currentSnackCost = 0;
        resetSnackSpinners();

        if (showtime == null) {
             titleLabel.setText("Select Seats");
             seatGridPanel.add(new JLabel("Error: No showtime selected."));
             updateCostLabels();
             seatGridPanel.revalidate(); seatGridPanel.repaint(); // Refresh empty grid
             return;
        }

        // Fetch Movie Title using MovieDAO
        Optional<Movie> movieOpt = movieDAO.getMovieByIntId(showtime.getMovieId());
        String movieTitle = movieOpt.map(Movie::getTitle).orElse("Unknown Movie");
        titleLabel.setText("Select Seats for: " + movieTitle + " (" + showtime.getShowDateTime().format(SHOWTIME_FORMAT) + ")");

        // Fetch Seat Status from DB using SeatDAO
        Map<String, String> seatStatusMap = seatDAO.getSeatStatusForShowtime(showtime.getShowtimeId());

        // Attempt seat initialization if map is empty and should have seats
        if (seatStatusMap.isEmpty() && showtime.getTotalSeats() > 0) {
             System.out.println("Seat map empty for showtime " + showtime.getShowtimeId() + ", attempting initialization...");
             boolean initOk = seatDAO.initializeSeatsForShowtime(showtime.getShowtimeId(), showtime.getTotalSeats());
             if (initOk) {
                 seatStatusMap = seatDAO.getSeatStatusForShowtime(showtime.getShowtimeId()); // Re-fetch
             } else {
                 seatGridPanel.add(new JLabel("Error: Failed to initialize/fetch seats."));
                 updateCostLabels();
                 seatGridPanel.revalidate(); seatGridPanel.repaint();
                 return; // Stop if seats couldn't be initialized
             }
        }

        // Build Seat Grid UI
        seatGridPanel.setLayout(new GridLayout(ROWS, COLS, 8, 8)); // Set layout
        int seatsInShow = showtime.getTotalSeats();

        for (int i = 0; i < TOTAL_POSITIONS; i++) {
            String seatNumber = generateSeatNumber(i, COLS);
            if (i < seatsInShow) { // Only process positions within the showtime's total seats
                int price = getSeatPrice(seatNumber);
                JToggleButton seatButton = new JToggleButton(seatNumber);
                // Basic styling
                seatButton.setFont(new Font("Arial", Font.BOLD, 10));
                seatButton.setMargin(new Insets(4, 4, 4, 4));
                seatButton.setFocusPainted(false);
                seatButton.setPreferredSize(new Dimension(55, 35)); // Uniform size

                // Determine status from fetched map
                String currentStatus = seatStatusMap.getOrDefault(seatNumber, "occupied"); // Default to occupied if unexpectedly missing
                String tooltipText;

                switch (currentStatus.toLowerCase()) {
                    case "available":
                        seatButton.setBackground(AVAILABLE_COLOR); seatButton.setForeground(Color.WHITE); seatButton.setEnabled(true);
                        seatButton.addActionListener(this::handleSeatClick); // Add listener ONLY if available
                        tooltipText = "Available - ₹" + price; break;
                    case "booked": case "occupied": // Treat both as unavailable for selection
                        seatButton.setBackground(OCCUPIED_COLOR); seatButton.setForeground(Color.WHITE); seatButton.setEnabled(false);
                        tooltipText = "Occupied"; break;
                    default: // Error state
                        seatButton.setBackground(Color.GRAY); seatButton.setEnabled(false); tooltipText = "Error"; break;
                }
                seatButton.setToolTipText(tooltipText);
                seatButtons.add(seatButton); // Keep track if needed
                seatGridPanel.add(seatButton);
            } else {
                // Add placeholder for empty grid spots
                JPanel placeholder = new JPanel();
                placeholder.setPreferredSize(new Dimension(55, 35));
                placeholder.setBackground(PLACEHOLDER_COLOR);
                seatGridPanel.add(placeholder);
            }
        }
        updateCostLabels(); // Update cost display
        seatGridPanel.revalidate(); seatGridPanel.repaint(); // Refresh grid UI
    }


    // --- Event Handlers & Calculations ---

    private void resetSnackSpinners() {
        for (JSpinner spinner : snackSpinners.values()) {
            spinner.removeChangeListener(snackChangeListener); // Avoid triggering listener
            spinner.setValue(0); // Reset value
            spinner.addChangeListener(snackChangeListener); // Re-add listener
        }
        calculateSnackCost(); // Recalculate cost (should be 0)
        updateCostLabels();   // Update display
    }

    private void handleSeatClick(ActionEvent e) { // Changed parameter to ActionEvent
        // Get the button that triggered the event
        Object source = e.getSource();
        if (!(source instanceof JToggleButton)) {
            return; // Should not happen, but good practice to check
        }
        JToggleButton seatButton = (JToggleButton) source; // Cast the source to JToggleButton

        // --- The rest of the logic remains the same ---
        String seatNumber = seatButton.getText();
        int price = getSeatPrice(seatNumber);

        if (seatButton.isSelected()) {
            seatButton.setBackground(SELECTED_COLOR);
            seatButton.setForeground(Color.BLACK);
            selectedSeatsWithPrices.put(seatNumber, price); // Track selected seat and its price
            seatButton.setToolTipText("Selected - ₹" + price);
        } else {
            seatButton.setBackground(AVAILABLE_COLOR);
            seatButton.setForeground(Color.WHITE);
            selectedSeatsWithPrices.remove(seatNumber); // Remove from selection
            seatButton.setToolTipText("Available - ₹" + price);
        }
        calculateSeatCost(); // Recalculate
        updateCostLabels(); // Update display
    }

    private void handleSnackQuantityChange(ChangeEvent e) {
        calculateSnackCost(); // Recalculate
        updateCostLabels(); // Update display
    }

    private int getSeatPrice(String seatNumber) {
        if (seatNumber == null || seatNumber.isEmpty()) return 0;
        char row = seatNumber.charAt(0);
        switch (row) {
            case 'A': case 'B': return PRICE_ROW_AB;
            case 'C': case 'D': return PRICE_ROW_CD;
            case 'E': case 'F': return PRICE_ROW_EF;
            case 'G': case 'H': return PRICE_ROW_GH;
            default: return 100; // Fallback
        }
    }

    private void calculateSeatCost() {
        currentSeatCost = 0;
        for (int price : selectedSeatsWithPrices.values()) {
            currentSeatCost += price;
        }
    }

    private void calculateSnackCost() {
        currentSnackCost = 0;
        for (Map.Entry<String, JSpinner> entry : snackSpinners.entrySet()) {
            String snackName = entry.getKey();
            int quantity = (Integer) entry.getValue().getValue();
            if (quantity > 0) {
                currentSnackCost += SNACK_PRICES.getOrDefault(snackName, 0) * quantity;
            }
        }
    }

    private Map<String, Integer> getSelectedSnacksWithQuantities() {
        Map<String, Integer> selected = new HashMap<>();
        for (Map.Entry<String, JSpinner> entry : snackSpinners.entrySet()) {
            String snackName = entry.getKey();
            int quantity = (Integer) entry.getValue().getValue();
            if (quantity > 0) {
                selected.put(snackName, quantity);
            }
        }
        return selected;
    }

    private void updateCostLabels() {
        seatCostLabel.setText("Seat Cost: " + currencyFormatter.format(currentSeatCost));
        snackCostLabel.setText("Snack Cost: " + currencyFormatter.format(currentSnackCost));
        totalCostLabel.setText("Total Cost: " + currencyFormatter.format(currentSeatCost + currentSnackCost));
    }

    // Static helper method for consistent seat number generation
    public static String generateSeatNumber(int index, int cols) {
        if (cols <= 0) return "Err";
        char rowChar = (char) ('A' + (index / cols));
        int seatNumInRow = (index % cols) + 1;
        return "" + rowChar + seatNumInRow;
    }


    // --- Confirmation and Booking ---
    private void confirmSelection(ActionEvent e) {
        // 1. Validate selection
        if (selectedSeatsWithPrices.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select at least one seat.", "No Seats Selected", JOptionPane.WARNING_MESSAGE); return;
        }
        if (currentShowtime == null) {
            JOptionPane.showMessageDialog(this, "Error: Showtime information missing.", "Internal Error", JOptionPane.ERROR_MESSAGE); return;
        }
        int loggedInUserId = mainApp.getLoggedInUserId();
        if (loggedInUserId == -1) {
            JOptionPane.showMessageDialog(this, "Error: User not logged in.", "Internal Error", JOptionPane.ERROR_MESSAGE); return;
        }

        // 2. Prepare data for confirmation dialog and booking
        List<String> selectedSeatNumbers = new ArrayList<>(selectedSeatsWithPrices.keySet());
        Collections.sort(selectedSeatNumbers, Comparator.comparing((String s) -> s.charAt(0)).thenComparingInt(s -> Integer.parseInt(s.substring(1)))); // Sort A1, A2.. B1..
        Map<String, Integer> selectedSnacks = getSelectedSnacksWithQuantities();
        int finalSeatCost = currentSeatCost;
        int finalSnackCost = currentSnackCost;
        int grandTotal = finalSeatCost + finalSnackCost;

        // Fetch movie title for dialog
        Optional<Movie> movieOpt = movieDAO.getMovieByIntId(currentShowtime.getMovieId());
        String movieTitle = movieOpt.map(Movie::getTitle).orElse("Unknown Movie");

        // 3. Build Confirmation Dialog Message (HTML formatted)
        StringBuilder confirmationMessage = new StringBuilder("<html><b>Confirm Your Booking:</b><br><br>");
        // Add details (Movie, Time, Seats, Seat Cost, Snacks, Snack Cost, Total) - same as previous version
        confirmationMessage.append("<b>Movie:</b> ").append(movieTitle).append("<br>");
        confirmationMessage.append("<b>Time:</b> ").append(currentShowtime.getShowDateTime().format(SHOWTIME_FORMAT)).append("<br>");
        confirmationMessage.append("<b>Seats (").append(selectedSeatNumbers.size()).append("):</b> ").append(String.join(", ", selectedSeatNumbers)).append("<br>");
        confirmationMessage.append("<b>Seat Cost:</b> ").append(currencyFormatter.format(finalSeatCost)).append("<br><br>");
        if (!selectedSnacks.isEmpty()) {
            confirmationMessage.append("<b>Snacks:</b><br>");
            for (Map.Entry<String, Integer> entry : selectedSnacks.entrySet()) { confirmationMessage.append("  - ").append(entry.getKey()).append(" x ").append(entry.getValue()).append("<br>"); }
            confirmationMessage.append("<b>Snack Cost:</b> ").append(currencyFormatter.format(finalSnackCost)).append("<br><br>");
        }
        confirmationMessage.append("<b><font color='green'>Grand Total: ").append(currencyFormatter.format(grandTotal)).append("</font></b><br><br>");
        confirmationMessage.append("Proceed with payment?</html>");


        // 4. Show Confirmation Dialog
        int choice = JOptionPane.showConfirmDialog(this, confirmationMessage.toString(), "Confirm Booking & Payment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        // 5. Process Confirmation -> Call BookingDAO Transaction
        if (choice == JOptionPane.YES_OPTION) {
            // Use the BookingDAO to perform the transactional booking
            String bookingMongoId = bookingDAO.addBookingTransaction(
                    loggedInUserId,
                    currentShowtime.getShowtimeId(),
                    selectedSeatNumbers,
                    selectedSnacks,
                    SNACK_PRICES, // Pass snack price map for storage in booking_snacks
                    finalSeatCost,
                    finalSnackCost,
                    grandTotal
            );

            // 6. Handle Transaction Result
            if (bookingMongoId != null) {
                 // Success! Store temporary details for the confirmation panel
                 mainApp.setBookingDetails(selectedSeatNumbers, selectedSnacks, finalSeatCost, finalSnackCost, grandTotal);
                 // Navigate to confirmation screen
                 mainApp.showPanel(MovieTicketBookingSystem.CONFIRMATION_PANEL);
            } else {
                 // Booking failed (DAO should have logged details, maybe handled rollback)
                 JOptionPane.showMessageDialog(this,
                     "Booking failed. Seat availability may have changed. Please review the seat map and try again.",
                     "Booking Error", JOptionPane.ERROR_MESSAGE);
                 // Refresh seat status from DB by reloading the panel
                 setupPanel(currentShowtime);
            }
        }
    }
} // End of SeatSelectionPanel class