package movieticketbookingsystem;

import movieticketbookingsystem.db.UserDAO; // Import DAO

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Optional;

public class LoginPanel extends JPanel {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private MovieTicketBookingSystem mainApp;
    private UserDAO userDAO; // DAO instance

    public LoginPanel(MovieTicketBookingSystem mainApp) {
        this.mainApp = mainApp;
        this.userDAO = new UserDAO(); // Instantiate DAO
        setupUI(); // Separate method for UI setup
    }

    private void setupUI() {
        // --- UI Layout Code (same as before) ---
        setLayout(new GridBagLayout()); setBackground(new Color(240, 240, 240)); GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(10, 10, 10, 10); gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel titleLabel = new JLabel("Movie Ticket Booking Login", SwingConstants.CENTER); titleLabel.setFont(new Font("Arial", Font.BOLD, 20)); gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0; add(titleLabel, gbc);
        JLabel userLabel = new JLabel("Username:"); gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST; add(userLabel, gbc);
        usernameField = new JTextField(15); gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST; add(usernameField, gbc);
        JLabel passLabel = new JLabel("Password:"); gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST; add(passLabel, gbc);
        passwordField = new JPasswordField(15); gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST; add(passwordField, gbc);
        loginButton = new JButton("Login"); loginButton.setFont(new Font("Arial", Font.BOLD, 14)); loginButton.setBackground(new Color(100, 150, 255)); loginButton.setForeground(Color.WHITE); gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.anchor = GridBagConstraints.CENTER; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0; add(loginButton, gbc);
        statusLabel = new JLabel(" ", SwingConstants.CENTER); statusLabel.setForeground(Color.RED); gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; add(statusLabel, gbc);
        // --- End UI Layout ---

        loginButton.addActionListener(this::handleLogin);
        passwordField.addActionListener(this::handleLogin);
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        // Use DAO to find and verify user
        // TODO: Replace plain 'password' with bcrypt hashing/checking here and in DAO
        Optional<UserData> userOpt = userDAO.findUserAndVerifyPassword(username, password);

        if (userOpt.isPresent()) {
            statusLabel.setText(" ");
            UserData loggedInUser = userOpt.get();
            mainApp.setLoggedInUser(loggedInUser.getUserId(), loggedInUser.getUsername());
            mainApp.showPanel(MovieTicketBookingSystem.MOVIE_SELECTION_PANEL);
        } else {
            statusLabel.setText("Invalid username or password.");
            passwordField.setText("");
            usernameField.requestFocus();
            mainApp.setLoggedInUser(-1, null);
        }
    }

    public void resetFields() {
        usernameField.setText("");
        passwordField.setText("");
        statusLabel.setText(" ");
        usernameField.requestFocus();
    }
}