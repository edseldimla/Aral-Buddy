package aralbuddy.view;

import aralbuddy.controller.AppController;
import aralbuddy.model.TutorApplication;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/** Collects a tutor application and shows every previous review decision. */
@SuppressWarnings("serial")
public final class ApplicationPage extends Page {
    private static final long serialVersionUID = 1L;
    private final AppController controller;

    /** Creates the application history and a separate, validated application form. */
    public ApplicationPage(MainFrame frame, AppController controller) {
        super("Share what you know", "Choose the courses you can teach and when you are free each week.");
        this.controller = controller;
        records = new DataTable("Courses", "Availability", "Reason", "Status"); body.add(records, BorderLayout.CENTER);
        actions.add(Theme.button("Start a tutor application", true, () -> controller.guard(() -> openForm(frame))));
        actions.add(Theme.button("View application details", false, () -> controller.guard(() -> {
            int id = (Integer) records.selectedKey();
            TutorApplication application = controller.service().applications().stream().filter(a -> a.getId() == id).findFirst().orElseThrow();
            Forms.details(frame, "Application details", application.getSubjects() + "\n" + application.getAvailability() + "\nStatus: " + application.getStatus() + "\n\n" + application.getReason());
        })));
    }

    /** Shows pending, approved, and rejected applications for the current account. */
    @Override public void refresh() {
        records.clearRows();
        controller.service().applications().stream().filter(a -> a.getStudentId() == controller.actor()).forEach(a ->
                records.addRow(a.getId(), a.getSubjects(), a.getAvailability(), a.getReason(), a.getStatus()));
    }

    private void openForm(MainFrame frame) {
        aralbuddy.util.Validation.require(!controller.currentUser().isTutor(), "You are already an approved tutor.");
        aralbuddy.util.Validation.require(controller.service().applications().stream().noneMatch(a -> a.getStudentId() == controller.actor() && a.isPending()),
                "Your application is already waiting for review.");
        JList<String> courses = new JList<>(controller.service().subjects().stream().map(s -> s.getName()).toArray(String[]::new));
        courses.setVisibleRowCount(7); courses.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        courses.setToolTipText("Use Ctrl+click or Shift+click to select several courses.");
        JList<String> days = new JList<>(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"});
        days.setVisibleRowCount(7); days.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        days.setToolTipText("Use Ctrl+click or Shift+click to select several days.");
        JComboBox<String> from = new JComboBox<>(hours()), to = new JComboBox<>(hours()); from.setSelectedItem("09:00"); to.setSelectedItem("17:00");
        JTextArea reason = new JTextArea(4, 25); reason.setLineWrap(true); reason.setWrapStyleWord(true);
        JPanel form = Forms.panel(new String[]{"Courses (Ctrl+click for several)", "Days (Ctrl+click for several)", "Available from", "Available until", "Why would you like to tutor?"},
                new JScrollPane(courses), new JScrollPane(days), from, to, new JScrollPane(reason));
        Forms.open(frame, "Become a peer tutor", form, "Submit application", dialog -> {
            List<String> selectedCourses = List.copyOf(courses.getSelectedValuesList()), selectedDays = List.copyOf(days.getSelectedValuesList());
            String start = (String) from.getSelectedItem(), end = (String) to.getSelectedItem(), explanation = reason.getText();
            controller.save("Application saved. An administrator will review it.", () -> controller.service().apply(controller.actor(), selectedCourses, selectedDays, start, end, explanation), null, dialog);
        });
    }

    private static String[] hours() {
        String[] values = new String[15];
        for (int i = 0; i < values.length; i++) values[i] = String.format(java.util.Locale.ROOT, "%02d:00", i + 6);
        return values;
    }
}
