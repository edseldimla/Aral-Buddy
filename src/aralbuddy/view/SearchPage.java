package aralbuddy.view;

import aralbuddy.controller.AppController;
import aralbuddy.model.Tutor;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import javax.swing.*;

/** Finds tutors using current course, date, time, availability, and name filters. */
@SuppressWarnings("serial")
public final class SearchPage extends Page {
    private static final long serialVersionUID = 1L;
    private final MainFrame frame;
    private final AppController controller;
    private final JComboBox<String> course = new JComboBox<>();
    private final JTextField date = new JTextField(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).toString(), 12);
    private final JTextField time = new JTextField("10:00", 7);
    private final JTextField name = new JTextField(18);

    /** Creates the tutor search form, results, and request action. */
    public SearchPage(MainFrame frame, AppController controller) {
        super("Find a tutor", "Use YYYY-MM-DD and 24-hour HH:mm. Every session lasts one hour.");
        this.frame = frame; this.controller = controller;
        JPanel filters = new JPanel(new GridLayout(2, 2, 14, 10)); filters.setOpaque(false);
        addFilter(filters, "Course", course);
        addFilter(filters, "Date", date);
        addFilter(filters, "Time", time);
        addFilter(filters, "Tutor name", name);
        date.setToolTipText("Future date in YYYY-MM-DD format");
        time.setToolTipText("24-hour start time, for example 14:00");
        name.setToolTipText("Optional tutor name filter");
        course.setName("searchCourse"); date.setName("searchDate"); time.setName("searchTime"); name.setName("searchTutorName");
        JPanel top = new JPanel(new BorderLayout(0, 10)); top.setOpaque(false); top.add(filters, BorderLayout.CENTER);
        top.add(Theme.button("Find available tutors", true, () -> controller.guard(this::search)), BorderLayout.SOUTH);
        body.add(top, BorderLayout.NORTH);
        records = new DataTable("Tutor", "Program", "Year", "Rating", "Sessions completed", "Availability"); body.add(records, BorderLayout.CENTER);
        actions.add(Theme.button("Send a session request", true, () -> controller.guard(this::request)));
    }

    private void addFilter(JPanel panel, String title, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0)); row.setOpaque(false);
        JLabel label = Theme.label(title, 12, true, Theme.MUTED); label.setLabelFor(field);
        row.add(label, BorderLayout.WEST); row.add(field, BorderLayout.CENTER); panel.add(row);
        field.getAccessibleContext().setAccessibleName(title);
    }

    /** Reloads courses so newly added administrator courses appear without restarting. */
    @Override public void refresh() {
        Object selected = course.getSelectedItem(); course.removeAllItems();
        controller.service().subjects().forEach(s -> course.addItem(s.getName()));
        if (selected != null) course.setSelectedItem(selected);
        records.clearRows();
    }

    private void search() {
        String subject = (String) course.getSelectedItem();
        records.clearRows();
        for (Tutor tutor : controller.service().findTutors(controller.actor(), subject, name.getText(), date.getText().trim(), time.getText().trim())) {
            long completed = controller.service().sessions().stream().filter(s -> s.getTutorId() == tutor.getId() && s.getStatus().equals(aralbuddy.model.Session.COMPLETED)).count();
            records.addRow(tutor.getId(), tutor.getName(), tutor.getProgram(), tutor.getYearLevelText(), tutor.getRatingText(), completed, tutor.getAvailability());
        }
        frame.showStatus(records.getTable().getRowCount() == 0 ? "No matching tutor is free. Try another day, time, or course." : "Select a tutor to request the selected hour.");
    }

    private void request() {
        int tutorId = (Integer) records.selectedKey();
        String subject = (String) course.getSelectedItem(), day = date.getText().trim(), hour = time.getText().trim();
        JTextField topic = new JTextField(26), venue = new JTextField("Library, Room 2", 26);
        JPanel form = Forms.panel(new String[]{"Tutor", "Course", "Date and time", "Topic or lesson", "Venue or meeting link"},
                Theme.label(controller.service().user(tutorId).getName(), 14, true, Theme.TEXT),
                Theme.label(subject, 13, false, Theme.TEXT), Theme.label(day + " at " + hour + " (1 hour)", 13, false, Theme.TEXT), topic, venue);
        Forms.open(frame, "Request a tutoring session", form, "Send request", dialog -> {
            String lesson = topic.getText(), location = venue.getText();
            controller.save("Your request was saved and sent to the tutor.",
                    () -> controller.service().requestSession(controller.actor(), tutorId, subject, lesson, day, hour, location), null, dialog);
        });
    }
}
