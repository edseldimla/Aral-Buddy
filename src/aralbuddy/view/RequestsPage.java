package aralbuddy.view;

import aralbuddy.controller.AppController;
import aralbuddy.model.*;
import java.awt.BorderLayout;

/** Displays the tutor's FIFO queue and always answers its oldest pending request. */
@SuppressWarnings("serial")
public final class RequestsPage extends Page {
    private static final long serialVersionUID = 1L;
    private final AppController controller;

    /** Creates a read-only queue with explicit accept-next and decline-next actions. */
    public RequestsPage(MainFrame frame, AppController controller) {
        super("Your tutoring requests", "First in, first out. Apply under Become a tutor to start receiving requests.");
        this.controller = controller;
        records = new DataTable("Queue position", "Student", "Course", "Topic", "Date", "Time", "Venue"); body.add(records, BorderLayout.CENTER);
        actions.add(Theme.button("Accept next request", true, () -> controller.guard(() -> {
            if (Forms.confirm(frame, "Review and accept the oldest pending request?", "Accept request")) controller.respond(true);
        })));
        actions.add(Theme.button("Decline next request", false, () -> controller.guard(() -> {
            if (Forms.confirm(frame, "Decline the oldest pending request?", "Decline request")) controller.respond(false);
        })));
    }

    /** Traverses a copy of the queue so sorting and searching cannot change its order. */
    @Override public void refresh() {
        records.clearRows(); SessionQueue queue = controller.service().requestQueue(controller.actor()); int position = 1;
        while (!queue.isEmpty()) {
            Session session = queue.dequeue(); records.addRow(session.getId(), position++, controller.service().user(session.getStudentId()).getName(),
                    session.getSubject(), session.getTopic(), session.getDate(), session.getTime(), session.getVenue());
        }
    }
}
