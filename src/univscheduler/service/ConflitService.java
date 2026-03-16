package univscheduler.service;

import univscheduler.dao.CoursDAO;
import univscheduler.model.Cours;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de détection intelligente des conflits horaires.
 * Vérifie les conflits de salle ET d'enseignant avant toute planification.
 */
public class ConflitService {

    private final CoursDAO coursDAO;

    public ConflitService() {
        this.coursDAO = new CoursDAO();
    }

    public static class ResultatAnalyse {
        public final List<String> conflits = new ArrayList<>();
        public final List<String> avertissements = new ArrayList<>();

        public boolean hasConflits() { return !conflits.isEmpty(); }
        public boolean hasAvertissements() { return !avertissements.isEmpty(); }

        public String getSummary() {
            if (!hasConflits() && !hasAvertissements()) return "✅ Aucun conflit détecté";
            StringBuilder sb = new StringBuilder();
            for (String c : conflits) sb.append("🔴 CONFLIT : ").append(c).append("\n");
            for (String a : avertissements) sb.append("🟡 AVERT. : ").append(a).append("\n");
            return sb.toString().trim();
        }
    }

    /**
     * Analyse complète avant ajout/modification d'un cours.
     */
    public ResultatAnalyse analyser(Cours cours) {
        ResultatAnalyse resultat = new ResultatAnalyse();

        try {
            // 1. Conflit de salle
            if (cours.getSalleId() != null) {
                List<Cours> conflitsSalle = coursDAO.detecterConflitsSalle(
                    cours.getSalleId(),
                    cours.getJourSemaine(),
                    cours.getHeureDebut().toString(),
                    cours.getDureeMinutes(),
                    cours.getId()
                );
                for (Cours c : conflitsSalle) {
                    resultat.conflits.add(String.format(
                        "Salle déjà occupée par '%s' (%s) de %s à %s",
                        c.getNomMatiere(), c.getNomClasse(),
                        c.getHeureDebut(), c.getHeureFin()
                    ));
                }
            }

            // 2. Conflit enseignant
            List<Cours> conflitsProf = coursDAO.detecterConflitsEnseignant(
                cours.getEnseignantId(),
                cours.getJourSemaine(),
                cours.getHeureDebut().toString(),
                cours.getDureeMinutes(),
                cours.getId()
            );
            for (Cours c : conflitsProf) {
                resultat.conflits.add(String.format(
                    "Enseignant déjà occupé : cours '%s' (%s) de %s à %s",
                    c.getNomMatiere(), c.getNomClasse(),
                    c.getHeureDebut(), c.getHeureFin()
                ));
            }

        } catch (SQLException e) {
            resultat.avertissements.add("Erreur lors de la vérification : " + e.getMessage());
        }

        return resultat;
    }

    /**
     * Vérifie si une salle est disponible à un créneau précis.
     */
    public boolean isSalleDisponible(int salleId, int jourSemaine,
                                      String heureDebut, int dureeMin) {
        try {
            List<Cours> conflits = coursDAO.detecterConflitsSalle(
                salleId, jourSemaine, heureDebut, dureeMin, -1
            );
            return conflits.isEmpty();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Retourne tous les cours en conflit dans la base de données.
     */
    public List<String> getAllConflits() {
        List<String> conflits = new ArrayList<>();
        try {
            List<Cours> tousLesCours = coursDAO.findAll();
            for (int i = 0; i < tousLesCours.size(); i++) {
                for (int j = i + 1; j < tousLesCours.size(); j++) {
                    Cours c1 = tousLesCours.get(i);
                    Cours c2 = tousLesCours.get(j);
                    if (c1.isEnConflitAvec(c2)) {
                        conflits.add(String.format(
                            "Conflit salle %s : '%s' et '%s' le %s",
                            c1.getNomSalle(), c1.getNomMatiere(),
                            c2.getNomMatiere(), c1.getJourLibelle()
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            conflits.add("Erreur : " + e.getMessage());
        }
        return conflits;
    }
}
