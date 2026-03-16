package univscheduler.util;

import java.sql.*;

/**
 * Gestionnaire de connexion MySQL (XAMPP).
 *
 * Strategie : une connexion par thread (ThreadLocal).
 * - Les DAOs qui utilisent getConn() sans try-with-resources obtiennent
 *   toujours la meme connexion dans leur thread => pas de conflit de ResultSet.
 * - Les DAOs qui utilisent try-with-resources ferment correctement leur connexion
 *   ce qui force une nouvelle connexion au prochain appel dans ce thread.
 * - Compatible avec tous les DAOs existants sans aucune modification.
 */
public class DatabaseConnection {

    // ── Modifier ces valeurs si besoin ────────────────────
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    private static final String DB_NAME  = "univ_scheduler";
    private static final String USER     = "root";
    private static final String PASSWORD = "";
    // ──────────────────────────────────────────────────────

    private static final String URL =
        "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
        + "?useSSL=false"
        + "&allowPublicKeyRetrieval=true"
        + "&serverTimezone=UTC"
        + "&useUnicode=true"
        + "&characterEncoding=UTF-8"
        + "&allowMultiQueries=true"
        + "&autoReconnect=true";

    // Une connexion independante par thread
    private static final ThreadLocal<Connection> threadConn = new ThreadLocal<>();

    // Singleton (conserve pour getInstance())
    private static final DatabaseConnection INSTANCE = new DatabaseConnection();

    private DatabaseConnection() {
        try { Class.forName("com.mysql.cj.jdbc.Driver"); }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "Driver MySQL introuvable ! Ajoutez mysql-connector-j.jar au Build Path.", e);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        return INSTANCE;
    }

    /**
     * Retourne la connexion du thread courant.
     * Si elle est absente, fermee ou invalide, une nouvelle est creee.
     * Chaque thread a sa propre connexion => pas de conflit entre DAOs concurrents.
     */
    public Connection getConnection() {
        try {
            Connection conn = threadConn.get();
            if (conn == null || conn.isClosed() || !conn.isValid(1)) {
                if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
                conn = DriverManager.getConnection(URL, USER, PASSWORD);
                threadConn.set(conn);
                System.out.println("Connexion MySQL etablie pour thread ["
                    + Thread.currentThread().getName() + "] : " + DB_NAME);
            }
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException(
                "Erreur connexion MySQL : " + e.getMessage()
                + "\n-> Verifiez que XAMPP est demarre et que la base '"
                + DB_NAME + "' existe.", e);
        }
    }

    public boolean isConnected() {
        try {
            Connection conn = threadConn.get();
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /** Ferme la connexion du thread courant. Appeler en fin de thread si necessaire. */
    public void closeConnection() {
        Connection conn = threadConn.get();
        if (conn != null) {
            try {
                if (!conn.isClosed()) conn.close();
                System.out.println("Connexion MySQL fermee pour thread ["
                    + Thread.currentThread().getName() + "]");
            } catch (SQLException e) {
                System.err.println("Erreur fermeture : " + e.getMessage());
            } finally {
                threadConn.remove();
            }
        }
    }
}
