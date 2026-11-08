package aralbuddy.service;

import aralbuddy.model.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Creates presentation accounts only when there is no saved file. */
final class DemoData {
    private DemoData() { }

    static AppData create() {
        AppData data = new AppData();
        String[] codes = {"PROG1", "OOP", "DSA", "IM", "DISC", "WEB", "NET", "CALC"};
        String[] names = {"Programming 1", "Object-Oriented Programming", "Data Structures and Algorithms",
                "Information Management", "Discrete Mathematics", "Web Development", "Networking", "Calculus"};
        for (int i = 0; i < codes.length; i++) data.subjects.add(new Subject(codes[i], names[i]));
        data.users.add(new Admin(1, "ADMIN-001", "System Administrator", "admin@aralbuddy.edu", "admin1234"));
        data.users.add(new Student(2, "2022-0002", "Juan Dela Cruz", "juan@gmail.com", "password123", "BSIT", "2"));
        data.users.add(new Tutor(3, "2021-0001", "Maria Santos", "maria@gmail.com", "password123", "BSIT", "3",
                names[0] + ", " + names[1] + ", " + names[2], "Monday-Friday, 08:00-17:00"));
        data.users.add(new Tutor(4, "2021-0003", "Ana Reyes", "ana@gmail.com", "password123", "BSIT", "3",
                names[3] + ", " + names[4] + ", " + names[5], "Monday-Friday, 09:00-16:00"));
        data.users.add(new Tutor(5, "2020-0004", "Carlo Mendoza", "carlo@gmail.com", "password123", "BSIT", "4",
                names[2] + ", " + names[6] + ", " + names[7], "Monday-Friday, 13:00-17:00"));
        data.users.add(new Student(6, "2023-0005", "Liza Garcia", "liza@gmail.com", "password123", "BSIT", "1"));
        data.applications.add(new TutorApplication(1, 6, names[0], "Tue, Thu, 13:00-17:00", "I enjoy helping classmates learn programming."));
        LocalDate next = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        addSession(data, 2, 3, names[1], "Interfaces", next, "15:00", Session.REQUESTED);
        addSession(data, 2, 3, names[2], "Stacks and queues", next.plusDays(1), "14:00", Session.CONFIRMED);
        addSession(data, 2, 4, names[3], "ER diagrams", next.minusWeeks(2), "14:00", Session.MISSED);
        addSession(data, 2, 5, names[2], "Linked lists", next.minusWeeks(3), "15:00", Session.COMPLETED);
        addSession(data, 2, 3, names[1], "Classes and inheritance", next.minusWeeks(4), "14:00", Session.COMPLETED);
        addSession(data, 6, 3, names[0], "Variables and data types", next.plusDays(2), "13:00", Session.REQUESTED);
        data.sessions.get(3).rate(4, "Clear examples.");
        ((Tutor) data.users.get(4)).addRating(4);
        data.sessions.get(4).rate(5, "Very helpful.");
        ((Tutor) data.users.get(2)).addRating(5);
        return data;
    }

    private static void addSession(AppData data, int student, int tutor, String subject, String topic,
            LocalDate date, String time, String status) {
        Session session = new Session(data.sessions.size() + 1, student, tutor, subject, topic, date.toString(), time, "Library, Room 2");
        session.setStatus(status);
        data.sessions.add(session);
    }
}
