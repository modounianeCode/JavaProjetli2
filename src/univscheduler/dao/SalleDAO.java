package univscheduler.dao;

import univscheduler.model.*;
import univscheduler.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) pour les opérations CRUD sur les salles.
 */
public class SalleDAO {

    private Connection getConn() {
        return DatabaseConnection.getInstance().getConnection();
    }

    /**
     * Récupère toutes les salles avec leurs informations complètes.
     */
    public List<Salle> findAll() throws SQLException {
        List<Salle> salles = new ArrayList<>();
        String sql = """
            SELECT s.*, ts.libelle AS type_libelle, b.nom AS batiment_nom, b.localisation
            FROM salles s
            JOIN types_salle ts ON s.type_salle_id = ts.id
            JOIN batiments b ON s.batiment_id = b.id
            ORDER BY b.nom, s.numero
        """;
        try (PreparedStatement ps = getConn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                salles.add(mapSalle(rs));
            }
        }
        // Charger les équipements
        for (Salle s : salles) {
            s.setEquipements(findEquipements(s.getId()));
        }
        return salles;
    }

    /**
     * Trouve une salle par son ID.
     */
    public Salle findById(int id) throws SQLException {
        String sql = """
            SELECT s.*, ts.libelle AS type_libelle, b.nom AS batiment_nom, b.localisation
            FROM salles s
            JOIN types_salle ts ON s.type_salle_id = ts.id
            JOIN batiments b ON s.batiment_id = b.id
            WHERE s.id = ?
        """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Salle s = mapSalle(rs);
                    s.setEquipements(findEquipements(id));
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Recherche de salles disponibles à un créneau donné.
     * @param jourSemaine 1=Lundi ... 6=Samedi
     * @param heureDebut  ex: "08:00"
     * @param dureeMin    durée en minutes
     * @param capaciteMin capacité minimale requise
     */
    public List<Salle> findDisponibles(int jourSemaine, String heureDebut,
                                       int dureeMin, int capaciteMin) throws SQLException {
        List<Salle> salles = new ArrayList<>();
        String heureFin = calculerHeureFin(heureDebut, dureeMin);

        String sql = """
            SELECT s.*, ts.libelle AS type_libelle, b.nom AS batiment_nom, b.localisation
            FROM salles s
            JOIN types_salle ts ON s.type_salle_id = ts.id
            JOIN batiments b ON s.batiment_id = b.id
            WHERE s.disponible = 1
            AND s.capacite >= ?
            AND s.id NOT IN (
                SELECT salle_id FROM cours
                WHERE jour_semaine = ?
                AND salle_id IS NOT NULL
                AND heure_debut < ?
                AND time(heure_debut, '+' || duree_minutes || ' minutes') > ?
            )
            ORDER BY s.capacite
        """;
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, capaciteMin);
            ps.setInt(2, jourSemaine);
            ps.setString(3, heureFin);
            ps.setString(4, heureDebut);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    salles.add(mapSalle(rs));
                }
            }
        }
        return salles;
    }

    /**
     * Insère une nouvelle salle.
     */
    public Salle save(Salle salle) throws SQLException {
        String sql = "INSERT INTO salles (numero, nom, capacite, etage, type_salle_id, batiment_id, disponible) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, salle.getNumero());
            ps.setString(2, salle.getNom());
            ps.setInt(3, salle.getCapacite());
            ps.setInt(4, salle.getEtage());
            ps.setInt(5, salle.getTypeSalleId());
            ps.setInt(6, salle.getBatimentId());
            ps.setBoolean(7, salle.isDisponible());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) salle.setId(keys.getInt(1));
            }
        }
        return salle;
    }

    /**
     * Met à jour une salle existante.
     */
    public void update(Salle salle) throws SQLException {
        String sql = "UPDATE salles SET numero=?, nom=?, capacite=?, etage=?, type_salle_id=?, batiment_id=?, disponible=? WHERE id=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, salle.getNumero());
            ps.setString(2, salle.getNom());
            ps.setInt(3, salle.getCapacite());
            ps.setInt(4, salle.getEtage());
            ps.setInt(5, salle.getTypeSalleId());
            ps.setInt(6, salle.getBatimentId());
            ps.setBoolean(7, salle.isDisponible());
            ps.setInt(8, salle.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Recherche avancée multi-critères.
     *
     * @param capaciteMin  capacité minimale (0 = ignoré)
     * @param typeSalle    type salle ex "TD", "TP", "Amphi" (null = tous)
     * @param equipementId ID équipement requis (0 = ignoré)
     * @param jourSemaine  1=Lundi..6=Samedi (0 = ignoré)
     * @param heureDebut   ex "08:00" (null = ignoré)
     * @param dureeMin     durée en minutes (0 = 60 par défaut)
     */
    public List<Salle> rechercheAvancee(int capaciteMin, String typeSalle,
                                        int equipementId, int jourSemaine,
                                        String heureDebut, int dureeMin) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT s.*, ts.libelle AS type_libelle, b.nom AS batiment_nom, b.localisation
            FROM salles s
            JOIN types_salle ts ON s.type_salle_id = ts.id
            JOIN batiments b ON s.batiment_id = b.id
            WHERE s.disponible = 1
        """);

        List<Object> params = new ArrayList<>();

        if (capaciteMin > 0) {
            sql.append(" AND s.capacite >= ?");
            params.add(capaciteMin);
        }

        if (typeSalle != null && !typeSalle.isEmpty()) {
            sql.append(" AND LOWER(ts.libelle) LIKE LOWER(?)");
            params.add("%" + typeSalle + "%");
        }

        if (equipementId > 0) {
            sql.append(" AND s.id IN (SELECT salle_id FROM salle_equipements WHERE equipement_id = ?)");
            params.add(equipementId);
        }

        if (jourSemaine > 0 && heureDebut != null && !heureDebut.isEmpty()) {
            int dur = dureeMin > 0 ? dureeMin : 60;
            String heureFin = calculerHeureFin(heureDebut, dur);
            sql.append("""
                 AND s.id NOT IN (
                    SELECT salle_id FROM cours
                    WHERE jour_semaine = ?
                    AND salle_id IS NOT NULL
                    AND heure_debut < ?
                    AND ADDTIME(heure_debut, SEC_TO_TIME(duree_minutes * 60)) > ?
                )
            """);
            params.add(jourSemaine);
            params.add(heureFin);
            params.add(heureDebut);
        }

        sql.append(" ORDER BY s.capacite, b.nom, s.numero");

        List<Salle> salles = new ArrayList<>();
        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else                     ps.setString(i + 1, (String) p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) salles.add(mapSalle(rs));
            }
        }
        for (Salle s : salles) s.setEquipements(findEquipements(s.getId()));
        return salles;
    }

    /**
     * Salles disponibles maintenant : pas de cours en cours à l'heure courante.
     */
    public List<Salle> findDisponiblesMaintenantAvancee(int capaciteMin, String typeSalle,
                                                        int equipementId) throws SQLException {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // jour semaine : 1=Lundi..7=Dimanche en Java -> adapter à 1=Lundi..6=Samedi
        int dow = now.getDayOfWeek().getValue(); // 1=Mon..7=Sun
        if (dow > 6) return new ArrayList<>();   // Dimanche -> aucun cours
        String hNow = String.format("%02d:%02d", now.getHour(), now.getMinute());
        return rechercheAvancee(capaciteMin, typeSalle, equipementId, dow, hNow, 1);
    }

    /**
     * Supprime une salle par son ID.
     */
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM salles WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Récupère les équipements d'une salle.
     */
    public List<String> findEquipements(int salleId) throws SQLException {
        List<String> eq = new ArrayList<>();
        String sql = "SELECT e.libelle FROM equipements e JOIN salle_equipements se ON e.id = se.equipement_id WHERE se.salle_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, salleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) eq.add(rs.getString("libelle"));
            }
        }
        return eq;
    }

    /**
     * Compte le nombre de cours pour chaque salle (taux d'occupation).
     */
    public int countCours(int salleId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM cours WHERE salle_id = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, salleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    // Mapping ResultSet → Salle
    private Salle mapSalle(ResultSet rs) throws SQLException {
        Salle s = new Salle();
        s.setId(rs.getInt("id"));
        s.setNumero(rs.getString("numero"));
        s.setNom(rs.getString("nom"));
        s.setCapacite(rs.getInt("capacite"));
        s.setEtage(rs.getInt("etage"));
        s.setTypeSalle(rs.getString("type_libelle"));
        s.setTypeSalleId(rs.getInt("type_salle_id"));
        s.setBatimentId(rs.getInt("batiment_id"));
        s.setDisponible(rs.getBoolean("disponible"));

        Batiment b = new Batiment();
        b.setId(rs.getInt("batiment_id"));
        b.setNom(rs.getString("batiment_nom"));
        b.setLocalisation(rs.getString("localisation"));
        s.setBatiment(b);

        return s;
    }

    private String calculerHeureFin(String heureDebut, int dureeMin) {
        String[] parts = heureDebut.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]) + dureeMin;
        h += m / 60;
        m = m % 60;
        return String.format("%02d:%02d", h, m);
    }
}
