package univscheduler.ui;

import univscheduler.dao.UtilisateurDAO;
import univscheduler.model.Utilisateur;
import univscheduler.service.SessionManager;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;

/**
 * Fenêtre de connexion à l'application UNIV-SCHEDULER.
 */
public class LoginFrame extends JFrame {

    private JTextField emailField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;
    private final UtilisateurDAO utilisateurDAO;

    public LoginFrame() {
        this.utilisateurDAO = new UtilisateurDAO();
        initUI();
    }

    private void initUI() {
        setTitle("UNIV-SCHEDULER - Connexion");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 360);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(52, 73, 94));

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1));
        headerPanel.setBackground(new Color(52, 73, 94));
        headerPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

        JLabel titleLabel = new JLabel("🎓 UNIV-SCHEDULER", SwingConstants.CENTER);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));

        JLabel subtitleLabel = new JLabel("Gestion des Salles et Emplois du Temps", SwingConstants.CENTER);
        subtitleLabel.setForeground(new Color(189, 195, 199));
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);

        // Formulaire de connexion
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);

        // Email
        JLabel emailLabel = new JLabel("Adresse Email :");
        emailLabel.setFont(new Font("Arial", Font.BOLD, 12));
        emailField = new JTextField(20);
        emailField.setFont(new Font("Arial", Font.PLAIN, 13));
        emailField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199)),
            new EmptyBorder(6, 8, 6, 8)
        ));

        // Mot de passe
        JLabel passLabel = new JLabel("Mot de passe :");
        passLabel.setFont(new Font("Arial", Font.BOLD, 12));
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 13));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199)),
            new EmptyBorder(6, 8, 6, 8)
        ));

        // Bouton connexion
        loginButton = new JButton("Se connecter");
        loginButton.setBackground(new Color(52, 152, 219));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setFocusPainted(false);
        loginButton.setBorderPainted(false);
        loginButton.setPreferredSize(new Dimension(200, 38));

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(231, 76, 60));
        statusLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Info compte démo
        JLabel demoLabel = new JLabel("Démo: admin@univ.sn / admin123");
        demoLabel.setForeground(new Color(149, 165, 166));
        demoLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        demoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // Layout du formulaire
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(emailLabel, gbc);
        gbc.gridy = 1;
        formPanel.add(emailField, gbc);
        gbc.gridy = 2;
        formPanel.add(passLabel, gbc);
        gbc.gridy = 3;
        formPanel.add(passwordField, gbc);
        gbc.gridy = 4;
        gbc.insets = new Insets(14, 0, 4, 0);
        formPanel.add(loginButton, gbc);
        gbc.gridy = 5;
        gbc.insets = new Insets(2, 0, 2, 0);
        formPanel.add(statusLabel, gbc);
        gbc.gridy = 6;
        formPanel.add(demoLabel, gbc);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Événements
        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin()); // Entrée sur le champ password
        emailField.addActionListener(e -> passwordField.requestFocus());

        // Focus initial
        SwingUtilities.invokeLater(() -> emailField.requestFocus());
    }

    private void doLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Connexion...");
        statusLabel.setText(" ");

        SwingWorker<Utilisateur, Void> worker = new SwingWorker<>() {
            @Override
            protected Utilisateur doInBackground() throws Exception {
                return utilisateurDAO.authenticate(email, password);
            }

            @Override
            protected void done() {
                try {
                    Utilisateur user = get();
                    if (user != null) {
                        SessionManager.getInstance().connect(user);
                        MainFrame mainFrame = new MainFrame(user);
                        mainFrame.setVisible(true);
                        dispose();
                    } else {
                        statusLabel.setText("Email ou mot de passe incorrect.");
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Erreur : " + ex.getMessage());
                } finally {
                    loginButton.setEnabled(true);
                    loginButton.setText("Se connecter");
                }
            }
        };
        worker.execute();
    }
}
