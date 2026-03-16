package univscheduler.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle représentant une salle de cours.
 */
public class Salle {
    private int id;
    private String numero;
    private String nom;
    private int capacite;
    private int etage;
    private String typeSalle; // TD, TP, Amphi, Réunion
    private int typeSalleId;
    private Batiment batiment;
    private int batimentId;
    private boolean disponible;
    private List<String> equipements;

    public Salle() {
        this.equipements = new ArrayList<>();
        this.disponible = true;
    }

    public Salle(String numero, String nom, int capacite, String typeSalle, Batiment batiment) {
        this();
        this.numero = numero;
        this.nom = nom;
        this.capacite = capacite;
        this.typeSalle = typeSalle;
        this.batiment = batiment;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }

    public int getEtage() { return etage; }
    public void setEtage(int etage) { this.etage = etage; }

    public String getTypeSalle() { return typeSalle; }
    public void setTypeSalle(String typeSalle) { this.typeSalle = typeSalle; }

    public int getTypeSalleId() { return typeSalleId; }
    public void setTypeSalleId(int typeSalleId) { this.typeSalleId = typeSalleId; }

    public Batiment getBatiment() { return batiment; }
    public void setBatiment(Batiment batiment) { this.batiment = batiment; }

    public int getBatimentId() { return batimentId; }
    public void setBatimentId(int batimentId) { this.batimentId = batimentId; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    public List<String> getEquipements() { return equipements; }
    public void setEquipements(List<String> equipements) { this.equipements = equipements; }
    public void addEquipement(String eq) { this.equipements.add(eq); }

    public String getNomComplet() {
        String batNom = batiment != null ? batiment.getNom() : "?";
        return numero + " - " + nom + " (" + batNom + ")";
    }

    @Override
    public String toString() {
        return numero + " | " + (nom != null ? nom : "") + " | Cap: " + capacite + " | " + typeSalle;
    }
}
