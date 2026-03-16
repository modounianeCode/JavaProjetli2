package univscheduler.model;

import java.time.LocalTime;
import java.time.LocalDate;

/**
 * Modèle représentant une séance de cours planifiée.
 */
public class Cours {
    public enum TypeCours { CM, TD, TP }

    private int id;
    private Matiere matiere;
    private int matiereId;
    private Utilisateur enseignant;
    private int enseignantId;
    private Classe classe;
    private int classeId;
    private Salle salle;
    private Integer salleId; // nullable
    private int jourSemaine; // 1=Lundi, 2=Mardi ... 6=Samedi
    private LocalTime heureDebut;
    private int dureeMinutes;
    private TypeCours typeCours;
    private boolean recurrent;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String commentaire;

    // Champs calculés (pour l'affichage)
    private String nomMatiere;
    private String nomEnseignant;
    private String nomClasse;
    private String nomSalle;
    private String couleurMatiere;

    public Cours() {
        this.recurrent = true;
        this.dureeMinutes = 90;
        this.typeCours = TypeCours.CM;
    }

    public static final String[] JOURS = {
        "", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    };

    public String getJourLibelle() {
        if (jourSemaine >= 1 && jourSemaine <= 7) return JOURS[jourSemaine];
        return "Inconnu";
    }

    public LocalTime getHeureFin() {
        if (heureDebut == null) return null;
        return heureDebut.plusMinutes(dureeMinutes);
    }

    /**
     * Vérifie si ce cours est en conflit horaire avec un autre cours sur la même salle.
     */
    public boolean isEnConflitAvec(Cours autre) {
        if (this.salleId == null || autre.salleId == null) return false;
        if (!this.salleId.equals(autre.salleId)) return false;
        if (this.jourSemaine != autre.jourSemaine) return false;
        if (this.id == autre.id) return false;

        // Vérification du chevauchement horaire
        LocalTime fin1 = this.getHeureFin();
        LocalTime fin2 = autre.getHeureFin();
        return this.heureDebut.isBefore(fin2) && autre.heureDebut.isBefore(fin1);
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Matiere getMatiere() { return matiere; }
    public void setMatiere(Matiere matiere) { this.matiere = matiere; }

    public int getMatiereId() { return matiereId; }
    public void setMatiereId(int matiereId) { this.matiereId = matiereId; }

    public Utilisateur getEnseignant() { return enseignant; }
    public void setEnseignant(Utilisateur enseignant) { this.enseignant = enseignant; }

    public int getEnseignantId() { return enseignantId; }
    public void setEnseignantId(int enseignantId) { this.enseignantId = enseignantId; }

    public Classe getClasse() { return classe; }
    public void setClasse(Classe classe) { this.classe = classe; }

    public int getClasseId() { return classeId; }
    public void setClasseId(int classeId) { this.classeId = classeId; }

    public Salle getSalle() { return salle; }
    public void setSalle(Salle salle) { this.salle = salle; }

    public Integer getSalleId() { return salleId; }
    public void setSalleId(Integer salleId) { this.salleId = salleId; }

    public int getJourSemaine() { return jourSemaine; }
    public void setJourSemaine(int jourSemaine) { this.jourSemaine = jourSemaine; }

    public LocalTime getHeureDebut() { return heureDebut; }
    public void setHeureDebut(LocalTime heureDebut) { this.heureDebut = heureDebut; }

    public int getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(int dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public TypeCours getTypeCours() { return typeCours; }
    public void setTypeCours(TypeCours typeCours) { this.typeCours = typeCours; }

    public boolean isRecurrent() { return recurrent; }
    public void setRecurrent(boolean recurrent) { this.recurrent = recurrent; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    // Champs calculés
    public String getNomMatiere() { return nomMatiere; }
    public void setNomMatiere(String nomMatiere) { this.nomMatiere = nomMatiere; }

    public String getNomEnseignant() { return nomEnseignant; }
    public void setNomEnseignant(String nomEnseignant) { this.nomEnseignant = nomEnseignant; }

    public String getNomClasse() { return nomClasse; }
    public void setNomClasse(String nomClasse) { this.nomClasse = nomClasse; }

    public String getNomSalle() { return nomSalle; }
    public void setNomSalle(String nomSalle) { this.nomSalle = nomSalle; }

    public String getCouleurMatiere() { return couleurMatiere; }
    public void setCouleurMatiere(String couleurMatiere) { this.couleurMatiere = couleurMatiere; }

    @Override
    public String toString() {
        return String.format("%s | %s | %s | %s %s-%s",
            nomMatiere, nomClasse, nomSalle,
            getJourLibelle(), heureDebut, getHeureFin());
    }
}
