package univscheduler;

import univscheduler.ui.LoginFrame;
import javax.swing.*;

/**
 * Point d'entrée principal de l'application UNIV-SCHEDULER.
 * Lance l'interface graphique Swing.
 */
public class Main {

    public static void main(String[] args) {
        // Utiliser le Look and Feel du systèmec  
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Ignorer, utiliser le L&F par défaut
        }

        // Lancer l'UI sur l'Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            System.out.println("🎓 UNIV-SCHEDULER - Démarrage...");
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
