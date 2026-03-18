package univscheduler.ui;

import univscheduler.dao.ReservationDAO;
import univscheduler.dao.SalleDAO;
import univscheduler.model.Reservation;
import univscheduler.model.Salle;
import univscheduler.model.Utilisateur;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Panneau de gestion des réservations — design cohérent avec AdminPanel.
 */
public class ReservationsPanel extends JPanel {

    // ── Palette identique à AdminPanel ──────────────────────────────────────
    private static final Color BG_PAGE    = new Color(232, 236, 242);
    private static final Color BG_HEADER  = new Color(255, 255, 255);
    private static final Color BG_CARD    = new Color(255, 255, 255);
    private static final Color BG_TABLE   = new Color(255, 255, 255);
    private static final Color BG_ROW_ALT = new Color(245, 248, 252);
    private static final Color BG_SEL     = new Color(59, 130, 246);
    private static final Color ACCENT     = new Color(99, 179, 237);
    private static final Color GREEN      = new Color(34, 197, 94);
    private static final Color RED        = new Color(239, 68, 68);
    private static final Color ORANGE     = new Color(245, 158, 11);
    private static final Color BORDER_C   = new Color(200, 208, 220);
    private static final Color TEXT_PRI   = new Color(15, 23, 42);
    private static final Color TEXT_SEC   = new Color(100, 116, 139);
    private static final Color HDR_BG     = new Color(248, 250, 252);

    // ── État ────────────────────────────────────────────────────────────────
    private final Utilisateur utilisateur;
    private final ReservationDAO reservationDAO = new ReservationDAO();

    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel statsLabel;
    private JLabel filterBadgeLabel;
    private JTextField searchField;
    private JComboBox<String> statutCombo;

    // Popup autocompletion
    private JPopupMenu suggestionPopup;
    private DefaultListModel<String> suggestionModel;
    private JList<String> suggestionList;

    // "TOUS" | "CONFIRMEE" | "EN_ATTENTE" | "ANNULEE"
    private String filtreStatut = "TOUS";
    private String rechercheActuelle = "";

    // Cache pour l'autocompletion (titres + noms salles)
    private final Set<String> suggestionsCache = new LinkedHashSet<>();

    // ── Vue toggle & période ─────────────────────────────────────────────────
    private boolean modeDetail = true;
    private JButton btnDetail, btnSynthese;
    private JComboBox<String> periodeCombo;
    private JTextField debutField, finField;
    private DefaultTableModel syntheseModel;
    private JTable syntheseTable;
    private CardLayout tableCards;
    private JPanel tableContainer;
    private List<Reservation> currentData = new ArrayList<>();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ── Constructeur ────────────────────────────────────────────────────────
    public ReservationsPanel(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        initUI();
        chargerDonnees();
    }

    // ── Construction de l'interface ─────────────────────────────────────────
    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PAGE);
        add(buildPageHeader(),  BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);
    }

    private JPanel buildPageHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_HEADER);
        h.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(16, 22, 16, 22)));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);

        JLabel title = new JLabel("Gestion des Reservations");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRI);

        JLabel sub = new JLabel("Suivi des reservations de salles — en cours et historique");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_SEC);

        text.add(title);
        text.add(Box.createVerticalStrut(2));
        text.add(sub);
        h.add(text, BorderLayout.WEST);

        filterBadgeLabel = new JLabel("Toutes les reservations");
        filterBadgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        filterBadgeLabel.setForeground(ACCENT);
        filterBadgeLabel.setBorder(new EmptyBorder(0, 0, 0, 4));
        h.add(filterBadgeLabel, BorderLayout.EAST);

        return h;
    }

    private JPanel buildMainContent() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(BG_PAGE);
        panel.add(buildToolbar(),   BorderLayout.NORTH);
        panel.add(buildTableArea(), BorderLayout.CENTER);
        panel.add(buildStatusBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(0, 0));
        toolbar.setBackground(BG_CARD);
        toolbar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(8, 14, 8, 14)));

        // ── Groupe gauche : filtre statut + bouton ajouter ───────────────────
        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftGroup.setOpaque(false);

        JLabel filtreLabel = new JLabel("Statut :");
        filtreLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filtreLabel.setForeground(TEXT_SEC);
        leftGroup.add(filtreLabel);

        // ComboBox statut stylée
        String[] statutItems = {
            "Tous les statuts",
            "Confirmee",
            "En attente",
            "Annulee",
            "Terminee"
        };
        statutCombo = new JComboBox<>(statutItems) {
            @Override public void updateUI() {
                super.updateUI();
                setBackground(BG_CARD);
            }
        };
        statutCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statutCombo.setBackground(BG_CARD);
        statutCombo.setForeground(TEXT_PRI);
        statutCombo.setFocusable(false);
        statutCombo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        statutCombo.setRenderer(new StatutComboRenderer());
        statutCombo.setBorder(BorderFactory.createLineBorder(BORDER_C, 1, true));
        statutCombo.setPreferredSize(new Dimension(130, 28));
        statutCombo.addActionListener(e -> {
            int idx = statutCombo.getSelectedIndex();
            switch (idx) {
                case 1: filtreStatut = "CONFIRMEE";  break;
                case 2: filtreStatut = "EN_ATTENTE"; break;
                case 3: filtreStatut = "ANNULEE";    break;
                case 4: filtreStatut = "TERMINEE";   break;
                default: filtreStatut = "TOUS";       break;
            }
            updateBadge();
            chargerDonnees();
        });
        leftGroup.add(statutCombo);

        // Séparateur
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 22));
        sep.setForeground(BORDER_C);
        leftGroup.add(Box.createHorizontalStrut(2));
        leftGroup.add(sep);
        leftGroup.add(Box.createHorizontalStrut(2));

        JButton addBtn = actionBtn("+ Nouveau", GREEN);
        addBtn.setPreferredSize(new Dimension(90, 28));
        addBtn.addActionListener(e -> showNouvelleReservationDialog());
        leftGroup.add(addBtn);

        // Séparateur
        JSeparator sep2 = new JSeparator(SwingConstants.VERTICAL);
        sep2.setPreferredSize(new Dimension(1, 22));
        sep2.setForeground(BORDER_C);
        leftGroup.add(Box.createHorizontalStrut(2));
        leftGroup.add(sep2);
        leftGroup.add(Box.createHorizontalStrut(2));

        JButton editBtn = actionBtn("✎ Modifier", ACCENT);
        editBtn.setPreferredSize(new Dimension(90, 28));
        editBtn.addActionListener(e -> modifierSelection());
        leftGroup.add(editBtn);

        JButton deleteBtn = actionBtn("✕ Supprimer", RED);
        deleteBtn.setPreferredSize(new Dimension(90, 28));
        deleteBtn.addActionListener(e -> supprimerSelection());
        leftGroup.add(deleteBtn);

        // ── Groupe droit : recherche avec autocompletion ─────────────────────
        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightGroup.setOpaque(false);

        searchField = new JTextField(16);
        styleField(searchField);
        searchField.setPreferredSize(new Dimension(160, 28));

        // Construction du popup d'autocompletion
        suggestionModel = new DefaultListModel<>();
        suggestionList  = new JList<>(suggestionModel);
        suggestionList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        suggestionList.setForeground(TEXT_PRI);
        suggestionList.setBackground(BG_CARD);
        suggestionList.setSelectionBackground(BG_SEL);
        suggestionList.setSelectionForeground(Color.WHITE);
        suggestionList.setFixedCellHeight(26);
        suggestionList.setBorder(new EmptyBorder(2, 8, 2, 8));

        suggestionPopup = new JPopupMenu();
        suggestionPopup.setBorder(BorderFactory.createLineBorder(BORDER_C, 1));
        suggestionPopup.setLayout(new BorderLayout());
        JScrollPane suggScroll = new JScrollPane(suggestionList);
        suggScroll.setBorder(null);
        suggScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        suggestionPopup.add(suggScroll, BorderLayout.CENTER);

        // Listener frappe → suggestions en temps réel
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { afficherSuggestions(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { afficherSuggestions(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { afficherSuggestions(); }
        });

        // Naviguer dans les suggestions avec les flèches
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && suggestionPopup.isVisible()) {
                    int next = Math.min(suggestionList.getSelectedIndex() + 1, suggestionModel.getSize() - 1);
                    suggestionList.setSelectedIndex(next);
                    suggestionList.ensureIndexIsVisible(next);
                } else if (e.getKeyCode() == KeyEvent.VK_UP && suggestionPopup.isVisible()) {
                    int prev = Math.max(suggestionList.getSelectedIndex() - 1, 0);
                    suggestionList.setSelectedIndex(prev);
                    suggestionList.ensureIndexIsVisible(prev);
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (suggestionPopup.isVisible() && suggestionList.getSelectedValue() != null) {
                        searchField.setText(suggestionList.getSelectedValue());
                        suggestionPopup.setVisible(false);
                    }
                    effectuerRecherche();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    suggestionPopup.setVisible(false);
                }
            }
        });

        // Clic sur une suggestion
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                String selected = suggestionList.getSelectedValue();
                if (selected != null) {
                    searchField.setText(selected);
                    suggestionPopup.setVisible(false);
                    effectuerRecherche();
                }
            }
        });

        JButton searchBtn = actionBtn("Rechercher", ACCENT);
        searchBtn.setPreferredSize(new Dimension(95, 28));
        searchBtn.addActionListener(e -> { suggestionPopup.setVisible(false); effectuerRecherche(); });

        JButton resetBtn = actionBtn("Reinitialiser", TEXT_SEC);
        resetBtn.setPreferredSize(new Dimension(95, 28));
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            rechercheActuelle = "";
            suggestionPopup.setVisible(false);
            chargerDonnees();
        });

        rightGroup.add(new JLabel("🔍"));
        rightGroup.add(searchField);
        rightGroup.add(searchBtn);
        rightGroup.add(resetBtn);

        toolbar.add(leftGroup,  BorderLayout.WEST);
        toolbar.add(rightGroup, BorderLayout.EAST);

        // ── Ligne 2 : période + toggle + exports ────────────────────────────
        JPanel line2 = new JPanel(new BorderLayout(0, 0));
        line2.setOpaque(false);
        line2.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_C),
            new EmptyBorder(7, 0, 0, 0)));

        JPanel line2Left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        line2Left.setOpaque(false);

        periodeCombo = new JComboBox<>(new String[]{
            "Cette semaine", "Semaine precedente", "Ce mois", "Mois precedent",
            "Les 3 derniers mois", "Cette annee", "Toutes les dates"
        });
        periodeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        periodeCombo.setBackground(BG_CARD);
        periodeCombo.setForeground(TEXT_PRI);
        periodeCombo.setPreferredSize(new Dimension(175, 28));
        periodeCombo.setSelectedIndex(6); // "Toutes les dates" par défaut
        periodeCombo.addActionListener(e -> {
            if (periodeCombo.getSelectedIndex() < 6) appliquerPeriode();
            else { debutField.setText(""); finField.setText(""); }
            chargerDonnees();
        });

        debutField = new JTextField(8);
        finField   = new JTextField(8);
        styleField(debutField); styleField(finField);
        debutField.setPreferredSize(new Dimension(88, 28));
        finField.setPreferredSize(new Dimension(88, 28));
        debutField.setToolTipText("dd/MM/yyyy");
        finField.setToolTipText("dd/MM/yyyy");

        // Toggle Détail / Synthèse
        btnDetail   = toggleBtn("Detail",   true);
        btnSynthese = toggleBtn("Synthese", false);
        btnDetail.addActionListener(e   -> switchMode(true));
        btnSynthese.addActionListener(e -> switchMode(false));

        line2Left.add(new JLabel("Periode :") {{ setFont(new Font("Segoe UI", Font.PLAIN, 12)); setForeground(TEXT_SEC); }});
        line2Left.add(periodeCombo);
        line2Left.add(new JLabel("Du :") {{ setFont(new Font("Segoe UI", Font.PLAIN, 12)); setForeground(TEXT_SEC); }});
        line2Left.add(debutField);
        line2Left.add(new JLabel("au :") {{ setFont(new Font("Segoe UI", Font.PLAIN, 12)); setForeground(TEXT_SEC); }});
        line2Left.add(finField);

        JSeparator sepT = new JSeparator(SwingConstants.VERTICAL);
        sepT.setPreferredSize(new Dimension(1, 22)); sepT.setForeground(BORDER_C);
        line2Left.add(Box.createHorizontalStrut(4));
        line2Left.add(sepT);
        line2Left.add(Box.createHorizontalStrut(4));
        line2Left.add(btnDetail);
        line2Left.add(btnSynthese);

        JPanel line2Right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        line2Right.setOpaque(false);
        JButton excelBtn = actionBtn("\u2b07 Excel (CSV)", GREEN);
        excelBtn.setPreferredSize(new Dimension(110, 28));
        excelBtn.addActionListener(e -> exportCsv());
        JButton pdfBtn = actionBtn("\u2b07 PDF", RED);
        pdfBtn.setPreferredSize(new Dimension(70, 28));
        pdfBtn.addActionListener(e -> exportPdf());
        line2Right.add(excelBtn);
        line2Right.add(pdfBtn);

        line2.add(line2Left,  BorderLayout.WEST);
        line2.add(line2Right, BorderLayout.EAST);

        JPanel toolbarWrapper = new JPanel(new BorderLayout(0, 0));
        toolbarWrapper.setBackground(BG_CARD);
        toolbarWrapper.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(8, 14, 8, 14)));
        toolbar.setBorder(null); // enlever la bordure de toolbar (déjà sur wrapper)
        toolbarWrapper.add(toolbar, BorderLayout.NORTH);
        toolbarWrapper.add(line2,   BorderLayout.SOUTH);
        return toolbarWrapper;
    }

    private JButton toggleBtn(String text, boolean leftSide) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                boolean active = (text.equals("Detail") && modeDetail) ||
                                 (text.equals("Synthese") && !modeDetail);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? BG_SEL : BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(active ? BG_SEL : BORDER_C);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(leftSide ? Color.WHITE : TEXT_SEC);
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 16, 5, 16));
        return btn;
    }

    private void switchMode(boolean detail) {
        modeDetail = detail;
        tableCards.show(tableContainer, detail ? "detail" : "synthese");
        btnDetail.setForeground(detail   ? Color.WHITE : TEXT_SEC);
        btnSynthese.setForeground(!detail ? Color.WHITE : TEXT_SEC);
        btnDetail.repaint();
        btnSynthese.repaint();
        updateStats(modeDetail ? tableModel.getRowCount() : syntheseModel.getRowCount());
    }

    private void appliquerPeriode() {
        LocalDate today = LocalDate.now();
        LocalDate debut, fin;
        switch (periodeCombo.getSelectedIndex()) {
            case 0: debut = today.with(DayOfWeek.MONDAY);               fin = debut.plusDays(6); break;
            case 1: debut = today.minusWeeks(1).with(DayOfWeek.MONDAY); fin = debut.plusDays(6); break;
            case 2: debut = today.withDayOfMonth(1);                    fin = today.with(TemporalAdjusters.lastDayOfMonth()); break;
            case 3: LocalDate lm = today.minusMonths(1); debut = lm.withDayOfMonth(1); fin = lm.with(TemporalAdjusters.lastDayOfMonth()); break;
            case 4: debut = today.minusMonths(3);                       fin = today; break;
            case 5: debut = today.withDayOfYear(1);                     fin = today.with(TemporalAdjusters.lastDayOfYear()); break;
            default: debutField.setText(""); finField.setText(""); return;
        }
        debutField.setText(debut.format(DATE_FMT));
        finField.setText(fin.format(DATE_FMT));
    }

    private LocalDate parseDateField(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return LocalDate.parse(s.trim(), DATE_FMT); } catch (Exception e) { return null; }
    }

    /** Affiche les suggestions filtrées selon la saisie (insensible à la casse). */
    private void afficherSuggestions() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }
        String lower = text.toLowerCase();
        List<String> matches = suggestionsCache.stream()
            .filter(s -> s.toLowerCase().contains(lower))
            .sorted()
            .limit(8)
            .collect(Collectors.toList());

        if (matches.isEmpty()) {
            suggestionPopup.setVisible(false);
            return;
        }
        suggestionModel.clear();
        matches.forEach(suggestionModel::addElement);
        suggestionList.setSelectedIndex(0);

        int popupH = Math.min(matches.size() * 26 + 6, 200);
        suggestionPopup.setPreferredSize(new Dimension(searchField.getWidth(), popupH));

        if (!suggestionPopup.isVisible()) {
            suggestionPopup.show(searchField, 0, searchField.getHeight());
            searchField.requestFocusInWindow();
        } else {
            suggestionPopup.revalidate();
            suggestionPopup.repaint();
        }
    }

    private JPanel buildTableArea() {
        String[] cols = {"ID", "Titre", "Salle", "Date", "Heure debut", "Heure fin", "Statut", "Reserve par"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = buildTable(tableModel);

        // Renderer Statut avec badge coloré
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String v = val != null ? val.toString() : "";
                Color c;
                switch (v) {
                    case "CONFIRMEE":  c = GREEN;    v = "Confirmee";  break;
                    case "EN_ATTENTE": c = ORANGE;   v = "En attente"; break;
                    case "ANNULEE":    c = RED;       v = "Annulee";    break;
                    case "TERMINEE":   c = new Color(100, 116, 139); v = "Terminee"; break;
                    default:           c = TEXT_SEC;
                }
                l.setText(v);
                l.setForeground(c);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setBackground(sel ? BG_SEL : (row % 2 == 0 ? BG_TABLE : BG_ROW_ALT));
                l.setOpaque(true);
                l.setBorder(new EmptyBorder(0, 10, 0, 0));
                return l;
            }
        });

        hideCol(table, 0);

        // Menu contextuel
        JPopupMenu popup = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Modifier cette reservation");
        editItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        editItem.setForeground(ACCENT);
        editItem.addActionListener(e -> modifierSelection());
        popup.add(editItem);
        JMenuItem deleteItem = new JMenuItem("Supprimer cette reservation");
        deleteItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        deleteItem.setForeground(RED);
        deleteItem.addActionListener(e -> supprimerSelection());
        popup.add(deleteItem);
        table.setComponentPopupMenu(popup);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) table.setRowSelectionInterval(row, row);
            }
        });

        JScrollPane detailScroll = new JScrollPane(table);
        detailScroll.setBorder(null);
        detailScroll.getViewport().setBackground(BG_TABLE);

        // ── Table Synthèse ────────────────────────────────────────────────────
        syntheseModel = new DefaultTableModel(
                new String[]{"Salle", "Nb reservations", "Heures utilisees", "Taux utilisation"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        syntheseTable = buildTable(syntheseModel);
        syntheseTable.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JPanel cell = new JPanel(new BorderLayout(8, 0));
                cell.setOpaque(true);
                cell.setBackground(sel ? BG_SEL : (row % 2 == 0 ? BG_TABLE : BG_ROW_ALT));
                cell.setBorder(new EmptyBorder(4, 10, 4, 10));
                String txt = val != null ? val.toString() : "0%";
                int pctParsed = 0;
                try { pctParsed = Integer.parseInt(txt.replace("%","").trim()); } catch (Exception ignored) {}
                final int pct = Math.min(pctParsed, 100);
                JLabel lbl = new JLabel(txt);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                lbl.setForeground(sel ? Color.WHITE : (pct > 70 ? GREEN : pct > 30 ? ORANGE : TEXT_SEC));
                JPanel bar = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        int w = (int)(getWidth() * pct / 100.0);
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(226, 232, 240));
                        g2.fillRoundRect(0, 2, getWidth(), getHeight()-4, 4, 4);
                        if (w > 0) {
                            Color c = pct > 70 ? GREEN : pct > 30 ? ORANGE : new Color(59,130,246);
                            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), sel ? 200 : 140));
                            g2.fillRoundRect(0, 2, w, getHeight()-4, 4, 4);
                        }
                        g2.dispose();
                    }
                };
                bar.setOpaque(false); bar.setPreferredSize(new Dimension(90, 16));
                cell.add(bar, BorderLayout.CENTER); cell.add(lbl, BorderLayout.EAST);
                return cell;
            }
        });
        JScrollPane syntheseScroll = new JScrollPane(syntheseTable);
        syntheseScroll.setBorder(null);
        syntheseScroll.getViewport().setBackground(BG_TABLE);

        // ── CardLayout ────────────────────────────────────────────────────────
        tableCards     = new CardLayout();
        tableContainer = new JPanel(tableCards);
        tableContainer.setBackground(BG_PAGE);
        tableContainer.add(detailScroll,   "detail");
        tableContainer.add(syntheseScroll, "synthese");

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_PAGE);
        wrapper.setBorder(new EmptyBorder(12, 14, 0, 14));
        wrapper.add(tableContainer, BorderLayout.CENTER);
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

    // ── Chargement des données ───────────────────────────────────────────────
    private void chargerDonnees() {
        final LocalDate filtreDebut = parseDateField(debutField != null ? debutField.getText() : "");
        final LocalDate filtreFin   = parseDateField(finField   != null ? finField.getText()   : "");

        new SwingWorker<List<Reservation>, Void>() {
            @Override protected List<Reservation> doInBackground() throws Exception {
                List<Reservation> all;
                if (!rechercheActuelle.isEmpty()) {
                    all = reservationDAO.search(rechercheActuelle);
                } else {
                    all = reservationDAO.findAll();
                }
                // Filtre statut
                if (!"TOUS".equals(filtreStatut)) {
                    all = all.stream()
                        .filter(r -> r.getStatut() != null &&
                                     r.getStatut().name().equals(filtreStatut))
                        .collect(Collectors.toList());
                }
                // Filtre période
                if (filtreDebut != null) {
                    all = all.stream()
                        .filter(r -> r.getDateReservation() != null &&
                                     !r.getDateReservation().isBefore(filtreDebut))
                        .collect(Collectors.toList());
                }
                if (filtreFin != null) {
                    all = all.stream()
                        .filter(r -> r.getDateReservation() != null &&
                                     !r.getDateReservation().isAfter(filtreFin))
                        .collect(Collectors.toList());
                }
                return all;
            }
            @Override protected void done() {
                try {
                    List<Reservation> list = get();
                    currentData = list;
                    tableModel.setRowCount(0);
                    suggestionsCache.clear();
                    for (Reservation r : list) {
                        String date  = r.getDateReservation() != null ? r.getDateReservation().format(DATE_FMT) : "-";
                        String debut = r.getHeureDebut()      != null ? r.getHeureDebut().format(TIME_FMT) : "-";
                        String fin   = r.getHeureFin()        != null ? r.getHeureFin().format(TIME_FMT)   : "-";
                        tableModel.addRow(new Object[]{
                            r.getId(), r.getTitre(),
                            r.getNomSalle()       != null ? r.getNomSalle()       : "Salle #" + r.getSalleId(),
                            date, debut, fin,
                            r.getStatut()         != null ? r.getStatut().name() : "CONFIRMEE",
                            r.getNomUtilisateur() != null ? r.getNomUtilisateur() : "-"
                        });
                        if (r.getTitre()    != null) suggestionsCache.add(r.getTitre());
                        if (r.getNomSalle() != null) suggestionsCache.add(r.getNomSalle());
                    }
                    // ── Peupler la synthèse ───────────────────────────────────
                    if (syntheseModel != null) {
                        syntheseModel.setRowCount(0);
                        java.util.Map<String, int[]> bySalle = new java.util.LinkedHashMap<>();
                        for (Reservation r : list) {
                            String salle = r.getNomSalle() != null ? r.getNomSalle() : "Salle inconnue";
                            bySalle.putIfAbsent(salle, new int[]{0, 0});
                            bySalle.get(salle)[0]++;
                            if (r.getHeureDebut() != null && r.getHeureFin() != null) {
                                int min = (int) Duration.between(r.getHeureDebut(), r.getHeureFin()).toMinutes();
                                if (min > 0) bySalle.get(salle)[1] += min;
                            }
                        }
                        long nbJours = (filtreDebut != null && filtreFin != null)
                            ? Math.max(1, ChronoUnit.DAYS.between(filtreDebut, filtreFin) + 1) : 30;
                        int hTheo = (int)(Math.max(1, nbJours / 7) * 5 * 8);
                        bySalle.entrySet().stream()
                            .sorted((a, b) -> b.getValue()[0] - a.getValue()[0])
                            .forEach(e -> {
                                int nb  = e.getValue()[0];
                                int min = e.getValue()[1];
                                double h = min / 60.0;
                                int taux = hTheo == 0 ? 0 : Math.min((int)(h / hTheo * 100), 100);
                                syntheseModel.addRow(new Object[]{
                                    e.getKey(), nb,
                                    String.format("%.1fh (%dmin)", h, min),
                                    taux + "%"
                                });
                            });
                    }
                    updateStats(modeDetail ? list.size() : (syntheseModel != null ? syntheseModel.getRowCount() : 0));
                } catch (Exception ex) {
                    statsLabel.setText("Erreur : " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void effectuerRecherche() {
        rechercheActuelle = searchField.getText().trim();
        updateBadge();
        chargerDonnees();
    }

    private void updateBadge() {
        String statLabel;
        switch (filtreStatut) {
            case "CONFIRMEE":  statLabel = "Confirmees";  break;
            case "EN_ATTENTE": statLabel = "En attente";  break;
            case "ANNULEE":    statLabel = "Annulees";    break;
            case "TERMINEE":   statLabel = "Terminees";   break;
            default:           statLabel = "Toutes";      break;
        }
        if (!rechercheActuelle.isEmpty()) {
            filterBadgeLabel.setText(statLabel + "  —  \"" + rechercheActuelle + "\"");
        } else {
            filterBadgeLabel.setText("Reservations : " + statLabel);
        }
    }

    private void updateStats(int count) {
        String label;
        if (modeDetail) {
            if (!rechercheActuelle.isEmpty()) {
                label = count + " resultat(s) pour \"" + rechercheActuelle + "\"";
            } else {
                switch (filtreStatut) {
                    case "CONFIRMEE":  label = count + " reservation(s) confirmee(s)";  break;
                    case "EN_ATTENTE": label = count + " reservation(s) en attente";    break;
                    case "ANNULEE":    label = count + " reservation(s) annulee(s)";    break;
                    case "TERMINEE":   label = count + " reservation(s) terminee(s)";   break;
                    default:           label = count + " reservation(s) au total";      break;
                }
            }
        } else {
            label = count + " salle(s) — vue synthese";
        }
        LocalDate d1 = parseDateField(debutField != null ? debutField.getText() : "");
        LocalDate d2 = parseDateField(finField   != null ? finField.getText()   : "");
        if (d1 != null && d2 != null)
            label += "  ·  " + d1.format(DATE_FMT) + " \u2192 " + d2.format(DATE_FMT);
        statsLabel.setText(label);
    }

    // ── Export CSV ────────────────────────────────────────────────────────────
    private void exportCsv() {
        DefaultTableModel model = modeDetail ? tableModel : syntheseModel;
        if (model == null || model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Aucune donnee a exporter.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File((modeDetail ? "reservations_" : "synthese_salles_")
            + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv"));
        fc.setFileFilter(new FileNameExtensionFilter("Fichier CSV (Excel)", "csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv"))
            file = new File(file.getAbsolutePath() + ".csv");
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            pw.print('\uFEFF');
            int startCol = modeDetail ? 1 : 0; // skip ID column in detail mode
            StringBuilder hdr = new StringBuilder();
            for (int c = startCol; c < model.getColumnCount(); c++) {
                if (c > startCol) hdr.append(";");
                hdr.append(model.getColumnName(c));
            }
            pw.println(hdr);
            for (int r = 0; r < model.getRowCount(); r++) {
                StringBuilder row = new StringBuilder();
                for (int c = startCol; c < model.getColumnCount(); c++) {
                    if (c > startCol) row.append(";");
                    Object v = model.getValueAt(r, c);
                    row.append(v != null ? v.toString().replace(";", ",") : "");
                }
                pw.println(row);
            }
            int res = JOptionPane.showConfirmDialog(this,
                "Exporte : " + file.getAbsolutePath() + "\n\nOuvrir ?",
                "Export reussi", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (res == JOptionPane.YES_OPTION)
                try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {}
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Export PDF ────────────────────────────────────────────────────────────
    private void exportPdf() {
        JTable srcTable = modeDetail ? table : syntheseTable;
        DefaultTableModel model = modeDetail ? tableModel : syntheseModel;
        if (model == null || model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Aucune donnee a exporter.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File((modeDetail ? "reservations_" : "synthese_salles_")
            + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf"));
        fc.setFileFilter(new FileNameExtensionFilter("PDF", "pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf"))
            file = new File(file.getAbsolutePath() + ".pdf");

        int nbColVis = 0;
        for (int c = 0; c < srcTable.getColumnCount(); c++)
            if (srcTable.getColumnModel().getColumn(c).getWidth() > 0) nbColVis++;
        int colW = 155, mg = 24;
        int imgW = Math.max(nbColVis * colW + mg * 2, 900);
        int rowH = 28, imgH = (model.getRowCount() + 1) * rowH + 90;

        java.awt.image.BufferedImage img =
            new java.awt.image.BufferedImage(imgW, imgH, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE); g.fillRect(0, 0, imgW, imgH);
        String titre = modeDetail ? "Historique des reservations" : "Utilisation des salles";
        g.setFont(new Font("Segoe UI", Font.BOLD, 18)); g.setColor(TEXT_PRI);
        g.drawString(titre, mg, 30);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11)); g.setColor(TEXT_SEC);
        g.drawString(statsLabel.getText(), mg, 48);
        g.drawString("Genere le " + LocalDate.now().format(DATE_FMT) + "  |  UNIV-SCHEDULER", mg, 64);
        int y = 78, x = mg;
        g.setColor(HDR_BG); g.fillRect(mg, y, imgW - mg*2, rowH);
        g.setColor(BORDER_C); g.drawRect(mg, y, imgW - mg*2, rowH);
        g.setFont(new Font("Segoe UI", Font.BOLD, 11)); g.setColor(TEXT_PRI);
        for (int c = 0; c < srcTable.getColumnCount(); c++) {
            if (srcTable.getColumnModel().getColumn(c).getWidth() == 0) continue;
            g.drawString(srcTable.getColumnName(c), x + 6, y + rowH - 9); x += colW;
        }
        y += rowH;
        g.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        for (int r = 0; r < model.getRowCount(); r++) {
            g.setColor(r % 2 == 0 ? Color.WHITE : new Color(245, 248, 252));
            g.fillRect(mg, y, imgW - mg*2, rowH);
            g.setColor(BORDER_C); g.drawRect(mg, y, imgW - mg*2, rowH);
            g.setColor(TEXT_PRI); x = mg;
            for (int c = 0; c < srcTable.getColumnCount(); c++) {
                if (srcTable.getColumnModel().getColumn(c).getWidth() == 0) continue;
                Object v = model.getValueAt(r, c);
                String txt = v != null ? v.toString() : "";
                if (txt.length() > 22) txt = txt.substring(0, 20) + "..";
                g.drawString(txt, x + 6, y + rowH - 9); x += colW;
            }
            y += rowH;
        }
        g.dispose();
        try {
            writePdfImage(img, file);
            int res = JOptionPane.showConfirmDialog(this,
                "Exporte : " + file.getAbsolutePath() + "\n\nOuvrir ?",
                "Export reussi", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (res == JOptionPane.YES_OPTION)
                try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ignored) {}
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erreur PDF : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void writePdfImage(java.awt.image.BufferedImage img, File file) throws IOException {
        java.io.ByteArrayOutputStream jpegOut = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpeg", jpegOut);
        byte[] jpeg = jpegOut.toByteArray();
        int iW = img.getWidth(), iH = img.getHeight(), pW = 842, pH = (int)(pW * (double) iH / iW);
        try (FileOutputStream fos = new FileOutputStream(file);
             PdfCOS cos = new PdfCOS(fos)) {
            List<Long> off = new ArrayList<>();
            pdfW(cos, "%PDF-1.4\n");
            off.add(cos.n()); pdfW(cos, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
            off.add(cos.n()); pdfW(cos, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
            off.add(cos.n()); pdfW(cos, "3 0 obj\n<< /Type /Page /Parent 2 0 R\n/MediaBox [0 0 " + pW + " " + pH +
                "]\n/Contents 4 0 R\n/Resources << /XObject << /Im0 5 0 R >> >>\n>>\nendobj\n");
            byte[] ct = ("q\n" + pW + " 0 0 " + pH + " 0 0 cm\n/Im0 Do\nQ\n").getBytes("ISO-8859-1");
            off.add(cos.n()); pdfW(cos, "4 0 obj\n<< /Length " + ct.length + " >>\nstream\n");
            cos.write(ct); pdfW(cos, "\nendstream\nendobj\n");
            off.add(cos.n()); pdfW(cos, "5 0 obj\n<< /Type /XObject /Subtype /Image\n/Width " + iW +
                " /Height " + iH + "\n/ColorSpace /DeviceRGB\n/BitsPerComponent 8\n/Filter /DCTDecode\n/Length " +
                jpeg.length + "\n>>\nstream\n");
            cos.write(jpeg); pdfW(cos, "\nendstream\nendobj\n");
            long xref = cos.n();
            pdfW(cos, "xref\n0 6\n0000000000 65535 f \r\n");
            for (Long o : off) pdfW(cos, String.format("%010d 00000 n \r\n", o));
            pdfW(cos, "trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%%%EOF\n");
        }
    }
    private void pdfW(OutputStream os, String s) throws IOException { os.write(s.getBytes("ISO-8859-1")); }
    private static class PdfCOS extends FilterOutputStream {
        private long cnt = 0;
        PdfCOS(OutputStream out) { super(out); }
        @Override public void write(int b) throws IOException { out.write(b); cnt++; }
        @Override public void write(byte[] b, int o, int l) throws IOException { out.write(b,o,l); cnt+=l; }
        long n() { return cnt; }
    }

    private void supprimerSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Veuillez selectionner une reservation.", "Aucune selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        int id = (int) tableModel.getValueAt(modelRow, 0);
        String titre = tableModel.getValueAt(modelRow, 1).toString();
        String statut = tableModel.getValueAt(modelRow, 6).toString();
        if ("TERMINEE".equals(statut)) {
            JOptionPane.showMessageDialog(this,
                "Impossible de supprimer une reservation terminee.",
                "Action interdite", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Supprimer la reservation : \"" + titre + "\" ?",
            "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                reservationDAO.delete(id); return null;
            }
            @Override protected void done() {
                try { get(); chargerDonnees(); }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(ReservationsPanel.this,
                        "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ── Modification d'une réservation ──────────────────────────────────────
    private void modifierSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this,
                "Veuillez selectionner une reservation.", "Aucune selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String statut = tableModel.getValueAt(modelRow, 6).toString();
        if ("TERMINEE".equals(statut)) {
            JOptionPane.showMessageDialog(this,
                "Impossible de modifier une reservation terminee.",
                "Action interdite", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id    = (int) tableModel.getValueAt(modelRow, 0);
        String titre = tableModel.getValueAt(modelRow, 1).toString();
        String salle = tableModel.getValueAt(modelRow, 2).toString();
        String date  = tableModel.getValueAt(modelRow, 3).toString();
        String debut = tableModel.getValueAt(modelRow, 4).toString();
        String fin   = tableModel.getValueAt(modelRow, 5).toString();
        showModifierReservationDialog(id, titre, salle, date, debut, fin, statut);
    }

    private void showModifierReservationDialog(int id, String titreInit, String salleInit,
            String dateInit, String debutInit, String finInit, String statutInit) {
        JDialog dlg = darkDialog("Modifier la reservation", 480, 460);
        JPanel form = formPanel();

        JTextField titreField = darkField(); titreField.setText(titreInit);
        JTextField descField  = darkField();

        JComboBox<String> salleCombo = new JComboBox<>();
        styleCombo(salleCombo);
        int salleIdPreselect = -1;
        try {
            java.util.List<univscheduler.model.Salle> salles = new SalleDAO().findAll();
            for (univscheduler.model.Salle s : salles) {
                String item = s.getId() + " | " + s.getNumero()
                    + (s.getNom() != null ? " - " + s.getNom() : "");
                salleCombo.addItem(item);
                // Pré-sélectionner la salle courante
                if (salleInit != null && salleInit.contains(s.getNumero())) {
                    salleCombo.setSelectedItem(item);
                    salleIdPreselect = s.getId();
                }
            }
        } catch (Exception e) { salleCombo.addItem("Erreur chargement"); }

        JTextField dateField  = darkField();
        // Convertir dd/MM/yyyy → yyyy-MM-dd pour l'édition
        try {
            LocalDate d = LocalDate.parse(dateInit, DATE_FMT);
            dateField.setText(d.toString());
        } catch (Exception e) { dateField.setText(dateInit); }

        JTextField debutField = darkField(); debutField.setText(debutInit);
        JTextField finField   = darkField(); finField.setText(finInit);

        // ComboBox statut (sans TERMINEE exclue si la resa n'est pas terminée — déjà vérifié)
        JComboBox<String> statutEdit = new JComboBox<>(new String[]{
            "CONFIRMEE", "EN_ATTENTE", "ANNULEE", "TERMINEE"
        });
        styleCombo(statutEdit);
        statutEdit.setSelectedItem(statutInit);

        addFormRow(form, "Titre *",               titreField);
        addFormRow(form, "Description",           descField);
        addFormRow(form, "Salle *",               salleCombo);
        addFormRow(form, "Date (AAAA-MM-JJ) *",   dateField);
        addFormRow(form, "Heure debut (HH:mm) *", debutField);
        addFormRow(form, "Heure fin (HH:mm) *",   finField);
        addFormRow(form, "Statut *",              statutEdit);

        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLbl.setBorder(new EmptyBorder(6, 0, 0, 0));
        form.add(statusLbl);

        final int finalSalleIdPreselect = salleIdPreselect;
        JButton save = actionBtn("Enregistrer les modifications", ACCENT);
        save.addActionListener(e -> {
            String titre = titreField.getText().trim();
            if (titre.isEmpty()) { statusLbl.setForeground(RED); statusLbl.setText("Le titre est obligatoire."); return; }
            try {
                String salleStr = salleCombo.getSelectedItem().toString();
                int salleId = Integer.parseInt(salleStr.split(" \\| ")[0].trim());
                LocalDate date2  = LocalDate.parse(dateField.getText().trim());
                LocalTime debut2 = LocalTime.parse(debutField.getText().trim());
                LocalTime fin2   = LocalTime.parse(finField.getText().trim());
                if (!fin2.isAfter(debut2)) {
                    statusLbl.setForeground(RED); statusLbl.setText("L'heure de fin doit etre superieure au debut."); return;
                }
                Reservation r = new Reservation();
                r.setId(id);
                r.setSalleId(salleId);
                r.setUtilisateurId(utilisateur != null ? utilisateur.getId() : 1);
                r.setTitre(titre);
                r.setDescription(descField.getText().trim());
                r.setDateReservation(date2);
                r.setHeureDebut(debut2);
                r.setHeureFin(fin2);
                r.setStatut(Reservation.Statut.valueOf(statutEdit.getSelectedItem().toString()));
                reservationDAO.update(r);
                dlg.dispose();
                chargerDonnees();
            } catch (Exception ex) {
                statusLbl.setForeground(RED); statusLbl.setText("Erreur : " + ex.getMessage());
            }
        });

        dlg.getContentPane().add(form,              BorderLayout.CENTER);
        dlg.getContentPane().add(btnRow(dlg, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── Dialogue nouvelle réservation ────────────────────────────────────────
    private void showNouvelleReservationDialog() {
        JDialog dlg = darkDialog("Nouvelle reservation", 480, 400);
        JPanel form = formPanel();

        JTextField titreField = darkField();
        JTextField descField  = darkField();

        JComboBox<String> salleCombo = new JComboBox<>();
        styleCombo(salleCombo);
        try {
            for (Salle s : new SalleDAO().findAll())
                salleCombo.addItem(s.getId() + " | " + s.getNumero()
                    + (s.getNom() != null ? " - " + s.getNom() : ""));
        } catch (Exception e) { salleCombo.addItem("Erreur chargement"); }

        JTextField dateField  = darkField(); dateField.setText(LocalDate.now().toString());
        JTextField debutField = darkField(); debutField.setText("08:00");
        JTextField finField   = darkField(); finField.setText("10:00");

        addFormRow(form, "Titre *",               titreField);
        addFormRow(form, "Description",           descField);
        addFormRow(form, "Salle *",               salleCombo);
        addFormRow(form, "Date (AAAA-MM-JJ) *",   dateField);
        addFormRow(form, "Heure debut (HH:mm) *", debutField);
        addFormRow(form, "Heure fin (HH:mm) *",   finField);

        JLabel statusLbl = new JLabel(" ");
        statusLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLbl.setBorder(new EmptyBorder(6, 0, 0, 0));
        form.add(statusLbl);

        JButton save = actionBtn("Confirmer la reservation", GREEN);
        save.addActionListener(e -> {
            String titre = titreField.getText().trim();
            if (titre.isEmpty()) { statusLbl.setForeground(RED); statusLbl.setText("Le titre est obligatoire."); return; }
            try {
                String salleStr = salleCombo.getSelectedItem().toString();
                int salleId = Integer.parseInt(salleStr.split(" \\| ")[0].trim());
                LocalDate date  = LocalDate.parse(dateField.getText().trim());
                LocalTime debut = LocalTime.parse(debutField.getText().trim());
                LocalTime fin   = LocalTime.parse(finField.getText().trim());
                if (!fin.isAfter(debut)) {
                    statusLbl.setForeground(RED); statusLbl.setText("L'heure de fin doit etre superieure au debut."); return;
                }
                Reservation r = new Reservation();
                r.setSalleId(salleId);
                r.setUtilisateurId(utilisateur != null ? utilisateur.getId() : 1);
                r.setTitre(titre);
                r.setDescription(descField.getText().trim());
                r.setDateReservation(date);
                r.setHeureDebut(debut);
                r.setHeureFin(fin);
                r.setStatut(Reservation.Statut.CONFIRMEE);
                reservationDAO.save(r);
                dlg.dispose();
                chargerDonnees();
            } catch (Exception ex) {
                statusLbl.setForeground(RED); statusLbl.setText("Erreur : " + ex.getMessage());
            }
        });

        dlg.getContentPane().add(form,              BorderLayout.CENTER);
        dlg.getContentPane().add(btnRow(dlg, save), BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── Renderer personnalisé pour le ComboBox statut ────────────────────────
    private class StatutComboRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            l.setBorder(new EmptyBorder(4, 10, 4, 10));
            l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            if (isSelected) {
                l.setBackground(BG_SEL);
                l.setForeground(Color.WHITE);
            } else {
                l.setBackground(BG_CARD);
                String v = value != null ? value.toString() : "";
                switch (v) {
                    case "Confirmee":  l.setForeground(GREEN);  break;
                    case "En attente": l.setForeground(ORANGE); break;
                    case "Annulee":    l.setForeground(RED);    break;
                    case "Terminee":   l.setForeground(new Color(100, 116, 139)); break;
                    default:           l.setForeground(TEXT_PRI);
                }
            }
            return l;
        }
    }

    // ── Utilitaires UI ───────────────────────────────────────────────────────
    private JTable buildTable(DefaultTableModel model) {
        JTable t = new JTable(model) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) c.setBackground(row % 2 == 0 ? BG_TABLE : BG_ROW_ALT);
                else c.setBackground(BG_SEL);
                return c;
            }
        };
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setForeground(TEXT_PRI);
        t.setBackground(BG_TABLE);
        t.setRowHeight(32);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(BG_SEL);
        t.setSelectionForeground(Color.WHITE);
        t.setFillsViewportHeight(true);
        t.setBorder(null);

        JTableHeader header = t.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(HDR_BG);
        header.setForeground(TEXT_PRI);
        header.setBorder(new MatteBorder(0, 0, 2, 0, BORDER_C));
        header.setPreferredSize(new Dimension(0, 38));

        DefaultTableCellRenderer defRend = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                l.setForeground(sel ? Color.WHITE : TEXT_PRI);
                l.setBackground(sel ? BG_SEL : (row%2==0 ? BG_TABLE : BG_ROW_ALT));
                l.setBorder(new EmptyBorder(0, 12, 0, 0));
                l.setOpaque(true);
                return l;
            }
        };
        t.setDefaultRenderer(Object.class, defRend);
        t.setAutoCreateRowSorter(true);
        return t;
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
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setBorder(new EmptyBorder(4, 0, 4, 0));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_SEC);
        lbl.setPreferredSize(new Dimension(150, 28));
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

    private void hideCol(JTable tbl, int col) {
        tbl.getColumnModel().getColumn(col).setMinWidth(0);
        tbl.getColumnModel().getColumn(col).setMaxWidth(0);
        tbl.getColumnModel().getColumn(col).setWidth(0);
    }
}
