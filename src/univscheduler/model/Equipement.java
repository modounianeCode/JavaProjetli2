package univscheduler.model;

/**
 * Modèle représentant un équipement de salle.
 * Ex : Vidéoprojecteur, Tableau interactif, Climatisation...
 */
public class Equipement {

    public enum Categorie {
        AUDIOVISUEL("Audiovisuel"),
        INFORMATIQUE("Informatique"),
        MOBILIER("Mobilier"),
        CONFORT("Confort"),
        AUTRE("Autre");

        private final String libelle;
        Categorie(String libelle) { this.libelle = libelle; }
        public String getLibelle() { return libelle; }
        @Override public String toString() { return libelle; }
    }

    private int       id;
    private String    libelle;
    private Categorie categorie;
    private String    description;

    // Pour l'affichage dans le panneau équipements ↔ salles
    private int    nbSalles;   // Nombre de salles qui ont cet équipement

    public Equipement() {}

    public Equipement(String libelle, Categorie categorie, String description) {
        this.libelle     = libelle;
        this.categorie   = categorie;
        this.description = description;
    }

    public int       getId()                              { return id; }
    public void      setId(int id)                        { this.id = id; }

    public String    getLibelle()                         { return libelle; }
    public void      setLibelle(String libelle)           { this.libelle = libelle; }

    public Categorie getCategorie()                       { return categorie; }
    public void      setCategorie(Categorie categorie)    { this.categorie = categorie; }

    public String    getDescription()                     { return description; }
    public void      setDescription(String description)   { this.description = description; }

    public int       getNbSalles()                        { return nbSalles; }
    public void      setNbSalles(int nbSalles)            { this.nbSalles = nbSalles; }

    @Override public String toString() { return libelle; }
}
