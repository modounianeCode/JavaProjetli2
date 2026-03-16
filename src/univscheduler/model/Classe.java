package univscheduler.model;

/**
 * Modèle représentant une classe/promotion d'étudiants.
 */
public class Classe {
    private int id;
    private String nom;
    private String filiere;
    private int effectif;
    private String niveau;

    public Classe() {}

    public Classe(String nom, String filiere, int effectif, String niveau) {
        this.nom = nom;
        this.filiere = filiere;
        this.effectif = effectif;
        this.niveau = niveau;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getFiliere() { return filiere; }
    public void setFiliere(String filiere) { this.filiere = filiere; }

    public int getEffectif() { return effectif; }
    public void setEffectif(int effectif) { this.effectif = effectif; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    @Override
    public String toString() { return nom + " (" + effectif + " étudiants)"; }
}
