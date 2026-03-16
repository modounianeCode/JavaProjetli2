package univscheduler.model;

/**
 * Enumération des rôles utilisateurs dans le système.
 */
public enum Role {
    ADMIN("Administrateur"),
    GESTIONNAIRE("Gestionnaire d'emploi du temps"),
    ENSEIGNANT("Enseignant"),
    ETUDIANT("Étudiant");

    private final String libelle;

    Role(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() { return libelle; }

    @Override
    public String toString() { return libelle; }
}
