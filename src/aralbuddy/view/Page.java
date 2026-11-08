package aralbuddy.view;

import java.awt.*;
import javax.swing.*;

/** Common page structure with a heading, content, and wrapping action area. */
// Swing panels are not saved, and the final page subclasses do not override setup setters.
@SuppressWarnings({"serial", "this-escape"})
public abstract class Page extends JPanel {
    private static final long serialVersionUID = 1L;
    /** Central content area. */
    protected final JPanel body = new JPanel(new BorderLayout(0, 16));
    /** Button area using a grid so actions remain visible when the window narrows. */
    protected final JPanel actions = new JPanel(new GridLayout(0, 2, 10, 8));
    /** Optional shared table for this page. */
    protected DataTable records;

    /** Creates the common page shell. */
    protected Page(String title, String description) {
        super(new BorderLayout(0, 22));
        setBackground(Theme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(24, 24, 22, 24)));
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 8)); heading.setOpaque(false);
        heading.add(Theme.label(title, 26, true, Theme.TEXT));
        heading.add(Theme.label(description, 13, false, Theme.MUTED));
        body.setOpaque(false); actions.setOpaque(false);
        add(heading, BorderLayout.NORTH); add(body, BorderLayout.CENTER); add(actions, BorderLayout.SOUTH);
    }

    /** Reloads records from the service after navigation or a successful operation. */
    public abstract void refresh();

    /** Focuses the current page's search box when it has one. */
    public void focusSearch() { if (records != null) records.focusSearch(); }

    /** Creates a labeled row in a form using a layout manager. */
    protected static void formRow(JPanel panel, int row, String label, Component field) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0; constraints.gridy = row; constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(7, 0, 7, 16);
        JLabel caption = Theme.label(label, 13, true, Theme.MUTED); caption.setLabelFor(field);
        panel.add(caption, constraints);
        constraints.gridx = 1; constraints.weightx = 1; constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(7, 0, 7, 0); panel.add(field, constraints);
        if (field instanceof JComponent) ((JComponent) field).getAccessibleContext().setAccessibleName(label);
    }
}
