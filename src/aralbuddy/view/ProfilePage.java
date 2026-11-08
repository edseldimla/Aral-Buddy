package aralbuddy.view;

import aralbuddy.controller.AppController;
import aralbuddy.model.Student;
import java.awt.*;
import java.util.Arrays;
import javax.swing.*;

/** Edits student profile fields while keeping account identifiers stable. */
@SuppressWarnings("serial")
public final class ProfilePage extends Page {
    private static final long serialVersionUID = 1L;
    private final AppController controller;
    private final JTextField name = new JTextField(28), program = new JTextField(28);
    private final JComboBox<String> year = new JComboBox<>(new String[]{"1", "2", "3", "4"});
    private final JPasswordField password = new JPasswordField(28), confirm = new JPasswordField(28);

    /** Creates a focused profile form and optional password change. */
    public ProfilePage(MainFrame frame, AppController controller) {
        super("My profile", "Leave both password fields empty to keep your current password.");
        this.controller = controller;
        Student student = (Student) controller.currentUser();
        JPanel form = Forms.panel(new String[]{"Student number", "Email", "Full name", "Program", "Year level", "New password (optional)", "Confirm new password"},
                Theme.label(student.getIdNumber(), 14, false, Theme.TEXT), Theme.label(student.getEmail(), 14, false, Theme.TEXT), name, program, year, password, confirm);
        JPanel wrapper = new JPanel(new BorderLayout()); wrapper.setOpaque(false); wrapper.add(form, BorderLayout.NORTH);
        body.add(new JScrollPane(wrapper), BorderLayout.CENTER);
        actions.add(Theme.button("Save profile", true, () -> controller.guard(() -> save(frame))));
    }

    /** Loads the latest profile and clears password fields when the page is reopened. */
    @Override public void refresh() {
        Student student = (Student) controller.currentUser(); name.setText(student.getName()); program.setText(student.getProgram());
        year.setSelectedItem(student.getYearLevel()); password.setText(""); confirm.setText("");
    }

    private void save(MainFrame frame) {
        char[] first = password.getPassword(), second = confirm.getPassword();
        boolean matches = Arrays.equals(first, second); String secret = new String(first);
        Arrays.fill(first, '\0'); Arrays.fill(second, '\0');
        aralbuddy.util.Validation.require(matches, "The new passwords do not match.");
        String fullName = name.getText(), course = program.getText(), level = (String) year.getSelectedItem();
        controller.save("Profile saved.", () -> controller.service().updateProfile(controller.actor(), fullName, course, level, secret), () -> frame.showWorkspace(controller));
    }
}
