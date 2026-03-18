package univscheduler.ui;

import univscheduler.dao.*;
import univscheduler.model.*;
import univscheduler.util.PasswordUtil;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Panneau d'administration - design moderne sombre UIDT.
 */
public class AdminPanel extends JPanel {

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final SalleDAO       salleDAO       = new SalleDAO();
    private final BatimentDAO    batimentDAO    = new BatimentDAO();

    // Palette
    private static final Color BG_PAGE    = new Color(232, 236, 242);
    private static final Color BG_HEADER  = new Color(255, 255, 255);
    private static final Color BG_CARD    = new Color(255, 255, 255);
    private static final Color BG_TABLE   = new Color(255, 255, 255);
    private static final Color BG_ROW_ALT = new Color(245, 248, 252);
    private static final Color BG_SEL     = new Color(59, 130, 246);
    private static final Color ACCENT     = new Color(99, 179, 237);
    private static final Color GREEN      = new Color( 34, 197,  94);
    private static final Color RED        = new Color(239,  68,  68);
    private static final Color ORANGE     = new Color(245, 158, 11);
    private static final Color BORDER_C   = new Color(200, 208, 220);
    private static final Color TEXT_PRI   = new Color( 15,  23,  42);
    private static final Color TEXT_SEC   = new Color(100, 116, 139);
    private static final Color HDR_BG     = new Color(248, 250, 252);

    // Couleurs par r-le
    private static final java.util.Map<String, Color> ROLE_COLORS = java.util.Map.of(
        "Administrateur", new Color(239, 68, 68),
        "Gestionnaire",   new Color(245, 158, 11),
        "Enseignant",     new Color(99, 179, 237),
        "Etudiant",       new Color(72, 199, 142)
    );

    public AdminPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PAGE);
        add(buildPageHeader(), BorderLayout.NORTH);
        add(buildTabs(),       BorderLayout.CENTER);
    }

    // -- En-t-te page --
    private JPanel buildPageHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_HEADER);
        h.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(16, 22, 16, 22)));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel title = new JLabel("Administration Systeme");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRI);

        JLabel sub = new JLabel("Gestion des utilisateurs, salles et batiments \u2014 UIDT");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(new Color(100, 116, 139));

        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(sub);
        h.add(text, BorderLayout.WEST);
        return h;
    }

    // -- Onglets --
    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(BG_PAGE);
        tabs.setForeground(TEXT_PRI);

        // Style onglets
        UIManager.put("TabbedPane.selected",           new Color(255,255,255));
        UIManager.put("TabbedPane.background",         new Color(232,236,242));
        UIManager.put("TabbedPane.foreground",         new Color(15,23,42));
        UIManager.put("TabbedPane.contentAreaColor",   new Color(232,236,242));
        UIManager.put("TabbedPane.shadow",             BORDER_C);
        UIManager.put("TabbedPane.darkShadow",         BORDER_C);
        UIManager.put("TabbedPane.light",              BG_CARD);
        UIManager.put("TabbedPane.highlight",          BG_CARD);

        tabs.addTab("  Utilisateurs  ", buildUsersPanel());
        tabs.addTab("  Salles  ",        buildSallesPanel());
        tabs.addTab("  Batiments  ",     buildBatimentsPanel());
        tabs.addTab("  Equipements  ",   new EquipementsPanel());

        tabs.setBackgroundAt(0, BG_PAGE);
        tabs.setForegroundAt(0, ACCENT);
        return tabs;
    }

    // ----------------------------------------------
    //  PANNEAU UTILISATEURS
    // ----------------------------------------------
    private JPanel buildUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_PAGE);

        String[] cols = {"ID", "Nom", "Prenom", "Email", "Role", "Actif"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = buildTable(model);
        hideCol(table, 0);

        // Renderer badge rôle
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String v = val != null ? val.toString() : "";
                Color c = ROLE_COLORS.getOrDefault(v, TEXT_SEC);
                l.setForeground(sel ? Color.WHITE : c);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setBackground(sel ? BG_SEL : (row % 2 == 0 ? BG_TABLE : BG_ROW_ALT));
                l.setOpaque(true);
                l.setBorder(new EmptyBorder(0, 8, 0, 0));
                return l;
            }
        });

        Set<String> cache = new LinkedHashSet<>();

        Runnable loadUsers = () -> new SwingWorker<List<Utilisateur>, Void>() {
            @Override protected List<Utilisateur> doInBackground() throws Exception {
                return utilisateurDAO.findAll();
            }
            @Override protected void done() {
                try {
                    model.setRowCount(0);
                    cache.clear();
                    for (Utilisateur u : get()) {
                        model.addRow(new Object[]{
                            u.getId(), u.getNom(), u.getPrenom(),
                            u.getEmail(), u.getRole().getLibelle(),
                            u.isActif() ? "Actif" : "Inactif"});
                        cache.add(u.getNom());
                        cache.add(u.getPrenom());
                        cache.add(u.getEmail());
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
        loadUsers.run();

        // ── Toolbar ──
        JPanel toolbar = new JPanel(new BorderLayout(0, 0));
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_C));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        leftGroup.setOpaque(false);

        JButton addBtn = actionBtn("+ Ajouter", GREEN);
        addBtn.addActionListener(e -> showAddUserDialog(loadUsers));

        JButton editUserBtn = actionBtn("✎ Modifier", ACCENT);
        editUserBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast(panel, "Selectionnez un utilisateur.", false); return; }
            int modelRow = table.convertRowIndexToModel(row);
            int id       = (int) model.getValueAt(modelRow, 0);
            String nom    = model.getValueAt(modelRow, 1).toString();
            String prenom = model.getValueAt(modelRow, 2).toString();
            String email  = model.getValueAt(modelRow, 3).toString();
            String role   = model.getValueAt(modelRow, 4).toString();
            showEditUserDialog(id, nom, prenom, email, role, loadUsers);
        });

        JButton delUserBtn = actionBtn("✕ Supprimer", RED);
        delUserBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast(panel, "Selectionnez un utilisateur.", false); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            int confirm = JOptionPane.showConfirmDialog(panel,
                "Supprimer cet utilisateur ?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try { utilisateurDAO.delete(id); loadUsers.run(); toast(panel, "Utilisateur supprime.", true); }
                catch (Exception ex) { toast(panel, "Erreur : " + ex.getMessage(), false); }
            }
        });

        JButton passBtn = actionBtn("Mot de passe", ORANGE);
        passBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast(panel, "Selectionnez un utilisateur.", false); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            String np = JOptionPane.showInputDialog(panel, "Nouveau mot de passe :");
            if (np != null && !np.isEmpty()) {
                try { utilisateurDAO.updatePassword(id, np); toast(panel, "Mot de passe mis a jour.", true); }
                catch (Exception ex) { toast(panel, "Erreur : " + ex.getMessage(), false); }
            }
        });

        JButton refreshBtn = actionBtn("Actualiser", ACCENT);
        refreshBtn.addActionListener(e -> loadUsers.run());

        leftGroup.add(addBtn); leftGroup.add(editUserBtn); leftGroup.add(delUserBtn);
        leftGroup.add(passBtn); leftGroup.add(refreshBtn);

        // ── Recherche droite ──
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        rightGroup.setOpaque(false);
        JTextField searchField = searchField();
        JPopupMenu popup = new JPopupMenu();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> suggList = suggestionList(listModel);
        buildSearchPopup(popup, suggList);
        wireSearch(searchField, suggList, listModel, popup, cache, table, model);
        JButton searchBtn = actionBtn("Rechercher", ACCENT);
        searchBtn.setPreferredSize(new Dimension(95, 28));
        searchBtn.addActionListener(e -> { popup.setVisible(false); filterTable(searchField.getText().trim(), table, model); });
        JButton resetBtn = actionBtn("Reinitialiser", TEXT_SEC);
        resetBtn.setPreferredSize(new Dimension(95, 28));
        resetBtn.addActionListener(e -> { searchField.setText(""); popup.setVisible(false); loadUsers.run(); });
        rightGroup.add(new JLabel("🔍")); rightGroup.add(searchField);
        rightGroup.add(searchBtn); rightGroup.add(resetBtn);

        toolbar.add(leftGroup,  BorderLayout.WEST);
        toolbar.add(rightGroup, BorderLayout.EAST);

        JPanel statsBar = buildStatsBar(model, "utilisateur(s)");
        panel.add(toolbar,  BorderLayout.NORTH);
        panel.add(new JScrollPane(table) {{ setBorder(null); getViewport().setBackground(BG_TABLE); }}, BorderLayout.CENTER);
        panel.add(statsBar, BorderLayout.SOUTH);
        return panel;
    }

    // ----------------------------------------------
    //  PANNEAU SALLES
    // ----------------------------------------------
    private JPanel buildSallesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_PAGE);

        String[] cols = {"ID", "Numero", "Nom", "Capacite", "Type", "Batiment", "Etage"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildTable(model);
        hideCol(table, 0);

        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String v = val != null ? val.toString().toLowerCase() : "";
                Color c = v.contains("amphi") ? new Color(139,92,246)
                        : v.contains("tp")    ? new Color(16,185,129)
                        : v.contains("reun")  ? new Color(245,158,11)
                        : ACCENT;
                l.setForeground(sel ? Color.WHITE : c);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setBackground(sel ? BG_SEL : (row % 2 == 0 ? BG_TABLE : BG_ROW_ALT));
                l.setOpaque(true);
                l.setBorder(new EmptyBorder(0, 8, 0, 0));
                return l;
            }
        });

        Set<String> cache = new LinkedHashSet<>();

        Runnable loadS = () -> new SwingWorker<List<Salle>, Void>() {
            @Override protected List<Salle> doInBackground() throws Exception { return salleDAO.findAll(); }
            @Override protected void done() {
                try {
                    model.setRowCount(0);
                    cache.clear();
                    for (Salle s : get()) {
                        model.addRow(new Object[]{
                            s.getId(), s.getNumero(),
                            s.getNom() != null ? s.getNom() : "",
                            s.getCapacite() + " pl.",
                            s.getTypeSalle(),
                            s.getBatiment() != null ? s.getBatiment().getNom() : "",
                            "Etage " + s.getEtage()});
                        cache.add(s.getNumero());
                        if (s.getNom() != null) cache.add(s.getNom());
                        if (s.getBatiment() != null) cache.add(s.getBatiment().getNom());
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
        loadS.run();

        JPanel toolbar = new JPanel(new BorderLayout(0, 0));
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_C));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        leftGroup.setOpaque(false);

        JButton addBtn = actionBtn("+ Ajouter", GREEN);
        addBtn.addActionListener(e -> showAddSalleDialog(loadS));

        JButton editSalleBtn = actionBtn("✎ Modifier", ACCENT);
        editSalleBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast(panel, "Selectionnez une salle.", false); return; }
            int modelRow  = table.convertRowIndexToModel(row);
            int id        = (int) model.getValueAt(modelRow, 0);
            String numero = model.getValueAt(modelRow, 1).toString();
            String nom    = model.getValueAt(modelRow, 2).toString();
            String cap    = model.getValueAt(modelRow, 3).toString().replace(" pl.", "").trim();
            String type   = model.getValueAt(modelRow, 4).toString();
            String etage  = model.getValueAt(modelRow, 6).toString().replace("Etage ", "").trim();
            showEditSalleDialog(id, numero, nom, cap, type, etage, loadS);
        });

        JButton delBtn = actionBtn("✕ Supprimer", RED);
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast(panel, "Selectionnez une salle.", false); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            int confirm = JOptionPane.showConfirmDialog(panel,
                "Supprimer cette salle ?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try { salleDAO.delete(id); loadS.run(); toast(panel, "Salle supprimee.", true); }
                catch (Exception ex) { toast(panel, "Erreur : " + ex.getMessage(), false); }
            }
        });

        JButton refreshBtn = actionBtn("Actualiser", ACCENT);
        refreshBtn.addActionListener(e -> loadS.run());

        leftGroup.add(addBtn); leftGroup.add(editSalleBtn);
        leftGroup.add(delBtn); leftGroup.add(refreshBtn);

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        rightGroup.setOpaque(false);
        JTextField searchField = searchField();
        JPopupMenu popup = new JPopupMenu();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> suggList = suggestionList(listModel);
        buildSearchPopup(popup, suggList);
        wireSearch(searchField, suggList, listModel, popup, cache, table, model);
        JButton searchBtn = actionBtn("Rechercher", ACCENT);
        searchBtn.setPreferredSize(new Dimension(95, 28));
        searchBtn.addActionListener(e -> { popup.setVisible(false); filterTable(searchField.getText().trim(), table, model); });
        JButton resetBtn = actionBtn("Reinitialiser", TEXT_SEC);
        resetBtn.setPreferredSize(new Dimension(95, 28));
        resetBtn.addActionListener(e -> { searchField.setText(""); popup.setVisible(false); loadS.run(); });
        rightGroup.add(new JLabel("🔍")); rightGroup.add(searchField);
        rightGroup.add(searchBtn); rightGroup.add(resetBtn);

        toolbar.add(leftGroup,  BorderLayout.WEST);
        toolbar.add(rightGroup, BorderLayout.EAST);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table) {{ setBorder(null); getViewport().setBackground(BG_TABLE); }}, BorderLayout.CENTER);
        panel.add(buildStatsBar(model, "salle(s)"), BorderLayout.SOUTH);
        return panel;
    }

    // ----------------------------------------------
    //  PANNEAU BATIMENTS
    // ----------------------------------------------
    private JPanel buildBatimentsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_PAGE);

        String[] cols = {"ID", "Nom", "Localisation", "Etages"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = buildTable(model);
        hideCol(table, 0);

        Set<String> cache = new LinkedHashSet<>();

        Runnable loadB = () -> new SwingWorker<List<Batiment>, Void>() {
            @Override protected List<Batiment> doInBackground() throws Exception { return batimentDAO.findAll(); }
            @Override protected void done() {
                try {
                    model.setRowCount(0);
                    cache.clear();
                    for (Batiment b : get()) {
                        model.addRow(new Object[]{
                            b.getId(), b.getNom(),
                            b.getLocalisation() != null ? b.getLocalisation() : "",
                            b.getNombreEtages() + " etage(s)"});
                        cache.add(b.getNom());
                        if (b.getLocalisation() != null) cache.add(b.getLocalisation());
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
        loadB.run();

        JPanel toolbar = new JPanel(new BorderLayout(0, 0));
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_C));

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        leftGroup.setOpaque(false);

        JButton addBtn = actionBtn("+ Ajouter", GREEN);
        addBtn.addActionListener(e -> showAddBatimentDialog(loadB));

        JButton editBatBtn = actionBtn("✎ Modifier", ACCENT);
        editBatBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast(panel, "Selectionnez un batiment.", false); return; }
            int modelRow = table.convertRowIndexToModel(row);
            int id       = (int) model.getValueAt(modelRow, 0);
            String nom   = model.getValueAt(modelRow, 1).toString();
            String loc   = model.getValueAt(modelRow, 2).toString();
            String etg   = model.getValueAt(modelRow, 3).toString().replace(" etage(s)", "").trim();
            showEditBatimentDialog(id, nom, loc, etg, loadB);
        });

        JButton delBatBtn = actionBtn("✕ Supprimer", RED);
        delBatBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast(panel, "Selectionnez un batiment.", false); return; }
            int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
            int confirm = JOptionPane.showConfirmDialog(panel,
                "Supprimer ce batiment ?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try { batimentDAO.delete(id); loadB.run(); toast(panel, "Batiment supprime.", true); }
                catch (Exception ex) { toast(panel, "Erreur : " + ex.getMessage(), false); }
            }
        });

        JButton refreshBtn = actionBtn("Actualiser", ACCENT);
        refreshBtn.addActionListener(e -> loadB.run());

        leftGroup.add(addBtn); leftGroup.add(editBatBtn);
        leftGroup.add(delBatBtn); leftGroup.add(refreshBtn);

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 8));
        rightGroup.setOpaque(false);
        JTextField searchField = searchField();
        JPopupMenu popup = new JPopupMenu();
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> suggList = suggestionList(listModel);
        buildSearchPopup(popup, suggList);
        wireSearch(searchField, suggList, listModel, popup, cache, table, model);
        JButton searchBtn = actionBtn("Rechercher", ACCENT);
        searchBtn.setPreferredSize(new Dimension(95, 28));
        searchBtn.addActionListener(e -> { popup.setVisible(false); filterTable(searchField.getText().trim(), table, model); });
        JButton resetBtn = actionBtn("Reinitialiser", TEXT_SEC);
        resetBtn.setPreferredSize(new Dimension(95, 28));
        resetBtn.addActionListener(e -> { searchField.setText(""); popup.setVisible(false); loadB.run(); });
        rightGroup.add(new JLabel("🔍")); rightGroup.add(searchField);
        rightGroup.add(searchBtn); rightGroup.add(resetBtn);

        toolbar.add(leftGroup,  BorderLayout.WEST);
        toolbar.add(rightGroup, BorderLayout.EAST);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(table) {{ setBorder(null); getViewport().setBackground(BG_TABLE); }}, BorderLayout.CENTER);
        panel.add(buildStatsBar(model, "batiment(s)"), BorderLayout.SOUTH);
        return panel;
    }

    // ----------------------------------------------
    //  DIALOGUES
    // ----------------------------------------------
    private void showAddUserDialog(Runnable onOk) {
        JDialog dlg = darkDialog("Nouvel utilisateur", 420, 340);

        JPanel form = formPanel();
        JTextField nomField    = darkField();
        JTextField prenomField = darkField();
        JTextField emailField  = darkField();
        JPasswordField passField = new JPasswordField();
        styleField(passField);
        JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
        styleCombo(roleCombo);

        addFormRow(form, "Nom",             nomField);
        addFormRow(form, "Prenom",          prenomField);
        addFormRow(form, "Email",           emailField);
        addFormRow(form, "Mot de passe",    passField);
        addFormRow(form, "Role",            roleCombo);

        JButton save = actionBtn("Creer l utilisateur", GREEN);
        save.addActionListener(e -> {
            try {
                Utilisateur u = new Utilisateur(
                    nomField.getText().trim(), prenomField.getText().trim(),
                    emailField.getText().trim(), new String(passField.getPassword()),
                    (Role) roleCombo.getSelectedItem());
                utilisateurDAO.save(u);
                toast(dlg.getContentPane(), "Utilisateur cree !", true);
                dlg.dispose(); onOk.run();
            } catch (Exception ex) {
                toast(dlg.getContentPane(), "Erreur : " + ex.getMessage(), false);
            }
        });

        dlg.getContentPane().add(form, BorderLayout.CENTER);
        dlg.getContentPane().add(btnRow(dlg, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void showAddSalleDialog(Runnable onOk) {
        JDialog dlg = darkDialog("Nouvelle salle", 420, 380);

        JPanel form = formPanel();
        JTextField numField  = darkField();
        JTextField nomField  = darkField();
        JTextField capField  = darkField(); capField.setText("30");
        JTextField etgField  = darkField(); etgField.setText("0");
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "Salle TD", "Salle TP", "Amphitheatre", "Salle de reunion"});
        styleCombo(typeCombo);
        JComboBox<String> batCombo = new JComboBox<>();
        styleCombo(batCombo);

        List<Batiment> bats;
        try { bats = batimentDAO.findAll(); }
        catch (Exception e) { bats = java.util.List.of(); }
        final List<Batiment> batsFinal = bats;
        for (Batiment b : bats) batCombo.addItem(b.getId() + " | " + b.getNom());

        addFormRow(form, "Numero",    numField);
        addFormRow(form, "Nom",       nomField);
        addFormRow(form, "Capacite",  capField);
        addFormRow(form, "Etage",     etgField);
        addFormRow(form, "Type",      typeCombo);
        addFormRow(form, "Batiment",  batCombo);

        JButton save = actionBtn("Ajouter la salle", GREEN);
        save.addActionListener(e -> {
            try {
                Salle s = new Salle();
                s.setNumero(numField.getText().trim());
                s.setNom(nomField.getText().trim());
                s.setCapacite(Integer.parseInt(capField.getText().trim()));
                s.setEtage(Integer.parseInt(etgField.getText().trim()));
                s.setTypeSalleId(typeCombo.getSelectedIndex() + 1);
                if (batCombo.getSelectedItem() != null) {
                    String sel = batCombo.getSelectedItem().toString();
                    s.setBatimentId(Integer.parseInt(sel.split(" \\| ")[0].trim()));
                }
                salleDAO.save(s);
                toast(dlg.getContentPane(), "Salle ajoutee !", true);
                dlg.dispose(); onOk.run();
            } catch (Exception ex) {
                toast(dlg.getContentPane(), "Erreur : " + ex.getMessage(), false);
            }
        });

        dlg.getContentPane().add(form, BorderLayout.CENTER);
        dlg.getContentPane().add(btnRow(dlg, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void showEditUserDialog(int id, String nomInit, String prenomInit,
            String emailInit, String roleInit, Runnable onOk) {
        JDialog dlg = darkDialog("Modifier l utilisateur", 420, 310);
        JPanel form = formPanel();

        JTextField nomField    = darkField(); nomField.setText(nomInit);
        JTextField prenomField = darkField(); prenomField.setText(prenomInit);
        JTextField emailField  = darkField(); emailField.setText(emailInit);
        JComboBox<Role> roleCombo = new JComboBox<>(Role.values());
        styleCombo(roleCombo);
        for (Role r : Role.values()) {
            if (r.getLibelle().equals(roleInit)) { roleCombo.setSelectedItem(r); break; }
        }

        addFormRow(form, "Nom",    nomField);
        addFormRow(form, "Prenom", prenomField);
        addFormRow(form, "Email",  emailField);
        addFormRow(form, "Role",   roleCombo);

        JButton save = actionBtn("Enregistrer", ACCENT);
        save.addActionListener(e -> {
            try {
                Utilisateur u = new Utilisateur(
                    nomField.getText().trim(), prenomField.getText().trim(),
                    emailField.getText().trim(), null,
                    (Role) roleCombo.getSelectedItem());
                u.setId(id);
                utilisateurDAO.update(u);
                toast(dlg.getContentPane(), "Utilisateur mis a jour !", true);
                dlg.dispose(); onOk.run();
            } catch (Exception ex) {
                toast(dlg.getContentPane(), "Erreur : " + ex.getMessage(), false);
            }
        });

        dlg.getContentPane().add(form, BorderLayout.CENTER);
        dlg.getContentPane().add(btnRow(dlg, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void showEditSalleDialog(int id, String numInit, String nomInit,
            String capInit, String typeInit, String etgInit, Runnable onOk) {
        JDialog dlg = darkDialog("Modifier la salle", 420, 380);
        JPanel form = formPanel();

        JTextField numField = darkField(); numField.setText(numInit);
        JTextField nomField = darkField(); nomField.setText(nomInit);
        JTextField capField = darkField(); capField.setText(capInit);
        JTextField etgField = darkField(); etgField.setText(etgInit);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "Salle TD", "Salle TP", "Amphitheatre", "Salle de reunion"});
        styleCombo(typeCombo);
        for (int i = 0; i < typeCombo.getItemCount(); i++) {
            if (typeCombo.getItemAt(i).equalsIgnoreCase(typeInit)) { typeCombo.setSelectedIndex(i); break; }
        }
        JComboBox<String> batCombo = new JComboBox<>();
        styleCombo(batCombo);
        try {
            for (Batiment b : batimentDAO.findAll()) batCombo.addItem(b.getId() + " | " + b.getNom());
        } catch (Exception ignored) {}

        addFormRow(form, "Numero",   numField);
        addFormRow(form, "Nom",      nomField);
        addFormRow(form, "Capacite", capField);
        addFormRow(form, "Etage",    etgField);
        addFormRow(form, "Type",     typeCombo);
        addFormRow(form, "Batiment", batCombo);

        JButton save = actionBtn("Enregistrer", ACCENT);
        save.addActionListener(e -> {
            try {
                Salle s = new Salle();
                s.setId(id);
                s.setNumero(numField.getText().trim());
                s.setNom(nomField.getText().trim());
                s.setCapacite(Integer.parseInt(capField.getText().trim()));
                s.setEtage(Integer.parseInt(etgField.getText().trim()));
                s.setTypeSalleId(typeCombo.getSelectedIndex() + 1);
                if (batCombo.getSelectedItem() != null) {
                    String sel = batCombo.getSelectedItem().toString();
                    s.setBatimentId(Integer.parseInt(sel.split(" \\| ")[0].trim()));
                }
                salleDAO.update(s);
                toast(dlg.getContentPane(), "Salle mise a jour !", true);
                dlg.dispose(); onOk.run();
            } catch (Exception ex) {
                toast(dlg.getContentPane(), "Erreur : " + ex.getMessage(), false);
            }
        });

        dlg.getContentPane().add(form, BorderLayout.CENTER);
        dlg.getContentPane().add(btnRow(dlg, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void showEditBatimentDialog(int id, String nomInit, String locInit,
            String etgInit, Runnable onOk) {
        JDialog dlg = darkDialog("Modifier le batiment", 400, 260);
        JPanel form = formPanel();

        JTextField nomField = darkField(); nomField.setText(nomInit);
        JTextField locField = darkField(); locField.setText(locInit);
        JTextField etgField = darkField(); etgField.setText(etgInit);

        addFormRow(form, "Nom",          nomField);
        addFormRow(form, "Localisation", locField);
        addFormRow(form, "Nb etages",    etgField);

        JButton save = actionBtn("Enregistrer", ACCENT);
        save.addActionListener(e -> {
            try {
                Batiment b = new Batiment(
                    nomField.getText().trim(), locField.getText().trim(),
                    Integer.parseInt(etgField.getText().trim()));
                b.setId(id);
                batimentDAO.update(b);
                toast(dlg.getContentPane(), "Batiment mis a jour !", true);
                dlg.dispose(); onOk.run();
            } catch (Exception ex) {
                toast(dlg.getContentPane(), "Erreur : " + ex.getMessage(), false);
            }
        });

        dlg.getContentPane().add(form, BorderLayout.CENTER);
        dlg.getContentPane().add(btnRow(dlg, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void showAddBatimentDialog(Runnable onOk) {
        JDialog dlg = darkDialog("Nouveau batiment", 400, 260);
        JPanel form = formPanel();
        JTextField nomField = darkField();
        JTextField locField = darkField();
        JTextField etgField = darkField(); etgField.setText("1");

        addFormRow(form, "Nom",          nomField);
        addFormRow(form, "Localisation", locField);
        addFormRow(form, "Nb etages",    etgField);

        JButton save = actionBtn("Ajouter le batiment", GREEN);
        save.addActionListener(e -> {
            try {
                Batiment b = new Batiment(
                    nomField.getText().trim(), locField.getText().trim(),
                    Integer.parseInt(etgField.getText().trim()));
                batimentDAO.save(b);
                toast(dlg.getContentPane(), "Batiment ajoute !", true);
                dlg.dispose(); onOk.run();
            } catch (Exception ex) {
                toast(dlg.getContentPane(), "Erreur : " + ex.getMessage(), false);
            }
        });

        dlg.getContentPane().add(form, BorderLayout.CENTER);
        dlg.getContentPane().add(btnRow(dlg, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ----------------------------------------------
    //  Composants utilitaires
    // ----------------------------------------------
    private JTable buildTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? BG_TABLE : BG_ROW_ALT);
                } else {
                    c.setBackground(BG_SEL);
                }
                return c;
            }
        };
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT_PRI);
        table.setBackground(BG_TABLE);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(BG_SEL);
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setBorder(null);

        // En-t-te
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(HDR_BG);
        header.setForeground(new Color(15, 23, 42));
        header.setBorder(new MatteBorder(0, 0, 2, 0, BORDER_C));
        header.setPreferredSize(new Dimension(0, 38));

        // Renderer par d-faut
        DefaultTableCellRenderer defRend = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                l.setForeground(sel ? Color.WHITE : TEXT_PRI);
                l.setBackground(sel ? BG_SEL : (row%2==0 ? BG_TABLE : BG_ROW_ALT));
                l.setBorder(new EmptyBorder(0, 12, 0, 0));
                l.setOpaque(true);
                return l;
            }
        };
        table.setDefaultRenderer(Object.class, defRend);

        // Tri
        table.setAutoCreateRowSorter(true);
        return table;
    }

    private JTextField searchField() {
        JTextField f = new JTextField();
        f.setPreferredSize(new Dimension(160, 28));
        styleField(f);
        return f;
    }

    private JList<String> suggestionList(DefaultListModel<String> model) {
        JList<String> list = new JList<>(model);
        list.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        list.setForeground(TEXT_PRI);
        list.setBackground(BG_CARD);
        list.setSelectionBackground(BG_SEL);
        list.setSelectionForeground(Color.WHITE);
        list.setFixedCellHeight(26);
        list.setBorder(new EmptyBorder(2, 8, 2, 8));
        return list;
    }

    private void buildSearchPopup(JPopupMenu popup, JList<String> suggList) {
        popup.setBorder(BorderFactory.createLineBorder(BORDER_C, 1));
        popup.setLayout(new BorderLayout());
        JScrollPane sc = new JScrollPane(suggList);
        sc.setBorder(null);
        sc.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        popup.add(sc, BorderLayout.CENTER);
    }

    private void wireSearch(JTextField field, JList<String> suggList,
            DefaultListModel<String> listModel, JPopupMenu popup,
            Set<String> cache, JTable table, DefaultTableModel model) {

        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { suggest(); }
            public void removeUpdate(DocumentEvent e)  { suggest(); }
            public void changedUpdate(DocumentEvent e) { suggest(); }
            private void suggest() {
                String text = field.getText().trim();
                if (text.isEmpty()) { popup.setVisible(false); return; }
                String lower = text.toLowerCase();
                java.util.List<String> matches = cache.stream()
                    .filter(s -> s.toLowerCase().contains(lower))
                    .sorted().limit(8).collect(Collectors.toList());
                if (matches.isEmpty()) { popup.setVisible(false); return; }
                listModel.clear();
                matches.forEach(listModel::addElement);
                suggList.setSelectedIndex(0);
                int h = Math.min(matches.size() * 26 + 6, 200);
                popup.setPreferredSize(new Dimension(field.getWidth(), h));
                if (!popup.isVisible()) popup.show(field, 0, field.getHeight());
                else { popup.revalidate(); popup.repaint(); }
                field.requestFocusInWindow();
                // Filtre en temps réel
                filterTable(text, table, model);
            }
        });

        field.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && popup.isVisible()) {
                    int n = Math.min(suggList.getSelectedIndex()+1, listModel.getSize()-1);
                    suggList.setSelectedIndex(n); suggList.ensureIndexIsVisible(n);
                } else if (e.getKeyCode() == KeyEvent.VK_UP && popup.isVisible()) {
                    int n = Math.max(suggList.getSelectedIndex()-1, 0);
                    suggList.setSelectedIndex(n); suggList.ensureIndexIsVisible(n);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (popup.isVisible() && suggList.getSelectedValue() != null) {
                        field.setText(suggList.getSelectedValue());
                        popup.setVisible(false);
                    }
                    filterTable(field.getText().trim(), table, model);
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.setVisible(false);
                }
            }
        });

        suggList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                String sel = suggList.getSelectedValue();
                if (sel != null) { field.setText(sel); popup.setVisible(false); filterTable(sel, table, model); }
            }
        });
    }

    /** Filtre toutes les colonnes visibles de la table selon le texte. */
    private void filterTable(String text, JTable table, DefaultTableModel model) {
        if (table.getRowSorter() instanceof javax.swing.table.TableRowSorter) {
            @SuppressWarnings("unchecked")
            javax.swing.table.TableRowSorter<DefaultTableModel> sorter =
                (javax.swing.table.TableRowSorter<DefaultTableModel>) table.getRowSorter();
            if (text == null || text.isEmpty()) { sorter.setRowFilter(null); return; }
            sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text)));
        }
    }

    private JPanel buildStatsBar(DefaultTableModel model, String label) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 5));
        bar.setBackground(new Color(232, 236, 242));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));
        JLabel lbl = new JLabel();
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(TEXT_SEC);
        model.addTableModelListener(e -> lbl.setText(model.getRowCount() + " " + label));
        bar.add(lbl);
        return bar;
    }

    private JButton actionBtn(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover()
                    ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 50)
                    : new Color(color.getRed(), color.getGreen(), color.getBlue(), 22);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(color);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(color);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        return btn;
    }

    private JDialog darkDialog(String title, int w, int h) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dlg.setSize(w, h);
        dlg.setLocationRelativeTo(this);
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(BG_CARD);
        dlg.setContentPane(content);
        return dlg;
    }

    private JPanel formPanel() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(BG_CARD);
        form.setBorder(new EmptyBorder(18, 22, 10, 22));
        return form;
    }

    private void addFormRow(JPanel form, String label, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setBorder(new EmptyBorder(4, 0, 4, 0));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_SEC);
        lbl.setPreferredSize(new Dimension(110, 28));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        form.add(row);
    }

    private JTextField darkField() {
        JTextField f = new JTextField();
        styleField(f);
        return f;
    }

    private void styleField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        f.setForeground(TEXT_PRI);
        f.setBackground(new Color(248, 250, 252));
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C, 1, true),
            new EmptyBorder(4, 8, 4, 8)));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setBackground(new Color(248, 250, 252));
        combo.setForeground(TEXT_PRI);
        combo.setBorder(BorderFactory.createLineBorder(BORDER_C, 1, true));
    }

    private JPanel btnRow(JDialog dlg, JButton action) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        row.setBackground(BG_CARD);
        row.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));
        JButton cancel = actionBtn("Annuler", TEXT_SEC);
        cancel.addActionListener(e -> dlg.dispose());
        row.add(cancel);
        row.add(action);
        return row;
    }

    private void toast(Container parent, String msg, boolean success) {
        JOptionPane.showMessageDialog(parent, msg,
            success ? "Succes" : "Erreur",
            success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    private void hideCol(JTable table, int col) {
        table.getColumnModel().getColumn(col).setMinWidth(0);
        table.getColumnModel().getColumn(col).setMaxWidth(0);
        table.getColumnModel().getColumn(col).setWidth(0);
    }
}
