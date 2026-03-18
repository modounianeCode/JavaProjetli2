package univscheduler.service;

import univscheduler.model.Utilisateur;

/**
 * Gestionnaire de session utilisateur (Singleton).
 * Maintient l'utilisateur connecté tout au long de la session.
 */
public class SessionManager {

    private static SessionManager instance;
    private Utilisateur utilisateurConnecte;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void connect(Utilisateur u) {
        this.utilisateurConnecte = u;
        System.out.println("🔐 Connexion : " + u.getNomComplet() + " [" + u.getRole() + "]");
    }

    public void disconnect() {
        if (utilisateurConnecte != null) {
            System.out.println("🔓 Déconnexion : " + utilisateurConnecte.getNomComplet());
        }
        utilisateurConnecte = null;
    }

    public Utilisateur getUtilisateur() { return utilisateurConnecte; }

    public boolean isConnected() { return utilisateurConnecte != null; }

    public boolean isAdmin() {
        return isConnected() && utilisateurConnecte.isAdmin();
    }

    public boolean isGestionnaire() {
        return isConnected() && (utilisateurConnecte.isGestionnaire() || utilisateurConnecte.isAdmin());
    }

    public boolean isEnseignant() {
        return isConnected() && utilisateurConnecte.isEnseignant();
    }

    public boolean canManageCours() {
        return isConnected() && (utilisateurConnecte.isAdmin() || utilisateurConnecte.isGestionnaire());
    }

    public boolean canReserverSalle() {
        return isConnected() && !utilisateurConnecte.isEtudiant();
    }
}
