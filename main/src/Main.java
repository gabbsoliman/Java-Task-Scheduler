import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } 
        catch (Exception e) {
            System.err.println("Unable to apply UI: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            TaskDatabase database = new TaskDatabase();
            Runtime.getRuntime().addShutdownHook(new Thread(database::close));
            TaskManager manager = new TaskManager(database);
            TaskSchedulerGUI gui = new TaskSchedulerGUI(manager);
            gui.setVisible(true);

            if (!database.isConnected()) {
                JOptionPane.showMessageDialog(gui,
                        "Could not open the task database, so changes will not be saved.\n"
                                + "Make sure sqlite-jdbc-3.36.0.3.jar is in the src/lib folder.",
                        "Database Error", JOptionPane.WARNING_MESSAGE);
            }

        });
    }
}
