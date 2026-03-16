package univscheduler.dao;

import univscheduler.model.*;
import univscheduler.util.DatabaseConnection;
import univscheduler.util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurDAO {

    public Utilisateur authenticate(String email, String password) throws SQLException {
        String sql = "SELECT * FROM utilisateurs WHERE email = ? AND actif = 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashBDD = rs.getString("mot_de_passe");
                    if (PasswordUtil.verify(password, hashBDD)) return mapUtilisateur(rs);
                }
            }
        }
        return null;
    }

    public List<Utilisateur> findAll() throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        String sql = "SELECT * FROM utilisateurs ORDER BY nom, prenom";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapUtilisateur(rs));
        }
        return list;
    }

    public List<Utilisateur> findByRole(Role role) throws SQLException {
        List<Utilisateur> list = new ArrayList<>();
        String sql = "SELECT * FROM utilisateurs WHERE role = ? AND actif = 1 ORDER BY nom";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapUtilisateur(rs));
            }
        }
        return list;
    }

    public Utilisateur findById(int id) throws SQLException {
        String sql = "SELECT * FROM utilisateurs WHERE id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUtilisateur(rs);
            }
        }
        return null;
    }

    public Utilisateur save(Utilisateur u) throws SQLException {
        String sql = "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role, actif) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, PasswordUtil.hash(u.getMotDePasse()));
            ps.setString(5, u.getRole().name());
            ps.setBoolean(6, u.isActif());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) u.setId(keys.getInt(1));
            }
        }
        return u;
    }

    public void update(Utilisateur u) throws SQLException {
        String sql = "UPDATE utilisateurs SET nom=?, prenom=?, email=?, role=?, actif=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getNom());
            ps.setString(2, u.getPrenom());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getRole().name());
            ps.setBoolean(5, u.isActif());
            ps.setInt(6, u.getId());
            ps.executeUpdate();
        }
    }

    public void updatePassword(int id, String newPassword) throws SQLException {
        String sql = "UPDATE utilisateurs SET mot_de_passe=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hash(newPassword));
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM utilisateurs WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean emailExists(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM utilisateurs WHERE email = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private Utilisateur mapUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasse(rs.getString("mot_de_passe"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setActif(rs.getBoolean("actif"));
        return u;
    }
}
