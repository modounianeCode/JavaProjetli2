package univscheduler.dao;

import univscheduler.model.Batiment;
import univscheduler.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BatimentDAO {
    private Connection getConn() { return DatabaseConnection.getInstance().getConnection(); }

    public List<Batiment> findAll() throws SQLException {
        List<Batiment> list = new ArrayList<>();
        try (ResultSet rs = getConn().createStatement().executeQuery("SELECT * FROM batiments ORDER BY nom")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Batiment save(Batiment b) throws SQLException {
        String sql = "INSERT INTO batiments (nom, localisation, nombre_etages) VALUES (?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getNom()); ps.setString(2, b.getLocalisation()); ps.setInt(3, b.getNombreEtages());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { if (k.next()) b.setId(k.getInt(1)); }
        }
        return b;
    }

    public void update(Batiment b) throws SQLException {
        String sql = "UPDATE batiments SET nom=?, localisation=?, nombre_etages=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, b.getNom()); ps.setString(2, b.getLocalisation());
            ps.setInt(3, b.getNombreEtages()); ps.setInt(4, b.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM batiments WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    private Batiment map(ResultSet rs) throws SQLException {
        Batiment b = new Batiment();
        b.setId(rs.getInt("id")); b.setNom(rs.getString("nom"));
        b.setLocalisation(rs.getString("localisation")); b.setNombreEtages(rs.getInt("nombre_etages"));
        return b;
    }
}
