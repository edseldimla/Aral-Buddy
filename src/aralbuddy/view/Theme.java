package aralbuddy.view;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

/** One restrained color and typography system for every Swing screen. */
public final class Theme {
    /** Dark navigation surface. */
    public static final Color NAVY = new Color(22, 39, 48);
    /** Main action color. */
    public static final Color TEAL = new Color(10, 112, 101);
    /** Hover and pressed action color. */
    public static final Color TEAL_DARK = new Color(7, 85, 78);
    /** Application canvas. */
    public static final Color BACKGROUND = new Color(243, 246, 245);
    /** Card and input surface. */
    public static final Color SURFACE = Color.WHITE;
    /** Main text color. */
    public static final Color TEXT = new Color(28, 46, 54);
    /** Supporting text color. */
    public static final Color MUTED = new Color(94, 112, 119);
    /** Quiet separator color. */
    public static final Color BORDER = new Color(218, 227, 225);
    /** Selected table row and navigation accent. */
    public static final Color TINT = new Color(220, 240, 233);
    /** Shared font family, with the logical Java font as fallback. */
    public static final String FONT = "Segoe UI";
    /** Smallest supported workspace size, verified by GUI tests. */
    public static final Dimension MIN_WINDOW = new Dimension(960, 680);
    /** Comfortable default workspace size. */
    public static final Dimension DEFAULT_WINDOW = new Dimension(1260, 820);
    /** Readable table row height. */
    public static final int TABLE_ROW_HEIGHT = 42;
    /** Height reserved for sortable column headings. */
    public static final int TABLE_HEADER_HEIGHT = 40;
    private Theme() { }

    /** Installs standard Swing defaults before components are constructed. */
    public static void install() {
        MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme() {
            @Override protected ColorUIResource getPrimary1() { return new ColorUIResource(TEAL_DARK); }
            @Override protected ColorUIResource getPrimary2() { return new ColorUIResource(BORDER); }
            @Override protected ColorUIResource getPrimary3() { return new ColorUIResource(TINT); }
            @Override protected ColorUIResource getSecondary1() { return new ColorUIResource(MUTED); }
            @Override protected ColorUIResource getSecondary2() { return new ColorUIResource(BORDER); }
            @Override protected ColorUIResource getSecondary3() { return new ColorUIResource(BACKGROUND); }
        });
        try { UIManager.setLookAndFeel(new MetalLookAndFeel()); }
        catch (UnsupportedLookAndFeelException exception) { throw new IllegalStateException("The standard Swing theme is unavailable.", exception); }
        FontUIResource font = new FontUIResource(FONT, Font.PLAIN, 14);
        for (Object key : java.util.Collections.list(UIManager.getDefaults().keys()))
            if (key.toString().endsWith(".font")) UIManager.put(key, font);
        String[] surfaces = {"Panel.background", "TextField.background", "PasswordField.background", "TextArea.background",
                "List.background", "Table.background", "ComboBox.background", "OptionPane.background"};
        for (String key : surfaces) UIManager.put(key, SURFACE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Table.selectionBackground", TINT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TextField.selectionBackground", TINT);
        UIManager.put("TableHeader.background", BACKGROUND);
        UIManager.put("TableHeader.foreground", MUTED);
        UIManager.put("Button.background", BACKGROUND);
        UIManager.put("Button.foreground", TEXT);
        UIManager.put("Button.select", TINT);
        UIManager.put("ToolTip.background", NAVY);
        UIManager.put("ToolTip.foreground", SURFACE);
        UIManager.put("ScrollPane.border", BorderFactory.createLineBorder(BORDER));
        UIManager.put("ScrollBarUI", QuietScrollBarUI.class.getName());
        UIManager.put("ScrollBar.width", 11);
        UIManager.put("TabbedPane.selected", SURFACE);
        UIManager.put("TabbedPane.contentAreaColor", SURFACE);
        UIManager.put("TabbedPane.background", BACKGROUND);
        UIManager.put("TabbedPane.focus", TEAL);
        ToolTipManager.sharedInstance().setInitialDelay(400);
    }

    /** Creates a focusable action with a visible border and immediate hover feedback. */
    public static JButton button(String text, boolean primary, Runnable action) {
        JButton button = new JButton(text);
        button.setFont(new Font(FONT, Font.BOLD, 13));
        button.setFocusPainted(true);
        button.setOpaque(true);
        button.setBackground(primary ? TEAL : SURFACE);
        button.setForeground(primary ? SURFACE : TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(primary ? TEAL : BORDER),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(text);
        button.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override public void mouseEntered(MouseEvent event) {
                if (button.isEnabled()) button.setBackground(button.getClientProperty("nav.background") != null ? TEAL_DARK : primary ? TEAL_DARK : TINT);
            }
            /** {@inheritDoc} */
            @Override public void mouseExited(MouseEvent event) {
                Color navigationColor = (Color) button.getClientProperty("nav.background");
                button.setBackground(navigationColor != null ? navigationColor : primary ? TEAL : SURFACE);
            }
        });
        button.addActionListener(event -> action.run());
        return button;
    }

    /** Creates consistently spaced, plain text headings and labels. */
    public static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel label = new JLabel();
        label.putClientProperty("html.disable", Boolean.TRUE);
        label.setText(text);
        label.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    /** Draws a small book mark without image files or icon libraries. */
    public static Icon bookIcon() {
        return new Icon() {
            /** Returns the icon's layout width. */
            @Override public int getIconWidth() { return 38; }
            /** Returns the icon's layout height. */
            @Override public int getIconHeight() { return 38; }
            /** Paints the book using scalable standard Java graphics. */
            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(TEAL); g.fillRoundRect(x, y, 38, 38, 10, 10);
                g.setColor(SURFACE); g.setStroke(new BasicStroke(1.7f));
                g.drawRoundRect(x + 8, y + 10, 22, 18, 3, 3); g.drawLine(x + 19, y + 10, x + 19, y + 28);
                g.dispose();
            }
        };
    }
}
