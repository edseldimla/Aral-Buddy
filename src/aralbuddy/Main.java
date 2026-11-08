package aralbuddy;

import aralbuddy.controller.AppController;
import aralbuddy.view.MainFrame;
import aralbuddy.view.Theme;
import java.nio.file.Path;
import javax.swing.*;

/** Starts the application on the Swing event thread using the standard Java library. */
public final class Main {
    private Main() { }

    /** Launches the workspace; an optional system property selects a separate data folder. */
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            exception.printStackTrace();
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "An unexpected error occurred. Please reopen the application. Details are in the console.", "Aral Buddy", JOptionPane.ERROR_MESSAGE));
        });
        SwingUtilities.invokeLater(() -> {
            Theme.install(); MainFrame frame = new MainFrame();
            AppController controller = new AppController(frame); frame.setController(controller); frame.setVisible(true);
            String local = System.getenv("LOCALAPPDATA");
            Path defaultFolder = local == null || local.isBlank() ? Path.of(System.getProperty("user.home"), ".aralbuddy") : Path.of(local, "AralBuddy");
            String folder = System.getProperty("aralbuddy.dataDir", defaultFolder.toString());
            controller.start(Path.of(folder));
        });
    }
}
