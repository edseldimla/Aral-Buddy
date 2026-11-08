package aralbuddy.view;

import aralbuddy.controller.AppController;
import aralbuddy.model.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

/** Groups the four small administrator screens around the same service boundary. */
public final class AdminPages {
    private AdminPages() { }

    /** Reviews applications with full details, confirmations, and a retained decision history. */
    @SuppressWarnings("serial")
    public static final class Applications extends Page {
        private static final long serialVersionUID = 1L;
        private final MainFrame frame;
        private final AppController controller;

        /** Creates the administrator's application review table. */
        public Applications(MainFrame frame, AppController controller) {
            super("Tutor applications", "Review the courses, availability, and reason before approving an application.");
            this.frame = frame; this.controller = controller;
            records = new DataTable("Student", "Program", "Courses", "Availability", "Reason", "Status"); body.add(records, BorderLayout.CENTER);
            actions.add(Theme.button("Approve selected application", true, () -> controller.guard(() -> review(true))));
            actions.add(Theme.button("Reject selected application", false, () -> controller.guard(() -> review(false))));
            actions.add(Theme.button("View application details", false, () -> controller.guard(this::details)));
        }

        /** Lists pending applications first and retains previously reviewed records. */
        @Override public void refresh() {
            records.clearRows();
            controller.service().applications().stream().sorted(Comparator.comparing(a -> !a.isPending())).forEach(a -> {
                User student = controller.service().user(a.getStudentId());
                records.addRow(a.getId(), student.getName(), student.getProgramText(), a.getSubjects(), a.getAvailability(), a.getReason(), a.getStatus());
            });
        }

        private TutorApplication selected() {
            int id = (Integer) records.selectedKey();
            return controller.service().applications().stream().filter(a -> a.getId() == id).findFirst().orElseThrow();
        }
        private void review(boolean approve) {
            TutorApplication application = selected();
            if (Forms.confirm(frame, (approve ? "Approve" : "Reject") + " this tutor application?", "Review application"))
                controller.save(approve ? "Tutor account approved." : "Application rejected.", () -> controller.service().review(controller.actor(), application.getId(), approve), null);
        }
        private void details() {
            TutorApplication application = selected();
            Forms.details(frame, "Tutor application", controller.service().user(application.getStudentId()).getName() + "\n\nCourses: " + application.getSubjects()
                    + "\nAvailability: " + application.getAvailability() + "\nStatus: " + application.getStatus() + "\n\nReason:\n" + application.getReason());
        }
    }

    /** Manages student and tutor account activation without allowing administrator lockout. */
    @SuppressWarnings("serial")
    public static final class Accounts extends Page {
        private static final long serialVersionUID = 1L;
        private final MainFrame frame;
        private final AppController controller;

        /** Creates account search, sorting, and activation actions. */
        public Accounts(MainFrame frame, AppController controller) {
            super("Student accounts", "Search by name, student number, email, role, program, or account status.");
            this.frame = frame; this.controller = controller;
            records = new DataTable("Name", "Student number", "Email", "Role", "Program", "Status"); body.add(records, BorderLayout.CENTER);
            actions.add(Theme.button("Activate account", true, () -> controller.guard(() -> change(true))));
            actions.add(Theme.button("Deactivate account", false, () -> controller.guard(() -> change(false))));
        }

        /** Shows the latest account roles and activation states. */
        @Override public void refresh() {
            records.clearRows();
            controller.service().users().forEach(u -> records.addRow(u.getId(), u.getName(), u.getIdNumber(), u.getEmail(), u.getRole(), u.getProgramText(), u.isActive() ? "Active" : "Inactive"));
        }
        private void change(boolean active) {
            int id = (Integer) records.selectedKey();
            if (Forms.confirm(frame, (active ? "Activate " : "Deactivate ") + controller.service().user(id).getName() + "?", "Change account status"))
                controller.save("Account status saved.", () -> controller.service().setActive(controller.actor(), id, active), null);
        }
    }

    /** Maintains unique course codes and names while protecting referenced records. */
    @SuppressWarnings("serial")
    public static final class Courses extends Page {
        private static final long serialVersionUID = 1L;
        private final MainFrame frame;
        private final AppController controller;

        /** Creates a course catalog with add and confirmed removal actions. */
        public Courses(MainFrame frame, AppController controller) {
            super("Course catalog", "Courses used by tutors, pending applications, or open sessions are protected.");
            this.frame = frame; this.controller = controller;
            records = new DataTable("Code", "Course name"); body.add(records, BorderLayout.CENTER);
            actions.add(Theme.button("Add a course", true, () -> addCourse()));
            actions.add(Theme.button("Delete selected course", false, () -> controller.guard(this::remove)));
        }

        /** Reloads courses immediately after a saved catalog change. */
        @Override public void refresh() {
            records.clearRows(); controller.service().subjects().forEach(s -> records.addRow(s.getCode(), s.getCode(), s.getName()));
        }
        private void addCourse() {
            JTextField code = new JTextField(24), name = new JTextField(24);
            JPanel form = Forms.panel(new String[]{"Course code", "Course name"}, code, name);
            Forms.open(frame, "Add a course", form, "Save course", dialog -> {
                String key = code.getText(), title = name.getText();
                controller.save("Course saved.", () -> controller.service().addCourse(controller.actor(), key, title), null, dialog);
            });
        }
        private void remove() {
            String code = (String) records.selectedKey();
            if (Forms.confirm(frame, "Delete course " + code + "? Referenced courses cannot be removed.", "Delete course"))
                controller.save("Course removed.", () -> controller.service().removeCourse(controller.actor(), code), null);
        }
    }

    /** Displays attendance, course demand, and tutor activity as readable sortable tables. */
    @SuppressWarnings("serial")
    public static final class Reports extends Page {
        private static final long serialVersionUID = 1L;
        private final AppController controller;
        private final JLabel summary = Theme.label("", 14, true, Theme.TEAL);
        private final DataTable courses = new DataTable("Code", "Course", "Requests");
        private final DataTable tutors = new DataTable("Tutor", "Completed", "Requests", "Rating");
        private final DataTable statuses = new DataTable("Session status", "Count");

        /** Creates tabs for the existing session, course, and tutor reports. */
        public Reports(AppController controller) {
            super("Reports", "Attendance is completed / (completed + missed). Counts include saved history.");
            this.controller = controller; body.add(summary, BorderLayout.NORTH);
            records = statuses;
            JTabbedPane tabs = new JTabbedPane(); tabs.addTab("Session summary", records); tabs.addTab("Course demand", courses); tabs.addTab("Tutor activity", tutors);
            tabs.addChangeListener(event -> records = tabs.getSelectedIndex() == 0 ? (DataTable) tabs.getComponentAt(0) : tabs.getSelectedIndex() == 1 ? courses : tutors);
            body.add(tabs, BorderLayout.CENTER);
            actions.add(Theme.button("Refresh reports", false, this::refresh));
        }

        /** Recalculates all report tables and sorts course demand from most requested. */
        @Override public void refresh() {
            statuses.clearRows(); courses.clearRows(); tutors.clearRows();
            java.util.List<Session> sessions = controller.service().sessions();
            for (String status : new String[]{Session.REQUESTED, Session.RESCHEDULE_NEEDED, Session.CONFIRMED, Session.COMPLETED, Session.CANCELLED, Session.MISSED})
                statuses.addRow(status, status, sessions.stream().filter(s -> s.getStatus().equals(status)).count());
            long completed = sessions.stream().filter(s -> Session.COMPLETED.equals(s.getStatus())).count();
            long missed = sessions.stream().filter(s -> Session.MISSED.equals(s.getStatus())).count();
            summary.setText("Total sessions: " + sessions.size() + "     |     Attendance: " + (completed + missed == 0 ? "No attendance recorded" : Math.round(100.0 * completed / (completed + missed)) + "%"));
            Map<String, Long> demand = new HashMap<>(); sessions.forEach(s -> demand.merge(s.getSubject(), 1L, Long::sum));
            demand.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).forEach(entry -> {
                String code = controller.service().subjects().stream().filter(s -> s.getName().equals(entry.getKey())).map(Subject::getCode).findFirst().orElse("Archived");
                courses.addRow(entry.getKey(), code, entry.getKey(), entry.getValue());
            });
            for (User user : controller.service().users()) if (user instanceof Tutor) {
                long done = sessions.stream().filter(s -> s.getTutorId() == user.getId() && Session.COMPLETED.equals(s.getStatus())).count();
                long total = sessions.stream().filter(s -> s.getTutorId() == user.getId()).count();
                tutors.addRow(user.getId(), user.getName(), done, total, ((Tutor) user).getRatingText());
            }
        }
    }
}
