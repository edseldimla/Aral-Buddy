package aralbuddy;

import aralbuddy.model.*;
import aralbuddy.service.*;
import aralbuddy.service.FileStore;
import aralbuddy.util.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Runs real service and file tests without JUnit or outside dependencies. */
public final class ServiceTests {
    private static int checks;
    private ServiceTests() { }

    /** Exercises account, scheduling, review, rating, persistence, and failure behavior. */
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("aralbuddy-test-");
        try {
            runWorkflows(directory);
            testSavedHistory(directory);
            testCorruption(directory);
            testDataStructures();
            System.out.println("PASS: " + checks + " checks. Temporary test folder: " + directory);
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static void runWorkflows(Path directory) throws Exception {
        try (AppService service = new AppService(new FileStore(directory))) {
            check(service.users().size() == 6, "Demo accounts");
            check(service.login("JUAN@GMAIL.COM", "password123").getId() == 2, "Case-insensitive login");
            reject(() -> service.login("juan@gmail.com", "wrong"), "Wrong password");
            reject(() -> new FileStore(directory), "Concurrent process lock");
            reject(() -> service.commit(() -> service.register("2022-0002", "Duplicate", "new@example.com", "password123", "BSIT", "1")), "Duplicate student number");
            reject(() -> service.commit(() -> service.register("new", "Name", "broken", "password123", "BSIT", "1")), "Invalid email");
            reject(() -> service.commit(() -> service.register("new", "Name", "new@example.com", "short", "BSIT", "1")), "Short password");
            reject(() -> service.commit(() -> service.register("new", "Name", "juan@gmail.com", "password123", "BSIT", "1")), "Duplicate email");
            Student student = service.commit(() -> service.register("TEST-1", "Test Student", "test@example.com", "password123", "BSCS", "2"));
            int studentId = student.getId();
            check(studentId > 6, "Registration");
            String monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).plusWeeks(2).toString();
            check(service.findTutors(studentId, "Programming 1", "maria", monday, "10:00").size() == 1, "Course and name search");
            check(service.findTutors(studentId, "Programming 1", "", monday, "17:00").isEmpty(), "Full hour availability");
            reject(() -> service.findTutors(studentId, "Programming", "", monday, "10:00"), "Exact course validation");
            reject(() -> service.requestSession(studentId, 3, "Programming 1", "Topic", "2020-01-01", "10:00", "Library"), "Past booking");
            reject(() -> service.requestSession(studentId, 3, "Calculus", "Topic", monday, "10:00", "Library"), "Stale course selection");
            reject(() -> service.requestSession(studentId, 3, "Programming 1", "Topic", monday, "17:00", "Library"), "Outside availability");
            Session requested = service.commit(() -> service.requestSession(studentId, 3, "Programming 1", "Loops", monday, "10:00", "Library"));
            reject(() -> service.reschedule(studentId, requested.getId(), monday, "18:00"), "Reschedule availability");
            reject(() -> service.reschedule(2, requested.getId(), monday, "11:00"), "Reschedule ownership");
            reject(() -> service.requestSession(studentId, 3, "Programming 1", "Again", monday, "10:00", "Library"), "Duplicate pending request");
            reject(() -> service.respondToNext(studentId, true), "Student cannot confirm");
            service.commit(() -> service.respondToNext(3, true));
            check(service.sessions().get(0).getStatus().equals(Session.CONFIRMED), "FIFO answers oldest request first");
            service.commit(() -> service.respondToNext(3, true));
            service.commit(() -> service.respondToNext(3, true));
            check(service.hasConflict(3, monday, "10:30", -1), "Different-start overlap");
            check(service.hasConflict(studentId, monday, "09:30", -1), "Student overlap");
            check(!service.hasConflict(3, monday, "11:00", -1), "Adjacent hours allowed");
            reject(() -> service.requestSession(2, 3, "Programming 1", "Topic", monday, "10:30", "Library"), "Reject overlapping booking");
            reject(() -> service.recordOutcome(3, requested.getId(), Session.COMPLETED), "No early attendance");
            reject(() -> service.cancel(2, requested.getId()), "No cancelling another student's request");
            service.commit(() -> { service.cancel(studentId, requested.getId()); return null; });
            check(service.sessions().stream().anyMatch(s -> s.getId() == requested.getId() && s.isHistory()), "Cancelled record kept in history");
            Session rescheduled = service.commit(() -> service.requestSession(studentId, 3, "Programming 1", "Arrays", monday, "11:00", "Library"));
            service.commit(() -> { service.reschedule(studentId, rescheduled.getId(), monday, "12:00"); return null; });
            check(service.requestQueue(3).dequeue().getTime().equals("12:00"), "Valid reschedule stays in request queue");
            service.commit(() -> service.respondToNext(3, false));
            service.commit(() -> { service.apply(studentId, List.of("Programming 1"), List.of("Mon"), "09:00", "12:00", "I can explain programming."); return null; });
            reject(() -> service.apply(studentId, List.of("Programming 1"), List.of("Mon"), "09:00", "12:00", "Again"), "Duplicate application");
            int application = service.applications().get(service.applications().size() - 1).getId();
            reject(() -> service.review(2, application, true), "Admin permission");
            service.commit(() -> { service.review(1, application, true); return null; });
            check(service.user(studentId).isTutor(), "Tutor promotion");
            check(service.login("test@example.com", "password123").isTutor(), "Promotion preserves password");
            service.commit(() -> { service.updateProfile(studentId, "Updated Student", "BSIT", "3", "newpassword123"); return null; });
            check(service.login("test@example.com", "newpassword123").getName().equals("Updated Student"), "Profile and password update");
            service.commit(() -> { service.setActive(1, studentId, false); return null; });
            reject(() -> service.login("test@example.com", "newpassword123"), "Inactive login");
            reject(() -> service.setActive(1, 1, false), "Protect administrator");
            reject(() -> service.removeCourse(1, "PROG1"), "Protect referenced course");
            service.commit(() -> { service.addCourse(1, "TEST", "Test Course"); return null; });
            service.commit(() -> { service.removeCourse(1, "TEST"); return null; });
            check(service.subjects().size() == 8, "Course add and remove");
            int count = service.users().size();
            reject(() -> service.commit(() -> { service.register("ROLLBACK", "Rollback", "rollback@example.com", "password123", "BSIT", "1"); throw new IllegalArgumentException("Simulated failure"); }), "Transaction failure");
            check(service.users().size() == count, "In-memory rollback");
            Session rating = service.sessions().stream().filter(s -> s.getStudentId() == 2 && s.getStatus().equals(Session.COMPLETED)).findFirst().orElseThrow();
            reject(() -> service.rate(2, rating.getId(), 4, "Again"), "No duplicate rating");
            check(Files.exists(directory.resolve("aralbuddy.dat.bak")), "Previous save backup");
            String bytes = new String(Files.readAllBytes(directory.resolve("aralbuddy.dat")), java.nio.charset.StandardCharsets.ISO_8859_1);
            check(!bytes.contains("password123") && !bytes.contains("newpassword123"), "No plaintext passwords in saved file");
        }
        try (AppService reopened = new AppService(new FileStore(directory))) {
            check(reopened.users().size() == 7 && reopened.user(7).isTutor(), "Restart restores accounts and role");
            check(!reopened.user(7).isActive() && reopened.user(7).getName().equals("Updated Student"), "Restart restores profile and status");
            check(reopened.sessions().size() == 8, "Restart restores sessions");
        }
    }

    private static void testSavedHistory(Path directory) throws Exception {
        AppData data;
        try (FileStore store = new FileStore(directory)) {
            data = store.load();
            Session past = new Session(50, 2, 3, "Programming 1", "Past confirmed session", LocalDate.now().minusDays(2).toString(), "10:00", "Library");
            past.setStatus(Session.CONFIRMED); data.sessions.add(past); store.save(data);
        }
        try (AppService service = new AppService(new FileStore(directory))) {
            service.commit(() -> { service.recordOutcome(3, 50, Session.COMPLETED); return null; });
            reject(() -> service.rate(3, 50, 5, "Self rating"), "Tutor cannot rate own teaching");
            reject(() -> service.rate(2, 50, 6, "Invalid"), "Rating range");
            service.commit(() -> { service.rate(2, 50, 5, "Useful lesson"); return null; });
            check(service.sessions().stream().anyMatch(s -> s.getId() == 50 && s.getFeedback().equals("Useful lesson")), "Attendance and feedback");
            // Turn the save target into a directory to simulate a real write failure.
            Path file = directory.resolve("aralbuddy.dat");
            Path held = directory.resolve("held.dat");
            Files.move(file, held); Files.createDirectory(file);
            Files.writeString(file.resolve("blocked"), "Prevent replacement");
            reject(() -> service.commit(() -> { service.addCourse(1, "FAIL", "Failed save"); return null; }), "Disk write failure");
            check(service.subjects().stream().noneMatch(s -> s.getCode().equals("FAIL")), "Write failure rolls back edit");
            check(Files.isRegularFile(directory.resolve("aralbuddy.dat.bak")), "Write failure preserves previous backup");
            Files.delete(file.resolve("blocked")); Files.delete(file); Files.move(held, file);
        }
    }

    private static void testCorruption(Path directory) throws Exception {
        Path file = directory.resolve("aralbuddy.dat");
        byte[] original = Files.readAllBytes(file);
        Files.writeString(file, "Damaged file");
        try (FileStore store = new FileStore(directory)) { reject(store::load, "Corrupt file rejected"); }
        check(Files.readString(file).equals("Damaged file"), "Corrupt file never overwritten");
        Files.write(file, original);
        Files.move(file, directory.resolve("held-main.dat"));
        try (FileStore store = new FileStore(directory)) { reject(store::load, "Missing main with backup requires recovery"); }
        Files.move(directory.resolve("held-main.dat"), file);
        AppData unsupported = FileStore.decode(original); unsupported.version = 99;
        reject(() -> FileStore.decode(FileStore.encode(unsupported)), "Unsupported snapshot version");
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream output = new java.io.ObjectOutputStream(bytes)) { output.writeObject(new java.util.Date()); }
        reject(() -> FileStore.decode(bytes.toByteArray()), "Unapproved serialized class rejected");
        String hash = PasswordUtil.hash("password123");
        check(!hash.equals(PasswordUtil.hash("password123")) && PasswordUtil.matches("password123", hash), "Random salt and password verification");
        reject(() -> Validation.futureSlot("2026-02-30", "12:00"), "Impossible calendar date");
    }

    private static void testDataStructures() {
        Session a = new Session(1, 2, 3, "Course", "Topic", "2030-01-01", "10:00", "Room");
        Session b = new Session(2, 2, 3, "Course", "Topic", "2030-01-01", "11:00", "Room");
        SinglyLinkedList list = new SinglyLinkedList();
        check(list.add(a) && list.add(b) && !list.add(a), "Linked list unique IDs");
        check(list.deleteById(1) && list.searchById(2) == b && list.deleteById(2) && list.size() == 0, "Linked list head and tail deletion");
        SessionQueue queue = new SessionQueue(); queue.enqueue(a); queue.enqueue(b);
        check(queue.linearSearchBySessionId(2) == b && queue.dequeue() == a && queue.dequeue() == b && queue.isEmpty(), "FIFO queue and search");
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private static void reject(CheckedAction action, String label) throws Exception {
        try { action.run(); } catch (IllegalArgumentException | IOException expected) { checks++; return; }
        throw new AssertionError("Expected rejection: " + label);
    }

    @FunctionalInterface private interface CheckedAction { void run() throws Exception; }
}
