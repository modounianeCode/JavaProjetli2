package univscheduler.dao;

import univscheduler.model.Classe;
import univscheduler.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClasseDAO {
    private Connection getConn() { return DatabaseConnection.getInstance().getConnection(); }

    public List<Classe> findAll() throws SQLException {
        List<Classe> list = new ArrayList<>();
        try (ResultSet rs = getConn().createStatement().executeQuery("SELECT * FROM classes ORDER BY nom")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Classe save(Classe c) throws SQLException {
        String sql = "INSERT INTO classes (nom, filiere, effectif, niveau) VALUES (?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNom()); ps.setString(2, c.getFiliere());
            ps.setInt(3, c.getEffectif()); ps.setString(4, c.getNiveau());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) c.setId(keys.getInt(1)); }
        }
        return c;
    }

    public void update(Classe c) throws SQLException {
        String sql = "UPDATE classes SET nom=?, filiere=?, effectif=?, niveau=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, c.getNom()); ps.setString(2, c.getFiliere());
            ps.setInt(3, c.getEffectif()); ps.setString(4, c.getNiveau()); ps.setInt(5, c.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM classes WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    private Classe map(ResultSet rs) throws SQLException {
        Classe c = new Classe();
        c.setId(rs.getInt("id")); c.setNom(rs.getString("nom"));
        c.setFiliere(rs.getString("filiere")); c.setEffectif(rs.getInt("effectif"));
        c.setNiveau(rs.getString("niveau"));
        return c;
    }
}
