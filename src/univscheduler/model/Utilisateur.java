package univscheduler.model;

import java.time.LocalDateTime;

/**
 * Modèle représentant un utilisateur du système.
 */
public class Utilisateur {
    private int id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasse; // hashé
    private Role role;
    private boolean actif;
    private LocalDateTime dateCreation;

    public Utilisateur() {}

    public Utilisateur(String nom, String prenom, String email, String motDePasse, Role role) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.actif = true;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNomComplet() { return prenom + " " + nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isGestionnaire() { return role == Role.GESTIONNAIRE; }
    public boolean isEnseignant() { return role == Role.ENSEIGNANT; }
    public boolean isEtudiant() { return role == Role.ETUDIANT; }

    @Override
    public String toString() {
        return getNomComplet() + " (" + role.getLibelle() + ")";
    }
}
