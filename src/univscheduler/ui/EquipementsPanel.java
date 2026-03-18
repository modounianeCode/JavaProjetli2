package univscheduler.ui;

import univscheduler.dao.EquipementDAO;
import univscheduler.dao.SalleDAO;
import univscheduler.model.Equipement;
import univscheduler.model.Salle;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panneau de gestion des équipements et liaison salle ↔ équipement.
 * Design cohérent avec AdminPanel / ReservationsPanel.
 */
public class EquipementsPanel extends JPanel {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BG_PAGE    = new Color(232, 236, 242);
    private static final Color BG_HEADER  = new Color(255, 255, 255);
    private static final Color BG_CARD    = new Color(255, 255, 255);
    private static final Color BG_TABLE   = new Color(255, 255, 255);
    private static final Color BG_ROW_ALT = new Color(245, 248, 252);
    private static final Color BG_SEL     = new Color(59, 130, 246);
    private static final Color ACCENT     = new Color(99, 179, 237);
    private static final Color GREEN      = new Color(34, 197, 94);
    private static final Color RED        = new Color(239, 68, 68);
    private static final Color BORDER_C   = new Color(200, 208, 220);
    private static final Color TEXT_PRI   = new Color(15, 23, 42);
    private static final Color TEXT_SEC   = new Color(100, 116, 139);
    private static final Color HDR_BG     = new Color(248, 250, 252);

    private static final Map<String, Color> CAT_COLORS = new HashMap<>();
    static {
        CAT_COLORS.put("AUDIOVISUEL", new Color(59, 130, 246));
        CAT_COLORS.put("INFORMATIQUE", new Color(139, 92, 246));
        CAT_COLORS.put("MOBILIER",     new Color(245, 158, 11));
        CAT_COLORS.put("CONFORT",      new Color(34, 197, 94));
        CAT_COLORS.put("AUTRE",        new Color(100, 116, 139));
    }

    private final EquipementDAO equipementDAO = new EquipementDAO();
    private final SalleDAO      salleDAO      = new SalleDAO();

    private DefaultTableModel tableModel;
    private JTable            table;
    private JLabel            statsLabel;
    private final Set<String> suggestionsCache = new LinkedHashSet<>();

    public EquipementsPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PAGE);
        add(buildPageHeader(),  BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
    }

    // ── En-tête ──────────────────────────────────────────────────────────────
    private JPanel buildPageHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_HEADER);
        h.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(16, 22, 16, 22)));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel title = new JLabel("Gestion des Equipements");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRI);

        JLabel sub = new JLabel("CRUD equipements et liaison avec les salles");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_SEC);

        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(sub);
        h.add(text, BorderLayout.WEST);
        return h;
    }

    // ── Contenu ───────────────────────────────────────────────────────────────
    private JPanel buildMainContent() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_PAGE);
        panel.add(buildToolbar(),   BorderLayout.NORTH);
        panel.add(buildTableArea(), BorderLayout.CENTER);
        panel.add(buildStatusBar(), BorderLayout.SOUTH);
        return panel;
    }

    // ── Toolbar ───────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(0, 0));
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(8, 14, 8, 14)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JButton addBtn = actionBtn("+ Ajouter", GREEN);
        addBtn.addActionListener(e -> showEditDialog(null));

        JButton editBtn = actionBtn("✎ Modifier", ACCENT);
        editBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast("Selectionnez un equipement."); return; }
            int id = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
            new SwingWorker<Equipement, Void>() {
                @Override protected Equipement doInBackground() throws Exception {
                    return equipementDAO.findById(id);
                }
                @Override protected void done() {
                    try { showEditDialog(get()); }
                    catch (Exception ex) { toast("Erreur : " + ex.getMessage()); }
                }
            }.execute();
        });

        JButton delBtn = actionBtn("✕ Supprimer", RED);
        delBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast("Selectionnez un equipement."); return; }
            int modelRow = table.convertRowIndexToModel(row);
            int id       = (int) tableModel.getValueAt(modelRow, 0);
            String lib   = tableModel.getValueAt(modelRow, 1).toString();
            int nb       = (int) tableModel.getValueAt(modelRow, 4);
            String msg   = nb > 0
                ? "\"" + lib + "\" est utilise dans " + nb + " salle(s).\nSupprimer quand meme ?"
                : "Supprimer l'equipement \"" + lib + "\" ?";
            int c = JOptionPane.showConfirmDialog(EquipementsPanel.this,
                msg, "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (c != JOptionPane.YES_OPTION) return;
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    equipementDAO.delete(id); return null;
                }
                @Override protected void done() {
                    try { get(); chargerDonnees(); }
                    catch (Exception ex) { toast("Erreur : " + ex.getMessage()); }
                }
            }.execute();
        });

        JButton liaisonBtn = actionBtn("🔗 Gerer liaison salle", new Color(139, 92, 246));
        liaisonBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { toast("Selectionnez un equipement."); return; }
            int id  = (int) tableModel.getValueAt(table.convertRowIndexToModel(row), 0);
            String lib = tableModel.getValueAt(table.convertRowIndexToModel(row), 1).toString();
            showLiaisonDialog(id, lib);
        });

        JButton refreshBtn = actionBtn("Actualiser", ACCENT);
        refreshBtn.addActionListener(e -> chargerDonnees());

        left.add(addBtn); left.add(editBtn); left.add(delBtn);
        left.add(liaisonBtn); left.add(refreshBtn);

        // ── Recherche droite ──────────────────────────────────────────────────
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        JTextField searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(160, 28));
        styleField(searchField);

        DefaultListModel<String> suggModel = new DefaultListModel<>();
        JList<String> suggList = new JList<>(suggModel);
        suggList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        suggList.setForeground(TEXT_PRI);
        suggList.setBackground(BG_CARD);
        suggList.setSelectionBackground(BG_SEL);
        suggList.setSelectionForeground(Color.WHITE);
        suggList.setFixedCellHeight(26);
        suggList.setBorder(new EmptyBorder(2, 8, 2, 8));

        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(BORDER_C, 1));
        popup.setLayout(new BorderLayout());
        JScrollPane suggScroll = new JScrollPane(suggList);
        suggScroll.setBorder(null);
        suggScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        popup.add(suggScroll, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { suggest(); filter(); }
            public void removeUpdate(DocumentEvent e)  { suggest(); filter(); }
            public void changedUpdate(DocumentEvent e) { suggest(); filter(); }
            private void suggest() {
                String t = searchField.getText().trim();
                if (t.isEmpty()) { popup.setVisible(false); return; }
                List<String> m = suggestionsCache.stream()
                    .filter(s -> s.toLowerCase().contains(t.toLowerCase()))
                    .sorted().limit(8).collect(Collectors.toList());
                if (m.isEmpty()) { popup.setVisible(false); return; }
                suggModel.clear(); m.forEach(suggModel::addElement);
                suggList.setSelectedIndex(0);
                int h = Math.min(m.size() * 26 + 6, 200);
                popup.setPreferredSize(new Dimension(searchField.getWidth(), h));
                if (!popup.isVisible()) popup.show(searchField, 0, searchField.getHeight());
                else { popup.revalidate(); popup.repaint(); }
                searchField.requestFocusInWindow();
            }
            private void filter() {
                String t = searchField.getText().trim();
                if (table.getRowSorter() instanceof TableRowSorter) {
                    @SuppressWarnings("unchecked")
                    TableRowSorter<DefaultTableModel> s =
                        (TableRowSorter<DefaultTableModel>) table.getRowSorter();
                    s.setRowFilter(t.isEmpty() ? null :
                        RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(t)));
                }
            }
        });

        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && popup.isVisible()) {
                    int n = Math.min(suggList.getSelectedIndex()+1, suggModel.getSize()-1);
                    suggList.setSelectedIndex(n); suggList.ensureIndexIsVisible(n);
                } else if (e.getKeyCode() == KeyEvent.VK_UP && popup.isVisible()) {
                    int n = Math.max(suggList.getSelectedIndex()-1, 0);
                    suggList.setSelectedIndex(n); suggList.ensureIndexIsVisible(n);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (popup.isVisible() && suggList.getSelectedValue() != null) {
                        searchField.setText(suggList.getSelectedValue());
                        popup.setVisible(false);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    popup.setVisible(false);
                }
            }
        });

        suggList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                String sel = suggList.getSelectedValue();
                if (sel != null) { searchField.setText(sel); popup.setVisible(false); }
            }
        });

        JButton searchBtn = actionBtn("Rechercher", ACCENT);
        searchBtn.setPreferredSize(new Dimension(95, 28));
        searchBtn.addActionListener(e -> popup.setVisible(false));

        JButton resetBtn = actionBtn("Reinitialiser", TEXT_SEC);
        resetBtn.setPreferredSize(new Dimension(95, 28));
        resetBtn.addActionListener(e -> {
            searchField.setText(""); popup.setVisible(false); chargerDonnees();
        });

        right.add(new JLabel("🔍")); right.add(searchField);
        right.add(searchBtn); right.add(resetBtn);

        toolbar.add(left,  BorderLayout.WEST);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    // ── Tableau ───────────────────────────────────────────────────────────────
    private JPanel buildTableArea() {
        String[] cols = {"ID", "Libelle", "Categorie", "Description", "Nb Salles"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return c == 4 ? Integer.class : String.class;
            }
        };

        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? BG_TABLE : BG_ROW_ALT);
                else c.setBackground(BG_SEL);
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
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(HDR_BG);
        header.setForeground(TEXT_PRI);
        header.setBorder(new MatteBorder(0, 0, 2, 0, BORDER_C));
        header.setPreferredSize(new Dimension(0, 38));

        // Renderer défaut
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
        table.setDefaultRenderer(Integer.class, defRend);

        // Renderer Catégorie — badge coloré
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String v = val != null ? val.toString() : "";
                Color c = CAT_COLORS.getOrDefault(v.toUpperCase(), TEXT_SEC);
                l.setForeground(sel ? Color.WHITE : c);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setBackground(sel ? BG_SEL : (row%2==0 ? BG_TABLE : BG_ROW_ALT));
                l.setBorder(new EmptyBorder(0, 12, 0, 0));
                l.setOpaque(true);
                return l;
            }
        });

        // Renderer Nb Salles — badge numérique
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                int nb = val instanceof Integer ? (int) val : 0;
                l.setText(nb + " salle(s)");
                l.setForeground(sel ? Color.WHITE : (nb > 0 ? new Color(59, 130, 246) : TEXT_SEC));
                l.setFont(new Font("Segoe UI", nb > 0 ? Font.BOLD : Font.PLAIN, 11));
                l.setBackground(sel ? BG_SEL : (row%2==0 ? BG_TABLE : BG_ROW_ALT));
                l.setBorder(new EmptyBorder(0, 12, 0, 0));
                l.setOpaque(true);
                return l;
            }
        });

        // Cacher colonne ID
        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);

        // Double-clic → modifier
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int id = (int) tableModel.getValueAt(
                        table.convertRowIndexToModel(table.getSelectedRow()), 0);
                    new SwingWorker<Equipement, Void>() {
                        @Override protected Equipement doInBackground() throws Exception {
                            return equipementDAO.findById(id);
                        }
                        @Override protected void done() {
                            try { showEditDialog(get()); }
                            catch (Exception ex) { toast("Erreur : " + ex.getMessage()); }
                        }
                    }.execute();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_TABLE);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_PAGE);
        wrapper.setBorder(new EmptyBorder(12, 14, 0, 14));
        wrapper.add(scroll, BorderLayout.CENTER);

        chargerDonnees();
        return wrapper;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 5));
        bar.setBackground(BG_PAGE);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_C),
            new EmptyBorder(4, 14, 4, 14)));
        statsLabel = new JLabel("Chargement...");
        statsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statsLabel.setForeground(TEXT_SEC);
        bar.add(statsLabel);
        return bar;
    }

    // ── Chargement ────────────────────────────────────────────────────────────
    private void chargerDonnees() {
        new SwingWorker<List<Equipement>, Void>() {
            @Override protected List<Equipement> doInBackground() throws Exception {
                return equipementDAO.findAll();
            }
            @Override protected void done() {
                try {
                    List<Equipement> list = get();
                    tableModel.setRowCount(0);
                    suggestionsCache.clear();
                    for (Equipement eq : list) {
                        String cat = eq.getCategorie() != null
                            ? eq.getCategorie().name() : "AUTRE";
                        tableModel.addRow(new Object[]{
                            eq.getId(), eq.getLibelle(), cat,
                            eq.getDescription() != null ? eq.getDescription() : "",
                            eq.getNbSalles()
                        });
                        suggestionsCache.add(eq.getLibelle());
                        if (eq.getDescription() != null && !eq.getDescription().isEmpty())
                            suggestionsCache.add(eq.getDescription());
                    }
                    statsLabel.setText(list.size() + " equipement(s) au total");
                } catch (Exception ex) {
                    statsLabel.setText("Erreur : " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ── Dialogue Ajouter / Modifier ───────────────────────────────────────────
    private void showEditDialog(Equipement existing) {
        boolean isNew = existing == null;
        JDialog dlg = darkDialog(isNew ? "Nouvel equipement" : "Modifier equipement", 440, 300);
        JPanel form = formPanel();

        JTextField libelleField = darkField();
        JTextField descField    = darkField();
        JComboBox<Equipement.Categorie> catCombo =
            new JComboBox<>(Equipement.Categorie.values());
        styleCombo(catCombo);

        if (!isNew) {
            libelleField.setText(existing.getLibelle());
            descField.setText(existing.getDescription() != null ? existing.getDescription() : "");
            if (existing.getCategorie() != null) catCombo.setSelectedItem(existing.getCategorie());
        }

        addFormRow(form, "Libelle *",    libelleField);
        addFormRow(form, "Categorie *",  catCombo);
        addFormRow(form, "Description",  descField);

        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLbl.setBorder(new EmptyBorder(6, 0, 0, 0));
        form.add(statusLbl);

        Color btnColor = isNew ? GREEN : ACCENT;
        JButton save = actionBtn(isNew ? "Ajouter" : "Enregistrer", btnColor);
        save.addActionListener(e -> {
            String lib = libelleField.getText().trim();
            if (lib.isEmpty()) {
                statusLbl.setForeground(RED); statusLbl.setText("Le libelle est obligatoire."); return;
            }
            Equipement eq = isNew ? new Equipement() : existing;
            eq.setLibelle(lib);
            eq.setDescription(descField.getText().trim());
            eq.setCategorie((Equipement.Categorie) catCombo.getSelectedItem());

            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    if (isNew) equipementDAO.save(eq);
                    else       equipementDAO.update(eq);
                    return null;
                }
                @Override protected void done() {
                    try { get(); dlg.dispose(); chargerDonnees(); }
                    catch (Exception ex) {
                        statusLbl.setForeground(RED);
                        statusLbl.setText("Erreur : " + ex.getMessage());
                    }
                }
            }.execute();
        });

        dlg.getContentPane().add(form,          BorderLayout.CENTER);
        dlg.getContentPane().add(btnRow(dlg, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── Dialogue Liaison Salle ↔ Équipement ──────────────────────────────────
    private void showLiaisonDialog(int equipementId, String equipementLib) {
        JDialog dlg = darkDialog("Salles equipees de : " + equipementLib, 520, 480);
        dlg.getContentPane().setLayout(new BorderLayout(0, 0));

        // En-tête
        JLabel titleLbl = new JLabel("  Cochez les salles qui possedent cet equipement");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(TEXT_PRI);
        titleLbl.setBorder(new EmptyBorder(14, 16, 10, 16));
        titleLbl.setOpaque(true);
        titleLbl.setBackground(BG_CARD);

        // Liste des salles avec cases à cocher
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG_CARD);
        listPanel.setBorder(new EmptyBorder(4, 16, 4, 16));

        JLabel loadingLbl = new JLabel("Chargement...");
        loadingLbl.setForeground(TEXT_SEC);
        listPanel.add(loadingLbl);

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_CARD);

        JButton saveBtn = actionBtn("Enregistrer", GREEN);

        // Charger salles + liaisons existantes
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() throws Exception {
                List<Salle> salles = salleDAO.findAll();
                List<Integer> deja = equipementDAO.findEquipementIdsBySalle(0); // unused
                // récupérer les salle_ids qui ont cet équipement
                List<Integer> sallesAvec = new ArrayList<>();
                for (Salle s : salles) {
                    List<Integer> eqs = equipementDAO.findEquipementIdsBySalle(s.getId());
                    if (eqs.contains(equipementId)) sallesAvec.add(s.getId());
                }
                return new Object[]{salles, sallesAvec};
            }
            @Override protected void done() {
                try {
                    Object[] res = get();
                    @SuppressWarnings("unchecked")
                    List<Salle> salles = (List<Salle>) res[0];
                    @SuppressWarnings("unchecked")
                    List<Integer> sallesAvec = (List<Integer>) res[1];

                    listPanel.removeAll();
                    List<JCheckBox> boxes = new ArrayList<>();

                    // Regrouper par bâtiment
                    String lastBat = "";
                    for (Salle s : salles) {
                        String batNom = s.getBatiment() != null ? s.getBatiment().getNom() : "?";
                        if (!batNom.equals(lastBat)) {
                            JLabel batLbl = new JLabel("  " + batNom);
                            batLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                            batLbl.setForeground(TEXT_SEC);
                            batLbl.setBorder(new EmptyBorder(8, 0, 2, 0));
                            batLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                            listPanel.add(batLbl);
                            lastBat = batNom;
                        }
                        JCheckBox cb = new JCheckBox(
                            s.getNumero() + " — " + (s.getNom() != null ? s.getNom() : "") +
                            "  (cap. " + s.getCapacite() + " | " + s.getTypeSalle() + ")");
                        cb.setSelected(sallesAvec.contains(s.getId()));
                        cb.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                        cb.setForeground(TEXT_PRI);
                        cb.setBackground(BG_CARD);
                        cb.setOpaque(true);
                        cb.setAlignmentX(Component.LEFT_ALIGNMENT);
                        cb.putClientProperty("salleId", s.getId());
                        boxes.add(cb);
                        listPanel.add(cb);
                        listPanel.add(Box.createVerticalStrut(2));
                    }
                    listPanel.revalidate();
                    listPanel.repaint();

                    saveBtn.addActionListener(ev -> {
                        List<Integer> selected = boxes.stream()
                            .filter(JCheckBox::isSelected)
                            .map(cb -> (int) cb.getClientProperty("salleId"))
                            .collect(Collectors.toList());
                        new SwingWorker<Void, Void>() {
                            @Override protected Void doInBackground() throws Exception {
                                // Pour chaque salle, ajouter ou retirer cet équipement
                                for (JCheckBox cb : boxes) {
                                    int salleId = (int) cb.getClientProperty("salleId");
                                    if (cb.isSelected()) {
                                        equipementDAO.addToSalle(salleId, equipementId);
                                    } else {
                                        equipementDAO.removeFromSalle(salleId, equipementId);
                                    }
                                }
                                return null;
                            }
                            @Override protected void done() {
                                try { get(); dlg.dispose(); chargerDonnees(); }
                                catch (Exception ex) {
                                    JOptionPane.showMessageDialog(dlg,
                                        "Erreur : " + ex.getMessage(), "Erreur",
                                        JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }.execute();
                    });

                } catch (Exception ex) {
                    listPanel.removeAll();
                    JLabel err = new JLabel("Erreur : " + ex.getMessage());
                    err.setForeground(RED);
                    listPanel.add(err);
                    listPanel.revalidate();
                }
            }
        }.execute();

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(BG_CARD);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));
        JButton cancelBtn = actionBtn("Annuler", TEXT_SEC);
        cancelBtn.addActionListener(e -> dlg.dispose());
        footer.add(cancelBtn);
        footer.add(saveBtn);

        dlg.getContentPane().add(titleLbl, BorderLayout.NORTH);
        dlg.getContentPane().add(scroll,   BorderLayout.CENTER);
        dlg.getContentPane().add(footer,   BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── Utilitaires UI ────────────────────────────────────────────────────────
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
        btn.setBorder(new EmptyBorder(7, 14, 7, 14));
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
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setBorder(new EmptyBorder(4, 0, 4, 0));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_SEC);
        lbl.setPreferredSize(new Dimension(120, 28));
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

    private void toast(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
}
