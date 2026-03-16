package univscheduler.model;

/**
 * Modèle représentant un bâtiment universitaire.
 */
public class Batiment {
    private int id;
    private String nom;
    private String localisation;
    private int nombreEtages;

    public Batiment() {}

    public Batiment(String nom, String localisation, int nombreEtages) {
        this.nom = nom;
        this.localisation = localisation;
        this.nombreEtages = nombreEtages;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public int getNombreEtages() { return nombreEtages; }
    public void setNombreEtages(int nombreEtages) { this.nombreEtages = nombreEtages; }

    @Override
    public String toString() { return nom; }
}
