package aralbuddy;

import aralbuddy.controller.AppController;
import aralbuddy.view.*;
import java.awt.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import javax.swing.*;

/** Exercises real Swing pages on the event thread without outside test libraries. */
public final class GuiTests {
    private static int checks;
    private GuiTests() { }

    /** Runs desktop smoke checks; --preview leaves the test window open for visual review. */
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("aralbuddy-gui-");
        MainFrame frame = edt(() -> { Theme.install(); MainFrame window = new MainFrame(); window.setVisible(true); return window; });
        AppController controller = edt(() -> { AppController value = new AppController(frame); frame.setController(value); value.start(directory); return value; });
        boolean preview = args.length > 0 && args[0].equals("--preview");
        try {
            waitFor(() -> visibleButton(frame, "Log in") != null, "Login screen loaded");
            render(frame, "login.png");
            edt(() -> {
                DataTable numeric = new DataTable("Number"); numeric.addRow(10, 10); numeric.addRow(2, 2);
                numeric.getTable().getRowSorter().toggleSortOrder(0);
                if (!numeric.getTable().getValueAt(0, 0).equals(2)) throw new AssertionError("Numeric table sorting");
                return null;
            }); checks++;
            edt(() -> { controller.login("juan@gmail.com", "password123"); return null; });
            waitFor(() -> visibleButton(frame, "Find a tutor") != null, "Student login");
            for (String title : new String[]{"Overview", "Find a tutor", "My sessions", "Session history", "Tutor requests", "Become a tutor", "My profile", "Alerts"}) {
                click(frame, title); check(edt(() -> frame.getContentPane().isShowing()), "Student page " + title);
            }
            click(frame, "Find a tutor"); click(frame, "Find available tutors");
            check(edt(() -> visible(frame, DataTable.class, x -> true).getTable().getRowCount() == 1), "Tutor search results");
            click(frame, "Session history");
            DataTable history = edt(() -> visible(frame, DataTable.class, x -> true));
            check(edt(() -> history.getTable().getRowCount() == 3), "History records");
            edt(() -> {
                history.getTable().getRowSorter().toggleSortOrder(0); history.getTable().setRowSelectionInterval(0, 0);
                Object key = history.selectedKey();
                if (!Integer.valueOf(4).equals(key)) throw new AssertionError("Sorted row must select the Data Structures session with ID 4");
                return null;
            }); checks++;
            edt(() -> { frame.setSize(960, 680); frame.validate(); return null; });
            click(frame, "My sessions"); checkVisibleActions(frame);
            click(frame, "Find a tutor");
            check(edt(() -> visible(frame, DataTable.class, x -> true).getTable().getParent().getHeight() >= 80), "Search table remains usable at minimum size");
            render(frame, "student-search-small.png");
            edt(() -> { frame.setSize(1260, 820); frame.validate(); return null; });
            click(frame, "Overview"); render(frame, "student-overview.png");
            edt(() -> { frame.setSize(1260, 820); controller.login("admin@aralbuddy.edu", "admin1234"); return null; });
            waitFor(() -> visibleButton(frame, "Tutor applications") != null, "Administrator login");
            for (String title : new String[]{"Overview", "Tutor applications", "Student accounts", "Courses", "Reports", "Alerts"}) {
                click(frame, title); check(edt(() -> visible(frame, DataTable.class, x -> true) != null), "Admin page " + title);
            }
            click(frame, "Reports");
            render(frame, "admin-reports.png");
            JTabbedPane tabs = edt(() -> visible(frame, JTabbedPane.class, x -> true));
            for (int i = 0; i < 3; i++) {
                int tab = i; edt(() -> { tabs.setSelectedIndex(tab); return null; }); click(frame, "Refresh reports");
                check(edt(() -> visible(frame, DataTable.class, x -> true).getTable().getRowCount() > 0), "Report tab " + i);
            }
            click(frame, "Student accounts");
            DataTable accounts = edt(() -> visible(frame, DataTable.class, x -> true));
            edt(() -> { visible(accounts, JTextField.class, x -> true).setText("maria"); return null; });
            check(edt(() -> accounts.getTable().getRowCount() == 1 && accounts.getTable().getValueAt(0, 0).equals("Maria Santos")), "Live table search");
            edt(() -> { visible(accounts, JTextField.class, x -> true).setText("["); return null; });
            check(edt(() -> accounts.getTable().getRowCount() == 0), "Literal search metacharacter");
            edt(() -> { visible(accounts, JTextField.class, x -> true).setText(""); return null; });
            edt(() -> { frame.setSize(960, 680); frame.validate(); return null; });
            click(frame, "Tutor applications"); checkVisibleActions(frame);
            edt(() -> { frame.setSize(1260, 820); frame.validate(); return null; });
            click(frame, "Overview");
            System.out.println("PASS: " + checks + " Swing checks. Test data: " + directory);
            if (preview) { System.out.println("Preview left open for visual review; close it when finished."); return; }
        } finally {
            if (!preview) {
                edt(() -> { controller.close(); return null; });
                try (var paths = Files.walk(directory)) { for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
            }
        }
    }

    private static void checkVisibleActions(MainFrame frame) throws Exception {
        check(edt(() -> {
            JButton button = visible(frame, JButton.class, b -> b.getText().contains("mark completed") || b.getText().contains("Approve selected"));
            if (button == null) return false;
            Rectangle rectangle = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), frame.getContentPane());
            return frame.getContentPane().getBounds().contains(rectangle);
        }), "Action buttons fit minimum window size");
    }

    private static void render(MainFrame frame, String name) throws Exception {
        String output = System.getProperty("aralbuddy.testImages");
        if (output == null) return;
        Path directory = Path.of(output); Files.createDirectories(directory);
        edt(() -> {
            Container content = frame.getContentPane();
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(content.getWidth(), content.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics(); content.printAll(graphics); graphics.dispose();
            javax.imageio.ImageIO.write(image, "png", directory.resolve(name).toFile()); return null;
        });
    }

    private static void click(MainFrame frame, String text) throws Exception {
        edt(() -> { JButton button = visibleButton(frame, text); if (button == null) throw new AssertionError("Missing button " + text); button.doClick(); return null; });
    }

    private static JButton visibleButton(Container root, String text) { return visible(root, JButton.class, button -> text.equals(button.getText())); }

    private static <T extends Component> T visible(Container root, Class<T> type, Predicate<T> match) {
        for (Component child : root.getComponents()) {
            if (child.isShowing() && type.isInstance(child) && match.test(type.cast(child))) return type.cast(child);
            if (child instanceof Container) { T nested = visible((Container) child, type, match); if (nested != null) return nested; }
        }
        return null;
    }

    private static void waitFor(Callable<Boolean> condition, String label) throws Exception {
        long deadline = System.nanoTime() + 30_000_000_000L;
        while (System.nanoTime() < deadline) { if (edt(condition)) { checks++; return; } Thread.sleep(50); }
        throw new AssertionError(label + " timed out");
    }

    private static <T> T edt(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action); SwingUtilities.invokeAndWait(task); return task.get();
    }

    private static void check(boolean condition, String label) { if (!condition) throw new AssertionError(label); checks++; }
}
