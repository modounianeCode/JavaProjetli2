package univscheduler.dao;

import univscheduler.model.*;
import univscheduler.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DAO pour les opérations CRUD sur les cours.
 */
public class CoursDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<Cours> findAll() throws SQLException {
        String sql = "SELECT c.*, m.libelle AS nomMatiere, m.couleur AS couleurMatiere," +
                     " CONCAT(u.nom,' ',u.prenom) AS nomEnseignant," +
                     " cl.nom AS nomClasse, s.numero AS nomSalle" +
                     " FROM cours c" +
                     " JOIN matieres m ON c.matiere_id = m.id" +
                     " JOIN utilisateurs u ON c.enseignant_id = u.id" +
                     " JOIN classes cl ON c.classe_id = cl.id" +
                     " LEFT JOIN salles s ON c.salle_id = s.id" +
                     " ORDER BY c.jour_semaine, c.heure_debut";
        return findBySqlRaw(sql, null);
    }

    public List<Cours> findByClasse(int classeId) throws SQLException {
        String sql = "SELECT c.*, m.libelle AS nomMatiere, m.couleur AS couleurMatiere," +
                     " CONCAT(u.nom,' ',u.prenom) AS nomEnseignant," +
                     " cl.nom AS nomClasse, s.numero AS nomSalle" +
                     " FROM cours c" +
                     " JOIN matieres m ON c.matiere_id = m.id" +
                     " JOIN utilisateurs u ON c.enseignant_id = u.id" +
                     " JOIN classes cl ON c.classe_id = cl.id" +
                     " LEFT JOIN salles s ON c.salle_id = s.id" +
                     " WHERE c.classe_id = ?" +
                     " ORDER BY c.jour_semaine, c.heure_debut";
        return findBySqlRaw(sql, ps -> ps.setInt(1, classeId));
    }

    public List<Cours> findByEnseignant(int enseignantId) throws SQLException {
        String sql = "SELECT c.*, m.libelle AS nomMatiere, m.couleur AS couleurMatiere," +
                     " CONCAT(u.nom,' ',u.prenom) AS nomEnseignant," +
                     " cl.nom AS nomClasse, s.numero AS nomSalle" +
                     " FROM cours c" +
                     " JOIN matieres m ON c.matiere_id = m.id" +
                     " JOIN utilisateurs u ON c.enseignant_id = u.id" +
                     " JOIN classes cl ON c.classe_id = cl.id" +
                     " LEFT JOIN salles s ON c.salle_id = s.id" +
                     " WHERE c.enseignant_id = ?" +
                     " ORDER BY c.jour_semaine, c.heure_debut";
        return findBySqlRaw(sql, ps -> ps.setInt(1, enseignantId));
    }

    public List<Cours> findBySalle(int salleId) throws SQLException {
        String sql = "SELECT c.*, m.libelle AS nomMatiere, m.couleur AS couleurMatiere," +
                     " CONCAT(u.nom,' ',u.prenom) AS nomEnseignant," +
                     " cl.nom AS nomClasse, s.numero AS nomSalle" +
                     " FROM cours c" +
                     " JOIN matieres m ON c.matiere_id = m.id" +
                     " JOIN utilisateurs u ON c.enseignant_id = u.id" +
                     " JOIN classes cl ON c.classe_id = cl.id" +
                     " LEFT JOIN salles s ON c.salle_id = s.id" +
                     " WHERE c.salle_id = ?" +
                     " ORDER BY c.jour_semaine, c.heure_debut";
        return findBySqlRaw(sql, ps -> ps.setInt(1, salleId));
    }

    public List<Cours> detecterConflitsSalle(int salleId, int jourSemaine,
                                              String heureDebut, int dureeMin,
                                              int excludeCoursId) throws SQLException {
        String heureFin = calculerHeureFin(heureDebut, dureeMin);
        String sql = "SELECT c.*, m.libelle AS nomMatiere, m.couleur AS couleurMatiere," +
                     " CONCAT(u.nom,' ',u.prenom) AS nomEnseignant," +
                     " cl.nom AS nomClasse, s.numero AS nomSalle" +
                     " FROM cours c" +
                     " JOIN matieres m ON c.matiere_id = m.id" +
                     " JOIN utilisateurs u ON c.enseignant_id = u.id" +
                     " JOIN classes cl ON c.classe_id = cl.id" +
                     " LEFT JOIN salles s ON c.salle_id = s.id" +
                     " WHERE c.salle_id = ?" +
                     " AND c.jour_semaine = ?" +
                     " AND c.id != ?" +
                     " AND c.heure_debut < ?" +
                     " AND ADDTIME(c.heure_debut, SEC_TO_TIME(c.duree_minutes * 60)) > ?";
        return findBySqlRaw(sql, ps -> {
            ps.setInt(1, salleId);
            ps.setInt(2, jourSemaine);
            ps.setInt(3, excludeCoursId);
            ps.setString(4, heureFin);
            ps.setString(5, heureDebut);
        });
    }

    public List<Cours> detecterConflitsEnseignant(int enseignantId, int jourSemaine,
                                                   String heureDebut, int dureeMin,
                                                   int excludeCoursId) throws SQLException {
        String heureFin = calculerHeureFin(heureDebut, dureeMin);
        String sql = "SELECT c.*, m.libelle AS nomMatiere, m.couleur AS couleurMatiere," +
                     " CONCAT(u.nom,' ',u.prenom) AS nomEnseignant," +
                     " cl.nom AS nomClasse, s.numero AS nomSalle" +
                     " FROM cours c" +
                     " JOIN matieres m ON c.matiere_id = m.id" +
                     " JOIN utilisateurs u ON c.enseignant_id = u.id" +
                     " JOIN classes cl ON c.classe_id = cl.id" +
                     " LEFT JOIN salles s ON c.salle_id = s.id" +
                     " WHERE c.enseignant_id = ?" +
                     " AND c.jour_semaine = ?" +
                     " AND c.id != ?" +
                     " AND c.heure_debut < ?" +
                     " AND ADDTIME(c.heure_debut, SEC_TO_TIME(c.duree_minutes * 60)) > ?";
        return findBySqlRaw(sql, ps -> {
            ps.setInt(1, enseignantId);
            ps.setInt(2, jourSemaine);
            ps.setInt(3, excludeCoursId);
            ps.setString(4, heureFin);
            ps.setString(5, heureDebut);
        });
    }

    public Cours save(Cours c) throws SQLException {
        String sql = "INSERT INTO cours (matiere_id, enseignant_id, classe_id, salle_id," +
                     " jour_semaine, heure_debut, duree_minutes, type_cours," +
                     " recurrent, date_debut, date_fin, commentaire)" +
                     " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getMatiereId());
            ps.setInt(2, c.getEnseignantId());
            ps.setInt(3, c.getClasseId());
            if (c.getSalleId() != null) ps.setInt(4, c.getSalleId()); else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, c.getJourSemaine());
            ps.setString(6, c.getHeureDebut().toString());
            ps.setInt(7, c.getDureeMinutes());
            ps.setString(8, c.getTypeCours().name());
            ps.setBoolean(9, c.isRecurrent());
            ps.setString(10, c.getDateDebut() != null ? c.getDateDebut().toString() : null);
            ps.setString(11, c.getDateFin()   != null ? c.getDateFin().toString()   : null);
            ps.setString(12, c.getCommentaire());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) c.setId(keys.getInt(1));
            }
        }
        return c;
    }

    public void update(Cours c) throws SQLException {
        String sql = "UPDATE cours SET matiere_id=?, enseignant_id=?, classe_id=?, salle_id=?," +
                     " jour_semaine=?, heure_debut=?, duree_minutes=?, type_cours=?," +
                     " recurrent=?, commentaire=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, c.getMatiereId());
            ps.setInt(2, c.getEnseignantId());
            ps.setInt(3, c.getClasseId());
            if (c.getSalleId() != null) ps.setInt(4, c.getSalleId()); else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, c.getJourSemaine());
            ps.setString(6, c.getHeureDebut().toString());
            ps.setInt(7, c.getDureeMinutes());
            ps.setString(8, c.getTypeCours().name());
            ps.setBoolean(9, c.isRecurrent());
            ps.setString(10, c.getCommentaire());
            ps.setInt(11, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM cours WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int countTotal() throws SQLException {
        try (ResultSet rs = getConn().createStatement().executeQuery("SELECT COUNT(*) FROM cours")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Retourne le nombre de cours planifiés par salle, triés par occupation décroissante.
     * Compatible MySQL.
     */
    public Map<String, Integer> getUsageParSalle() throws SQLException {
        String sql = "SELECT s.numero AS nomSalle, COUNT(c.id) AS nbCours" +
                     " FROM cours c" +
                     " JOIN salles s ON c.salle_id = s.id" +
                     " GROUP BY s.id, s.numero" +
                     " ORDER BY nbCours DESC";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString("nomSalle"), rs.getInt("nbCours"));
            }
        }
        return result;
    }

    // ---- helpers ----
    @FunctionalInterface
    interface PSetter { void set(PreparedStatement ps) throws SQLException; }

    private List<Cours> findBySqlRaw(String sql, PSetter setter) throws SQLException {
        List<Cours> list = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            if (setter != null) setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCours(rs));
            }
        }
        return list;
    }

    private Cours mapCours(ResultSet rs) throws SQLException {
        Cours c = new Cours();
        c.setId(rs.getInt("id"));
        c.setMatiereId(rs.getInt("matiere_id"));
        c.setEnseignantId(rs.getInt("enseignant_id"));
        c.setClasseId(rs.getInt("classe_id"));

        int salleId = rs.getInt("salle_id");
        if (!rs.wasNull()) c.setSalleId(salleId);

        c.setJourSemaine(rs.getInt("jour_semaine"));

        String hd = rs.getString("heure_debut");
        if (hd != null) {
            if (hd.length() == 5) hd = hd + ":00";
            c.setHeureDebut(LocalTime.parse(hd));
        }

        c.setDureeMinutes(rs.getInt("duree_minutes"));

        String type = rs.getString("type_cours");
        if (type != null) c.setTypeCours(Cours.TypeCours.valueOf(type));
        c.setRecurrent(rs.getBoolean("recurrent"));

        try { c.setNomMatiere(rs.getString("nomMatiere"));         } catch (SQLException ignored) {}
        try { c.setNomEnseignant(rs.getString("nomEnseignant"));   } catch (SQLException ignored) {}
        try { c.setNomClasse(rs.getString("nomClasse"));           } catch (SQLException ignored) {}
        try { c.setNomSalle(rs.getString("nomSalle"));             } catch (SQLException ignored) {}
        try { c.setCouleurMatiere(rs.getString("couleurMatiere")); } catch (SQLException ignored) {}

        return c;
    }

    private String calculerHeureFin(String heureDebut, int dureeMin) {
        String[] parts = heureDebut.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]) + dureeMin;
        h += m / 60;
        m  = m % 60;
        return String.format("%02d:%02d", h, m);
    }
}
