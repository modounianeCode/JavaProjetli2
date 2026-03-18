package univscheduler.ui;

import univscheduler.model.*;
import univscheduler.service.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Fenêtre principale de l'application UNIV-SCHEDULER.
 * Contient les onglets pour chaque module fonctionnel.
 */
public class MainFrame extends JFrame {

    private final Utilisateur utilisateur;
    private JTabbedPane tabbedPane;

    public MainFrame(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        initUI();
    }

    private void initUI() {
        setTitle("UNIV-SCHEDULER - " + utilisateur.getNomComplet() + " [" + utilisateur.getRole().getLibelle() + "]");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 720);
        setLocationRelativeTo(null);

        // Menu bar
        setJMenuBar(createMenuBar());

        // Panel principal
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Header
        mainPanel.add(createHeader(), BorderLayout.NORTH);

        // Onglets
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 13));

        // Onglet Dashboard (tous les rôles)
        tabbedPane.addTab("Dashboard", new DashboardPanel());

        // Onglet Emploi du temps (tous les rôles)
        tabbedPane.addTab("Emploi du temps", new EmploiDuTempsPanel(utilisateur));

        // Onglet Salles (tous les rôles)
        tabbedPane.addTab("Salles et Espace", new SallesPanel(utilisateur));

        // Onglet Réservations (Enseignants, Gestionnaires, Admin)
        if (!utilisateur.isEtudiant()) {
            tabbedPane.addTab("Réservations", new ReservationsPanel(utilisateur));
        }

        // Onglet Gestion cours (Gestionnaire + Admin)
        if (utilisateur.isAdmin() || utilisateur.isGestionnaire()) {
            tabbedPane.addTab("Gérer Cours", new GestionCoursPanel());
        }

        // Onglet Administration (Admin uniquement)
        if (utilisateur.isAdmin()) {
            tabbedPane.addTab("Administration", new AdminPanel());
        }

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        mainPanel.add(createStatusBar(), BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80));
        header.setBorder(new EmptyBorder(8, 15, 8, 15));

        JLabel titleLabel = new JLabel("🎓 UNIV-SCHEDULER");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        // Rôle badge
        JLabel roleLabel = new JLabel(utilisateur.getRole().getLibelle());
        roleLabel.setForeground(new Color(52, 152, 219));
        roleLabel.setFont(new Font("Arial", Font.BOLD, 12));

        JLabel userLabel = new JLabel(utilisateur.getNomComplet());
        userLabel.setForeground(new Color(189, 195, 199));
        userLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        JButton logoutBtn = new JButton("Déconnexion");
        logoutBtn.setBackground(new Color(231, 76, 60));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFont(new Font("Arial", Font.BOLD, 11));
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logout());

        rightPanel.add(userLabel);
        rightPanel.add(new JSeparator(SwingConstants.VERTICAL));
        rightPanel.add(roleLabel);
        rightPanel.add(logoutBtn);

        header.add(titleLabel, BorderLayout.WEST);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBackground(new Color(236, 240, 241));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(189, 195, 199)));

        JLabel status = new JLabel("  ✅ Connecté | Base de données SQLite | Version 1.0");
        status.setFont(new Font("Arial", Font.PLAIN, 11));
        status.setForeground(new Color(127, 140, 141));
        statusBar.add(status);

        return statusBar;
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Fichier
        JMenu fileMenu = new JMenu("Fichier");
        JMenuItem exportItem = new JMenuItem("Exporter PDF");
        JMenuItem exitItem = new JMenuItem("Quitter");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Menu Aide
        JMenu helpMenu = new JMenu("Aide");
        JMenuItem aboutItem = new JMenuItem("À propos");
        aboutItem.addActionListener(e -> showAbout());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        return menuBar;
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Voulez-vous vous déconnecter ?",
            "Déconnexion", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            SessionManager.getInstance().disconnect();
            new LoginFrame().setVisible(true);
            dispose();
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
            "UNIV-SCHEDULER v1.0\n" +
            "Application de Gestion des Salles et Emplois du Temps\n\n" +
            "Développé dans le cadre du cours POO Java - L2 Informatique\n" +
            "Université Iba Der Thiam de Thiès",
            "À propos", JOptionPane.INFORMATION_MESSAGE);
    }
}
