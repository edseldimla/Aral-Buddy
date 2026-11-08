package aralbuddy.view;

import aralbuddy.controller.AppController;
import aralbuddy.model.*;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/** Summarizes the current user's work and gives administrators an overall view. */
@SuppressWarnings("serial")
public final class OverviewPage extends Page {
    private static final long serialVersionUID = 1L;
    private final AppController controller;
    private final JPanel metrics = new JPanel(new GridLayout(1, 3, 14, 0));

    /** Creates summary cards and an open-session table. */
    public OverviewPage(AppController controller) {
        super("Overview", "See what is coming up and what needs your attention.");
        this.controller = controller;
        metrics.setOpaque(false); body.add(metrics, BorderLayout.NORTH);
        records = new DataTable("Course", "Topic", "Date", "Time", "Student", "Tutor", "Status");
        body.add(records, BorderLayout.CENTER);
    }

    /** Recalculates counts from saved records, including the current account role. */
    @Override public void refresh() {
        boolean admin = controller.currentUser().isAdmin();
        List<Session> sessions = controller.service().sessions().stream()
                .filter(s -> admin || s.involves(controller.actor())).toList();
        metrics.removeAll();
        addMetric(admin ? "All sessions" : "My sessions", sessions.size());
        addMetric("Confirmed", sessions.stream().filter(s -> Session.CONFIRMED.equals(s.getStatus())).count());
        long attention = admin ? controller.service().applications().stream().filter(TutorApplication::isPending).count()
                : sessions.stream().filter(s -> Session.REQUESTED.equals(s.getStatus()) || Session.RESCHEDULE_NEEDED.equals(s.getStatus())).count();
        addMetric(admin ? "Applications to review" : "Awaiting action", attention);
        records.clearRows();
        sessions.stream().filter(s -> !s.isHistory()).sorted(java.util.Comparator.comparing(Session::getDate).thenComparing(Session::getTime))
                .forEach(s -> records.addRow(s.getId(), s.getSubject(), s.getTopic(), s.getDate(), s.getTime(),
                        controller.service().user(s.getStudentId()).getName(), controller.service().user(s.getTutorId()).getName(), s.getStatus()));
        metrics.revalidate(); metrics.repaint();
    }

    private void addMetric(String label, long value) {
        JPanel card = new JPanel(new BorderLayout(0, 8)); card.setBackground(Theme.BACKGROUND);
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.add(Theme.label(Long.toString(value), 32, true, Theme.TEAL), BorderLayout.CENTER);
        card.add(Theme.label(label, 12, false, Theme.MUTED), BorderLayout.SOUTH); metrics.add(card);
    }
}
