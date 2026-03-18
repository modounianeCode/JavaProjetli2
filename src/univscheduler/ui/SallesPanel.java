package univscheduler.ui;

import univscheduler.dao.*;
import univscheduler.model.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Vue Salles -- palette claire coherente avec le Dashboard.
 * Niveau 1 : 4 grandes cartes categories
 * Niveau 2 : grille des salles + fiche complete
 */
public class SallesPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Utilisateur utilisateur;
    private final SalleDAO    salleDAO;
    private final EquipementDAO equipementDAO = new EquipementDAO();
    private List<Salle>       allSalles = new ArrayList<>();

    private Boolean activeFilter = null;

    private CardLayout cardLayout;
    private JPanel     mainArea;
    private JPanel     viewCategories;
    private JPanel     viewDetail;
    private JPanel     viewRecherche;

    private JButton btnDispo;
    private JButton btnOccupe;

    // -- Palette claire (identique au Dashboard) --
    private static final Color BG_PAGE   = new Color(232, 236, 242);
    private static final Color BG_CARD   = new Color(255, 255, 255);
    private static final Color BG_HEADER = new Color(255, 255, 255);
    private static final Color BG_CHIP   = new Color(248, 250, 253);
    private static final Color BORDER_C  = new Color(200, 208, 220);
    private static final Color TEXT_PRI  = new Color( 15,  23,  42);
    private static final Color TEXT_SEC  = new Color(100, 116, 139);
    private static final Color GREEN     = new Color( 34, 197,  94);
    private static final Color RED       = new Color(239,  68,  68);

    // -- Categories --
    private static final Object[][] CATS = {
        {"amphi",   "Amphitheatres",    "\uD83C\uDF93", new Color(139,  92, 246), "Cours magistraux & grands auditoires", "\uD83C\uDFDB"},
        {"reunion", "Salles Reunion",   "\uD83E\uDD1D", new Color(245, 158,  11), "Espaces de travail & concertation",    "\uD83D\uDCBC"},
        {"td",      "Salles TD",        "\uD83D\uDCDA", new Color( 59, 130, 246), "Travaux diriges & enseignements",      "\uD83D\uDD0A"},
        {"tp",      "Salles TP",        "\uD83D\uDD2C", new Color( 16, 185, 129), "Laboratoires & travaux pratiques",     "\u2697"},
    };

    public SallesPanel(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        this.salleDAO    = new SalleDAO();
        init();
        loadSalles();
    }

    // ---------------------------------------------
    //  INIT
    // ---------------------------------------------
    private void init() {
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);
        add(buildPageHeader(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        mainArea   = new JPanel(cardLayout);
        mainArea.setBackground(BG_PAGE);

        viewCategories = buildCategoriesView();
        viewDetail     = new JPanel(new BorderLayout());
        viewDetail.setBackground(BG_PAGE);
        viewRecherche  = new JPanel(new BorderLayout());
        viewRecherche.setBackground(BG_PAGE);

        mainArea.add(viewCategories, "cats");
        mainArea.add(viewDetail,     "detail");
        mainArea.add(viewRecherche,  "recherche");
        add(mainArea, BorderLayout.CENTER);
        add(buildHint(), BorderLayout.SOUTH);
    }

    // ---------------------------------------------
    //  EN-TETE GLOBAL
    // ---------------------------------------------
    private JPanel buildPageHeader() {
        JPanel h = new JPanel(new BorderLayout(12, 0));
        h.setBackground(BG_HEADER);
        h.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(16, 26, 16, 26)));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        JLabel t = lbl("Salles & Espaces", 22, Font.BOLD, TEXT_PRI);
        JLabel s = lbl("Campus  \u00b7  Universite Iba Der Thiam de Thies", 11, Font.PLAIN, TEXT_SEC);
        t.setAlignmentX(LEFT_ALIGNMENT);
        s.setAlignmentX(LEFT_ALIGNMENT);
        left.add(t);
        left.add(Box.createVerticalStrut(3));
        left.add(s);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        btnDispo  = filterBtn("\u25cf  Disponibles", GREEN, true);
        btnOccupe = filterBtn("\u25cf  Occupees",    RED,   false);

        JButton refresh = pill("\u21bb  Actualiser", TEXT_SEC);
        refresh.addActionListener(e -> { activeFilter = null; updateFilterButtons(); loadSalles(); });

        JButton btnRecherche = pill("\uD83D\uDD0D  Recherche avancee", new Color(59, 130, 246));
        btnRecherche.addActionListener(e -> openRechercheView());

        right.add(btnDispo);
        right.add(btnOccupe);
        right.add(Box.createHorizontalStrut(8));
        right.add(btnRecherche);
        right.add(refresh);

        h.add(left,  BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private JButton filterBtn(String text, Color col, boolean isDispoBtn) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g2 = (Graphics2D) g0.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = (isDispoBtn ? Boolean.TRUE : Boolean.FALSE).equals(activeFilter);
                g2.setColor(active
                    ? new Color(col.getRed(), col.getGreen(), col.getBlue(), 22)
                    : new Color(col.getRed(), col.getGreen(), col.getBlue(), 8));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(active ? col : new Color(col.getRed(), col.getGreen(), col.getBlue(), 100));
                g2.setStroke(new BasicStroke(active ? 1.6f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g0);
            }
        };
        btn.setForeground(col);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(6, 16, 6, 16));
        btn.addActionListener(e -> {
            Boolean wanted = isDispoBtn ? Boolean.TRUE : Boolean.FALSE;
            activeFilter = wanted.equals(activeFilter) ? null : wanted;
            updateFilterButtons();
            applyFilter();
        });
        return btn;
    }

    private void updateFilterButtons() { btnDispo.repaint(); btnOccupe.repaint(); }

    // ---------------------------------------------
    //  FILTRE
    // ---------------------------------------------
    private void applyFilter() {
        List<Salle> filtered = new ArrayList<>();
        for (Salle s : allSalles)
            if (activeFilter == null || s.isDisponible() == activeFilter) filtered.add(s);
        rebuildCategories(filtered);
        refreshBadges(filtered);
        cardLayout.show(mainArea, "cats");
    }

    private void rebuildCategories(List<Salle> salles) {
        viewCategories.removeAll();
        viewCategories.setLayout(new GridBagLayout());
        viewCategories.setBackground(BG_PAGE);
        viewCategories.setBorder(new EmptyBorder(36, 36, 36, 36));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1; g.weighty = 1;
        g.insets  = new Insets(10, 10, 10, 10);
        for (int i = 0; i < CATS.length; i++) {
            g.gridx = i % 2; g.gridy = i / 2;
            viewCategories.add(makeCatCard(i, salles), g);
        }
        viewCategories.revalidate();
        viewCategories.repaint();
    }

    // ---------------------------------------------
    //  NIVEAU 1 -- CARTES CATEGORIES
    // ---------------------------------------------
    private JPanel buildCategoriesView() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG_PAGE);
        outer.setBorder(new EmptyBorder(36, 36, 36, 36));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.BOTH;
        g.weightx = 1; g.weighty = 1;
        g.insets  = new Insets(10, 10, 10, 10);
        for (int i = 0; i < CATS.length; i++) {
            g.gridx = i % 2; g.gridy = i / 2;
            outer.add(makeCatCard(i, allSalles), g);
        }
        return outer;
    }

    private JPanel makeCatCard(int idx, List<Salle> salles) {
        Object[] c     = CATS[idx];
        String   key   = (String) c[0];
        String   label = (String) c[1];
        String   emoji = (String) c[2];
        Color    col   = (Color)  c[3];
        String   desc  = (String) c[4];
        String   big   = (String) c[5];

        long total    = salles.stream().filter(s -> matchType(s.getTypeSalle(), key)).count();
        long dispos   = salles.stream().filter(s -> matchType(s.getTypeSalle(), key) && s.isDisponible()).count();
        long occupees = total - dispos;

        JPanel card = new JPanel(new GridBagLayout()) {
            boolean hover = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) { openCategory(idx, salles); }
            }); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g2 = (Graphics2D) g0.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fond blanc / legere teinte hover
                g2.setColor(hover ? new Color(248, 250, 255) : BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                // Bordure
                g2.setColor(hover ? new Color(col.getRed(), col.getGreen(), col.getBlue(), 180) : BORDER_C);
                g2.setStroke(new BasicStroke(hover ? 1.8f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 18, 18);
                // Bande top coloree
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), hover ? 230 : 180));
                g2.fillRoundRect(0, 0, getWidth(), 7, 5, 5);
                g2.fillRect(0, 4, getWidth(), 3);
                // Fond teint-
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), hover ? 10 : 5));
                g2.fillRoundRect(0, 5, getWidth(), getHeight()-5, 18, 18);
                g2.dispose();
                super.paintComponent(g0);
            }
        };
        card.setOpaque(false);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints gb = new GridBagConstraints();
        gb.gridx = 0; gb.gridy = 0;
        gb.anchor = GridBagConstraints.NORTHWEST;
        gb.weightx = 1; gb.fill = GridBagConstraints.HORIZONTAL;

        // Icone + fleche
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel circle = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g2 = (Graphics2D) g0.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 15));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 60));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(1, 1, getWidth()-2, getHeight()-2);
                g2.dispose();
                super.paintComponent(g0);
            }
        };
        circle.setOpaque(false);
        circle.setPreferredSize(new Dimension(54, 54));
        circle.add(lbl(big, 22, Font.PLAIN, col));

        JLabel arrow = lbl("\u2192", 18, Font.BOLD,
            new Color(col.getRed(), col.getGreen(), col.getBlue(), 160));
        topRow.add(circle, BorderLayout.WEST);
        topRow.add(arrow,  BorderLayout.EAST);

        JLabel nameLbl = lbl(label, 21, Font.BOLD, TEXT_PRI);
        JLabel descLbl = lbl(desc,  13, Font.PLAIN, TEXT_SEC);

        // Badges
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);
        if (activeFilter == null || Boolean.TRUE.equals(activeFilter))
            badges.add(badgeLbl("\u25cf  " + dispos + " dispo",                            GREEN));
        if (activeFilter == null || Boolean.FALSE.equals(activeFilter))
            badges.add(badgeLbl("\u25cf  " + occupees + " occupee" + (occupees>1?"s":""), RED));

        // Compteur total
        Color totalColor = Boolean.TRUE.equals(activeFilter)  ? GREEN
                         : Boolean.FALSE.equals(activeFilter) ? RED : col;
        long  totalShow  = Boolean.TRUE.equals(activeFilter)  ? dispos
                         : Boolean.FALSE.equals(activeFilter) ? occupees : total;

        JLabel totalLbl = lbl(totalShow + " salle" + (totalShow>1?"s":""), 11, Font.BOLD, totalColor);
        totalLbl.setName("badge_" + key);
        totalLbl.setBackground(new Color(totalColor.getRed(), totalColor.getGreen(), totalColor.getBlue(), 10));
        totalLbl.setOpaque(true);
        totalLbl.setBorder(new CompoundBorder(
            new RoundBorder(new Color(totalColor.getRed(), totalColor.getGreen(), totalColor.getBlue(), 80), 1, 8),
            new EmptyBorder(3, 10, 3, 10)));

        JPanel col0 = new JPanel();
        col0.setLayout(new BoxLayout(col0, BoxLayout.Y_AXIS));
        col0.setOpaque(false);
        for (JComponent comp : new JComponent[]{topRow, nameLbl, descLbl, badges, totalLbl}) {
            comp.setAlignmentX(LEFT_ALIGNMENT);
            col0.add(comp);
            if (comp != totalLbl) col0.add(Box.createVerticalStrut(comp == topRow ? 12 : 6));
        }

        card.add(col0, gb);
        return card;
    }

    private JLabel badgeLbl(String text, Color col) {
        JLabel l = lbl(text, 10, Font.BOLD, col);
        l.setBackground(new Color(col.getRed(), col.getGreen(), col.getBlue(), 8));
        l.setOpaque(true);
        l.setBorder(new CompoundBorder(
            new RoundBorder(new Color(col.getRed(), col.getGreen(), col.getBlue(), 60), 1, 8),
            new EmptyBorder(2, 8, 2, 8)));
        return l;
    }

    // ---------------------------------------------
    //  NIVEAU 2 -- LISTE DES SALLES
    // ---------------------------------------------
    private void openCategory(int idx, List<Salle> salles) {
        Object[] cat = CATS[idx];
        String   key   = (String) cat[0];
        String   label = (String) cat[1];
        String   emoji = (String) cat[2];
        Color    col   = (Color)  cat[3];

        List<Salle> groupe = new ArrayList<>();
        for (Salle s : salles) if (matchType(s.getTypeSalle(), key)) groupe.add(s);

        viewDetail.removeAll();
        viewDetail.add(buildDetailHeader(label, emoji, col, groupe), BorderLayout.NORTH);
        viewDetail.add(buildDetailGrid(groupe, col),                  BorderLayout.CENTER);
        viewDetail.revalidate();
        viewDetail.repaint();
        cardLayout.show(mainArea, "detail");
    }

    private JPanel buildDetailHeader(String label, String emoji, Color col, List<Salle> salles) {
        JPanel h = new JPanel(new BorderLayout(12, 0));
        h.setBackground(BG_HEADER);
        h.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(12, 22, 12, 22)));

        JButton back = pill("\u2190  Categories", TEXT_SEC);
        back.addActionListener(e -> cardLayout.show(mainArea, "cats"));

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        center.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col); g2.fillOval(0, 4, 10, 10); g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(10, 18));

        long d = salles.stream().filter(Salle::isDisponible).count();
        long o = salles.size() - d;

        center.add(dot);
        center.add(lbl(label, 16, Font.BOLD, TEXT_PRI));
        center.add(pill2(salles.size() + " salle" + (salles.size()>1?"s":""), col));
        center.add(pill2("\u25cf  " + d + " dispo",                           GREEN));
        center.add(pill2("\u25cf  " + o + " occupee" + (o>1?"s":""),         RED));

        h.add(back,   BorderLayout.WEST);
        h.add(center, BorderLayout.CENTER);
        return h;
    }

    private JLabel pill2(String text, Color col) {
        JLabel l = lbl(text, 10, Font.BOLD, col);
        l.setBackground(new Color(col.getRed(), col.getGreen(), col.getBlue(), 10));
        l.setOpaque(true);
        l.setBorder(new CompoundBorder(
            new RoundBorder(new Color(col.getRed(), col.getGreen(), col.getBlue(), 80), 1, 8),
            new EmptyBorder(3, 10, 3, 10)));
        return l;
    }

    private JScrollPane buildDetailGrid(List<Salle> salles, Color col) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 16, 16));
        grid.setBackground(BG_PAGE);
        grid.setBorder(new EmptyBorder(22, 22, 22, 22));
        for (Salle s : salles) grid.add(buildSalleCard(s, col));

        JScrollPane sp = new JScrollPane(grid);
        sp.setBorder(null);
        sp.setBackground(BG_PAGE);
        sp.getViewport().setBackground(BG_PAGE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private JPanel buildSalleCard(Salle salle, Color col) {
        boolean dispo    = salle.isDisponible();
        Color   dispoCol = dispo ? GREEN : RED;

        JPanel card = new JPanel(new BorderLayout()) {
            boolean hover = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) { showFicheSalle(salle, col); }
            }); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g2 = (Graphics2D) g0.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? new Color(245, 248, 255) : BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(hover ? new Color(col.getRed(), col.getGreen(), col.getBlue(), 160) : BORDER_C);
                g2.setStroke(new BasicStroke(hover ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                // Barre gauche dispo / occupee
                g2.setColor(dispoCol);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.fillRect(2, 0, 2, getHeight());
                g2.dispose();
                super.paintComponent(g0);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(265, 205));
        card.setBorder(new EmptyBorder(14, 18, 14, 14));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel row1 = new JPanel(new BorderLayout(6, 0));
        row1.setOpaque(false);
        row1.add(lbl(salle.getNumero(), 18, Font.BOLD, TEXT_PRI), BorderLayout.WEST);
        JLabel db = lbl(dispo ? "\u25cf  Disponible" : "\u25cf  Occupee", 10, Font.BOLD, dispoCol);
        db.setBackground(new Color(dispoCol.getRed(), dispoCol.getGreen(), dispoCol.getBlue(), 10));
        db.setOpaque(true);
        db.setBorder(new CompoundBorder(new RoundBorder(dispoCol, 1, 8), new EmptyBorder(2, 8, 2, 8)));
        row1.add(db, BorderLayout.EAST);

        JLabel nomLbl = lbl(salle.getNom() != null ? salle.getNom() : "\u2014", 13, Font.PLAIN, TEXT_SEC);
        nomLbl.setBorder(new EmptyBorder(2, 0, 8, 0));

        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(BORDER_C); g.fillRect(0, 0, getWidth(), 1);
            }
        };
        sep.setOpaque(false);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        JPanel stats = new JPanel(new GridLayout(1, 3, 6, 0));
        stats.setOpaque(false);
        stats.setBorder(new EmptyBorder(8, 0, 8, 0));
        stats.add(chip("\uD83D\uDC64", salle.getCapacite() + " pl.", col));
        stats.add(chip("\uD83C\uDFE2", "Etg " + salle.getEtage(), TEXT_SEC));
        stats.add(chip("\uD83D\uDD0C", salle.getEquipements().size() + " eq.",
            salle.getEquipements().isEmpty() ? TEXT_SEC : new Color(139, 92, 246)));

        String batNom = salle.getBatiment() != null ? salle.getBatiment().getNom() : "\u2014";
        JLabel batLbl = lbl("\uD83D\uDCCD  " + trunc(batNom, 30), 11, Font.PLAIN, TEXT_SEC);

        JPanel tags = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 3));
        tags.setOpaque(false);
        tags.setBorder(new EmptyBorder(5, 0, 0, 0));
        int n = 0;
        for (String eq : salle.getEquipements()) {
            if (n++ >= 3) break;
            tags.add(tagLbl(trunc(eq, 12)));
        }
        if (salle.getEquipements().size() > 3)
            tags.add(tagLbl("+" + (salle.getEquipements().size() - 3)));

        JLabel tip = lbl("Cliquez pour la fiche  \u2192", 10, Font.PLAIN,
            new Color(col.getRed(), col.getGreen(), col.getBlue(), 140));
        tip.setBorder(new EmptyBorder(5, 0, 0, 0));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(row1); body.add(nomLbl); body.add(sep);
        body.add(stats); body.add(batLbl); body.add(tags); body.add(tip);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    // ---------------------------------------------
    //  FICHE COMPLETE
    // ---------------------------------------------
    private void showFicheSalle(Salle s, Color col) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Fiche salle", true);
        dlg.setSize(500, 530);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_CARD);

        JPanel banner = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g2 = (Graphics2D) g0.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, col,
                    getWidth(), getHeight(),
                    new Color(col.getRed(), col.getGreen(), col.getBlue(), 160)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255,255,255, 12));
                g2.fillOval(getWidth()-100, -30, 140, 140);
                g2.fillOval(getWidth()-50, getHeight()-40, 90, 90);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(22, 26, 22, 26));
        banner.setPreferredSize(new Dimension(0, 96));

        JPanel bannerLeft = new JPanel(new BorderLayout(14, 0));
        bannerLeft.setOpaque(false);
        JLabel bigEmoji = lbl(typeEmoji(s.getTypeSalle()), 32, Font.PLAIN, Color.WHITE);

        JPanel bText = new JPanel();
        bText.setLayout(new BoxLayout(bText, BoxLayout.Y_AXIS));
        bText.setOpaque(false);
        JLabel bNum  = lbl(s.getNumero(), 19, Font.BOLD, Color.WHITE);
        JLabel bName = lbl(s.getNom() != null ? s.getNom()
            : (s.getTypeSalle() != null ? s.getTypeSalle() : ""),
            12, Font.PLAIN, new Color(255,255,255,200));
        bNum.setAlignmentX(LEFT_ALIGNMENT);
        bName.setAlignmentX(LEFT_ALIGNMENT);
        bText.add(bNum); bText.add(bName);
        bannerLeft.add(bigEmoji, BorderLayout.WEST);
        bannerLeft.add(bText,    BorderLayout.CENTER);

        Color dispoCol = s.isDisponible() ? GREEN : RED;
        JLabel dispBadge = lbl(s.isDisponible() ? "\u2713  Disponible" : "\u2717  Occupee",
            11, Font.BOLD, Color.WHITE);
        dispBadge.setBackground(new Color(255,255,255,28));
        dispBadge.setOpaque(true);
        dispBadge.setBorder(new CompoundBorder(
            new RoundBorder(new Color(255,255,255,120), 1, 10),
            new EmptyBorder(4, 12, 4, 12)));
        banner.add(bannerLeft, BorderLayout.CENTER);
        banner.add(dispBadge,  BorderLayout.EAST);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_CARD);
        body.setBorder(new EmptyBorder(22, 26, 18, 26));

        body.add(sectionHead("Informations generales", col));
        body.add(Box.createVerticalStrut(10));
        JPanel statsGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        statsGrid.setOpaque(false);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        statsGrid.setAlignmentX(LEFT_ALIGNMENT);
        statsGrid.add(statBox("\uD83D\uDC64", String.valueOf(s.getCapacite()), "places", col));
        statsGrid.add(statBox("\uD83C\uDFE2", "Etage " + s.getEtage(), "", TEXT_SEC));
        statsGrid.add(statBox("\uD83C\uDFD7", s.getBatiment() != null ? trunc(s.getBatiment().getNom(), 18) : "\u2014", "", TEXT_SEC));
        statsGrid.add(statBox("\uD83D\uDCD0", s.getTypeSalle() != null ? s.getTypeSalle() : "\u2014", "", col));
        body.add(statsGrid);

        body.add(Box.createVerticalStrut(18));
        body.add(sectionHead("Equipements (" + s.getEquipements().size() + ")", col));
        body.add(Box.createVerticalStrut(10));
        if (s.getEquipements().isEmpty()) {
            JLabel none = lbl("Aucun equipement renseigne", 12, Font.PLAIN, TEXT_SEC);
            none.setAlignmentX(LEFT_ALIGNMENT);
            body.add(none);
        } else {
            JPanel eqWrap = new JPanel(new WrapLayout(FlowLayout.LEFT, 7, 7));
            eqWrap.setOpaque(false);
            eqWrap.setAlignmentX(LEFT_ALIGNMENT);
            eqWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            for (String eq : s.getEquipements()) eqWrap.add(eqTag(eq, col));
            body.add(eqWrap);
        }

        body.add(Box.createVerticalStrut(18));
        body.add(sectionHead("Utilisation", col));
        body.add(Box.createVerticalStrut(10));
        try {
            int nb = salleDAO.countCours(s.getId());
            body.add(ficheRow("Cours planifies", nb + " seance" + (nb>1?"s":"")));
        } catch (SQLException ignored) {}
        body.add(ficheRow("Numero salle", s.getNumero()));
        if (s.getBatiment() != null && s.getBatiment().getLocalisation() != null)
            body.add(ficheRow("Localisation", s.getBatiment().getLocalisation()));

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        footer.setBackground(new Color(249, 250, 251));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));
        JButton closeBtn = pill("Fermer", TEXT_SEC);
        closeBtn.addActionListener(e -> dlg.dispose());
        footer.add(closeBtn);

        root.add(banner, BorderLayout.NORTH);
        root.add(scroll,  BorderLayout.CENTER);
        root.add(footer,  BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    //  RECHERCHE AVANCEE
    // ─────────────────────────────────────────────────────────────

    /** Construit et affiche la vue recherche avancée dans le CardLayout. */
    private void openRechercheView() {
        viewRecherche.removeAll();

        // ── En-tête ──────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(new Color(255, 255, 255));
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            new EmptyBorder(12, 22, 12, 22)));

        JButton back = pill("\u2190  Retour", TEXT_SEC);
        back.addActionListener(e -> cardLayout.show(mainArea, "cats"));

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        center.setOpaque(false);
        center.add(lbl("\uD83D\uDD0D", 16, Font.PLAIN, new Color(59, 130, 246)));
        center.add(lbl("Recherche avancee de salles", 16, Font.BOLD, TEXT_PRI));

        header.add(back,   BorderLayout.WEST);
        header.add(center, BorderLayout.CENTER);

        // ── Corps : formulaire + résultats côte à côte ───────────
        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(BG_PAGE);

        // FORMULAIRE (panneau gauche fixe)
        JPanel formPanel = buildRechercheForm(body);
        formPanel.setPreferredSize(new Dimension(310, 0));

        // RÉSULTATS (panneau droit)
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBackground(BG_PAGE);
        resultsPanel.setBorder(new EmptyBorder(16, 0, 16, 16));

        JLabel resultsTitle = lbl("Lancez une recherche pour voir les salles disponibles",
            13, Font.PLAIN, TEXT_SEC);
        resultsTitle.setHorizontalAlignment(SwingConstants.CENTER);
        resultsTitle.setBorder(new EmptyBorder(60, 0, 0, 0));
        resultsPanel.add(resultsTitle, BorderLayout.NORTH);

        body.add(formPanel,   BorderLayout.WEST);
        body.add(resultsPanel, BorderLayout.CENTER);

        viewRecherche.add(header,      BorderLayout.NORTH);
        viewRecherche.add(body,        BorderLayout.CENTER);
        viewRecherche.revalidate();
        viewRecherche.repaint();
        cardLayout.show(mainArea, "recherche");
    }

    private JPanel buildRechercheForm(JPanel resultsHolder) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(new Color(255, 255, 255));
        outer.setBorder(new MatteBorder(0, 1, 0, 0, BORDER_C));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(new Color(255, 255, 255));
        form.setBorder(new EmptyBorder(20, 18, 20, 18));

        // ── Titre formulaire ──────────────────────────────────────
        JLabel fTitle = lbl("Criteres de recherche", 14, Font.BOLD, TEXT_PRI);
        fTitle.setAlignmentX(LEFT_ALIGNMENT);
        fTitle.setBorder(new EmptyBorder(0, 0, 14, 0));
        form.add(fTitle);

        // ── 1. Capacité minimale ──────────────────────────────────
        form.add(formSectionLabel("Capacite minimale"));
        JSpinner capaciteSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 5));
        capaciteSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        styleSpinner(capaciteSpinner);
        capaciteSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        capaciteSpinner.setAlignmentX(LEFT_ALIGNMENT);
        form.add(capaciteSpinner);
        form.add(Box.createVerticalStrut(12));

        // ── 2. Type de salle ──────────────────────────────────────
        form.add(formSectionLabel("Type de salle"));
        String[] types = {"Tous les types", "Amphi", "TD", "TP", "Reunion"};
        JComboBox<String> typeCombo = new JComboBox<>(types);
        styleCombo(typeCombo);
        typeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        typeCombo.setAlignmentX(LEFT_ALIGNMENT);
        form.add(typeCombo);
        form.add(Box.createVerticalStrut(12));

        // ── 3. Équipement requis ──────────────────────────────────
        form.add(formSectionLabel("Equipement requis"));
        JComboBox<Object[]> equipCombo = new JComboBox<>();
        equipCombo.addItem(new Object[]{0, "Aucun equipement specifique"});
        new SwingWorker<List<Equipement>, Void>() {
            @Override protected List<Equipement> doInBackground() throws Exception {
                return equipementDAO.findAll();
            }
            @Override protected void done() {
                try {
                    for (Equipement eq : get())
                        equipCombo.addItem(new Object[]{eq.getId(), eq.getLibelle()});
                } catch (Exception ignored) {}
            }
        }.execute();
        equipCombo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> l, Object v, int i, boolean sel, boolean foc) {
                Object[] arr = (Object[]) v;
                return super.getListCellRendererComponent(l, arr[1], i, sel, foc);
            }
        });
        styleCombo(equipCombo);
        equipCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        equipCombo.setAlignmentX(LEFT_ALIGNMENT);
        form.add(equipCombo);
        form.add(Box.createVerticalStrut(16));

        // ── Séparateur ────────────────────────────────────────────
        JSeparator sep1 = new JSeparator();
        sep1.setForeground(BORDER_C);
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep1.setAlignmentX(LEFT_ALIGNMENT);
        form.add(sep1);
        form.add(Box.createVerticalStrut(14));

        JLabel dispLabel = lbl("Disponibilite a un creneau", 14, Font.BOLD, TEXT_PRI);
        dispLabel.setAlignmentX(LEFT_ALIGNMENT);
        dispLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        form.add(dispLabel);

        // ── 4. Jour ───────────────────────────────────────────────
        form.add(formSectionLabel("Jour de la semaine"));
        String[] jours = {"Ignorer le jour", "Lundi", "Mardi", "Mercredi",
                          "Jeudi", "Vendredi", "Samedi"};
        JComboBox<String> jourCombo = new JComboBox<>(jours);
        styleCombo(jourCombo);
        jourCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        jourCombo.setAlignmentX(LEFT_ALIGNMENT);
        form.add(jourCombo);
        form.add(Box.createVerticalStrut(12));

        // ── 5. Heure début ────────────────────────────────────────
        form.add(formSectionLabel("Heure de debut"));
        String[] heures = buildHeuresList();
        JComboBox<String> heureCombo = new JComboBox<>(heures);
        styleCombo(heureCombo);
        heureCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        heureCombo.setAlignmentX(LEFT_ALIGNMENT);
        // Pré-sélectionner l'heure courante
        String hNow = String.format("%02d:%02d", LocalTime.now().getHour(),
            LocalTime.now().getMinute() < 30 ? 0 : 30);
        for (int i = 0; i < heureCombo.getItemCount(); i++)
            if (heureCombo.getItemAt(i).equals(hNow)) { heureCombo.setSelectedIndex(i); break; }
        form.add(heureCombo);
        form.add(Box.createVerticalStrut(12));

        // ── 6. Durée ──────────────────────────────────────────────
        form.add(formSectionLabel("Duree (minutes)"));
        JSpinner dureeSpinner = new JSpinner(new SpinnerNumberModel(90, 15, 480, 15));
        dureeSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        styleSpinner(dureeSpinner);
        dureeSpinner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        dureeSpinner.setAlignmentX(LEFT_ALIGNMENT);
        form.add(dureeSpinner);
        form.add(Box.createVerticalStrut(16));

        // ── Bouton "Disponibles maintenant" ──────────────────────
        JButton maintenantBtn = buildActionBtn(
            "\u23F0  Disponibles maintenant", new Color(245, 158, 11));
        maintenantBtn.setAlignmentX(LEFT_ALIGNMENT);
        maintenantBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        maintenantBtn.addActionListener(e -> {
            int cap  = (int) capaciteSpinner.getValue();
            String t = typeCombo.getSelectedIndex() == 0 ? null
                       : (String) typeCombo.getSelectedItem();
            int eqId = (int) ((Object[]) equipCombo.getSelectedItem())[0];
            lancerRechercheMaintenantUI(resultsHolder, cap, t, eqId);
        });
        form.add(maintenantBtn);
        form.add(Box.createVerticalStrut(8));

        // ── Bouton Rechercher ─────────────────────────────────────
        JButton searchBtn = buildActionBtn(
            "\uD83D\uDD0D  Rechercher", new Color(59, 130, 246));
        searchBtn.setAlignmentX(LEFT_ALIGNMENT);
        searchBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        searchBtn.addActionListener(e -> {
            int    cap     = (int) capaciteSpinner.getValue();
            String type    = typeCombo.getSelectedIndex() == 0 ? null
                             : (String) typeCombo.getSelectedItem();
            int    eqId    = (int) ((Object[]) equipCombo.getSelectedItem())[0];
            int    jour    = jourCombo.getSelectedIndex(); // 0=ignoré, 1=Lun..
            String heure   = jourCombo.getSelectedIndex() == 0 ? null
                             : (String) heureCombo.getSelectedItem();
            int    duree   = (int) dureeSpinner.getValue();
            lancerRechercheUI(resultsHolder, cap, type, eqId, jour, heure, duree);
        });
        form.add(searchBtn);
        form.add(Box.createVerticalStrut(8));

        // ── Bouton Réinitialiser ──────────────────────────────────
        JButton resetBtn = buildActionBtn("Reinitialiser", TEXT_SEC);
        resetBtn.setAlignmentX(LEFT_ALIGNMENT);
        resetBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        resetBtn.addActionListener(e -> {
            capaciteSpinner.setValue(0);
            typeCombo.setSelectedIndex(0);
            equipCombo.setSelectedIndex(0);
            jourCombo.setSelectedIndex(0);
            dureeSpinner.setValue(90);
            afficherPlaceholderResultats(resultsHolder);
        });
        form.add(resetBtn);

        // ── Envelopper dans un JScrollPane pour permettre le défilement ──
        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScroll.getVerticalScrollBar().setUnitIncrement(12);
        formScroll.getViewport().setBackground(new Color(255, 255, 255));

        outer.add(formScroll, BorderLayout.CENTER);
        return outer;
    }

    private void lancerRechercheUI(JPanel holder, int cap, String type,
                                    int eqId, int jour, String heure, int duree) {
        afficherChargement(holder, "Recherche en cours...");
        new SwingWorker<List<Salle>, Void>() {
            @Override protected List<Salle> doInBackground() throws Exception {
                return salleDAO.rechercheAvancee(cap, type, eqId, jour, heure, duree);
            }
            @Override protected void done() {
                try { afficherResultats(holder, get(), false); }
                catch (Exception ex) { afficherErreur(holder, ex.getMessage()); }
            }
        }.execute();
    }

    private void lancerRechercheMaintenantUI(JPanel holder, int cap, String type, int eqId) {
        afficherChargement(holder, "Verification en temps reel...");
        new SwingWorker<List<Salle>, Void>() {
            @Override protected List<Salle> doInBackground() throws Exception {
                return salleDAO.findDisponiblesMaintenantAvancee(cap, type, eqId);
            }
            @Override protected void done() {
                try { afficherResultats(holder, get(), true); }
                catch (Exception ex) { afficherErreur(holder, ex.getMessage()); }
            }
        }.execute();
    }

    private void afficherResultats(JPanel holder, List<Salle> salles, boolean maintenant) {
        holder.removeAll();

        // ── En-tête résultats ─────────────────────────────────────
        JPanel rHeader = new JPanel(new BorderLayout(10, 0));
        rHeader.setOpaque(false);
        rHeader.setBorder(new EmptyBorder(0, 16, 12, 0));

        String titre = maintenant
            ? "\u23F0  Salles disponibles en ce moment"
            : "\uD83D\uDD0D  Resultats de la recherche";
        JLabel rTitle = lbl(titre, 15, Font.BOLD, TEXT_PRI);

        Color badgeCol = salles.isEmpty() ? RED : GREEN;
        String badgeTxt = salles.isEmpty()
            ? "Aucune salle trouvee"
            : salles.size() + " salle" + (salles.size() > 1 ? "s" : "") + " disponible" + (salles.size() > 1 ? "s" : "");
        JLabel badge = lbl(badgeTxt, 11, Font.BOLD, badgeCol);
        badge.setOpaque(true);
        badge.setBackground(new Color(badgeCol.getRed(), badgeCol.getGreen(), badgeCol.getBlue(), 15));
        badge.setBorder(new CompoundBorder(
            new RoundBorder(new Color(badgeCol.getRed(), badgeCol.getGreen(), badgeCol.getBlue(), 100), 1, 8),
            new EmptyBorder(3, 10, 3, 10)));

        rHeader.add(rTitle, BorderLayout.WEST);
        rHeader.add(badge,  BorderLayout.EAST);
        holder.add(rHeader, BorderLayout.NORTH);

        if (salles.isEmpty()) {
            JPanel vide = new JPanel(new GridBagLayout());
            vide.setOpaque(false);
            JPanel msg = new JPanel();
            msg.setLayout(new BoxLayout(msg, BoxLayout.Y_AXIS));
            msg.setOpaque(false);
            JLabel ico = lbl("\uD83D\uDD0D", 36, Font.PLAIN, new Color(200, 210, 220));
            ico.setAlignmentX(CENTER_ALIGNMENT);
            JLabel txt = lbl("Aucune salle ne correspond a ces criteres", 13, Font.PLAIN, TEXT_SEC);
            txt.setAlignmentX(CENTER_ALIGNMENT);
            JLabel hint = lbl("Essayez d'assouplir vos criteres de recherche", 11, Font.PLAIN,
                new Color(160, 170, 180));
            hint.setAlignmentX(CENTER_ALIGNMENT);
            msg.add(ico); msg.add(Box.createVerticalStrut(10));
            msg.add(txt); msg.add(Box.createVerticalStrut(4));
            msg.add(hint);
            vide.add(msg);
            holder.add(vide, BorderLayout.CENTER);
        } else {
            JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 14, 14));
            grid.setOpaque(false);
            grid.setBorder(new EmptyBorder(4, 12, 16, 0));
            Color accentCol = new Color(59, 130, 246);
            for (Salle s : salles) grid.add(buildSalleCardRecherche(s, accentCol));
            JScrollPane scroll = new JScrollPane(grid);
            scroll.setBorder(null);
            scroll.setOpaque(false);
            scroll.getViewport().setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(16);
            holder.add(scroll, BorderLayout.CENTER);
        }

        holder.revalidate();
        holder.repaint();
    }

    private JPanel buildSalleCardRecherche(Salle salle, Color col) {
        Color dispoCol = GREEN;
        JPanel card = new JPanel(new BorderLayout()) {
            boolean hover = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                public void mouseClicked(MouseEvent e) { showFicheSalle(salle, col); }
            }); }
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g2 = (Graphics2D) g0.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? new Color(245, 248, 255) : BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(hover ? new Color(col.getRed(), col.getGreen(), col.getBlue(), 160) : BORDER_C);
                g2.setStroke(new BasicStroke(hover ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                g2.setColor(dispoCol);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.fillRect(2, 0, 2, getHeight());
                g2.dispose();
                super.paintComponent(g0);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(265, 195));
        card.setBorder(new EmptyBorder(14, 18, 14, 14));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Ligne 1 : numéro + badge dispo
        JPanel row1 = new JPanel(new BorderLayout(6, 0));
        row1.setOpaque(false);
        row1.add(lbl(salle.getNumero(), 18, Font.BOLD, TEXT_PRI), BorderLayout.WEST);
        JLabel db = lbl("\u25cf  Disponible", 10, Font.BOLD, dispoCol);
        db.setBackground(new Color(dispoCol.getRed(), dispoCol.getGreen(), dispoCol.getBlue(), 10));
        db.setOpaque(true);
        db.setBorder(new CompoundBorder(new RoundBorder(dispoCol, 1, 8), new EmptyBorder(2, 8, 2, 8)));
        row1.add(db, BorderLayout.EAST);

        JLabel nomLbl = lbl(salle.getNom() != null ? salle.getNom() : "\u2014", 13, Font.PLAIN, TEXT_SEC);
        nomLbl.setBorder(new EmptyBorder(2, 0, 6, 0));

        // Stats : capacité + étage + nb équipements
        JPanel stats = new JPanel(new GridLayout(1, 3, 6, 0));
        stats.setOpaque(false);
        stats.setBorder(new EmptyBorder(6, 0, 6, 0));
        stats.add(chip("\uD83D\uDC64", salle.getCapacite() + " pl.", col));
        stats.add(chip("\uD83C\uDFE2", "Etg " + salle.getEtage(), TEXT_SEC));
        stats.add(chip("\uD83D\uDD0C", salle.getEquipements().size() + " eq.",
            salle.getEquipements().isEmpty() ? TEXT_SEC : new Color(139, 92, 246)));

        String batNom = salle.getBatiment() != null ? salle.getBatiment().getNom() : "\u2014";
        JLabel batLbl = lbl("\uD83D\uDCCD  " + trunc(batNom, 28), 11, Font.PLAIN, TEXT_SEC);

        // Tags équipements
        JPanel tags = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 2));
        tags.setOpaque(false);
        tags.setBorder(new EmptyBorder(3, 0, 0, 0));
        int n = 0;
        for (String eq : salle.getEquipements()) {
            if (n++ >= 3) break;
            tags.add(tagLbl(trunc(eq, 14)));
        }
        if (salle.getEquipements().size() > 3)
            tags.add(tagLbl("+" + (salle.getEquipements().size() - 3)));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(row1); body.add(nomLbl); body.add(stats); body.add(batLbl); body.add(tags);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void afficherPlaceholderResultats(JPanel holder) {
        holder.removeAll();
        JLabel lbl = lbl("Lancez une recherche pour voir les salles disponibles",
            13, Font.PLAIN, TEXT_SEC);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setBorder(new EmptyBorder(60, 0, 0, 0));
        holder.add(lbl, BorderLayout.NORTH);
        holder.revalidate();
        holder.repaint();
    }

    private void afficherChargement(JPanel holder, String msg) {
        holder.removeAll();
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JLabel l = lbl(msg, 13, Font.ITALIC, TEXT_SEC);
        p.add(l);
        holder.add(p, BorderLayout.CENTER);
        holder.revalidate();
        holder.repaint();
    }

    private void afficherErreur(JPanel holder, String err) {
        holder.removeAll();
        JLabel l = lbl("Erreur : " + err, 12, Font.PLAIN, RED);
        l.setBorder(new EmptyBorder(20, 20, 0, 0));
        holder.add(l, BorderLayout.NORTH);
        holder.revalidate();
        holder.repaint();
    }

    // Helpers formulaire
    private JLabel formSectionLabel(String text) {
        JLabel l = lbl(text, 11, Font.BOLD, TEXT_SEC);
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        return l;
    }

    private void styleSpinner(JSpinner s) {
        s.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_C, 1, true),
            new EmptyBorder(2, 6, 2, 6)));
        ((JSpinner.DefaultEditor) s.getEditor()).getTextField()
            .setFont(new Font("Segoe UI", Font.PLAIN, 13));
    }

    private void styleCombo(JComboBox<?> c) {
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.setBackground(new Color(248, 250, 252));
        c.setForeground(TEXT_PRI);
    }

    private JButton buildActionBtn(String text, Color col) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover()
                    ? new Color(col.getRed(), col.getGreen(), col.getBlue(), 50)
                    : new Color(col.getRed(), col.getGreen(), col.getBlue(), 22);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(col);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(col);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(9, 14, 9, 14));
        return btn;
    }

    private String[] buildHeuresList() {
        List<String> h = new ArrayList<>();
        for (int hh = 7; hh <= 21; hh++)
            for (int mm : new int[]{0, 30})
                h.add(String.format("%02d:%02d", hh, mm));
        return h.toArray(new String[0]);
    }

    // ─────────────────────────────────────────────────────────────
    //  CHARGEMENT
    // ─────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    private void loadSalles() {
        new SwingWorker<List<Salle>, Void>() {
            @Override protected List<Salle> doInBackground() throws Exception { return salleDAO.findAll(); }
            @Override protected void done() {
                try { allSalles = get(); applyFilter(); }
                catch (Exception ex) { System.err.println("Erreur chargement salles : " + ex.getMessage()); }
            }
        }.execute();
    }

    private void refreshBadges(List<Salle> salles) {
        for (Object[] cat : CATS) {
            String key = (String) cat[0];
            long   cnt = salles.stream().filter(s -> matchType(s.getTypeSalle(), key)).count();
            findAndSet(viewCategories, "badge_" + key, cnt + " salle" + (cnt>1?"s":""));
        }
        viewCategories.repaint();
    }

    private void findAndSet(Container parent, String name, String text) {
        for (Component c : parent.getComponents()) {
            if (c instanceof JLabel && name.equals(c.getName())) { ((JLabel)c).setText(text); return; }
            if (c instanceof Container) findAndSet((Container)c, name, text);
        }
    }

    private boolean matchType(String type, String key) {
        if (type == null) return false;
        String t = type.toLowerCase();
        return switch (key) {
            case "amphi"   -> t.contains("amphi");
            case "reunion" -> t.contains("reun") || t.contains("r\u00e9un");
            case "td"      -> t.contains("td") && !t.contains("tp");
            case "tp"      -> t.contains("tp");
            default        -> false;
        };
    }

    // ---------------------------------------------
    //  HINT BAS
    // ---------------------------------------------
    private JPanel buildHint() {
        JPanel f = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 7));
        f.setBackground(BG_HEADER);
        f.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));
        f.add(lbl("Cliquez sur une categorie pour explorer les salles  \u00b7  Filtre actif : Tous",
            11, Font.PLAIN, TEXT_SEC));
        return f;
    }

    // ---------------------------------------------
    //  HELPERS UI
    // ---------------------------------------------
    private JLabel lbl(String t, int sz, int st, Color c) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", st, sz));
        l.setForeground(c);
        return l;
    }

    private JPanel chip(String icon, String val, Color col) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setBackground(BG_CHIP);
        p.setBorder(new CompoundBorder(new RoundBorder(BORDER_C, 1, 8), new EmptyBorder(5, 7, 5, 7)));
        p.add(lbl(icon, 11, Font.PLAIN, col),  BorderLayout.WEST);
        p.add(lbl(val,  12, Font.BOLD,  col),  BorderLayout.CENTER);
        return p;
    }

    private JPanel statBox(String icon, String val, String sub, Color col) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(BG_CHIP);
        p.setBorder(new CompoundBorder(new RoundBorder(BORDER_C, 1, 10), new EmptyBorder(10, 12, 10, 12)));
        JLabel i = lbl(icon, 18, Font.PLAIN, col);
        JPanel tx = new JPanel();
        tx.setLayout(new BoxLayout(tx, BoxLayout.Y_AXIS));
        tx.setOpaque(false);
        JLabel v = lbl(val, 14, Font.BOLD, TEXT_PRI);
        JLabel s = lbl(sub, 10, Font.PLAIN, TEXT_SEC);
        v.setAlignmentX(LEFT_ALIGNMENT);
        s.setAlignmentX(LEFT_ALIGNMENT);
        tx.add(v);
        if (!sub.isEmpty()) tx.add(s);
        p.add(i,  BorderLayout.WEST);
        p.add(tx, BorderLayout.CENTER);
        return p;
    }

    private JLabel tagLbl(String text) {
        JLabel l = lbl(text, 9, Font.PLAIN, TEXT_SEC);
        l.setBorder(new CompoundBorder(new RoundBorder(BORDER_C, 1, 6), new EmptyBorder(2, 7, 2, 7)));
        return l;
    }

    private JLabel eqTag(String text, Color col) {
        JLabel l = lbl(text, 11, Font.PLAIN, TEXT_PRI);
        l.setBackground(new Color(col.getRed(), col.getGreen(), col.getBlue(), 8));
        l.setOpaque(true);
        l.setBorder(new CompoundBorder(
            new RoundBorder(new Color(col.getRed(), col.getGreen(), col.getBlue(), 80), 1, 8),
            new EmptyBorder(4, 10, 4, 10)));
        return l;
    }

    private JPanel sectionHead(String text, Color col) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col); g2.fillOval(0, 2, 8, 8); g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(10, 14));
        row.add(dot);
        row.add(lbl(text, 12, Font.BOLD, col));
        return row;
    }

    private JPanel ficheRow(String key, String val) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(new EmptyBorder(3, 0, 3, 0));
        JLabel k = lbl(key, 13, Font.PLAIN, TEXT_SEC);
        k.setPreferredSize(new Dimension(130, 20));
        row.add(k, BorderLayout.WEST);
        row.add(lbl(val, 13, Font.BOLD, TEXT_PRI), BorderLayout.CENTER);
        return row;
    }

    private JButton pill(String text, Color col) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g2 = (Graphics2D) g0.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(),
                    getModel().isRollover() ? 30 : 10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 140));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g0);
            }
        };
        b.setForeground(col);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setOpaque(false); b.setContentAreaFilled(false);
        b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new EmptyBorder(7, 18, 7, 18));
        return b;
    }

    private String typeEmoji(String type) {
        if (type == null) return "\uD83D\uDCDA";
        String t = type.toLowerCase();
        if (t.contains("amphi"))                              return "\uD83C\uDF93";
        if (t.contains("reun") || t.contains("r\u00e9un"))   return "\uD83E\uDD1D";
        if (t.contains("tp"))                                 return "\uD83D\uDD2C";
        return "\uD83D\uDCDA";
    }

    private String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max-1) + ".." : s;
    }

    // ---------------------------------------------
    //  CLASSES INTERNES
    // ---------------------------------------------
    static class RoundBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final Color color; private final int thick, arc;
        RoundBorder(Color c, int t, int a) { color = c; thick = t; arc = a; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.setStroke(new BasicStroke(thick));
            g2.drawRoundRect(x, y, w-1, h-1, arc*2, arc*2);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(thick,thick,thick,thick); }
    }

    static class WrapLayout extends FlowLayout {
        private static final long serialVersionUID = 1L;
        WrapLayout(int a, int h, int v) { super(a, h, v); }
        @Override public Dimension preferredLayoutSize(Container t) { return doLayout(t, true);  }
        @Override public Dimension minimumLayoutSize (Container t)  { return doLayout(t, false); }
        private Dimension doLayout(Container t, boolean pref) {
            synchronized (t.getTreeLock()) {
                int tw = t.getSize().width;
                if (tw == 0) tw = Integer.MAX_VALUE;
                Insets ins = t.getInsets();
                int maxW = tw - (ins.left + ins.right + getHgap()*2);
                int totalW = 0, totalH = 0, rw = 0, rh = 0;
                for (int i = 0; i < t.getComponentCount(); i++) {
                    Component m = t.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = pref ? m.getPreferredSize() : m.getMinimumSize();
                    if (rw + d.width > maxW && rw > 0) {
                        totalW = Math.max(totalW, rw); totalH += rh + getVgap(); rw = 0; rh = 0;
                    }
                    rw += d.width + getHgap();
                    rh = Math.max(rh, d.height);
                }
                totalW = Math.max(totalW, rw);
                totalH += rh + ins.top + ins.bottom + getVgap()*2;
                return new Dimension(totalW, totalH);
            }
        }
    }
}
