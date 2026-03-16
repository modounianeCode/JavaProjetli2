package univscheduler.model;

/**
 * Modèle représentant une matière/module d'enseignement.
 */
public class Matiere {
    private int id;
    private String code;
    private String libelle;
    private int volumeHoraire;
    private String couleur;

    public Matiere() {}

    public Matiere(String code, String libelle) {
        this.code = code;
        this.libelle = libelle;
        this.couleur = "#3498db";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public int getVolumeHoraire() { return volumeHoraire; }
    public void setVolumeHoraire(int volumeHoraire) { this.volumeHoraire = volumeHoraire; }

    public String getCouleur() { return couleur; }
    public void setCouleur(String couleur) { this.couleur = couleur; }

    @Override
    public String toString() { return code + " - " + libelle; }
}
