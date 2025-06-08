package movieticketbookingsystem;

import movieticketbookingsystem.db.MovieDAO; // Import DAOs
import movieticketbookingsystem.db.ShowtimeDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator; // Import Comparator
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class MovieSelectionPanel extends JPanel {

    private JComboBox<LocalDate> dateComboBox;
    private JComboBox<LocalTime> timeComboBox;
    private JComboBox<Movie> movieComboBox;
    private JTextArea movieDetailsArea;
    private JButton selectSeatsButton;
    private MovieTicketBookingSystem mainApp;

    // DAOs needed
    private MovieDAO movieDAO;
    private ShowtimeDAO showtimeDAO;

    // Formatters/Constants remain the same
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final LocalTime[] TIME_SLOTS = {
            LocalTime.of(10, 0), LocalTime.of(13, 0), LocalTime.of(16, 0), LocalTime.of(19, 0)
    };

    public MovieSelectionPanel(MovieTicketBookingSystem mainApp) {
        this.mainApp = mainApp;
        this.movieDAO = new MovieDAO(); // Instantiate DAOs
        this.showtimeDAO = new ShowtimeDAO();
        setupUI();
        resetSelections(); // Initial population
    }

    private void setupUI() {
         // --- UI Layout Code (same as before, using GridBagLayout) ---
        setLayout(new BorderLayout(10, 10)); setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel topPanel = new JPanel(new GridBagLayout()); GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(2, 5, 2, 5); gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0; gbc.gridy = 0; topPanel.add(new JLabel("Date:"), gbc); dateComboBox = new JComboBox<>(); LocalDate today = LocalDate.now(); for (int i = 0; i < 7; i++) { dateComboBox.addItem(today.plusDays(i)); } dateComboBox.setRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) { super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); if (value instanceof LocalDate) { setText(((LocalDate) value).format(DATE_FORMAT)); } return this; } }); gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=0.3; topPanel.add(dateComboBox, gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx=0; topPanel.add(new JLabel("Time:"), gbc); timeComboBox = new JComboBox<>(TIME_SLOTS); timeComboBox.setRenderer(new DefaultListCellRenderer() { @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) { super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); if (value instanceof LocalTime) { setText(((LocalTime) value).format(TIME_FORMAT)); } return this; } }); gbc.gridx = 3; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=0.3; topPanel.add(timeComboBox, gbc);
        gbc.gridx = 4; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.weightx=0; topPanel.add(new JLabel("Movie:"), gbc); movieComboBox = new JComboBox<>(); gbc.gridx = 5; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx=0.4; topPanel.add(movieComboBox, gbc);
        ActionListener filterListener = e -> filterAndDisplayMovies(); dateComboBox.addActionListener(filterListener); timeComboBox.addActionListener(filterListener); movieComboBox.addActionListener(e -> displaySelectedMovieDetails());
        movieDetailsArea = new JTextArea(8, 40); movieDetailsArea.setEditable(false); movieDetailsArea.setLineWrap(true); movieDetailsArea.setWrapStyleWord(true); movieDetailsArea.setFont(new Font("Arial", Font.PLAIN, 14)); JScrollPane scrollPane = new JScrollPane(movieDetailsArea);
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); selectSeatsButton = new JButton("Select Seats"); selectSeatsButton.setFont(new Font("Arial", Font.BOLD, 14)); selectSeatsButton.addActionListener(this::handleSelectSeats); bottomPanel.add(selectSeatsButton);
        add(topPanel, BorderLayout.NORTH); add(scrollPane, BorderLayout.CENTER); add(bottomPanel, BorderLayout.SOUTH);
         // --- End UI Layout ---
    }

    // Use DAOs to filter movies
    private void filterAndDisplayMovies() {
    LocalDate selectedDate = (LocalDate) dateComboBox.getSelectedItem();
    LocalTime selectedTime = (LocalTime) timeComboBox.getSelectedItem();
    if (selectedDate == null || selectedTime == null) return;

    System.out.println("Filtering for Date: " + selectedDate + ", Time: " + selectedTime); // DEBUG

    // Get showtimes for the date using DAO
    List<Showtime> showtimesOnDate = showtimeDAO.getShowtimesByDate(selectedDate);
    System.out.println("Showtimes found for date " + selectedDate + ": " + showtimesOnDate.size()); // DEBUG

    // Filter further by time and get distinct movie IDs
    List<Integer> movieIds = showtimesOnDate.stream()
            .filter(st -> {
                // DEBUG: Check individual showtime match
                boolean timeMatch = st.getShowDateTime().toLocalTime().equals(selectedTime);
                // System.out.println(" Checking Showtime ID " + st.getShowtimeId() + " ("+st.getShowDateTime()+"): Time match? " + timeMatch);
                return timeMatch;
            })
            .map(Showtime::getMovieId)
            .distinct()
            .collect(Collectors.toList());
    System.out.println("Movie IDs found for specific time: " + movieIds); // DEBUG

    // Get movie details for those IDs using DAO
    List<Movie> availableMovies = movieIds.stream()
            .map(id -> movieDAO.getMovieByIntId(id))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted(Comparator.comparing(Movie::getTitle))
            .collect(Collectors.toList());
            System.out.println("Available Movie objects found: " + availableMovies.size());

        // Update UI (same logic as before)
        ActionListener movieListener = movieComboBox.getActionListeners().length > 0 ? movieComboBox.getActionListeners()[0] : null;
        if (movieListener != null) movieComboBox.removeActionListener(movieListener);
        movieComboBox.removeAllItems();
        if (availableMovies.isEmpty()) {
            movieComboBox.setEnabled(false);
            selectSeatsButton.setEnabled(false);
            movieDetailsArea.setText("No movies available for the selected date and time.");
        } else {
            availableMovies.forEach(movieComboBox::addItem);
            movieComboBox.setEnabled(true);
            selectSeatsButton.setEnabled(true);
            if (movieComboBox.getItemCount() > 0) movieComboBox.setSelectedIndex(0);
        }
        if (movieListener != null) movieComboBox.addActionListener(movieListener);
        displaySelectedMovieDetails();
    }

    // Display details (no DB interaction here)
    private void displaySelectedMovieDetails() {
         Movie selectedMovie = (Movie) movieComboBox.getSelectedItem();
         if (selectedMovie != null) {
             movieDetailsArea.setText(
                     "Title: " + selectedMovie.getTitle() + "\n\n" +
                     "Description: " + selectedMovie.getDescription() + "\n\n" +
                     "Theatre: " + selectedMovie.getTheatre()
             );
             selectSeatsButton.setEnabled(true);
         } else {
             if (movieComboBox.getItemCount() == 0) {
                  movieDetailsArea.setText("No movies available for the selected date and time.");
             } else {
                  movieDetailsArea.setText("Please select a movie.");
             }
             selectSeatsButton.setEnabled(false);
         }
         movieDetailsArea.setCaretPosition(0);
    }

    // Use DAO to find the specific showtime object
    private void handleSelectSeats(ActionEvent e) {
        Movie selectedMovie = (Movie) movieComboBox.getSelectedItem();
        LocalDate selectedDate = (LocalDate) dateComboBox.getSelectedItem();
        LocalTime selectedTime = (LocalTime) timeComboBox.getSelectedItem();
        if (selectedMovie == null || selectedDate == null || selectedTime == null) {
            JOptionPane.showMessageDialog(this, "Please ensure date, time, and movie are selected.", "Selection Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDateTime selectedDateTime = LocalDateTime.of(selectedDate, selectedTime);

        // Find the Showtime using DAO
        Optional<Showtime> showtimeOpt = showtimeDAO.findShowtime(selectedMovie.getMovieId(), selectedDateTime);

        if (showtimeOpt.isPresent()) {
            mainApp.setSelectedShowtime(showtimeOpt.get()); // Pass the actual Showtime object
            mainApp.showPanel(MovieTicketBookingSystem.SEAT_SELECTION_PANEL);
        } else {
            JOptionPane.showMessageDialog(this, "Error: Could not find the selected showtime in the database.", "Internal Error", JOptionPane.ERROR_MESSAGE);
            filterAndDisplayMovies(); // Refresh list
        }
    }

    public void resetSelections() {
         if (dateComboBox.getItemCount() > 0) dateComboBox.setSelectedIndex(0);
         if (timeComboBox.getItemCount() > 0) timeComboBox.setSelectedIndex(0);
         filterAndDisplayMovies();
    }
}