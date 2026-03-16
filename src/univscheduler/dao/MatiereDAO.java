package univscheduler.dao;

import univscheduler.model.*;
import univscheduler.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour les Matières.
 */
public class MatiereDAO {
    private Connection getConn() { return DatabaseConnection.getInstance().getConnection(); }

    public List<Matiere> findAll() throws SQLException {
        List<Matiere> list = new ArrayList<>();
        try (ResultSet rs = getConn().createStatement().executeQuery("SELECT * FROM matieres ORDER BY libelle")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Matiere save(Matiere m) throws SQLException {
        String sql = "INSERT INTO matieres (code, libelle, volume_horaire, couleur) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getCode());
            ps.setString(2, m.getLibelle());
            ps.setInt(3, m.getVolumeHoraire());
            ps.setString(4, m.getCouleur());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) m.setId(keys.getInt(1)); }
        }
        return m;
    }

    public void update(Matiere m) throws SQLException {
        String sql = "UPDATE matieres SET code=?, libelle=?, volume_horaire=?, couleur=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, m.getCode()); ps.setString(2, m.getLibelle());
            ps.setInt(3, m.getVolumeHoraire()); ps.setString(4, m.getCouleur()); ps.setInt(5, m.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM matieres WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    private Matiere map(ResultSet rs) throws SQLException {
        Matiere m = new Matiere();
        m.setId(rs.getInt("id")); m.setCode(rs.getString("code"));
        m.setLibelle(rs.getString("libelle")); m.setVolumeHoraire(rs.getInt("volume_horaire"));
        m.setCouleur(rs.getString("couleur"));
        return m;
    }
}
