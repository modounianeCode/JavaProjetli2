package univscheduler.dao;

import univscheduler.model.Equipement;
import univscheduler.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des équipements et leur liaison avec les salles.
 */
public class EquipementDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    // ── CRUD Équipements ────────────────────────────────────────────────────

    /** Retourne tous les équipements avec le nombre de salles associées. */
    public List<Equipement> findAll() throws SQLException {
        List<Equipement> list = new ArrayList<>();
        String sql = "SELECT e.*, " +
                     "(SELECT COUNT(*) FROM salle_equipements se WHERE se.equipement_id = e.id) AS nb_salles " +
                     "FROM equipements e ORDER BY e.categorie, e.libelle";
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    /** Trouve un équipement par son ID. */
    public Equipement findById(int id) throws SQLException {
        String sql = "SELECT e.*, " +
                     "(SELECT COUNT(*) FROM salle_equipements se WHERE se.equipement_id = e.id) AS nb_salles " +
                     "FROM equipements e WHERE e.id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    /** Insère un nouvel équipement. */
    public void save(Equipement e) throws SQLException {
        String sql = "INSERT INTO equipements (libelle, categorie, description) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getLibelle());
            ps.setString(2, e.getCategorie() != null ? e.getCategorie().name() : "AUTRE");
            ps.setString(3, e.getDescription() != null ? e.getDescription() : "");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) e.setId(keys.getInt(1));
            }
        }
    }

    /** Met à jour un équipement existant. */
    public void update(Equipement e) throws SQLException {
        String sql = "UPDATE equipements SET libelle=?, categorie=?, description=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, e.getLibelle());
            ps.setString(2, e.getCategorie() != null ? e.getCategorie().name() : "AUTRE");
            ps.setString(3, e.getDescription() != null ? e.getDescription() : "");
            ps.setInt(4, e.getId());
            ps.executeUpdate();
        }
    }

    /** Supprime un équipement (et ses liaisons salle_equipements en cascade). */
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM salle_equipements WHERE equipement_id = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM equipements WHERE id = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    // ── Liaison Salle ↔ Équipement ──────────────────────────────────────────

    /** Retourne les IDs des équipements d'une salle. */
    public List<Integer> findEquipementIdsBySalle(int salleId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT equipement_id FROM salle_equipements WHERE salle_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, salleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    /** Retourne les équipements d'une salle (objets complets). */
    public List<Equipement> findBySalle(int salleId) throws SQLException {
        List<Equipement> list = new ArrayList<>();
        String sql = "SELECT e.*, 0 AS nb_salles FROM equipements e " +
                     "JOIN salle_equipements se ON e.id = se.equipement_id " +
                     "WHERE se.salle_id = ? ORDER BY e.categorie, e.libelle";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, salleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Ajoute un équipement à une salle (ignore si déjà présent). */
    public void addToSalle(int salleId, int equipementId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO salle_equipements (salle_id, equipement_id) VALUES (?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, salleId); ps.setInt(2, equipementId);
            ps.executeUpdate();
        }
    }

    /** Retire un équipement d'une salle. */
    public void removeFromSalle(int salleId, int equipementId) throws SQLException {
        String sql = "DELETE FROM salle_equipements WHERE salle_id = ? AND equipement_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, salleId); ps.setInt(2, equipementId);
            ps.executeUpdate();
        }
    }

    /** Remplace tous les équipements d'une salle par une nouvelle liste. */
    public void setSalleEquipements(int salleId, List<Integer> equipementIds) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement(
                "DELETE FROM salle_equipements WHERE salle_id = ?")) {
            ps.setInt(1, salleId); ps.executeUpdate();
        }
        for (int eqId : equipementIds) addToSalle(salleId, eqId);
    }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private Equipement map(ResultSet rs) throws SQLException {
        Equipement e = new Equipement();
        e.setId(rs.getInt("id"));
        e.setLibelle(rs.getString("libelle"));
        e.setDescription(rs.getString("description"));
        e.setNbSalles(rs.getInt("nb_salles"));
        try {
            String cat = rs.getString("categorie");
            if (cat != null) e.setCategorie(Equipement.Categorie.valueOf(cat));
        } catch (IllegalArgumentException ex) {
            e.setCategorie(Equipement.Categorie.AUTRE);
        }
        return e;
    }
}
