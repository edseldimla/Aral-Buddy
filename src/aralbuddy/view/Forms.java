package aralbuddy.view;

import java.awt.*;
import javax.swing.*;

/** Keeps modal forms readable and preserves entered values after validation errors. */
public final class Forms {
    private Forms() { }

    /** Creates a grid form with aligned labels and growing fields. */
    public static JPanel panel(String[] labels, Component... fields) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        for (int i = 0; i < fields.length; i++) Page.formRow(form, i, labels[i], fields[i]);
        return form;
    }

    /** Asks for confirmation using explicit action and cancel choices. */
    public static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    /** Opens a resizable form dialog that remains open until validation and saving succeed. */
    public static void open(MainFrame owner, String title, JPanel content, String actionLabel,
            java.util.function.Consumer<JDialog> action) {
        JDialog dialog = new JDialog(owner, title, true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel container = new JPanel(new BorderLayout(0, 18));
        container.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JScrollPane scroll = new JScrollPane(content); scroll.setBorder(null);
        container.add(scroll, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton cancel = Theme.button("Cancel", false, dialog::dispose);
        JButton submit = Theme.button(actionLabel, true, () -> action.accept(dialog));
        buttons.add(cancel); buttons.add(submit); container.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(container); dialog.getRootPane().setDefaultButton(submit);
        dialog.getRootPane().registerKeyboardAction(event -> { if (dialog.isEnabled()) dialog.dispose(); },
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setSize(Math.min(Math.max(dialog.getWidth(), 550), 800), Math.min(dialog.getHeight(), 650));
        dialog.setMinimumSize(new Dimension(500, Math.min(dialog.getHeight(), 420)));
        dialog.setLocationRelativeTo(owner); dialog.setVisible(true);
    }

    /** Shows selected record details as plain, wrapped text. */
    public static void details(Component owner, String title, String text) {
        JTextArea area = new JTextArea(text, 14, 48); area.setEditable(false);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setCaretPosition(0);
        JOptionPane.showMessageDialog(owner, new JScrollPane(area), title, JOptionPane.PLAIN_MESSAGE);
    }
}
