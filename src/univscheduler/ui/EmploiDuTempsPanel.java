package univscheduler.ui;

import univscheduler.dao.*;
import univscheduler.model.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.util.List;
import java.awt.Desktop;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Emploi du temps hebdomadaire — design élégant fond blanc UIDT.
 * Fonctionnalités : sélection classe, planning enseignant, conflits, impression.
 */
public class EmploiDuTempsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Utilisateur utilisateur;
    private final CoursDAO    coursDAO;
    private final ClasseDAO   classeDAO;

    private JComboBox<String> classeCombo;
    private JPanel            gridPanel;
    private JLabel            statusLabel;
    private List<Cours>       coursList;
    private List<Classe>      classes;

    private static final String[] CRENEAUX = {
        "07:30","09:00","10:30","12:00","14:00","15:30","17:00","18:30"
    };
    private static final String[] JOURS = {
        "Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi"
    };

    // ── Palette fond blanc ──
    private static final Color BG_PAGE    = Color.WHITE;
    private static final Color BG_HEADER  = new Color(250, 251, 253);
    private static final Color BG_GRID    = Color.WHITE;
    private static final Color BG_DAYHDR  = new Color(248, 249, 252);
    private static final Color BG_TIMECOL = new Color(241, 245, 255);  // bleu clair marque
    private static final Color BG_EMPTY   = new Color(253, 254, 255);
    // Couleurs alternees pour les lignes horaires
    private static final Color ROW_EVEN   = new Color(247, 250, 255);  // bleu tres pale
    private static final Color ROW_ODD    = new Color(250, 248, 255);  // violet tres pale
    private static final Color BORDER_C   = new Color(226, 230, 238);
    private static final Color BORDER_HD  = new Color(210, 215, 225);
    private static final Color TEXT_TITLE = new Color(30,  41,  59);
    private static final Color TEXT_PRI   = new Color(30,  41,  59);
    private static final Color TEXT_SEC   = new Color(100, 116, 139);
    private static final Color TEXT_TIME  = new Color(71,  85, 105);
    private static final Color ACCENT     = new Color(59, 130, 246);

    // Couleurs jours (douces, lisibles sur blanc)
    private static final Color[] DAY_COLORS = {
        new Color( 59, 130, 246),  // Lundi    – bleu
        new Color(139,  92, 246),  // Mardi    – violet
        new Color( 16, 185, 129),  // Mercredi – vert
        new Color(245, 158,  11),  // Jeudi    – orange
        new Color(239,  68,  68),  // Vendredi – rouge
        new Color(236,  72, 153),  // Samedi   – rose
    };

    // Couleurs cartes cours (pastel sur blanc)
    private static final Color CM_COLOR = new Color( 59, 130, 246);
    private static final Color TD_COLOR = new Color(139,  92, 246);
    private static final Color TP_COLOR = new Color( 16, 185, 129);

    public EmploiDuTempsPanel(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        this.coursDAO    = new CoursDAO();
        this.classeDAO   = new ClasseDAO();
        initUI();
        loadClasses();
    }

    // ════════════════════════════════════════════════════════
    //  INIT UI
    // ════════════════════════════════════════════════════════
    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PAGE);
        add(buildHeader(),   BorderLayout.NORTH);
        add(buildGridArea(), BorderLayout.CENTER);
        add(buildFooter(),   BorderLayout.SOUTH);
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

        JLabel title = new JLabel("Emploi du Temps");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_TITLE);

        JLabel sub = new JLabel("Ann\u00e9e acad\u00e9mique 2024-2025  .  UIDT Thi\u00e8s");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_SEC);

        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        // Contrôles
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.setOpaque(false);

        JLabel classeLbl = new JLabel("Classe :");
        classeLbl.setForeground(TEXT_SEC);
        classeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        classeCombo = new JComboBox<>();
        classeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        classeCombo.setPreferredSize(new Dimension(170, 32));
        classeCombo.addActionListener(e -> loadCours());

        JButton myPlanBtn = pillBtn("Mon planning", ACCENT);
        myPlanBtn.setVisible(utilisateur != null && utilisateur.isEnseignant());
        myPlanBtn.addActionListener(e -> loadCoursEnseignant());

        JButton conflitsBtn = pillBtn("Conflits", new Color(239, 68, 68));
        conflitsBtn.addActionListener(e -> showConflits());

        JButton printBtn = pillBtn("Imprimer", new Color(34, 197, 94));
        printBtn.addActionListener(e -> printTimetable());

        JButton pdfBtn = pillBtn("PDF", new Color(220, 38, 38));
        pdfBtn.addActionListener(e -> exportPdf());

        JButton xlsBtn = pillBtn("Excel", new Color(34, 197, 94));
        xlsBtn.addActionListener(e -> exportExcel());

        controls.add(classeLbl);
        controls.add(classeCombo);
        controls.add(myPlanBtn);
        controls.add(conflitsBtn);
        controls.add(printBtn);
        controls.add(pdfBtn);
        controls.add(xlsBtn);

        header.add(left,     BorderLayout.WEST);
        header.add(controls, BorderLayout.EAST);
        return header;
    }

    // ── Zone grille ──
    private JScrollPane buildGridArea() {
        gridPanel = new JPanel(new BorderLayout());
        gridPanel.setBackground(BG_PAGE);

        JScrollPane sp = new JScrollPane(gridPanel);
        sp.setBorder(null);
        sp.setBackground(BG_PAGE);
        sp.getViewport().setBackground(BG_PAGE);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        return sp;
    }

    // ── Footer légende ──
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 7));
        footer.setBackground(BG_HEADER);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_HD));

        addLegend(footer, "CM", CM_COLOR);
        addLegend(footer, "TD", TD_COLOR);
        addLegend(footer, "TP", TP_COLOR);

        footer.add(Box.createHorizontalStrut(16));

        statusLabel = new JLabel("--");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_SEC);
        footer.add(statusLabel);
        return footer;
    }

    private void addLegend(JPanel p, String text, Color col) {
        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col);
                g2.fillRoundRect(0, 2, getWidth(), getHeight()-2, 4, 4);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(14, 14));

        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_PRI);

        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        item.add(dot);
        item.add(lbl);
        p.add(item);
    }

    // ════════════════════════════════════════════════════════
    //  CHARGEMENTS
    // ════════════════════════════════════════════════════════
    private void loadClasses() {
        new SwingWorker<List<Classe>, Void>() {
            @Override protected List<Classe> doInBackground() throws Exception {
                return classeDAO.findAll();
            }
            @Override protected void done() {
                try {
                    classes = get();
                    classeCombo.removeAllItems();
                    classeCombo.addItem("-- Toutes les classes --");
                    for (Classe c : classes) classeCombo.addItem(c.getNom());
                    loadCours();
                } catch (Exception e) {
                    showError("Chargement classes : " + e.getMessage());
                }
            }
        }.execute();
    }

    private void loadCours() {
        if (statusLabel != null) statusLabel.setText("Chargement...");
        new SwingWorker<List<Cours>, Void>() {
            @Override protected List<Cours> doInBackground() throws Exception {
                int idx = classeCombo.getSelectedIndex();
                if (idx <= 0 || classes == null) return coursDAO.findAll();
                return coursDAO.findByClasse(classes.get(idx - 1).getId());
            }
            @Override protected void done() {
                try { coursList = get(); buildTimetable(coursList); }
                catch (Exception e) { showError(e.getMessage()); }
            }
        }.execute();
    }

    private void loadCoursEnseignant() {
        statusLabel.setText("Chargement...");
        new SwingWorker<List<Cours>, Void>() {
            @Override protected List<Cours> doInBackground() throws Exception {
                return coursDAO.findByEnseignant(utilisateur.getId());
            }
            @Override protected void done() {
                try { coursList = get(); buildTimetable(coursList); }
                catch (Exception e) { showError(e.getMessage()); }
            }
        }.execute();
    }

    // ════════════════════════════════════════════════════════
    //  CONSTRUCTION DE LA GRILLE
    // ════════════════════════════════════════════════════════
    private void buildTimetable(List<Cours> cours) {
        gridPanel.removeAll();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_PAGE);
        wrapper.setBorder(new EmptyBorder(18, 18, 18, 18));

        // Titre classe sélectionnée
        String classeTitle = classeCombo.getSelectedIndex() > 0
            ? classeCombo.getSelectedItem().toString()
            : "Toutes les classes";

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel classeLbl = new JLabel("  " + classeTitle);
        classeLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        classeLbl.setForeground(TEXT_TITLE);

        JLabel weekLbl = new JLabel("Semaine type -- S1/S2", SwingConstants.RIGHT);
        weekLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        weekLbl.setForeground(TEXT_SEC);

        titleRow.add(classeLbl, BorderLayout.WEST);
        titleRow.add(weekLbl,   BorderLayout.EAST);

        // Grille
        JPanel grid = buildGrid(cours);

        wrapper.add(titleRow, BorderLayout.NORTH);
        wrapper.add(grid,     BorderLayout.CENTER);

        gridPanel.add(wrapper, BorderLayout.CENTER);
        if (statusLabel != null)
            statusLabel.setText(cours.size() + " cours affich\u00e9" + (cours.size() > 1 ? "s" : ""));

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private JPanel buildGrid(List<Cours> cours) {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(BG_GRID);
        grid.setBorder(BorderFactory.createLineBorder(BORDER_HD, 1, true));

        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.BOTH;
        g.weighty = 1.0;

        // Coin haut-gauche
        g.gridx = 0; g.gridy = 0; g.weightx = 0.09;
        grid.add(cornerCell(), g);

        // En-têtes jours
        for (int j = 0; j < JOURS.length; j++) {
            g.gridx = j + 1; g.gridy = 0; g.weightx = 0.155;
            grid.add(dayHeader(JOURS[j], DAY_COLORS[j]), g);
        }

        // Lignes horaires
        for (int h = 0; h < CRENEAUX.length - 1; h++) {
            // Colonne heure
            g.gridx = 0; g.gridy = h + 1; g.weightx = 0.09;
            grid.add(timeCell(CRENEAUX[h], CRENEAUX[h + 1], h), g);

            // Cellules par jour
            for (int j = 0; j < JOURS.length; j++) {
                g.gridx = j + 1; g.gridy = h + 1; g.weightx = 0.155;
                grid.add(courseCell(cours, j + 1, CRENEAUX[h], DAY_COLORS[j], h), g);
            }
        }
        return grid;
    }

    // ── Coin ──
    private JPanel cornerCell() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_DAYHDR);
        p.setBorder(new MatteBorder(0, 0, 1, 1, BORDER_HD));
        p.setPreferredSize(new Dimension(82, 56));

        JLabel l = new JLabel("Horaire", SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_SEC);
        p.add(l);
        return p;
    }

    // ── En-tête jour ──
    private JPanel dayHeader(String jour, Color color) {
        // Fond teinté fort pour l'en-tête
        Color bgFull = new Color(color.getRed(), color.getGreen(), color.getBlue(), 38);
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Fond teinté plein
                g2.setColor(bgFull);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Barre colorée en bas (4px)
                g2.setColor(color);
                g2.fillRect(0, getHeight() - 4, getWidth(), 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setBorder(new MatteBorder(0, 0, 1, 1, BORDER_HD));
        p.setPreferredSize(new Dimension(0, 56));

        JLabel l = new JLabel(jour, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(new Color(
            Math.max(0, color.getRed()   - 30),
            Math.max(0, color.getGreen() - 30),
            Math.max(0, color.getBlue()  - 30)));
        p.add(l);
        return p;
    }

    // ── Cellule heure ──
    private JPanel timeCell(String from, String to, int rowIdx) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Fond dégradé vertical discret
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(241, 245, 249),
                    0, getHeight(), new Color(248, 250, 252));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Barre bleue gauche (2px)
                g2.setColor(new Color(59, 130, 246, 60));
                g2.fillRect(0, 0, 2, getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new MatteBorder(0, 0, 1, 1, BORDER_C));
        p.setPreferredSize(new Dimension(82, 110));

        JLabel fromLbl = new JLabel(from, SwingConstants.CENTER);
        fromLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        fromLbl.setForeground(new Color(30, 64, 120));
        fromLbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sep = new JLabel("|", SwingConstants.CENTER);
        sep.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        sep.setForeground(new Color(180, 190, 210));
        sep.setAlignmentX(CENTER_ALIGNMENT);

        JLabel toLbl = new JLabel(to, SwingConstants.CENTER);
        toLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        toLbl.setForeground(new Color(100, 120, 150));
        toLbl.setAlignmentX(CENTER_ALIGNMENT);

        p.add(Box.createVerticalGlue());
        p.add(fromLbl);
        p.add(Box.createVerticalStrut(3));
        p.add(sep);
        p.add(Box.createVerticalStrut(3));
        p.add(toLbl);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ── Cellule cours ──
    private JPanel courseCell(List<Cours> cours, int jour, String heureSlot, Color dayColor, int rowIdx) {
        // Alternance blanc pur / bleu nuit tres pale selon la colonne (jour pair/impair)
        Color emptyBg = (jour % 2 == 1)
            ? new Color(255, 255, 255)        // blanc pur  (Lundi, Mercredi, Vendredi)
            : new Color(240, 245, 255);       // #F0F5FF    (Mardi, Jeudi, Samedi)

        JPanel cell = new JPanel(new BorderLayout());
        cell.setBackground(emptyBg);
        cell.setBorder(new MatteBorder(0, 0, 1, 1, BORDER_C));
        cell.setPreferredSize(new Dimension(0, 110));

        for (Cours c : cours) {
            if (c.getJourSemaine() != jour || c.getHeureDebut() == null) continue;
            String hhMM = c.getHeureDebut().toString().substring(0, 5);
            if (!hhMM.equals(heureSlot)) continue;

            cell.setBackground(Color.WHITE);
            cell.setBorder(new MatteBorder(0, 0, 1, 1, new Color(dayColor.getRed(), dayColor.getGreen(), dayColor.getBlue(), 60)));
            cell.add(buildCourseCard(c, dayColor), BorderLayout.CENTER);
            break;
        }
        return cell;
    }

    // ── Carte cours ──
    private JPanel buildCourseCard(Cours c, Color dayColor) {
        Color base   = courseColor(c);
        Color bgCard = new Color(base.getRed(), base.getGreen(), base.getBlue(), 18);
        Color bgBadge= new Color(base.getRed(), base.getGreen(), base.getBlue(), 30);

        JPanel card = new JPanel(new BorderLayout(0, 2)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // fond pastel
                g2.setColor(bgCard);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // barre gauche colorée (3px)
                g2.setColor(base);
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(6, 10, 6, 8));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ── Badge type (CM / TD / TP) ──
        String typeStr = c.getTypeCours() != null ? c.getTypeCours().name() : "CM";
        JLabel typeLbl = new JLabel(typeStr, SwingConstants.CENTER);
        typeLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        typeLbl.setForeground(base);
        typeLbl.setOpaque(true);
        typeLbl.setBackground(bgBadge);
        typeLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(base.getRed(), base.getGreen(), base.getBlue(), 80), 1, true),
            new EmptyBorder(1, 5, 1, 5)));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topRow.setOpaque(false);
        topRow.add(typeLbl);

        // ── Contenu textuel ──
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Matière — texte sombre, jamais transparent
        String mat = c.getNomMatiere() != null ? c.getNomMatiere() : "--";
        JLabel matLbl = new JLabel(trunc(mat, 22));
        matLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        matLbl.setForeground(TEXT_PRI);           // noir pur → toujours lisible
        matLbl.setAlignmentX(LEFT_ALIGNMENT);

        // Enseignant
        JLabel ensLbl = new JLabel(c.getNomEnseignant() != null ? trunc(c.getNomEnseignant(), 20) : "");
        ensLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ensLbl.setForeground(new Color(45, 55, 72));    // gris foncé → lisible
        ensLbl.setAlignmentX(LEFT_ALIGNMENT);

        // Salle
        JLabel salLbl = new JLabel(" " + (c.getNomSalle() != null ? c.getNomSalle() : "--"));
        salLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        salLbl.setForeground(new Color(71, 85, 105));    // gris lisible
        salLbl.setAlignmentX(LEFT_ALIGNMENT);

        content.add(matLbl);
        content.add(Box.createVerticalStrut(3));
        content.add(ensLbl);
        content.add(Box.createVerticalStrut(2));
        content.add(salLbl);

        card.add(topRow,  BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);

        // Tooltip HTML
        card.setToolTipText(String.format(
            "<html><body style='font-family:Segoe UI;padding:4px'>" +
            "<b style='font-size:13px;color:#1e293b'>%s</b><br>" +
            "<span style='color:#64748b'>Type : </span><b>%s</b><br>" +
            "<span style='color:#64748b'>Classe : </span><b>%s</b><br>" +
            "<span style='color:#64748b'>Enseignant : </span><b>%s</b><br>" +
            "<span style='color:#64748b'>Salle : </span><b>%s</b><br>" +
            "<span style='color:#64748b'>Horaire : </span><b>%s -> %s</b>" +
            " <span style='color:#94a3b8'>(%d min)</span>" +
            "</body></html>",
            c.getNomMatiere(), typeStr, c.getNomClasse(),
            c.getNomEnseignant(), c.getNomSalle() != null ? c.getNomSalle() : "--",
            c.getHeureDebut(), c.getHeureFin(), c.getDureeMinutes()));

        // Clic simple → fiche détail
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                showCourseDetail(c);
            }
            @Override public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(base.getRed(), base.getGreen(), base.getBlue(), 120), 1),
                    new EmptyBorder(4, 7, 4, 5)));
                card.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                card.setBorder(new EmptyBorder(6, 10, 6, 8));
                card.repaint();
            }
        });

        return card;
    }

    // ════════════════════════════════════════════════════════
    //  FICHE DETAIL COURS
    // ════════════════════════════════════════════════════════
    private void showCourseDetail(Cours c) {
        Color base = courseColor(c);
        String typeStr = c.getTypeCours() != null ? c.getTypeCours().name() : "CM";

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "D\u00e9tail du cours", true);
        dlg.setSize(420, 340);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Bandeau couleur en haut
        JPanel banner = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0,
                    new Color(base.getRed(), base.getGreen(), base.getBlue(), 200),
                    getWidth(), 0,
                    new Color(base.getRed(), base.getGreen(), base.getBlue(), 60));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setPreferredSize(new Dimension(0, 80));
        banner.setBorder(new EmptyBorder(16, 22, 16, 22));

        JPanel bannerContent = new JPanel();
        bannerContent.setLayout(new BoxLayout(bannerContent, BoxLayout.Y_AXIS));
        bannerContent.setOpaque(false);

        JLabel matLbl = new JLabel(c.getNomMatiere() != null ? c.getNomMatiere() : "--");
        matLbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        matLbl.setForeground(new Color(30, 41, 59));
        matLbl.setAlignmentX(LEFT_ALIGNMENT);

        JLabel typeBadge = new JLabel(typeStr);
        typeBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        typeBadge.setForeground(base);
        typeBadge.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(base, 1, true),
            new EmptyBorder(1, 8, 1, 8)));
        typeBadge.setAlignmentX(LEFT_ALIGNMENT);

        bannerContent.add(matLbl);
        bannerContent.add(Box.createVerticalStrut(5));
        bannerContent.add(typeBadge);
        banner.add(bannerContent, BorderLayout.CENTER);

        // Corps infos
        JPanel body = new JPanel(new GridLayout(0, 2, 0, 0));
        body.setBackground(Color.WHITE);
        body.setBorder(new EmptyBorder(14, 22, 14, 22));

        addDetailRow(body, "Enseignant",  c.getNomEnseignant());
        addDetailRow(body, "Classe",        c.getNomClasse());
        addDetailRow(body, "Salle",         c.getNomSalle() != null ? c.getNomSalle() : "Non assign\u00e9e");
        addDetailRow(body, "Jour",          c.getJourLibelle());
        addDetailRow(body, "Debut",         c.getHeureDebut() != null ? c.getHeureDebut().toString() : "--");
        addDetailRow(body, "Duree",         c.getDureeMinutes() + " min");

        // Pied
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        footer.setBackground(new Color(249, 250, 251));
        footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));

        JButton closeBtn = pillBtn("Fermer", TEXT_SEC);
        closeBtn.addActionListener(e -> dlg.dispose());
        footer.add(closeBtn);

        root.add(banner, BorderLayout.NORTH);
        root.add(body,   BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    private void addDetailRow(JPanel p, String key, String val) {
        JLabel k = new JLabel(key);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        k.setForeground(TEXT_SEC);
        k.setBorder(new EmptyBorder(6, 0, 6, 10));

        JLabel v = new JLabel(val != null ? val : "--");
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        v.setForeground(TEXT_PRI);

        p.add(k);
        p.add(v);
    }

    // ════════════════════════════════════════════════════════
    //  IMPRESSION
    // ════════════════════════════════════════════════════════
    private void printTimetable() {
        String classeTitle = classeCombo.getSelectedIndex() > 0
            ? classeCombo.getSelectedItem().toString()
            : "Toutes les classes";

        // On imprime le gridPanel (la grille)
        Printable printable = (graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            double pw = pageFormat.getImageableWidth();
            double ph = pageFormat.getImageableHeight();
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            // Titre
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setColor(Color.BLACK);
            g2.drawString("Emploi du temps -- " + classeTitle, 0, 16);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            g2.setColor(new Color(100, 116, 139));
            g2.drawString("UIDT . Ann\u00e9e acad\u00e9mique 2024-2025", 0, 28);

            // Ligne séparatrice
            g2.setColor(BORDER_HD);
            g2.drawLine(0, 32, (int) pw, 32);

            // Grille imprimée
            JPanel printGrid = buildGrid(coursList != null ? coursList : List.of());
            printGrid.setSize((int) pw, (int) (ph - 40));
            printGrid.doLayout();

            g2.translate(0, 38);
            double scaleX = pw / printGrid.getPreferredSize().getWidth();
            double scaleY = (ph - 40) / printGrid.getPreferredSize().getHeight();
            double scale  = Math.min(scaleX, scaleY);
            g2.scale(scale, scale);

            printGrid.print(g2);
            return Printable.PAGE_EXISTS;
        };

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("EDT -- " + classeTitle);

        PageFormat pf = job.defaultPage();
        pf.setOrientation(PageFormat.LANDSCAPE);

        job.setPrintable(printable, pf);

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this,
                    "Erreur d'impression : " + ex.getMessage(),
                    "Impression", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ════════════════════════════════════════════════════════
    //  EXPORT PDF  (Java pur — image haute résolution → PDF)
    // ════════════════════════════════════════════════════════
    private void exportPdf() {
        String classeTitle = classeCombo.getSelectedIndex() > 0
            ? classeCombo.getSelectedItem().toString()
            : "Toutes_classes";

        // Choisir l'emplacement de sauvegarde
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Enregistrer l'emploi du temps en PDF");
        chooser.setSelectedFile(new File("EDT_" + classeTitle.replace(" ", "_") + ".pdf"));
        chooser.setFileFilter(new FileNameExtensionFilter("Fichiers PDF (*.pdf)", "pdf"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf"))
            file = new File(file.getAbsolutePath() + ".pdf");

        final File finalFile = file;

        // Génération en arrière-plan
        statusLabel.setText("G\u00e9n\u00e9ration du PDF...");
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {

                // ════════════════════════════════════════════════════════
                //  RENDU PDF 100 % MANUEL — Graphics2D direct
                //  A4 paysage 200 dpi  :  2338 x 1654 px
                //  Pas de scale, polices natives grandes → lisibilité max
                // ════════════════════════════════════════════════════════

                final int PW      = 2338;   // 297 mm @ 200 dpi
                final int PH      = 1654;   // 210 mm @ 200 dpi
                final int HDR     = 130;    // hauteur bandeau titre
                final int MX      = 48;     // marge gauche/droite
                final int MY      = 20;     // marge haut/bas grille
                final int TIMEW   = 130;    // largeur colonne Horaire
                final int COL_HDR = 64;     // hauteur ligne en-têtes jours
                final int ROW_H   = 154;    // hauteur d'une ligne horaire

                // Largeur d'une colonne jour (6 jours)
                final int gridW = PW - MX * 2;
                final int dayW  = (gridW - TIMEW) / JOURS.length;

                // ── Palette colonnes alternées (élégant neutre) ──
                // Jours pairs  : blanc pur        → propre, aéré
                // Jours impairs: bleu nuit très pâle → distinction subtile
                final Color COL_EVEN  = new Color(255, 255, 255);        // blanc pur
                final Color COL_ODD   = new Color(240, 245, 255);        // #F0F5FF bleu nuit tres pale
                final Color COL_TIME  = new Color(30,  58, 138);         // #1E3A8A bleu nuit
                final Color COL_TIMEH = new Color(23,  36, 84);          // #172454 bleu tres fonce
                final Color ROW_EVEN  = new Color(255, 255, 255);        // blanc
                final Color ROW_ODD2  = new Color(249, 250, 252);        // gris très très pâle pour zébrage

                BufferedImage img = new BufferedImage(PW, PH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

                // ── Fond global blanc ──
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, PW, PH);

                // ════════════════════════════════════
                //  BANDEAU TITRE
                // ════════════════════════════════════
                // Dégradé bleu → bleu foncé
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(37, 99, 235),
                    PW, 0, new Color(79, 70, 229));
                g.setPaint(gp);
                g.fillRect(0, 0, PW, HDR);

                // Titre
                g.setFont(new Font("Segoe UI", Font.BOLD, 48));
                g.setColor(Color.WHITE);
                g.drawString("Emploi du Temps  \u2014  " + classeTitle, MX, 68);

                // Sous-titre
                g.setFont(new Font("Segoe UI", Font.PLAIN, 22));
                g.setColor(new Color(199, 210, 254));
                g.drawString("UIDT  \u00b7  Universit\u00e9 Iba Der Thiam de Thi\u00e8s  \u00b7  Ann\u00e9e acad\u00e9mique 2024-2025", MX, 104);

                // Ligne de séparation sous bandeau
                g.setColor(new Color(196, 181, 253));
                g.setStroke(new BasicStroke(2f));
                g.drawLine(0, HDR, PW, HDR);

                // ════════════════════════════════════
                //  ZONE GRILLE
                // ════════════════════════════════════
                final int GX = MX;           // x départ grille
                final int GY = HDR + MY;     // y départ grille
                final int nRows = CRENEAUX.length - 1;
                final int totalH = COL_HDR + nRows * ROW_H;

                // ── Fond colonnes alternées ──
                for (int j = 0; j < JOURS.length; j++) {
                    int cx = GX + TIMEW + j * dayW;
                    g.setColor(j % 2 == 0 ? COL_EVEN : COL_ODD);
                    g.fillRect(cx, GY, dayW, COL_HDR + nRows * ROW_H);
                }

                // ── Fond colonne horaire ──
                g.setColor(new Color(30, 58, 138));
                g.fillRect(GX, GY, TIMEW, COL_HDR + nRows * ROW_H);

                // ── En-têtes jours ──
                for (int j = 0; j < JOURS.length; j++) {
                    int cx = GX + TIMEW + j * dayW;
                    Color dc = DAY_COLORS[j];

                    // Fond alterné blanc/bleu de l'en-tête
                    g.setColor(j % 2 == 0 ? new Color(245, 248, 255) : new Color(230, 238, 255));
                    g.fillRect(cx, GY, dayW, COL_HDR);

                    // Bande colorée en bas de l'en-tête (6px)
                    g.setColor(dc);
                    g.fillRect(cx, GY + COL_HDR - 6, dayW, 6);

                    // Nom du jour centré
                    g.setFont(new Font("Segoe UI", Font.BOLD, 26));
                    g.setColor(dc);
                    FontMetrics fm = g.getFontMetrics();
                    int tx = cx + (dayW - fm.stringWidth(JOURS[j])) / 2;
                    g.drawString(JOURS[j], tx, GY + COL_HDR - 16);
                }

                // ── En-tête colonne horaire ──
                g.setFont(new Font("Segoe UI", Font.BOLD, 18));
                g.setColor(Color.WHITE);
                FontMetrics fmT = g.getFontMetrics();
                g.drawString("Horaire", GX + (TIMEW - fmT.stringWidth("Horaire")) / 2, GY + COL_HDR - 18);

                // ════════════════════════════════════
                //  LIGNES HORAIRES
                // ════════════════════════════════════
                List<Cours> cl = coursList != null ? coursList : List.of();

                for (int h = 0; h < nRows; h++) {
                    int ry = GY + COL_HDR + h * ROW_H;

                    // Zébrage léger sur les lignes impaires
                    if (h % 2 != 0) {
                        for (int j = 0; j < JOURS.length; j++) {
                            int cx = GX + TIMEW + j * dayW;
                            Color base = j % 2 == 0 ? COL_EVEN : COL_ODD;
                            // assombrir très légèrement
                            g.setColor(new Color(
                                Math.max(0, base.getRed()   - 10),
                                Math.max(0, base.getGreen() - 8),
                                Math.max(0, base.getBlue()  - 4)));
                            g.fillRect(cx, ry, dayW, ROW_H);
                        }
                    }

                    // ── Cellule horaire ──
                    // Fond alterné bleu nuit / encore plus sombre
                    // Fond horaire alterne bleu nuit / bleu tres fonce
                    g.setColor(h % 2 == 0 ? new Color(30, 58, 138) : new Color(23, 36, 84));
                    g.fillRect(GX, ry, TIMEW, ROW_H);

                    // Heure début
                    g.setFont(new Font("Segoe UI", Font.BOLD, 24));
                    g.setColor(Color.WHITE);
                    FontMetrics fm1 = g.getFontMetrics();
                    g.drawString(CRENEAUX[h],
                        GX + (TIMEW - fm1.stringWidth(CRENEAUX[h])) / 2,
                        ry + 44);

                    // Séparateur flèche
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
                    g.setColor(new Color(147, 197, 253));
                    g.drawString("\u2193", GX + TIMEW / 2 - 5, ry + 74);

                    // Heure fin
                    g.setFont(new Font("Segoe UI", Font.PLAIN, 20));
                    g.setColor(new Color(186, 230, 253));
                    FontMetrics fm2 = g.getFontMetrics();
                    g.drawString(CRENEAUX[h + 1],
                        GX + (TIMEW - fm2.stringWidth(CRENEAUX[h + 1])) / 2,
                        ry + 100);

                    // ── Cours dans chaque colonne ──
                    for (int j = 0; j < JOURS.length; j++) {
                        int cx = GX + TIMEW + j * dayW;
                        Cours found = null;
                        for (Cours c : cl) {
                            if (c.getJourSemaine() != j + 1 || c.getHeureDebut() == null) continue;
                            if (c.getHeureDebut().toString().substring(0, 5).equals(CRENEAUX[h])) {
                                found = c; break;
                            }
                        }
                        if (found != null) {
                            drawPdfCard(g, found, DAY_COLORS[j], cx + 5, ry + 5, dayW - 10, ROW_H - 10);
                        }
                    }
                }

                // ════════════════════════════════════
                //  BORDURES DE GRILLE
                // ════════════════════════════════════
                g.setColor(new Color(203, 213, 225));
                g.setStroke(new BasicStroke(1f));

                // Lignes horizontales (entre créneaux)
                for (int h = 1; h < nRows; h++) {
                    int ry = GY + COL_HDR + h * ROW_H;
                    g.drawLine(GX, ry, GX + TIMEW + JOURS.length * dayW, ry);
                }

                // Séparation en-tête / corps
                g.setColor(new Color(148, 163, 184));
                g.setStroke(new BasicStroke(2f));
                g.drawLine(GX, GY + COL_HDR, GX + TIMEW + JOURS.length * dayW, GY + COL_HDR);

                // Lignes verticales
                g.setColor(new Color(203, 213, 225));
                g.setStroke(new BasicStroke(1f));
                for (int j = 1; j < JOURS.length; j++) {
                    int cx = GX + TIMEW + j * dayW;
                    g.drawLine(cx, GY, cx, GY + totalH);
                }
                // Séparation colonne horaire
                g.setColor(new Color(79, 70, 229));
                g.setStroke(new BasicStroke(2f));
                g.drawLine(GX + TIMEW, GY, GX + TIMEW, GY + totalH);

                // Bordure extérieure
                g.setColor(new Color(100, 116, 139));
                g.setStroke(new BasicStroke(2f));
                g.drawRect(GX, GY, TIMEW + JOURS.length * dayW, totalH);

                g.dispose();

                // ── PDF A4 paysage (842 × 595 pt) ──
                writePdf(finalFile, img, PW, PH);
                return null;
            }

            @Override protected void done() {
                try {
                    get();
                    statusLabel.setText("PDF enregistr\u00e9 : " + finalFile.getName());
                    int choice = JOptionPane.showConfirmDialog(EmploiDuTempsPanel.this,
                        "PDF g\u00e9n\u00e9r\u00e9 avec succ\u00e8s !\n" + finalFile.getAbsolutePath() +
                        "\n\nOuvrir le dossier ?",
                        "Export PDF", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (choice == JOptionPane.YES_OPTION) {
                        try { Desktop.getDesktop().open(finalFile.getParentFile()); }
                        catch (IOException ex) { /* silencieux */ }
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Erreur PDF");
                    JOptionPane.showMessageDialog(EmploiDuTempsPanel.this,
                        "Erreur lors de la g\u00e9n\u00e9ration PDF :\n" + ex.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Force un layout complet récursif sur un composant hors-écran.
     * Sans cela, les cellules enfants ne sont pas dimensionnées et le rendu est vide.
     */
    private void layoutComponent(java.awt.Component c, int w, int h) {
        c.setSize(w, h);
        if (c instanceof java.awt.Container) {
            java.awt.Container ct = (java.awt.Container) c;
            ct.doLayout();
            for (java.awt.Component child : ct.getComponents()) {
                if (child.getWidth() == 0 || child.getHeight() == 0) {
                    child.setSize(child.getPreferredSize());
                }
                layoutComponent(child, child.getWidth(), child.getHeight());
            }
        }
    }

    /**
     * Dessine une carte cours directement en Graphics2D pour le PDF.
     * Polices grandes, lisibles, avec barre colorée et badge type.
     */
    private void drawPdfCard(Graphics2D g, Cours c, Color dayColor,
                             int x, int y, int w, int h) {
        Color base   = courseColor(c);
        Color bgCard = new Color(base.getRed(), base.getGreen(), base.getBlue(), 22);

        // Fond de la carte
        g.setColor(bgCard);
        g.fillRoundRect(x, y, w, h, 8, 8);

        // Barre gauche colorée (6px)
        g.setColor(base);
        g.fillRoundRect(x, y, 6, h, 4, 4);

        // ── Badge type (CM / TD / TP) ──
        String typeStr = c.getTypeCours() != null ? c.getTypeCours().name() : "CM";
        g.setFont(new Font("Segoe UI", Font.BOLD, 16));
        FontMetrics fmB = g.getFontMetrics();
        int bw = fmB.stringWidth(typeStr) + 16;
        int bh = 24;
        int bx = x + 12;
        int by = y + 10;

        // Fond badge
        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 35));
        g.fillRoundRect(bx, by, bw, bh, bh, bh);
        // Bordure badge
        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 120));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(bx, by, bw, bh, bh, bh);
        // Texte badge
        g.setColor(base);
        g.drawString(typeStr, bx + 8, by + 17);

        // ── Matière ──
        g.setFont(new Font("Segoe UI", Font.BOLD, 22));
        g.setColor(new Color(15, 23, 42));
        String mat = c.getNomMatiere() != null ? c.getNomMatiere() : "--";
        if (mat.length() > 22) mat = mat.substring(0, 20) + "...";
        g.drawString(mat, x + 12, y + 58);

        // ── Enseignant ──
        g.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        g.setColor(new Color(51, 65, 85));
        String ens = c.getNomEnseignant() != null ? c.getNomEnseignant() : "";
        if (ens.length() > 24) ens = ens.substring(0, 22) + "...";
        g.drawString(ens, x + 12, y + 84);

        // ── Salle ──
        g.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        g.setColor(new Color(100, 116, 139));
        String salle = c.getNomSalle() != null ? c.getNomSalle() : "Non assign\u00e9e";
        g.drawString(salle, x + 12, y + 108);
    }

    /**
     * Écrit un PDF minimal (PDF 1.4) contenant une seule image JPEG pleine page.
     * Aucune dépendance externe requise.
     */
    private void writePdf(File dest, BufferedImage img, int imgW, int imgH) throws IOException {
        // Compresser l'image en JPEG dans un tableau d'octets
        ByteArrayOutputStream jpegOut = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", jpegOut);
        byte[] jpegBytes = jpegOut.toByteArray();

        // Dimensions page PDF en points (A4 paysage : 842 × 595 pt)
        int pdfW = 842;
        int pdfH = 595;

        try (FileOutputStream fos = new FileOutputStream(dest)) {
            // Références d'objets PDF
            List<Long> offsets = new java.util.ArrayList<>();
            CountingOutputStream cos = new CountingOutputStream(fos);

            // ── Header ──
            write(cos, "%PDF-1.4\n");

            // Obj 1 : Catalog
            offsets.add(cos.count());
            write(cos, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            // Obj 2 : Pages
            offsets.add(cos.count());
            write(cos, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            // Obj 3 : Page
            offsets.add(cos.count());
            write(cos, "3 0 obj\n<< /Type /Page /Parent 2 0 R\n/MediaBox [0 0 " + pdfW + " " + pdfH + "]\n/Contents 4 0 R\n/Resources << /XObject << /Im0 5 0 R >> >>\n>>\nendobj\n");

            // Obj 4 : Content stream (place l'image pleine page)
            String contentStr = "q\n" + pdfW + " 0 0 " + pdfH + " 0 0 cm\n/Im0 Do\nQ\n";
            byte[] contentBytes = contentStr.getBytes("ISO-8859-1");
            offsets.add(cos.count());
            write(cos, "4 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n");
            cos.write(contentBytes);
            write(cos, "\nendstream\nendobj\n");

            // Obj 5 : Image XObject (JPEG)
            offsets.add(cos.count());
            write(cos, "5 0 obj\n<< /Type /XObject /Subtype /Image\n/Width " + imgW + " /Height " + imgH + "\n/ColorSpace /DeviceRGB\n/BitsPerComponent 8\n/Filter /DCTDecode\n/Length " + jpegBytes.length + "\n>>\nstream\n");
            cos.write(jpegBytes);
            write(cos, "\nendstream\nendobj\n");

            // ── Cross-reference table ──
            long xrefOffset = cos.count();
            write(cos, "xref\n0 6\n");
            write(cos, "0000000000 65535 f \r\n");
            for (Long off : offsets) {
                write(cos, String.format("%010d 00000 n \r\n", off));
            }

            // ── Trailer ──
            write(cos, "trailer\n<< /Size 6 /Root 1 0 R >>\n");
            write(cos, "startxref\n" + xrefOffset + "\n%%EOF\n");
        }
    }

    private void write(OutputStream os, String s) throws IOException {
        os.write(s.getBytes("ISO-8859-1"));
    }

    /** OutputStream qui compte les octets écrits. */
    private static class CountingOutputStream extends FilterOutputStream {
        private long count = 0;
        CountingOutputStream(OutputStream out) { super(out); }
        @Override public void write(int b) throws IOException { out.write(b); count++; }
        @Override public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len); count += len;
        }
        long count() { return count; }
    }

    // ════════════════════════════════════════════════════════
    //  EXPORT EXCEL (CSV)
    // ════════════════════════════════════════════════════════
    private void exportExcel() {
        if (coursList == null || coursList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Aucun cours a exporter. Selectionnez une classe ou chargez un planning.",
                "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("emploi_du_temps_" +
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Fichier CSV (Excel)", "csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv"))
            file = new File(file.getAbsolutePath() + ".csv");

        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.OutputStreamWriter(new java.io.FileOutputStream(file), "UTF-8"))) {
            // BOM UTF-8 pour Excel
            pw.print('\uFEFF');
            pw.println("Jour;Heure debut;Duree (min);Matiere;Type;Enseignant;Classe;Salle");

            String[] jours = {"", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi"};
            for (Cours c : coursList) {
                String jour = (c.getJourSemaine() >= 1 && c.getJourSemaine() <= 6)
                    ? jours[c.getJourSemaine()] : String.valueOf(c.getJourSemaine());
                String type = c.getTypeCours() != null ? c.getTypeCours().name() : "";
                pw.printf("%s;%s;%d;%s;%s;%s;%s;%s%n",
                    jour,
                    c.getHeureDebut() != null ? c.getHeureDebut().toString().substring(0, 5) : "",
                    c.getDureeMinutes(),
                    csvEsc(c.getNomMatiere()),
                    type,
                    csvEsc(c.getNomEnseignant()),
                    csvEsc(c.getNomClasse()),
                    csvEsc(c.getNomSalle())
                );
            }

            int r = JOptionPane.showConfirmDialog(this,
                "Emploi du temps exporte :\n" + file.getAbsolutePath() + "\n\nOuvrir le fichier ?",
                "Export Excel reussi", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (r == JOptionPane.YES_OPTION) {
                try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ex) { /* ignore */ }
            }
        } catch (java.io.IOException ex) {
            JOptionPane.showMessageDialog(this,
                "Erreur lors de l'export : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csvEsc(String s) {
        if (s == null) return "";
        return s.replace(";", ",").replace("\n", " ");
    }

    // ════════════════════════════════════════════════════════
    //  CONFLITS
    // ════════════════════════════════════════════════════════
    private void showConflits() {
        univscheduler.service.ConflitService svc = new univscheduler.service.ConflitService();
        List<String> conflits = svc.getAllConflits();
        if (conflits.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "[OK]  Aucun conflit d\u00e9tect\u00e9 dans l'emploi du temps.",
                "Conflits horaires", JOptionPane.INFORMATION_MESSAGE);
        } else {
            StringBuilder sb = new StringBuilder(
                "<html><body style='font-family:Segoe UI;padding:6px'>" +
                "<b style='color:#ef4444'>" + conflits.size() + " conflit(s) d\u00e9tect\u00e9(s)</b><ul>");
            conflits.forEach(co -> sb.append("<li style='margin:3px 0'>").append(co).append("</li>"));
            sb.append("</ul></body></html>");
            JOptionPane.showMessageDialog(this, sb.toString(),
                "Conflits horaires", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════
    private void showError(String msg) {
        gridPanel.removeAll();
        gridPanel.setBackground(BG_PAGE);
        JLabel err = new JLabel("Erreur : " + msg, SwingConstants.CENTER);
        err.setForeground(new Color(239, 68, 68));
        err.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gridPanel.add(err, BorderLayout.CENTER);
        gridPanel.revalidate();
    }

    private Color courseColor(Cours c) {
        if (c.getCouleurMatiere() != null) {
            try { return Color.decode(c.getCouleurMatiere()); } catch (Exception ignored) {}
        }
        if (c.getTypeCours() != null) {
            return switch (c.getTypeCours()) {
                case CM -> CM_COLOR;
                case TD -> TD_COLOR;
                case TP -> TP_COLOR;
            };
        }
        return ACCENT;
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

    private String trunc(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max - 1) + "..." : (s != null ? s : "");
    }
}
