package univscheduler.ui;

import univscheduler.dao.*;
import univscheduler.model.Salle;
import univscheduler.service.ConflitService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Dashboard moderne UIDT \u2014 KPI + diagramme vertical interactif.
 * Clic sur une barre = fiche detail salle. Badge CRITIQUE si suroccupee.
 */
public class DashboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Color BG_PAGE  = new Color(248, 250, 252);
    private static final Color BG_CARD  = Color.WHITE;
    private static final Color BORDER_C = new Color(226, 230, 238);
    private static final Color TEXT_PRI = new Color( 15,  23,  42);
    private static final Color TEXT_SEC = new Color(100, 116, 139);
    private static final Color CRITICAL = new Color(239,  68,  68);

    // Couleurs par niveau (appliquees selon nb de cours)
    private static Color niveauColor(int nbCours) {
        if (nbCours >= SEUIL_CRITIQUE) return new Color(239,  68,  68); // rouge
        if (nbCours >= SEUIL_ELEVE)   return new Color(249, 115,  22); // orange
        if (nbCours >= SEUIL_MODERE)  return new Color(234, 179,   8); // jaune
        return new Color( 34, 197,  94);                               // vert NORMAL
    }
    private static String niveauLabel(int nbCours) {
        if (nbCours >= SEUIL_CRITIQUE) return "\u26a0 CRITIQUE";
        if (nbCours >= SEUIL_ELEVE)   return "\u26a0 ELEVE";
        if (nbCours >= SEUIL_MODERE)  return "MODERE";
        return "NORMAL";
    }
    private static boolean isCritique(int nbCours) { return nbCours >= SEUIL_CRITIQUE; }
    private static boolean isEleve(int nbCours)    { return nbCours >= SEUIL_ELEVE; }
    private static boolean needsBadge(int nbCours) { return true; } // badge sur toutes les barres

    // Seuils absolus (nb de cours planifies)
    private static final int SEUIL_CRITIQUE = 30;  // rouge
    private static final int SEUIL_ELEVE    = 26;  // orange
    private static final int SEUIL_MODERE   = 18;  // jaune

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 20, 20, 20));
        loadData();
    }

    // ---------------------------------------------
    //  CHARGEMENT
    // ---------------------------------------------
    @SuppressWarnings("unchecked")
    private void loadData() {
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                int[] stats = new int[5];
                Map<String, Integer> usage    = new LinkedHashMap<>();
                Map<String, Salle>   salleMap = new LinkedHashMap<>();
                try {
                    SalleDAO salleDAO = new SalleDAO();
                    CoursDAO coursDAO = new CoursDAO();
                    stats[0] = salleDAO.findAll().size();
                    stats[1] = coursDAO.countTotal();
                    stats[2] = new UtilisateurDAO().findAll().size();
                    stats[3] = new ClasseDAO().findAll().size();
                    stats[4] = new ConflitService().getAllConflits().size();
                    usage    = coursDAO.getUsageParSalle();
                    for (Salle s : salleDAO.findAll())
                        salleMap.put(s.getNumero(), s);
                } catch (Exception e) { e.printStackTrace(); }
                return new Object[]{ stats, usage, salleMap };
            }
            @Override protected void done() {
                try {
                    Object[] r = get();
                    buildUI((int[]) r[0],
                            (Map<String, Integer>) r[1],
                            (Map<String, Salle>)   r[2]);
                } catch (Exception e) {
                    buildUI(new int[]{0,0,0,0,0}, new LinkedHashMap<>(), new LinkedHashMap<>());
                }
            }
        }.execute();
    }

    // ---------------------------------------------
    //  CONSTRUCTION UI
    // ---------------------------------------------
    private void buildUI(int[] stats, Map<String, Integer> usage, Map<String, Salle> salleMap) {
        removeAll();
        add(buildHeader(), BorderLayout.NORTH);

        JPanel kpi = new JPanel(new GridLayout(1, 5, 12, 0));
        kpi.setOpaque(false);
        kpi.add(kpiCard("Salles",          stats[0], new Color(59, 130, 246)));
        kpi.add(kpiCard("Cours planifies", stats[1], new Color(16, 185, 129)));
        kpi.add(kpiCard("Utilisateurs",    stats[2], new Color(139, 92, 246)));
        kpi.add(kpiCard("Classes",         stats[3], new Color(245, 158,  11)));
        kpi.add(kpiCard("Conflits",        stats[4],
            stats[4] > 0 ? CRITICAL : new Color(16, 185, 129)));

        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(usage.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (sorted.size() > 10) sorted = sorted.subList(0, 10);
        int maxVal = sorted.isEmpty() ? 1
            : sorted.stream().mapToInt(Map.Entry::getValue).max().orElse(1);

        JPanel bottom = new JPanel(new BorderLayout(16, 0));
        bottom.setOpaque(false);
        bottom.add(buildChartCard(sorted, maxVal, salleMap), BorderLayout.CENTER);
        bottom.add(buildInfoCard(stats),                     BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(kpi,    BorderLayout.NORTH);
        center.add(bottom, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    // ---------------------------------------------
    //  HEADER
    // ---------------------------------------------
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 18, 0));

        JLabel title = new JLabel("Tableau de bord");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(TEXT_PRI);

        JLabel sub = new JLabel("UIDT  \u00b7  Annee academique 2024-2025");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TEXT_SEC);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(title);
        left.add(Box.createVerticalStrut(3));
        left.add(sub);
        p.add(left, BorderLayout.WEST);
        return p;
    }

    // ---------------------------------------------
    //  KPI CARD
    // ---------------------------------------------
    private JPanel kpiCard(String label, int value, Color color) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), 5, 5, 5);
                g2.fillRect(0, 3, getWidth(), 2);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 10));
                g2.fillRoundRect(0, 5, getWidth(), getHeight() - 5, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(BORDER_C, 1, 14),
            new EmptyBorder(16, 18, 16, 18)));

        JLabel valLbl = new JLabel(String.valueOf(value));
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 34));
        valLbl.setForeground(color);

        JLabel nameLbl = new JLabel(label);
        nameLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        nameLbl.setForeground(TEXT_SEC);

        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
                g2.fillOval(0, 0, 36, 36);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(1, 1, 34, 34);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(36, 36));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(valLbl, BorderLayout.WEST);
        topRow.add(dot,    BorderLayout.EAST);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(topRow);
        content.add(Box.createVerticalStrut(6));
        content.add(nameLbl);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    // ---------------------------------------------
    //  CARTE DIAGRAMME
    // ---------------------------------------------
    private JLabel legendItem(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(color);
        return l;
    }

    private JPanel buildChartCard(List<Map.Entry<String, Integer>> data,
                                  int maxVal, Map<String, Salle> salleMap) {
        JPanel card = new JPanel(new BorderLayout(0, 14)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(BORDER_C, 1, 14),
            new EmptyBorder(20, 22, 20, 22)));

        // -- En-tete --
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);

        JPanel hdrLeft = new JPanel();
        hdrLeft.setLayout(new BoxLayout(hdrLeft, BoxLayout.Y_AXIS));
        hdrLeft.setOpaque(false);

        JLabel chartTitle = new JLabel("Utilisation des salles");
        chartTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        chartTitle.setForeground(TEXT_PRI);

        JLabel sub = new JLabel(data.isEmpty()
            ? "Aucun cours planifie"
            : "Top " + data.size() + " salles  \u2014  cliquez sur une barre pour les details");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_SEC);

        hdrLeft.add(chartTitle);
        hdrLeft.add(Box.createVerticalStrut(2));
        hdrLeft.add(sub);

        // Legende niveaux
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        legend.setOpaque(false);
        legend.add(legendItem("\u25cf  Critique \u226530", new Color(239, 68, 68)));
        legend.add(legendItem("\u25cf  Eleve 26-29",       new Color(249,115, 22)));
        legend.add(legendItem("\u25cf  Modere 18-25",      new Color(234,179,  8)));
        legend.add(legendItem("\u25cf  Normal <18",        new Color( 34,197, 94)));

        hdr.add(hdrLeft, BorderLayout.WEST);
        hdr.add(legend,  BorderLayout.EAST);

        VerticalBarChart chart = new VerticalBarChart(data, maxVal, salleMap);
        chart.setPreferredSize(new Dimension(0, 310));

        card.add(hdr,   BorderLayout.NORTH);
        card.add(chart, BorderLayout.CENTER);
        return card;
    }

    // ---------------------------------------------
    //  DIAGRAMME VERTICAL INTERACTIF
    // ---------------------------------------------
    private class VerticalBarChart extends JPanel {

        private static final long serialVersionUID = 1L;

        private final List<Map.Entry<String, Integer>> data;
        private final int                              maxVal;
        private final Map<String, Salle>               salleMap;
        private       int                              hoveredIdx = -1;
        private final Rectangle[]                      barRects;

        VerticalBarChart(List<Map.Entry<String, Integer>> data,
                         int maxVal, Map<String, Salle> salleMap) {
            this.data     = data;
            this.maxVal   = maxVal;
            this.salleMap = salleMap;
            this.barRects = new Rectangle[data.size()];
            setOpaque(false);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    int prev = hoveredIdx;
                    hoveredIdx = -1;
                    for (int i = 0; i < barRects.length; i++)
                        if (barRects[i] != null && barRects[i].contains(e.getPoint())) { hoveredIdx = i; break; }
                    setCursor(hoveredIdx >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
                    if (prev != hoveredIdx) repaint();
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    for (int i = 0; i < barRects.length; i++)
                        if (barRects[i] != null && barRects[i].contains(e.getPoint())) { showDetail(i); break; }
                }
                @Override public void mouseExited(MouseEvent e) {
                    hoveredIdx = -1; setCursor(Cursor.getDefaultCursor()); repaint();
                }
            });
        }

        // -- Boite de dialogue detail ----------------------------------
        private void showDetail(int idx) {
            if (idx < 0 || idx >= data.size()) return;
            Map.Entry<String, Integer> entry = data.get(idx);
            String nomCle  = entry.getKey();
            int    nbCours = entry.getValue();
            boolean crit   = isCritique(nbCours);
            Salle  salle   = salleMap.get(nomCle);
            Color  col     = niveauColor(nbCours);
            Color  colLt   = new Color(
                Math.min(255, col.getRed() + 30),
                Math.min(255, col.getGreen() + 30),
                Math.min(255, col.getBlue() + 30));

            Frame   frame = (Frame) SwingUtilities.getWindowAncestor(this);
            JDialog dlg   = new JDialog(frame, "Detail de la salle", true);
            dlg.setSize(440, 400);
            dlg.setLocationRelativeTo(this);
            dlg.setResizable(false);

            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(BG_CARD);

            // -- Bandeau haut --
            JPanel banner = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setPaint(new GradientPaint(0, 0, col, getWidth(), 0, colLt));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            banner.setOpaque(false);
            banner.setPreferredSize(new Dimension(0, 96));
            banner.setBorder(new EmptyBorder(16, 22, 14, 22));

            JPanel bannerInner = new JPanel();
            bannerInner.setLayout(new BoxLayout(bannerInner, BoxLayout.Y_AXIS));
            bannerInner.setOpaque(false);

            String displayName = (salle != null && salle.getNom() != null && !salle.getNom().isEmpty())
                ? salle.getNom() + " (" + nomCle + ")"
                : nomCle;
            JLabel nomLbl = new JLabel(displayName);
            nomLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
            nomLbl.setForeground(Color.WHITE);
            nomLbl.setAlignmentX(LEFT_ALIGNMENT);

            JPanel badgesRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            badgesRow.setOpaque(false);
            badgesRow.add(badge("#" + (idx + 1) + " classement"));
            badgesRow.add(badge(nbCours + " cours"));
            if (needsBadge(nbCours)) badgesRow.add(badgeCritique(niveauLabel(nbCours), niveauColor(nbCours)));

            bannerInner.add(nomLbl);
            bannerInner.add(Box.createVerticalStrut(8));
            bannerInner.add(badgesRow);
            banner.add(bannerInner, BorderLayout.CENTER);

            // -- Corps details --
            JPanel body = new JPanel(new GridLayout(0, 2, 4, 4));
            body.setBackground(BG_CARD);
            body.setBorder(new EmptyBorder(16, 22, 8, 22));

            int pct = (int) Math.round((double) nbCours / maxVal * 100);
            detailRow(body, "Cours planifies",  nbCours + " cours",       col);
            detailRow(body, "Taux occupation",
                pct + "%  " + (crit ? "\u2014 SURCHARGE" : "\u2014 normal"),
                crit ? CRITICAL : new Color(16, 185, 129));
            if (salle != null) {
                detailRow(body, "Type de salle",
                    salle.getTypeSalle() != null ? salle.getTypeSalle() : "\u2014", col);
                detailRow(body, "Capacite",
                    salle.getCapacite() + " places", col);
                detailRow(body, "Etage",
                    "Etage " + salle.getEtage(), col);
                if (salle.getBatiment() != null)
                    detailRow(body, "Batiment",
                        salle.getBatiment().getNom(), col);
                detailRow(body, "Disponible",
                    salle.isDisponible() ? "Oui" : "Non",
                    salle.isDisponible() ? new Color(16,185,129) : CRITICAL);
            }

            // -- Barre de progression --
            JPanel progArea = new JPanel(new BorderLayout(0, 5));
            progArea.setBackground(BG_CARD);
            progArea.setBorder(new EmptyBorder(4, 22, 12, 22));

            JLabel progLbl = new JLabel("Occupation relative (" + pct + "%)");
            progLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            progLbl.setForeground(TEXT_SEC);

            double ratio = Math.min(1.0, (double) nbCours / maxVal);
            JPanel progBar = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(226, 230, 238));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    if (ratio > 0) {
                        g2.setPaint(new GradientPaint(0, 0, colLt, (int)(getWidth()*ratio), 0, col));
                        g2.fillRoundRect(0, 0, (int)(getWidth() * ratio), getHeight(), 8, 8);
                    }
                    g2.dispose();
                }
            };
            progBar.setOpaque(false);
            progBar.setPreferredSize(new Dimension(0, 14));

            progArea.add(progLbl,  BorderLayout.NORTH);
            progArea.add(progBar,  BorderLayout.CENTER);

            // -- Pied --
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
            footer.setBackground(new Color(249, 250, 251));
            footer.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_C));
            JButton closeBtn = pillBtn("Fermer", TEXT_SEC);
            closeBtn.addActionListener(ev -> dlg.dispose());
            footer.add(closeBtn);

            root.add(banner,   BorderLayout.NORTH);
            root.add(body,     BorderLayout.CENTER);
            root.add(progArea, BorderLayout.AFTER_LAST_LINE);
            root.add(footer,   BorderLayout.SOUTH);
            dlg.setContentPane(root);
            dlg.setVisible(true);
        }

        private JLabel badge(String text) {
            JLabel l = new JLabel(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(255,255,255,55));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            l.setFont(new Font("Segoe UI", Font.BOLD, 10));
            l.setForeground(Color.WHITE);
            l.setOpaque(false);
            l.setBorder(new EmptyBorder(2,8,2,8));
            return l;
        }

        private JLabel badgeCritique(String txt, Color col) {
            JLabel l = new JLabel(txt) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),getHeight(),getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            l.setFont(new Font("Segoe UI", Font.BOLD, 10));
            l.setForeground(col);
            l.setOpaque(false);
            l.setBorder(new EmptyBorder(2,8,2,8));
            return l;
        }

        private void detailRow(JPanel p, String key, String val, Color accent) {
            JLabel k = new JLabel(key);
            k.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            k.setForeground(TEXT_SEC);
            k.setBorder(new EmptyBorder(5,0,5,8));

            JLabel v = new JLabel(val != null ? val : "\u2014");
            v.setFont(new Font("Segoe UI", Font.BOLD, 12));
            v.setForeground(accent);
            p.add(k); p.add(v);
        }

        // -- Rendu -----------------------------------------------------
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();

            if (data.isEmpty()) {
                g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
                g2.setColor(new Color(160, 170, 185));
                String msg = "Aucune donnee disponible";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (W - fm.stringWidth(msg)) / 2, H / 2);
                g2.dispose();
                return;
            }

            int n      = data.size();
            int padL   = 30;    // axe Y labels
            int padR   = 10;
            int padTop = 38;    // valeur + badge critique
            int padBot = 72;    // labels bas (2 lignes + marge)
            int axisH  = H - padTop - padBot;
            int totalW = W - padL - padR;
            int gap    = Math.max(8, totalW / (n * 5));
            int colW   = (totalW - gap * (n + 1)) / n;

            // -- Grille --
            int nLines = 4;
            for (int i = 0; i <= nLines; i++) {
                int gy   = padTop + axisH - axisH * i / nLines;
                int yVal = maxVal * i / nLines;
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{4f, 4f}, 0f));
                g2.setColor(new Color(226, 230, 238));
                g2.drawLine(padL, gy, W - padR, gy);
                g2.setStroke(new BasicStroke(1f));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(148, 163, 184));
                g2.drawString(String.valueOf(yVal), 0, gy + 4);
            }

            // -- Axe bas --
            g2.setColor(new Color(203, 213, 225));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(padL, padTop + axisH, W - padR, padTop + axisH);
            g2.setStroke(new BasicStroke(1f));

            // -- Colonnes --
            for (int i = 0; i < n; i++) {
                Map.Entry<String, Integer> entry = data.get(i);
                int    val    = entry.getValue();
                int    barH   = (int)((double) val / maxVal * axisH);
                int    x      = padL + gap + i * (colW + gap);
                int    y      = padTop + axisH - barH;
                boolean crit  = isCritique(val);
                boolean badge = needsBadge(val);
                boolean hov   = (i == hoveredIdx);
                Color  col    = niveauColor(val);
                Color  colLt  = new Color(
                    Math.min(255, col.getRed()   + 45),
                    Math.min(255, col.getGreen() + 45),
                    Math.min(255, col.getBlue()  + 45));

                // Zone cliquable (barre + zone label)
                barRects[i] = new Rectangle(x, y, colW, barH + padBot - 8);

                // Halo hover
                if (hov) {
                    g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 18));
                    g2.fillRoundRect(x - 5, padTop - 12, colW + 10, axisH + 12 + padBot, 10, 10);
                }

                // Ombre
                g2.setColor(new Color(0, 0, 0, hov ? 26 : 14));
                g2.fillRoundRect(x + 3, y + 3, colW, barH, 8, 8);

                // Barre degradee
                g2.setPaint(new GradientPaint(x, y, colLt, x, y + barH, col));
                g2.fillRoundRect(x, y, colW, barH, 8, 8);

                // Reflet
                g2.setColor(new Color(255, 255, 255, hov ? 60 : 40));
                g2.fillRoundRect(x + 3, y + 2, colW - 6, Math.min(barH / 3, 18), 6, 6);

                // Contour
                g2.setPaint(col.darker());
                g2.setStroke(new BasicStroke(hov ? 1.5f : 0.8f));
                g2.drawRoundRect(x, y, colW, barH, 8, 8);
                g2.setStroke(new BasicStroke(1f));

                // -- Badge niveau --
                if (badge) {
                    String critTxt = niveauLabel(val);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    FontMetrics fmC = g2.getFontMetrics();
                    int bw = fmC.stringWidth(critTxt) + 14;
                    int bh = 16;
                    int bx = x + (colW - bw) / 2;
                    int by = y - bh - 5;
                    g2.setColor(col);
                    g2.fillRoundRect(bx, by, bw, bh, bh, bh);
                    g2.setColor(Color.WHITE);
                    g2.drawString(critTxt, bx + 7, by + 11);
                }

                // -- Valeur numerique --
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fmV = g2.getFontMetrics();
                String valStr   = String.valueOf(val);
                int    vy       = crit ? y - 22 : y - 6;
                g2.setColor(col);
                g2.drawString(valStr, x + (colW - fmV.stringWidth(valStr)) / 2, vy);

                // -- Hint clic si hover --
                if (hov) {
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                    g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 160));
                    String hint = "\u25b2 Cliquer pour details";
                    FontMetrics fmH = g2.getFontMetrics();
                    g2.drawString(hint, x + (colW - fmH.stringWidth(hint)) / 2,
                        padTop + axisH + padBot - 6);
                }

                // --------------------------------
                //  LABELS EN BAS \u2014 bien lisibles
                // --------------------------------
                int labelY = padTop + axisH + 14;

                // Fond label si critique (petite pastille rouge)
                if (badge) {
                    g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 12));
                    g2.fillRoundRect(x - 2, labelY - 12, colW + 4, 44, 8, 8);
                }

                // Nom complet de la salle (ligne 1 \u2014 gras, bien visible)
                Salle  s         = salleMap.get(entry.getKey());
                String nomAffiche = (s != null && s.getNom() != null && !s.getNom().isEmpty())
                                   ? s.getNom()
                                   : entry.getKey();
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(col);
                FontMetrics fm1 = g2.getFontMetrics();
                String line1    = fitText(nomAffiche, fm1, colW + gap - 4);
                g2.drawString(line1, x + (colW - fm1.stringWidth(line1)) / 2, labelY);

                // Numero + type (ligne 2 \u2014 gris teint\u00e9 couleur)
                String line2 = entry.getKey();
                if (s != null && s.getTypeSalle() != null && !s.getTypeSalle().isEmpty())
                    line2 += "  \u00b7  " + s.getTypeSalle();
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 200));
                FontMetrics fm2 = g2.getFontMetrics();
                String l2       = fitText(line2, fm2, colW + gap - 4);
                g2.drawString(l2, x + (colW - fm2.stringWidth(l2)) / 2, labelY + 16);

                // Rang (ligne 3 \u2014 petit, centr\u00e9)
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 140));
                String rank = "#" + (i + 1);
                FontMetrics fm3 = g2.getFontMetrics();
                g2.drawString(rank, x + (colW - fm3.stringWidth(rank)) / 2, labelY + 30);
            }

            g2.dispose();
        }

        private String fitText(String t, FontMetrics fm, int maxW) {
            if (fm.stringWidth(t) <= maxW) return t;
            while (t.length() > 1 && fm.stringWidth(t + "..") > maxW)
                t = t.substring(0, t.length() - 1);
            return t + "..";
        }
    }

    // ---------------------------------------------
    //  CARTE INFOS
    // ---------------------------------------------
    private JPanel buildInfoCard(int[] stats) {
        JPanel card = new JPanel(new BorderLayout(0, 12)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(230, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(BORDER_C, 1, 14),
            new EmptyBorder(20, 18, 20, 18)));

        JLabel t = new JLabel("Informations");
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(TEXT_PRI);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        addInfoRow(rows, "Base de donnees", "MySQL / XAMPP",   new Color(59,130,246));
        addInfoRow(rows, "Connexion",       "localhost:3306",  new Color(16,185,129));
        addInfoRow(rows, "Schema",          "univ_scheduler", new Color(139,92,246));
        addInfoRow(rows, "Conflits",
            stats[4] > 0 ? stats[4] + " detecte(s)" : "Aucun",
            stats[4] > 0 ? CRITICAL : new Color(16,185,129));

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_C);

        JLabel note = new JLabel(
            "<html><span style='color:#94a3b8;font-size:10px'>"
            + "admin@univ.sn<br>Detection conflits : activee</span></html>");

        JPanel bottom = new JPanel();
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));
        bottom.setOpaque(false);
        bottom.add(sep);
        bottom.add(Box.createVerticalStrut(8));
        bottom.add(note);

        card.add(t,      BorderLayout.NORTH);
        card.add(rows,   BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void addInfoRow(JPanel p, String key, String val, Color accent) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 0, 5, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel dot = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillOval(0, 4, 8, 8);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(8, 16));

        JPanel txt = new JPanel();
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        txt.setOpaque(false);

        JLabel kLbl = new JLabel(key);
        kLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        kLbl.setForeground(TEXT_SEC);

        JLabel vLbl = new JLabel(val);
        vLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        vLbl.setForeground(TEXT_PRI);

        txt.add(kLbl); txt.add(vLbl);
        row.add(dot, BorderLayout.WEST);
        row.add(txt, BorderLayout.CENTER);
        p.add(row);
    }

    // ---------------------------------------------
    //  HELPERS
    // ---------------------------------------------
    private JButton pillBtn(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    getModel().isRollover() ? 35 : 12));
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
        btn.setBorder(new EmptyBorder(6, 16, 6, 16));
        return btn;
    }

    private static class RoundedBorder extends AbstractBorder {
        private final Color color; private final int thickness, radius;
        RoundedBorder(Color c, int t, int r) { color=c; thickness=t; radius=r; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w-1, h-1, radius, radius);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) {
            return new Insets(radius/2, radius/2, radius/2, radius/2);
        }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.left=i.right=i.top=i.bottom=radius/2; return i;
        }
    }
}
