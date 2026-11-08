package aralbuddy.view;

import aralbuddy.controller.AppController;
import aralbuddy.model.*;
import java.awt.BorderLayout;

/** Makes session and application notifications searchable instead of a long text block. */
@SuppressWarnings("serial")
public final class AlertsPage extends Page {
    private static final long serialVersionUID = 1L;
    private final AppController controller;

    /** Creates a structured notification table for either account role. */
    public AlertsPage(AppController controller) {
        super("Stay in the loop", "Updates from your tutoring sessions and tutor applications.");
        this.controller = controller;
        records = new DataTable("Type", "Update", "Course", "Date", "Time", "Status"); body.add(records, BorderLayout.CENTER);
    }

    /** Derives alerts from the latest saved state so they cannot become stale. */
    @Override public void refresh() {
        records.clearRows(); boolean admin = controller.currentUser().isAdmin();
        for (TutorApplication application : controller.service().applications()) {
            if ((admin && application.isPending()) || (!admin && application.getStudentId() == controller.actor()))
                records.addRow("a" + application.getId(), "Tutor application", controller.service().user(application.getStudentId()).getName(),
                        application.getSubjects(), "", "", application.getStatus());
        }
        for (Session session : controller.service().sessions()) {
            if ((admin && Session.REQUESTED.equals(session.getStatus())) || (!admin && session.involves(controller.actor())))
                records.addRow("s" + session.getId(), "Session", session.getTopic(), session.getSubject(), session.getDate(), session.getTime(), session.getStatus());
        }
    }
}
