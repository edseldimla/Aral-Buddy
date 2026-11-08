package aralbuddy.view;

import aralbuddy.controller.AppController;
import aralbuddy.model.Session;
import java.awt.*;
import javax.swing.*;

/** Shows active or historical sessions with permission-checked actions. */
@SuppressWarnings("serial")
public final class SessionsPage extends Page {
    private static final long serialVersionUID = 1L;
    private final MainFrame frame;
    private final AppController controller;
    private final boolean history;

    /** Creates either the active session page or the full attendance and rating history. */
    public SessionsPage(MainFrame frame, AppController controller, boolean history) {
        super(history ? "Your learning history" : "Your scheduled sessions", history ? "Completed, missed, and cancelled sessions stay here for reference." : "Manage requests and attendance. Times use the 24-hour clock.");
        this.frame = frame; this.controller = controller; this.history = history;
        records = new DataTable("Course", "Topic", "Date", "Time", "With", "My role", "Status", "Venue", "Rating", "Feedback");
        body.add(records, BorderLayout.CENTER);
        if (history) {
            actions.add(Theme.button("Rate a completed session", true, () -> controller.guard(this::rate)));
            actions.add(Theme.button("View session details", false, () -> controller.guard(this::details)));
        } else {
            actions.add(Theme.button("Choose another time", false, () -> controller.guard(this::reschedule)));
            actions.add(Theme.button("Cancel session", false, () -> controller.guard(this::cancel)));
            actions.add(Theme.button("Tutor: mark completed", true, () -> controller.guard(() -> outcome(Session.COMPLETED))));
            actions.add(Theme.button("Tutor: mark missed", false, () -> controller.guard(() -> outcome(Session.MISSED))));
        }
    }

    /** Reloads only this user's sessions and never confuses sorted rows with record IDs. */
    @Override public void refresh() {
        records.clearRows();
        for (Session session : controller.service().sessions()) if (session.involves(controller.actor()) && session.isHistory() == history) {
            records.addRow(session.getId(), session.getSubject(), session.getTopic(), session.getDate(), session.getTime(),
                    controller.service().user(session.getOtherPersonId(controller.actor())).getName(), session.getRoleOf(controller.actor()), session.getStatus(), session.getVenue(),
                    session.isRated() ? session.getRating() + " / 5" : "Not rated", session.getFeedback());
        }
    }

    private Session selected() {
        int id = (Integer) records.selectedKey();
        return controller.service().sessions().stream().filter(s -> s.getId() == id).findFirst().orElseThrow();
    }

    private void reschedule() {
        Session session = selected();
        aralbuddy.util.Validation.require(session.getStudentId() == controller.actor() &&
                (Session.REQUESTED.equals(session.getStatus()) || Session.RESCHEDULE_NEEDED.equals(session.getStatus())),
                "Select your requested session or one that needs a new time.");
        JTextField date = new JTextField(session.getDate(), 16), time = new JTextField(session.getTime(), 16);
        JPanel form = Forms.panel(new String[]{"New date (YYYY-MM-DD)", "New start time (HH:mm)"}, date, time);
        Forms.open(frame, "Choose another time", form, "Send new time", dialog -> {
            String day = date.getText().trim(), hour = time.getText().trim();
            controller.save("New time saved. The tutor will review it.", () -> controller.service().reschedule(controller.actor(), session.getId(), day, hour), null, dialog);
        });
    }

    private void cancel() {
        Session session = selected();
        if (Forms.confirm(frame, "Cancel this session? It will stay in your history.", "Cancel session"))
            controller.save("Session cancelled.", () -> controller.service().cancel(controller.actor(), session.getId()), null);
    }

    private void outcome(String status) {
        Session session = selected();
        if (Forms.confirm(frame, "Mark this session " + status.toLowerCase() + "? Attendance can be recorded after the scheduled hour ends.", "Record attendance"))
            controller.save("Attendance saved.", () -> controller.service().recordOutcome(controller.actor(), session.getId(), status), null);
    }

    private void rate() {
        Session session = selected();
        aralbuddy.util.Validation.require(session.getStudentId() == controller.actor() && Session.COMPLETED.equals(session.getStatus()) && !session.isRated(),
                "Select one of your completed sessions that has not been rated.");
        JComboBox<Integer> rating = new JComboBox<>(new Integer[]{5, 4, 3, 2, 1});
        JTextArea feedback = new JTextArea(5, 26); feedback.setLineWrap(true); feedback.setWrapStyleWord(true);
        JPanel form = Forms.panel(new String[]{"Rating (1 to 5)", "Feedback (optional)"}, rating, new JScrollPane(feedback));
        Forms.open(frame, "Rate your session", form, "Save rating", dialog -> {
            int score = (Integer) rating.getSelectedItem(); String comment = feedback.getText();
            controller.save("Thank you. Your rating and feedback were saved.", () -> controller.service().rate(controller.actor(), session.getId(), score, comment), null, dialog);
        });
    }

    private void details() {
        Session session = selected();
        Forms.details(frame, "Session details", session.getSummary() + "\n\nTopic: " + session.getTopic() + "\nVenue: " + session.getVenue()
                + "\nRating: " + (session.isRated() ? session.getRating() + " / 5" : "Not rated") + "\n\nFeedback:\n" + session.getFeedback());
    }
}
