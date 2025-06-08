package movieticketbookingsystem;

import javax.swing.*;
// Imports needed for file handling
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent; // Import ActionEvent
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter; // Import DateTimeFormatter
import java.util.Locale;
import java.util.Map;
import java.util.List; // Import List (though not directly used in signature anymore)

public class ConfirmationPanel extends JPanel {

    private MovieTicketBookingSystem mainApp;
    private JTextArea confirmationDetailsArea;
    private JButton newBookingButton;
    private JButton downloadBillButton; // Download Button field
    private JButton exitButton;

    public ConfirmationPanel(MovieTicketBookingSystem mainApp) {
        this.mainApp = mainApp;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20)); // Padding

        JLabel titleLabel = new JLabel("Booking Confirmed!", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(new Color(0, 128, 0)); // Green color
        add(titleLabel, BorderLayout.NORTH);

        // Text Area for details
        confirmationDetailsArea = new JTextArea(15, 50); // Adjusted size
        confirmationDetailsArea.setEditable(false);
        confirmationDetailsArea.setFont(new Font("Monospaced", Font.PLAIN, 14)); // Monospaced for alignment
        confirmationDetailsArea.setLineWrap(true);
        confirmationDetailsArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(confirmationDetailsArea);
        add(scrollPane, BorderLayout.CENTER);

        // Button Panel setup
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        // New Booking Button
        newBookingButton = new JButton("Make Another Booking");
        newBookingButton.setFont(new Font("Arial", Font.PLAIN, 14));
        newBookingButton.addActionListener(e -> {
             mainApp.showPanel(MovieTicketBookingSystem.MOVIE_SELECTION_PANEL); // Go back to movie selection
        });

        // Download Bill Button
        downloadBillButton = new JButton("Download Bill (.txt)");
        downloadBillButton.setFont(new Font("Arial", Font.PLAIN, 14));
        downloadBillButton.addActionListener(this::handleDownloadBill); // Add action listener

        // Exit Button
        exitButton = new JButton("Exit Application");
        exitButton.setFont(new Font("Arial", Font.PLAIN, 14));
        exitButton.addActionListener(e -> System.exit(0)); // Close the application

        // Add buttons to panel
        buttonPanel.add(newBookingButton);
        buttonPanel.add(downloadBillButton); // Add the download button
        buttonPanel.add(exitButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // Method to display the confirmation details
    // Accepts Movie for title/theatre and BookingDetails for cost breakdown
    public void showConfirmation(Movie movie, MovieTicketBookingSystem.BookingDetails details) {
        if (movie == null || details == null || details.bookedSeats == null || details.bookedSeats.isEmpty()) {
            confirmationDetailsArea.setText("Error displaying confirmation details.");
            downloadBillButton.setEnabled(false); // Disable download if no details
            return;
        }
        downloadBillButton.setEnabled(true); // Ensure download is enabled if details are present

        // Currency formatter for price display
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN")); // For INR (₹)
        currencyFormatter.setMaximumFractionDigits(0); // No decimals needed

        // Attempt to get showtime from mainApp (as it was selected just before booking)
        // This is a slight workaround because BookingDetails doesn't store Showtime directly
        Showtime showtime = mainApp.getSelectedShowtime();
        String theatre = movie.getTheatre(); // Get theatre from Movie object
        String showtimeStr = (showtime != null) ? showtime.getShowDateTime().format(DateTimeFormatter.ofPattern("EEE, MMM dd 'at' hh:mm a")) : "N/A";

        // Build the confirmation text
        StringBuilder confirmationText = new StringBuilder();
        String separator = "*********************************************\n";
        confirmationText.append(separator);
        confirmationText.append("        MOVIE TICKET CONFIRMATION        \n");
        confirmationText.append(separator).append("\n");
        confirmationText.append(String.format("%-12s %s\n", "Movie:", movie.getTitle()));
        confirmationText.append(String.format("%-12s %s\n", "Theatre:", theatre));
        confirmationText.append(String.format("%-12s %s\n", "Showtime:", showtimeStr)).append("\n");

        confirmationText.append(String.format("%-12s %s (%d)\n", "Seats:",
                String.join(", ", details.bookedSeats), details.bookedSeats.size()));
        confirmationText.append(String.format("%-12s %s\n", "Seat Cost:",
                currencyFormatter.format(details.seatCost))).append("\n");

        // Handle Snack Display using the Map
        if (details.selectedSnacksWithQuantities != null && !details.selectedSnacksWithQuantities.isEmpty()) {
            confirmationText.append(String.format("%-12s\n", "Snacks:")); // Header for snacks
             // Iterate through the map
            for (Map.Entry<String, Integer> entry : details.selectedSnacksWithQuantities.entrySet()) {
                 confirmationText.append(String.format("  %-10s x %d\n", // Indent snack lines
                                         entry.getKey() + ":", entry.getValue()));
            }
            confirmationText.append(String.format("%-12s %s\n", "Snack Cost:",
                    currencyFormatter.format(details.snackCost))).append("\n");
        } else {
             confirmationText.append(String.format("%-12s %s\n", "Snacks:", "None")).append("\n");
        }

        // Total Cost and Footer
        confirmationText.append("---------------------------------------------\n"); // Line separator for total
        confirmationText.append(String.format("%-12s %s\n", "TOTAL COST:",
                 currencyFormatter.format(details.totalCost))).append("\n");
        confirmationText.append(separator);
        confirmationText.append("       Thank you for your booking!       \n");
        confirmationText.append(separator);

        // Set the text area content
        confirmationDetailsArea.setText(confirmationText.toString());
        confirmationDetailsArea.setCaretPosition(0); // Scroll to top
    }

    // Method to handle Bill Download
    private void handleDownloadBill(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Bill As");
        // Suggest a filename
        fileChooser.setSelectedFile(new File("MovieTicketBill.txt"));
        // Filter for .txt files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Text Files (*.txt)", "txt");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showSaveDialog(this); // Show save dialog relative to this panel

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            // Ensure the file has a .txt extension
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".txt")) {
                fileToSave = new File(filePath + ".txt");
            }

            // Get text from the confirmation area
            String billContent = confirmationDetailsArea.getText();

            // Write the text to the file using try-with-resources
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileToSave))) {
                writer.write(billContent);
                JOptionPane.showMessageDialog(this,
                        "Bill saved successfully to:\n" + fileToSave.getAbsolutePath(),
                        "Save Successful",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error saving file: " + ex.getMessage(),
                        "Save Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace(); // Log detailed error to console
            }
        }
    }
}