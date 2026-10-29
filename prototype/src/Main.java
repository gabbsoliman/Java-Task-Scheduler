import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            System.err.println("Unable to apply UI: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            TaskSchedulerGUI gui = new TaskSchedulerGUI();
            gui.setVisible(true);
        });
    }
}