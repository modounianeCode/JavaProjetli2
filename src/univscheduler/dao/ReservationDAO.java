package univscheduler.dao;

import univscheduler.model.Reservation;
import univscheduler.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des réservations ponctuelles.
 */
public class ReservationDAO {

    /** Retourne toutes les réservations (toutes dates confondues). */
    public List<Reservation> findAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, CONCAT(s.numero, ' - ', IFNULL(s.nom, '')) AS nom_salle, " +
                     "CONCAT(u.prenom, ' ', u.nom) AS nom_utilisateur " +
                     "FROM reservations r " +
                     "LEFT JOIN salles s ON r.salle_id = s.id " +
                     "LEFT JOIN utilisateurs u ON r.utilisateur_id = u.id " +
                     "ORDER BY r.date_reservation DESC, r.heure_debut DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Réservations en cours ou futures (date >= aujourd'hui), triées du plus proche au plus éloigné. */
    public List<Reservation> findEnCours() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, CONCAT(s.numero, ' - ', IFNULL(s.nom, '')) AS nom_salle, " +
                     "CONCAT(u.prenom, ' ', u.nom) AS nom_utilisateur " +
                     "FROM reservations r " +
                     "LEFT JOIN salles s ON r.salle_id = s.id " +
                     "LEFT JOIN utilisateurs u ON r.utilisateur_id = u.id " +
                     "WHERE r.date_reservation >= CURDATE() " +
                     "ORDER BY r.date_reservation ASC, r.heure_debut ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Réservations passées (date < aujourd'hui), triées de la plus récente à la plus ancienne. */
    public List<Reservation> findPassees() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, CONCAT(s.numero, ' - ', IFNULL(s.nom, '')) AS nom_salle, " +
                     "CONCAT(u.prenom, ' ', u.nom) AS nom_utilisateur " +
                     "FROM reservations r " +
                     "LEFT JOIN salles s ON r.salle_id = s.id " +
                     "LEFT JOIN utilisateurs u ON r.utilisateur_id = u.id " +
                     "WHERE r.date_reservation < CURDATE() " +
                     "ORDER BY r.date_reservation DESC, r.heure_debut DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Recherche par titre ou nom de salle (toutes dates). */
    public List<Reservation> search(String query) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        String sql = "SELECT r.*, CONCAT(s.numero, ' - ', IFNULL(s.nom, '')) AS nom_salle, " +
                     "CONCAT(u.prenom, ' ', u.nom) AS nom_utilisateur " +
                     "FROM reservations r " +
                     "LEFT JOIN salles s ON r.salle_id = s.id " +
                     "LEFT JOIN utilisateurs u ON r.utilisateur_id = u.id " +
                     "WHERE LOWER(r.titre) LIKE ? OR LOWER(CONCAT(s.numero, ' - ', IFNULL(s.nom, ''))) LIKE ? " +
                     "ORDER BY r.date_reservation DESC, r.heure_debut DESC";
        String like = "%" + query.toLowerCase() + "%";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Sauvegarde une nouvelle réservation. */
    public void save(Reservation r) throws SQLException {
        String sql = "INSERT INTO reservations (salle_id, utilisateur_id, titre, description, " +
                     "date_reservation, heure_debut, heure_fin, statut) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getSalleId());
            ps.setInt(2, r.getUtilisateurId());
            ps.setString(3, r.getTitre());
            ps.setString(4, r.getDescription() != null ? r.getDescription() : "");
            ps.setDate(5, Date.valueOf(r.getDateReservation()));
            ps.setTime(6, Time.valueOf(r.getHeureDebut()));
            ps.setTime(7, Time.valueOf(r.getHeureFin()));
            ps.setString(8, r.getStatut().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) r.setId(keys.getInt(1));
            }
        }
    }

    /**
     * Historique des réservations avec filtres combinés :
     * période (dateDebut..dateFin), statut, et mot-clé libre.
     */
    public List<Reservation> findByPeriode(LocalDate dateDebut, LocalDate dateFin,
                                            String statut, String motCle) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT r.*, CONCAT(s.numero, ' - ', IFNULL(s.nom, '')) AS nom_salle, " +
            "CONCAT(u.prenom, ' ', u.nom) AS nom_utilisateur " +
            "FROM reservations r " +
            "LEFT JOIN salles s ON r.salle_id = s.id " +
            "LEFT JOIN utilisateurs u ON r.utilisateur_id = u.id " +
            "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (dateDebut != null) {
            sql.append("AND r.date_reservation >= ? ");
            params.add(Date.valueOf(dateDebut));
        }
        if (dateFin != null) {
            sql.append("AND r.date_reservation <= ? ");
            params.add(Date.valueOf(dateFin));
        }
        if (statut != null && !statut.isEmpty() && !statut.equals("TOUS")) {
            sql.append("AND r.statut = ? ");
            params.add(statut);
        }
        if (motCle != null && !motCle.trim().isEmpty()) {
            sql.append("AND (LOWER(r.titre) LIKE ? OR LOWER(CONCAT(s.numero,' - ',IFNULL(s.nom,''))) LIKE ? " +
                       "OR LOWER(CONCAT(u.prenom,' ',u.nom)) LIKE ?) ");
            String like = "%" + motCle.toLowerCase().trim() + "%";
            params.add(like); params.add(like); params.add(like);
        }
        sql.append("ORDER BY r.date_reservation DESC, r.heure_debut DESC");

        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Date)   ps.setDate(i + 1, (Date) p);
                else                     ps.setString(i + 1, (String) p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Statistiques d'utilisation par salle sur une période.
     * Retourne : [nom_salle, nb_reservations, nb_heures]
     */
    public List<Object[]> statsSallesParPeriode(LocalDate debut, LocalDate fin) throws SQLException {
        String sql = "SELECT CONCAT(s.numero, ' - ', IFNULL(s.nom,'')) AS nom_salle, " +
                     "COUNT(r.id) AS nb_resa, " +
                     "SUM(TIMESTAMPDIFF(MINUTE, r.heure_debut, r.heure_fin)) AS total_minutes " +
                     "FROM reservations r " +
                     "JOIN salles s ON r.salle_id = s.id " +
                     "WHERE r.date_reservation BETWEEN ? AND ? " +
                     "GROUP BY r.salle_id, s.numero, s.nom " +
                     "ORDER BY nb_resa DESC";
        List<Object[]> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(debut));
            ps.setDate(2, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Object[]{
                        rs.getString("nom_salle"),
                        rs.getInt("nb_resa"),
                        rs.getInt("total_minutes")
                    });
                }
            }
        }
        return result;
    }

    /**
     * Statistiques par statut sur une période.
     * Retourne : [statut, count]
     */
    public List<Object[]> statsParStatut(LocalDate debut, LocalDate fin) throws SQLException {
        String sql = "SELECT statut, COUNT(*) AS cnt FROM reservations " +
                     "WHERE date_reservation BETWEEN ? AND ? " +
                     "GROUP BY statut ORDER BY cnt DESC";
        List<Object[]> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(debut));
            ps.setDate(2, Date.valueOf(fin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    result.add(new Object[]{ rs.getString("statut"), rs.getInt("cnt") });
            }
        }
        return result;
    }

    /** Met à jour tous les champs d'une réservation existante. */
    public void update(Reservation r) throws SQLException {
        String sql = "UPDATE reservations SET salle_id=?, utilisateur_id=?, titre=?, description=?, " +
                     "date_reservation=?, heure_debut=?, heure_fin=?, statut=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getSalleId());
            ps.setInt(2, r.getUtilisateurId());
            ps.setString(3, r.getTitre());
            ps.setString(4, r.getDescription() != null ? r.getDescription() : "");
            ps.setDate(5, Date.valueOf(r.getDateReservation()));
            ps.setTime(6, Time.valueOf(r.getHeureDebut()));
            ps.setTime(7, Time.valueOf(r.getHeureFin()));
            ps.setString(8, r.getStatut().name());
            ps.setInt(9, r.getId());
            ps.executeUpdate();
        }
    }

    /** Supprime une réservation par ID. */
    public void delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM reservations WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setSalleId(rs.getInt("salle_id"));
        r.setUtilisateurId(rs.getInt("utilisateur_id"));
        r.setTitre(rs.getString("titre"));
        r.setDescription(rs.getString("description"));

        Date d = rs.getDate("date_reservation");
        if (d != null) r.setDateReservation(d.toLocalDate());

        Time t1 = rs.getTime("heure_debut");
        if (t1 != null) r.setHeureDebut(t1.toLocalTime());

        Time t2 = rs.getTime("heure_fin");
        if (t2 != null) r.setHeureFin(t2.toLocalTime());

        String statut = rs.getString("statut");
        if (statut != null) {
            try { r.setStatut(Reservation.Statut.valueOf(statut)); }
            catch (IllegalArgumentException e) { r.setStatut(Reservation.Statut.CONFIRMEE); }
        }

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) r.setDateCreation(ts.toLocalDateTime());

        r.setNomSalle(rs.getString("nom_salle"));
        r.setNomUtilisateur(rs.getString("nom_utilisateur"));
        return r;
    }
}
