package univscheduler.ui;

import univscheduler.dao.*;
import univscheduler.model.*;
import univscheduler.service.ConflitService;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.util.List;

/**
 * Panneau de gestion des cours -- ajout, modification, suppression.
 * Design moderne fond blanc, coherent avec EmploiDuTempsPanel / SallesPanel.
 */
public class GestionCoursPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // ── DAOs ──
    private final CoursDAO       coursDAO       = new CoursDAO();
    private final MatiereDAO     matiereDAO     = new MatiereDAO();
    private final ClasseDAO      classeDAO      = new ClasseDAO();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final SalleDAO       salleDAO       = new SalleDAO();
    private final ConflitService conflitService = new ConflitService();

    // ── Etat ──
    private JTable         table;
    private DefaultTableModel tableModel;
    private JLabel         statusLabel;
    private JTextField     searchField;
    private TableRowSorter<DefaultTableModel> sorter;

    // Listes chargees une fois
    private List<Matiere>     matieres;
    private List<Utilisateur> enseignants;
    private List<Classe>      classes;
    private List<Salle>       salles;

    private static final String[] COLUMNS = {
        "ID", "Matiere", "Enseignant", "Classe", "Salle", "Jour", "Debut", "Duree", "Type"
    };

    // ── Palette (coherente avec le reste de l'appli) ──
    private static final Color BG_PAGE   = Color.WHITE;
    private static final Color BG_HEADER = new Color(250, 251, 253);
    private static final Color BORDER_C  = new Color(226, 230, 238);
    private static final Color BORDER_HD = new Color(210, 215, 225);
    private static final Color TEXT_PRI  = new Color(30,  41,  59);
    private static final Color TEXT_SEC  = new Color(100, 116, 139);
    private static final Color ACCENT    = new Color(59,  130, 246);
    private static final Color GREEN     = new Color(34,  197,  94);
    private static final Color RED       = new Color(239,  68,  68);
    private static final Color ORANGE    = new Color(245, 158,  11);

    // ═══════════════════════════════════════════════════════
    //  CONSTRUCTEUR
    // ═══════════════════════════════════════════════════════
    public GestionCoursPanel() {
        initUI();
        loadData();
    }

    // ═══════════════════════════════════════════════════════
    //  INIT UI
    // ═══════════════════════════════════════════════════════
    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PAGE);
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
        add(buildFooter(),  BorderLayout.SOUTH);
    }

    // ── Header ──
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(BG_HEADER);
        header.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_HD),
            new EmptyBorder(14, 22, 14, 22)));

        // Titre
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("Gestion des Cours");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRI);

        JLabel sub = new JLabel("Ajouter, modifier ou supprimer des seances planifiees");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_SEC);

        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        // Controles droite
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        // Barre de recherche
        searchField = new JTextField(16);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C, 1, true),
            new EmptyBorder(4, 10, 4, 10)));
        searchField.putClientProperty("JTextField.placeholderText", "Rechercher...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { applySearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { applySearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applySearch(); }
        });

        JButton addBtn    = pillBtn("+ Nouveau",   GREEN);
        JButton editBtn   = pillBtn("Modifier",    ACCENT);
        JButton deleteBtn = pillBtn("Supprimer",   RED);
        JButton refreshBtn= pillBtn("Actualiser",  TEXT_SEC);

        addBtn.addActionListener(e -> showCoursDialog(null));
        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> loadData());

        right.add(searchField);
        right.add(addBtn);
        right.add(editBtn);
        right.add(deleteBtn);
        right.add(refreshBtn);

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // ── Table ──
    private JScrollPane buildTable() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? Integer.class : String.class;
            }
        };

        table = new JTable(tableModel);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(36);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(BORDER_C);
        table.setBackground(BG_PAGE);
        table.setSelectionBackground(new Color(59, 130, 246));
        table.setSelectionForeground(Color.WHITE);
        table.setFocusable(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // En-tete
        JTableHeader th = table.getTableHeader();
        th.setFont(new Font("Segoe UI", Font.BOLD, 12));
        th.setBackground(BG_HEADER);
        th.setForeground(TEXT_SEC);
        th.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_HD));
        th.setPreferredSize(new Dimension(0, 38));
        th.setReorderingAllowed(false);

        // Cacher colonne ID
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        // Largeurs relatives
        int[] widths = {0, 200, 160, 120, 100, 80, 70, 70, 60};
        for (int i = 1; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Renderer : colorier badge Type + alternance lignes
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, focus, row, col);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setFont(new Font("Segoe UI", Font.PLAIN, 13));

                if (sel) {
                    setBackground(new Color(59, 130, 246));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(row % 2 == 0 ? BG_PAGE : new Color(248, 250, 252));
                    setForeground(TEXT_PRI);
                }

                // Colonne Type : badge colore
                int modelCol = t.convertColumnIndexToModel(col);
                if (modelCol == 8 && val != null && !sel) {
                    String type = val.toString();
                    Color c = "CM".equals(type) ? new Color(59,130,246)
                            : "TD".equals(type) ? new Color(139,92,246)
                            : new Color(16,185,129);
                    setForeground(c);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                }
                // Colonne Salle manquante
                if (modelCol == 4 && "Non assignee".equals(val) && !sel) {
                    setForeground(new Color(245, 158, 11));
                }
                return this;
            }
        });

        // Tri
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Double-clic → modifier
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.getViewport().setBackground(BG_PAGE);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        return sp;
    }

    // ── Footer ──
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 7));
        footer.setBackground(BG_HEADER);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_HD));

        statusLabel = new JLabel("Chargement...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_SEC);
        footer.add(statusLabel);

        // Legende types
        footer.add(Box.createHorizontalStrut(20));
        addLegend(footer, "CM", new Color(59,130,246));
        addLegend(footer, "TD", new Color(139,92,246));
        addLegend(footer, "TP", new Color(16,185,129));
        return footer;
    }

    private void addLegend(JPanel p, String text, Color col) {
        JLabel l = new JLabel("  " + text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(col);
        p.add(l);
    }

    // ═══════════════════════════════════════════════════════
    //  CHARGEMENT DONNEES
    // ═══════════════════════════════════════════════════════
    private void loadData() {
        if (statusLabel != null) statusLabel.setText("Chargement...");
        new SwingWorker<List<Cours>, Void>() {
            @Override protected List<Cours> doInBackground() throws Exception {
                // Charger les listes de reference une seule fois
                matieres    = matiereDAO.findAll();
                enseignants = utilisateurDAO.findByRole(Role.ENSEIGNANT);
                classes     = classeDAO.findAll();
                salles      = salleDAO.findAll();
                return coursDAO.findAll();
            }
            @Override protected void done() {
                try {
                    List<Cours> list = get();
                    tableModel.setRowCount(0);
                    for (Cours c : list) {
                        tableModel.addRow(new Object[]{
                            c.getId(),
                            c.getNomMatiere(),
                            c.getNomEnseignant(),
                            c.getNomClasse(),
                            c.getNomSalle() != null ? c.getNomSalle() : "Non assignee",
                            c.getJourLibelle(),
                            c.getHeureDebut() != null ? c.getHeureDebut().toString().substring(0,5) : "",
                            c.getDureeMinutes() + " min",
                            c.getTypeCours() != null ? c.getTypeCours().name() : ""
                        });
                    }
                    statusLabel.setText(list.size() + " cours au total  --  double-clic pour modifier");
                } catch (Exception ex) {
                    statusLabel.setText("Erreur : " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void applySearch() {
        String txt = searchField.getText().trim();
        if (txt.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + txt));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  DIALOGUE AJOUT / MODIFICATION
    // ═══════════════════════════════════════════════════════
    private void showCoursDialog(Cours existing) {
        boolean isNew = (existing == null);
        JDialog dlg = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            isNew ? "Nouveau cours" : "Modifier le cours", true);
        dlg.setSize(520, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);

        // ── Bandeau titre ──
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(isNew ? GREEN : ACCENT);
        banner.setBorder(new EmptyBorder(16, 22, 16, 22));
        JLabel bannerLbl = new JLabel(isNew ? "Nouveau cours" : "Modifier le cours");
        bannerLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        bannerLbl.setForeground(Color.WHITE);
        banner.add(bannerLbl);

        // ── Formulaire ──
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(BG_PAGE);
        form.setBorder(new EmptyBorder(18, 24, 10, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(5, 0, 5, 0);

        // Combos et champs
        JComboBox<String> matiereCombo    = new JComboBox<>();
        JComboBox<String> enseignantCombo = new JComboBox<>();
        JComboBox<String> classeCombo     = new JComboBox<>();
        JComboBox<String> salleCombo      = new JComboBox<>();
        JComboBox<String> jourCombo       = new JComboBox<>(
            new String[]{"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi"});
        JTextField heureField = new JTextField("08:00");
        JTextField dureeField = new JTextField("90");
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"CM","TD","TP"});

        // Remplir les combos
        if (matieres != null)    for (Matiere m : matieres)       matiereCombo.addItem(m.getId() + " | " + m.getLibelle());
        if (enseignants != null) for (Utilisateur u : enseignants) enseignantCombo.addItem(u.getId() + " | " + u.getNomComplet());
        if (classes != null)     for (Classe c : classes)          classeCombo.addItem(c.getId() + " | " + c.getNom());
        salleCombo.addItem("0 | Non assignee");
        if (salles != null)      for (Salle s : salles)            salleCombo.addItem(s.getId() + " | " + s.getNomComplet());

        // Pre-remplir si modification
        if (!isNew) {
            selectById(matiereCombo,    existing.getMatiereId());
            selectById(enseignantCombo, existing.getEnseignantId());
            selectById(classeCombo,     existing.getClasseId());
            selectById(salleCombo,      existing.getSalleId() != null ? existing.getSalleId() : 0);
            jourCombo.setSelectedIndex(existing.getJourSemaine() - 1);
            heureField.setText(existing.getHeureDebut() != null
                ? existing.getHeureDebut().toString().substring(0,5) : "08:00");
            dureeField.setText(String.valueOf(existing.getDureeMinutes()));
            if (existing.getTypeCours() != null)
                typeCombo.setSelectedItem(existing.getTypeCours().name());
        }

        // Ajouter les lignes label + champ
        Object[][] rows = {
            {"Matiere :",          matiereCombo},
            {"Enseignant :",       enseignantCombo},
            {"Classe :",           classeCombo},
            {"Salle :",            salleCombo},
            {"Jour :",             jourCombo},
            {"Heure debut (HH:mm):", heureField},
            {"Duree (minutes) :", dureeField},
            {"Type :",             typeCombo},
        };

        for (int i = 0; i < rows.length; i++) {
            gc.gridx = 0; gc.gridy = i; gc.weightx = 0.35;
            JLabel lbl = new JLabel((String) rows[i][0]);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lbl.setForeground(TEXT_SEC);
            form.add(lbl, gc);

            gc.gridx = 1; gc.weightx = 0.65;
            JComponent comp = (JComponent) rows[i][1];
            comp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            if (comp instanceof JTextField) {
                comp.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_C, 1, true),
                    new EmptyBorder(4, 8, 4, 8)));
            }
            form.add(comp, gc);
        }

        // ── Pied : boutons ──
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setBackground(new Color(249, 250, 251));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));

        JButton cancelBtn = pillBtn("Annuler", TEXT_SEC);
        JButton saveBtn   = pillBtn(isNew ? "Creer le cours" : "Enregistrer", isNew ? GREEN : ACCENT);

        cancelBtn.addActionListener(e -> dlg.dispose());

        saveBtn.addActionListener(e -> {
            try {
                Cours cours = isNew ? new Cours() : existing;

                // Extraire IDs depuis les combos
                cours.setMatiereId(    extractId(matiereCombo));
                cours.setEnseignantId( extractId(enseignantCombo));
                cours.setClasseId(     extractId(classeCombo));
                int salleId = extractId(salleCombo);
                cours.setSalleId(salleId > 0 ? salleId : null);
                cours.setJourSemaine(  jourCombo.getSelectedIndex() + 1);
                cours.setHeureDebut(   LocalTime.parse(heureField.getText().trim()));
                cours.setDureeMinutes( Integer.parseInt(dureeField.getText().trim()));
                cours.setTypeCours(    Cours.TypeCours.valueOf(
                                           typeCombo.getSelectedItem().toString()));

                // Verification conflits
                ConflitService.ResultatAnalyse analyse = conflitService.analyser(cours);
                if (analyse.hasConflits()) {
                    int choice = JOptionPane.showConfirmDialog(dlg,
                        "Conflits detectes :\n" + analyse.getSummary() +
                        "\n\nVoulez-vous forcer l'enregistrement ?",
                        "Conflits", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (choice != JOptionPane.YES_OPTION) return;
                }

                if (isNew) coursDAO.save(cours);
                else       coursDAO.update(cours);

                dlg.dispose();
                loadData();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg,
                    "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        footer.add(cancelBtn);
        footer.add(saveBtn);

        root.add(banner, BorderLayout.NORTH);
        root.add(new JScrollPane(form) {{
            setBorder(null);
            getViewport().setBackground(BG_PAGE);
        }}, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════
    //  ACTIONS TABLEAU
    // ═══════════════════════════════════════════════════════
    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Selectionnez un cours dans le tableau.", "Modifier", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);

        // Charger le cours complet depuis la BDD
        new SwingWorker<Cours, Void>() {
            @Override protected Cours doInBackground() throws Exception {
                for (Cours c : coursDAO.findAll()) {
                    if (c.getId() == id) return c;
                }
                return null;
            }
            @Override protected void done() {
                try {
                    Cours c = get();
                    if (c != null) showCoursDialog(c);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(GestionCoursPanel.this,
                        "Erreur : " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Selectionnez un cours a supprimer.", "Supprimer", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int    id  = (int)    tableModel.getValueAt(modelRow, 0);
        String mat = (String) tableModel.getValueAt(modelRow, 1);
        String cls = (String) tableModel.getValueAt(modelRow, 3);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Supprimer le cours \"" + mat + "\" (" + cls + ") ?\n" +
            "Cette action est irreversible.",
            "Confirmer la suppression",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                coursDAO.delete(id);
                loadData();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════
    /** Extrait l'ID numerique depuis un item "123 | Libelle" */
    private int extractId(JComboBox<String> combo) {
        String s = combo.getSelectedItem().toString();
        return Integer.parseInt(s.split(" \\| ")[0].trim());
    }

    /** Selectionne dans le combo l'item dont l'ID correspond */
    private void selectById(JComboBox<String> combo, int id) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).startsWith(id + " | ")) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private JButton pillBtn(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()
                    ? new Color(color.getRed(), color.getGreen(), color.getBlue(), 40)
                    : new Color(color.getRed(), color.getGreen(), color.getBlue(), 15));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(color);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 14, 6, 14));
        return btn;
    }
}
