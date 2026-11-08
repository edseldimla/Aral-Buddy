package aralbuddy.service;

import aralbuddy.model.*;
import aralbuddy.util.Validation;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import static aralbuddy.util.Validation.require;

/** Applies permissions and business rules independently of Swing screens. */
public final class AppService implements AutoCloseable {
    private final FileStore store;
    private AppData data;
    private SinglyLinkedList sessions;

    /** Loads saved work or creates the first-run demonstration data. */
    public AppService(FileStore store) throws IOException {
        this.store = store;
        data = store.load();
        if (data == null) { data = DemoData.create(); store.save(data); }
        validateSavedData();
        rebuildSessionList();
    }

    /** Returns the saved file location for the status bar. */
    public String getDataLocation() { return store.getPath().toString(); }

    /** Saves the whole operation or restores the previous in-memory state on failure. */
    public synchronized <T> T commit(Supplier<T> operation) throws IOException {
        byte[] before = FileStore.encode(data);
        try {
            T result = operation.get();
            store.save(data);
            return result;
        } catch (IOException | RuntimeException exception) {
            data = FileStore.decode(before);
            rebuildSessionList();
            throw exception;
        }
    }

    /** Authenticates an active account with a salted password check. */
    public synchronized User login(String email, String password) {
        User user = data.users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email.trim())).findFirst().orElse(null);
        require(user != null && user.checkPassword(password), "The email or password is incorrect.");
        require(user.isActive(), "This account is inactive. Ask your administrator for help.");
        return user;
    }

    /** Finds the current account object, including a newly approved tutor role. */
    public synchronized User user(int id) {
        return data.users.stream().filter(u -> u.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("That account no longer exists."));
    }

    /** Gives screens a read-only account list. Changes go through service methods. */
    public synchronized List<User> users() { return List.copyOf(data.users); }
    /** Gives screens the current course catalog. */
    public synchronized List<Subject> subjects() { return List.copyOf(data.subjects); }
    /** Gives administrators and applicants their review records. */
    public synchronized List<TutorApplication> applications() { return List.copyOf(data.applications); }
    /** Traverses the teaching project's custom linked list in insertion order. */
    public synchronized List<Session> sessions() { return List.copyOf(sessions.display()); }

    /** Registers a student after checking all fields and both unique account identifiers. */
    public synchronized Student register(String number, String name, String email, String password, String program, String year) {
        number = shortText(number, "Student number"); name = shortText(name, "Full name");
        email = Validation.email(email); program = shortText(program, "Program");
        Validation.password(password); validateYear(year);
        for (User existing : data.users) {
            require(!existing.getEmail().equalsIgnoreCase(email), "That email is already registered.");
            require(!existing.getIdNumber().equalsIgnoreCase(number), "That student number is already registered.");
        }
        int id = data.users.stream().mapToInt(User::getId).max().orElse(0) + 1;
        Student student = new Student(id, number, name, email, password, program, year);
        data.users.add(student);
        return student;
    }

    /** Finds active tutors who teach the exact course and can take the full hour. */
    public synchronized List<Tutor> findTutors(int actor, String subject, String name, String date, String time) {
        activeStudent(actor); validateCourse(subject); Validation.futureSlot(date, time);
        return data.users.stream().filter(Tutor.class::isInstance).map(Tutor.class::cast)
                .filter(t -> t.isActive() && t.getId() != actor && t.teaches(subject))
                .filter(t -> t.getName().toLowerCase(Locale.ROOT).contains(name.trim().toLowerCase(Locale.ROOT)))
                .filter(t -> t.isFreeAt(date, time) && !hasConflict(t.getId(), date, time, -1) && !hasConflict(actor, date, time, -1))
                .toList();
    }

    /** Rechecks the current inputs so a stale search result cannot create an invalid booking. */
    public synchronized Session requestSession(int actor, int tutorId, String subject, String topic, String date, String time, String venue) {
        activeStudent(actor);
        validateBooking(actor, tutorId, subject, date, time, -1);
        topic = shortText(topic, "Topic"); venue = shortText(venue, "Venue or meeting link");
        for (Session existing : data.sessions) {
            require(!(existing.getStudentId() == actor && existing.getTutorId() == tutorId && existing.getDate().equals(date)
                    && existing.getTime().equals(time) && !existing.isHistory()), "You already have an open request with this tutor at that time.");
        }
        int id = data.sessions.stream().mapToInt(Session::getId).max().orElse(0) + 1;
        Session session = new Session(id, actor, tutorId, subject, topic, date, time, venue);
        data.sessions.add(session); sessions.add(session);
        return session;
    }

    /** Detects any overlapping confirmed one-hour session, including the other user role. */
    public synchronized boolean hasConflict(int userId, String date, String time, int ignoredId) {
        LocalDateTime start = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
        for (Session session : data.sessions) {
            if (session.getId() == ignoredId || !session.involves(userId) || !Session.CONFIRMED.equals(session.getStatus())) continue;
            LocalDateTime other = LocalDateTime.of(LocalDate.parse(session.getDate()), LocalTime.parse(session.getTime()));
            if (start.isBefore(other.plusHours(Session.DURATION_HOURS)) && other.isBefore(start.plusHours(Session.DURATION_HOURS))) return true;
        }
        return false;
    }

    /** Builds an independent FIFO queue; viewing it does not delete stored requests. */
    public synchronized SessionQueue requestQueue(int tutorId) {
        SessionQueue queue = new SessionQueue();
        for (Session session : data.sessions)
            if (session.getTutorId() == tutorId && Session.REQUESTED.equals(session.getStatus())) queue.enqueue(session);
        return queue;
    }

    /** Answers the oldest pending request, checking both participants before confirmation. */
    public synchronized String respondToNext(int actor, boolean accept) {
        require(activeStudent(actor).isTutor(), "Only an approved tutor can answer requests.");
        Session next = requestQueue(actor).dequeue();
        require(next != null, "There are no pending requests.");
        if (!accept) { next.setStatus(Session.CANCELLED); return "Request declined."; }
        try {
            validateBooking(next.getStudentId(), actor, next.getSubject(), next.getDate(), next.getTime(), next.getId());
        } catch (IllegalArgumentException exception) {
            next.setStatus(Session.RESCHEDULE_NEEDED);
            return "The request needs a new time: " + exception.getMessage();
        }
        next.setStatus(Session.CONFIRMED);
        return "Session confirmed.";
    }

    /** Sends an eligible student's new time back to the tutor for approval. */
    public synchronized void reschedule(int actor, int id, String date, String time) {
        activeStudent(actor); Session session = session(id);
        require(session.getStudentId() == actor, "Only the requesting student can choose a new time.");
        require(Set.of(Session.REQUESTED, Session.RESCHEDULE_NEEDED).contains(session.getStatus()), "This session cannot be rescheduled.");
        validateBooking(actor, session.getTutorId(), session.getSubject(), date, time, id);
        session.setDate(date); session.setTime(time); session.setStatus(Session.REQUESTED);
        // A new time is a new request and should go to the back of the FIFO queue.
        data.sessions.remove(session); data.sessions.add(session); rebuildSessionList();
    }

    /** Cancels a student's open session while keeping its history. */
    public synchronized void cancel(int actor, int id) {
        activeStudent(actor); Session session = session(id);
        require(session.getStudentId() == actor && !session.isHistory(), "Only your own open requests can be cancelled.");
        session.setStatus(Session.CANCELLED);
    }

    /** Lets the assigned tutor record attendance after the confirmed hour ends. */
    public synchronized void recordOutcome(int actor, int id, String status) {
        activeStudent(actor); Session session = session(id);
        require(session.getTutorId() == actor && Session.CONFIRMED.equals(session.getStatus()), "Select your confirmed tutoring session.");
        require(Set.of(Session.COMPLETED, Session.MISSED).contains(status), "Choose completed or missed.");
        LocalDateTime end = LocalDateTime.of(LocalDate.parse(session.getDate()), LocalTime.parse(session.getTime())).plusHours(Session.DURATION_HOURS);
        require(!end.isAfter(LocalDateTime.now()), "Record attendance after the session's scheduled hour ends.");
        session.setStatus(status);
    }

    /** Saves exactly one student rating per completed session and updates the tutor average. */
    public synchronized void rate(int actor, int id, int rating, String feedback) {
        activeStudent(actor); Session session = session(id);
        require(session.getStudentId() == actor && Session.COMPLETED.equals(session.getStatus()) && !session.isRated(),
                "Only your unrated completed sessions can be rated.");
        require(rating >= 1 && rating <= 5, "Choose a whole-number rating from 1 to 5.");
        require(feedback != null && feedback.length() <= Validation.NOTE_LIMIT, "Feedback must have at most 2000 characters.");
        ((Tutor) user(session.getTutorId())).addRating(rating);
        session.rate(rating, feedback.trim());
    }

    /** Creates one pending application with exact courses and a weekly availability range. */
    public synchronized void apply(int actor, List<String> courses, List<String> days, String from, String to, String reason) {
        Student student = activeStudent(actor);
        require(!student.isTutor(), "Your tutor account is already approved.");
        require(data.applications.stream().noneMatch(a -> a.getStudentId() == actor && a.isPending()), "You already have a pending application.");
        require(!courses.isEmpty(), "Choose at least one course.");
        courses.forEach(this::validateCourse);
        Set<String> validDays = Set.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
        require(!days.isEmpty() && validDays.containsAll(days), "Choose at least one valid day.");
        try { require(LocalTime.parse(from).isBefore(LocalTime.parse(to)), "The end time must be later than the start time."); }
        catch (java.time.format.DateTimeParseException exception) { throw new IllegalArgumentException("Choose valid availability times."); }
        reason = Validation.text(reason, "Reason", Validation.NOTE_LIMIT);
        int id = data.applications.stream().mapToInt(TutorApplication::getId).max().orElse(0) + 1;
        data.applications.add(new TutorApplication(id, actor, String.join(", ", courses), String.join(", ", days) + ", " + from + "-" + to, reason));
    }

    /** Keeps the same account ID, profile, and password when approving a tutor. */
    public synchronized void review(int actor, int applicationId, boolean approve) {
        administrator(actor);
        TutorApplication application = data.applications.stream().filter(a -> a.getId() == applicationId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select an application."));
        require(application.isPending(), "This application was already reviewed.");
        Student student = activeStudent(application.getStudentId());
        if (approve) {
            require(!student.isTutor(), "This student is already a tutor.");
            for (String course : application.getSubjects().split(", ")) validateCourse(course);
            data.users.set(data.users.indexOf(student), new Tutor(student, application.getSubjects(), application.getAvailability()));
            application.approve();
        } else application.reject();
    }

    /** Changes only non-administrator accounts; inactive tutors are hidden from new bookings. */
    public synchronized void setActive(int actor, int userId, boolean active) {
        administrator(actor); User user = user(userId);
        require(!user.isAdmin(), "Administrator accounts cannot be deactivated here.");
        user.setActive(active);
    }

    /** Adds a unique course without changing historical session labels. */
    public synchronized void addCourse(int actor, String code, String name) {
        administrator(actor); code = shortText(code, "Course code").toUpperCase(Locale.ROOT); name = shortText(name, "Course name");
        require(!name.contains(","), "Use a course name without commas.");
        for (Subject subject : data.subjects) {
            require(!subject.getCode().equalsIgnoreCase(code), "That course code already exists.");
            require(!subject.getName().equalsIgnoreCase(name), "That course name already exists.");
        }
        data.subjects.add(new Subject(code, name));
    }

    /** Removes an unused course and protects current tutor and application references. */
    public synchronized void removeCourse(int actor, String code) {
        administrator(actor);
        Subject subject = data.subjects.stream().filter(s -> s.getCode().equals(code)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Select a course."));
        require(data.users.stream().filter(Tutor.class::isInstance).map(Tutor.class::cast).noneMatch(t -> t.teaches(subject.getName())),
                "A tutor teaches this course. Keep it in the catalog.");
        require(data.applications.stream().noneMatch(a -> a.isPending() && Arrays.asList(a.getSubjects().split(", ")).contains(subject.getName())),
                "A pending application uses this course. Review the application first.");
        require(data.sessions.stream().noneMatch(s -> !s.isHistory() && s.getSubject().equals(subject.getName())), "An open session uses this course.");
        data.subjects.remove(subject);
    }

    /** Validates the entire edit before changing any profile field. */
    public synchronized void updateProfile(int actor, String name, String program, String year, String password) {
        Student student = activeStudent(actor); name = shortText(name, "Full name"); program = shortText(program, "Program");
        validateYear(year); if (!password.isEmpty()) Validation.password(password);
        student.updateProfile(name, program, year);
        if (!password.isEmpty()) student.changePassword(password);
    }

    private void validateBooking(int student, int tutorId, String subject, String date, String time, int ignoredId) {
        activeStudent(student); User account = user(tutorId);
        require(account instanceof Tutor && account.isActive() && student != tutorId, "Choose a different active tutor.");
        validateCourse(subject); Validation.futureSlot(date, time);
        Tutor tutor = (Tutor) account;
        require(tutor.teaches(subject), "This tutor does not teach the selected course. Search again.");
        require(tutor.isFreeAt(date, time), "The full hour must fit the tutor's weekly availability.");
        require(!hasConflict(student, date, time, ignoredId) && !hasConflict(tutorId, date, time, ignoredId),
                "The student or tutor already has an overlapping confirmed session.");
    }

    private Student activeStudent(int id) {
        User user = user(id);
        require(user instanceof Student && user.isActive(), "An active student account is required.");
        return (Student) user;
    }

    private void administrator(int id) { require(user(id).isAdmin() && user(id).isActive(), "Administrator access is required."); }
    private void validateCourse(String name) { require(data.subjects.stream().anyMatch(s -> s.getName().equals(name)), "Choose a course from the current catalog."); }
    private void validateYear(String year) { require(Set.of("1", "2", "3", "4").contains(year), "Choose a year level from 1 to 4."); }
    private String shortText(String value, String label) { return Validation.text(value, label, Validation.TEXT_LIMIT); }
    private Session session(int id) {
        Session session = sessions.searchById(id);
        require(session != null, "That session no longer exists."); return session;
    }
    private void rebuildSessionList() { sessions = new SinglyLinkedList(); data.sessions.forEach(sessions::add); }

    private void validateSavedData() throws IOException {
        try {
            require(data.users != null && data.subjects != null && data.sessions != null && data.applications != null, "Missing data collections.");
            validateAccounts();
            validateCourses();
            validateSessions();
            validateApplications();
        } catch (RuntimeException exception) { throw new IOException("Saved data is invalid: " + exception.getMessage() + " The file was kept.", exception); }
    }

    private void validateAccounts() {
        Set<Integer> ids = new HashSet<>(); Set<String> emails = new HashSet<>(); Set<String> numbers = new HashSet<>();
        for (User user : data.users) {
            require(user != null && user.getId() > 0 && ids.add(user.getId()), "Duplicate or invalid account ID.");
            require(emails.add(Validation.email(user.getEmail()).toLowerCase(Locale.ROOT)), "Duplicate email.");
            require(numbers.add(shortText(user.getIdNumber(), "Student number").toLowerCase(Locale.ROOT)), "Duplicate student number.");
            shortText(user.getName(), "Name");
            if (user instanceof Student) { validateYear(((Student) user).getYearLevel()); shortText(((Student) user).getProgram(), "Program"); }
            if (user instanceof Tutor) {
                Validation.text(((Tutor) user).getSubjects(), "Tutor courses", Validation.NOTE_LIMIT);
                Validation.text(((Tutor) user).getAvailability(), "Tutor availability", Validation.NOTE_LIMIT);
            }
        }
        require(data.users.stream().anyMatch(u -> u.isAdmin() && u.isActive()), "No active administrator.");
    }

    private void validateCourses() {
        Set<String> codes = new HashSet<>(); Set<String> names = new HashSet<>();
        for (Subject subject : data.subjects) {
            require(codes.add(shortText(subject.getCode(), "Code").toLowerCase(Locale.ROOT)), "Duplicate course code.");
            require(names.add(shortText(subject.getName(), "Course").toLowerCase(Locale.ROOT)), "Duplicate course.");
        }
    }

    private void validateSessions() {
        Set<Integer> sessionIds = new HashSet<>();
        Set<String> statuses = Set.of(Session.REQUESTED, Session.CONFIRMED, Session.RESCHEDULE_NEEDED, Session.COMPLETED, Session.MISSED, Session.CANCELLED);
        for (Session session : data.sessions) {
            require(sessionIds.add(session.getId()) && session.getId() > 0, "Duplicate session ID.");
            require(user(session.getStudentId()) instanceof Student && user(session.getTutorId()) instanceof Tutor
                    && session.getStudentId() != session.getTutorId(), "Invalid session participants.");
            LocalDate.parse(session.getDate()); LocalTime.parse(session.getTime());
            require(statuses.contains(session.getStatus()), "Unknown session status.");
            shortText(session.getSubject(), "Course"); shortText(session.getTopic(), "Topic"); shortText(session.getVenue(), "Venue");
            require(!session.isRated() || (Session.COMPLETED.equals(session.getStatus()) && session.getRating() >= 1 && session.getRating() <= 5), "Invalid rating.");
            require(session.getFeedback() != null && session.getFeedback().length() <= Validation.NOTE_LIMIT, "Invalid feedback.");
        }
    }

    private void validateApplications() {
        Set<Integer> applicationIds = new HashSet<>();
        for (TutorApplication application : data.applications) {
            require(applicationIds.add(application.getId()) && application.getId() > 0, "Duplicate application ID.");
            require(user(application.getStudentId()) instanceof Student, "Invalid applicant.");
            require(Set.of(TutorApplication.PENDING, TutorApplication.APPROVED, TutorApplication.REJECTED).contains(application.getStatus()), "Invalid application status.");
            Validation.text(application.getReason(), "Reason", Validation.NOTE_LIMIT);
            Validation.text(application.getSubjects(), "Application courses", Validation.NOTE_LIMIT);
            Validation.text(application.getAvailability(), "Application availability", Validation.NOTE_LIMIT);
        }
    }

    /** Releases the data-folder lock after all saves have completed. */
    @Override public void close() throws IOException { store.close(); }
}
