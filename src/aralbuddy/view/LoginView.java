package aralbuddy.view;

import aralbuddy.controller.AppController;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Keyboard-friendly sign-in and registration with resizable layout managers. */
@SuppressWarnings("serial")
public final class LoginView extends JPanel {
    private static final long serialVersionUID = 1L;
    private final JTextField email = new JTextField(24);
    private final JPasswordField password = new JPasswordField(24);
    private final JButton login;

    /** Constructs the sign-in screen and its registration form. */
    public LoginView(MainFrame frame, AppController controller) {
        super(new GridBagLayout()); setBackground(Theme.BACKGROUND);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER), BorderFactory.createEmptyBorder(36, 40, 36, 40)));
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        JLabel brand = Theme.label("Aral Buddy", 30, true, Theme.TEXT); brand.setIcon(Theme.bookIcon()); brand.setIconTextGap(12);
        c.gridy = 0; card.add(brand, c);
        c.gridy++; c.insets = new Insets(12, 0, 26, 0); card.add(Theme.label("A little help. A lot of progress.", 16, false, Theme.MUTED), c);
        JPanel fields = Forms.panel(new String[]{"Email", "Password"}, email, password);
        email.setName("loginEmail"); password.setName("loginPassword");
        c.gridy++; c.insets = new Insets(0, 0, 18, 0); card.add(fields, c);
        login = Theme.button("Log in", true, () -> {
            char[] value = password.getPassword();
            try { controller.login(email.getText().trim(), new String(value)); }
            finally { Arrays.fill(value, '\0'); password.setText(""); }
        });
        login.setName("loginButton"); login.setMnemonic('L');
        JPanel buttons = new JPanel(new GridLayout(1, 2, 12, 0));
        buttons.add(Theme.button("Create account", false, () -> register(frame, controller))); buttons.add(login);
        c.gridy++; card.add(buttons, c);
        c.gridy++; c.insets = new Insets(12, 0, 5, 0); card.add(Theme.label("DEMONSTRATION ACCOUNTS", 11, true, Theme.TEAL), c);
        c.gridy++; card.add(Theme.label("Student: juan@gmail.com / password123", 12, false, Theme.MUTED), c);
        c.gridy++; card.add(Theme.label("Tutor: maria@gmail.com / password123", 12, false, Theme.MUTED), c);
        c.gridy++; card.add(Theme.label("Admin: admin@aralbuddy.edu / admin1234", 12, false, Theme.MUTED), c);
        c.gridy++; c.insets = new Insets(18, 0, 0, 0); card.add(Theme.label("Your work is saved on this computer.", 12, false, Theme.MUTED), c);
        add(card);
    }

    /** Exposes the default Enter action to the frame. */
    public JButton loginButton() { return login; }
    /** Places the typing cursor in the first field. */
    public void focusEmail() { SwingUtilities.invokeLater(email::requestFocusInWindow); }

    private void register(MainFrame frame, AppController controller) {
        JTextField number = new JTextField(24), name = new JTextField(24), address = new JTextField(24), program = new JTextField(24);
        JPasswordField secret = new JPasswordField(24), confirm = new JPasswordField(24);
        JComboBox<String> year = new JComboBox<>(new String[]{"1", "2", "3", "4"});
        JPanel form = Forms.panel(new String[]{"Student number", "Full name", "Email", "Password (8+ characters)", "Confirm password", "Program", "Year level"},
                number, name, address, secret, confirm, program, year);
        Forms.open(frame, "Create your student account", form, "Create account", dialog -> {
            String id = number.getText(), fullName = name.getText(), emailValue = address.getText(), course = program.getText(), level = (String) year.getSelectedItem();
            char[] first = secret.getPassword(), second = confirm.getPassword();
            if (!Arrays.equals(first, second)) { JOptionPane.showMessageDialog(dialog, "The passwords do not match."); Arrays.fill(first, '\0'); Arrays.fill(second, '\0'); return; }
            String pass = new String(first); Arrays.fill(first, '\0'); Arrays.fill(second, '\0');
            controller.save("Account created. You can now log in.", () -> controller.service().register(id, fullName, emailValue, pass, course, level),
                    () -> { email.setText(emailValue.trim()); JOptionPane.showMessageDialog(frame, "Account created. You can now log in."); }, dialog);
        });
    }
}
