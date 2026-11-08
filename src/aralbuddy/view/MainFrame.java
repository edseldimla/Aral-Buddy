package aralbuddy.view;

import aralbuddy.controller.AppController;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

/** Resizable application shell with persistent navigation and a visible save status. */
@SuppressWarnings("serial")
public final class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private final JLabel status = Theme.label("Opening workspace...", 12, false, Theme.MUTED);
    private final JPanel cards = new JPanel(new CardLayout());
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, JButton> navigation = new LinkedHashMap<>();
    private final JPanel blocker = new JPanel(new GridBagLayout());
    private final JLabel progress = Theme.label("Please wait...", 16, true, Theme.TEXT);
    private AppController controller;
    private String selectedPage;

    /** Creates a visible loading screen before slow startup work begins. */
    public MainFrame() {
        super("Aral Buddy | Peer tutoring workspace");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(Theme.MIN_WINDOW); setSize(Theme.DEFAULT_WINDOW); setLocationRelativeTo(null);
        JPanel loading = new JPanel(new GridBagLayout()); loading.setBackground(Theme.BACKGROUND);
        loading.add(Theme.label("Opening Aral Buddy...", 26, true, Theme.TEAL)); setContentPane(loading);
        blocker.setOpaque(true); blocker.setBackground(new Color(243, 246, 245, 235)); blocker.add(progress);
        blocker.addMouseListener(new MouseAdapter() { });
        blocker.addKeyListener(new KeyAdapter() { }); blocker.setFocusable(true); setGlassPane(blocker);
        addWindowListener(new WindowAdapter() {
            /** {@inheritDoc} */
            @Override public void windowClosing(WindowEvent event) { if (controller != null) controller.close(); }
        });
    }

    /** Assigns the controller before startup, including close handling on the loading screen. */
    public void setController(AppController controller) { this.controller = controller; }

    /** Shows the login view and makes Enter activate its login button. */
    public void showLogin(AppController controller) {
        this.controller = controller; pages.clear(); navigation.clear(); selectedPage = null;
        LoginView login = new LoginView(this, controller);
        setContentPane(login); getRootPane().setDefaultButton(login.loginButton());
        revalidate(); repaint(); login.focusEmail();
    }

    /** Builds role-appropriate pages inside a shared sidebar and CardLayout. */
    public void showWorkspace(AppController controller) {
        this.controller = controller; pages.clear(); navigation.clear(); cards.removeAll();
        getRootPane().setDefaultButton(null);
        pages.put("Overview", new OverviewPage(controller));
        if (controller.currentUser().isAdmin()) {
            pages.put("Tutor applications", new AdminPages.Applications(this, controller));
            pages.put("Student accounts", new AdminPages.Accounts(this, controller));
            pages.put("Courses", new AdminPages.Courses(this, controller));
            pages.put("Reports", new AdminPages.Reports(controller));
        } else {
            pages.put("Find a tutor", new SearchPage(this, controller));
            pages.put("My sessions", new SessionsPage(this, controller, false));
            pages.put("Session history", new SessionsPage(this, controller, true));
            pages.put("Tutor requests", new RequestsPage(this, controller));
            pages.put("Become a tutor", new ApplicationPage(this, controller));
            pages.put("My profile", new ProfilePage(this, controller));
        }
        pages.put("Alerts", new AlertsPage(controller));
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(Theme.BACKGROUND);
        root.add(createSidebar(), BorderLayout.WEST);
        JPanel content = new JPanel(new BorderLayout(0, 22)); content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(24, 26, 14, 26));
        JPanel header = new JPanel(new BorderLayout(12, 0)); header.setOpaque(false);
        header.add(Theme.label("Your learning workspace", 15, true, Theme.MUTED), BorderLayout.WEST);
        header.add(Theme.label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")), 13, false, Theme.MUTED), BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);
        for (Map.Entry<String, Page> entry : pages.entrySet()) cards.add(entry.getValue(), entry.getKey());
        cards.setOpaque(false); content.add(cards, BorderLayout.CENTER);
        status.setToolTipText(controller.service().getDataLocation());
        content.add(status, BorderLayout.SOUTH); root.add(content, BorderLayout.CENTER);
        setContentPane(root); installShortcuts(); showPage("Overview");
        showStatus("All successful changes are saved automatically."); revalidate(); repaint();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout()); sidebar.setBackground(Theme.NAVY);
        sidebar.setPreferredSize(new Dimension(216, 0)); sidebar.setBorder(BorderFactory.createEmptyBorder(26, 16, 20, 16));
        JPanel top = new JPanel(); top.setOpaque(false); top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        JLabel brand = Theme.label("Aral Buddy", 23, true, Color.WHITE); brand.setIcon(Theme.bookIcon()); brand.setIconTextGap(10);
        top.add(brand); top.add(Box.createVerticalStrut(8));
        top.add(Theme.label("LEARN TOGETHER", 10, true, new Color(154, 184, 184))); top.add(Box.createVerticalStrut(30));
        int index = 1;
        for (String name : pages.keySet()) {
            JButton button = Theme.button(name, false, () -> showPage(name));
            button.setHorizontalAlignment(SwingConstants.LEFT); button.setAlignmentX(LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
            button.setToolTipText("Open " + name + " (Ctrl+" + index + ")"); index++;
            navigation.put(name, button); top.add(button); top.add(Box.createVerticalStrut(7));
        }
        JScrollPane navScroll = new JScrollPane(top); navScroll.setBorder(null); navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false); navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.getVerticalScrollBar().setUnitIncrement(18); sidebar.add(navScroll, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(0, 1, 0, 7)); bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        JLabel name = Theme.label(controller.currentUser().getName(), 13, true, Color.WHITE);
        name.setToolTipText(controller.currentUser().getName()); bottom.add(name);
        bottom.add(Theme.label(controller.currentUser().getRole(), 12, false, new Color(174, 197, 196)));
        bottom.add(Theme.button("Log out", false, controller::logout)); sidebar.add(bottom, BorderLayout.SOUTH);
        return sidebar;
    }

    private void showPage(String name) {
        if (!pages.containsKey(name)) return;
        selectedPage = name; ((CardLayout) cards.getLayout()).show(cards, name);
        navigation.forEach((key, button) -> {
            button.setBackground(key.equals(name) ? Theme.TINT : Theme.NAVY);
            button.putClientProperty("nav.background", key.equals(name) ? Theme.TINT : Theme.NAVY);
            button.setForeground(key.equals(name) ? Theme.TEXT : Color.WHITE);
            button.setBorder(BorderFactory.createEmptyBorder(11, 12, 11, 8));
        });
        controller.guard(pages.get(name)::refresh);
        showStatus("Changes are saved automatically. Ctrl+F searches this page; F1 opens help.");
    }

    /** Refreshes only the visible page after a completed mutation. */
    public void refreshCurrentPage() { if (selectedPage != null) controller.guard(pages.get(selectedPage)::refresh); }

    /** Displays the result of the last operation without repeated success dialogs. */
    public void showStatus(String text) { status.setText(text); status.setToolTipText(text + " | " + (controller.service() == null ? "" : controller.service().getDataLocation())); }

    /** Blocks repeated actions while a worker loads, hashes, or saves. */
    public void setBusy(boolean busy, String text) {
        progress.setText(text); blocker.setVisible(busy); setCursor(Cursor.getPredefinedCursor(busy ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
        if (busy) blocker.requestFocusInWindow(); status.setText(text);
    }

    private void installShortcuts() {
        int index = 1;
        for (String name : pages.keySet()) bind("control " + index++, "nav-" + name, () -> showPage(name));
        bind("control F", "search", () -> { if (selectedPage != null) pages.get(selectedPage).focusSearch(); });
        bind("control R", "refresh", this::refreshCurrentPage);
        bind("control L", "logout", controller::logout);
        bind("F1", "help", () -> Forms.details(this, "Keyboard shortcuts",
                "Ctrl+1 to Ctrl+9: Open a page\nCtrl+F: Search the current table\nCtrl+R: Refresh the page\nCtrl+L: Log out\nEnter: Submit a form\nEscape: Close a form\n\nClick a table heading to sort. Scroll horizontally for more columns.\nAll sessions last one hour.\n\nData file:\n" + controller.service().getDataLocation()));
    }

    private void bind(String stroke, String name, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(stroke), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;
            /** {@inheritDoc} */
            @Override public void actionPerformed(ActionEvent event) { if (!blocker.isVisible() && selectedPage != null) action.run(); }
        });
    }
}
