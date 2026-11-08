package aralbuddy.model;

import java.time.*;

/** Approved student tutor with weekly availability and accumulated ratings. */
public class Tutor extends Student {
    private static final long serialVersionUID = 1L;

    private String subjects;
    private String availability;
    private int ratingTotal;
    private int ratingCount;

    /** Creates a Tutor record from validated values. */
    public Tutor(int id, String idNumber, String name, String email, String password,
            String program, String yearLevel, String subjects, String availability) {
        super(id, idNumber, name, email, password, program, yearLevel);
        this.subjects = subjects;
        this.availability = availability;
    }

    /** Creates a Tutor record from validated values. */
    public Tutor(Student student, String subjects, String availability) {
        super(student);
        this.subjects = subjects;
        this.availability = availability;
    }

    /** Returns the exact course names separated by a comma and space. */
    public String getSubjects() {
        return subjects;
    }

    /** Returns the weekly days and available time range. */
    public String getAvailability() {
        return availability;
    }

    /** Accumulates one rating after the service checks session ownership. */
    public void addRating(int rating) {
        ratingTotal += rating;
        ratingCount++;
    }

    /** Returns the average score and the number of submitted ratings. */
    public String getRatingText() {
        if (ratingCount == 0) {
            return "No rating yet";
        }
        double average = (double) ratingTotal / ratingCount;
        return String.format(java.util.Locale.ROOT, "%.1f / 5 (%d ratings)", average, ratingCount);
    }

    /** Matches an entire course name without treating partial names as courses. */
    public boolean teaches(String subjectName) {
        return java.util.Arrays.stream(subjects.split(", ")).anyMatch(subject -> subject.equalsIgnoreCase(subjectName));
    }

    /** Checks whether a full one-hour session fits the weekly availability. */
    public boolean isFreeAt(String date, String time) {
        int separator = availability.lastIndexOf(", ");
        if (separator < 0) {
            return false;
        }
        String days = availability.substring(0, separator).toLowerCase();
        String[] hours = availability.substring(separator + 2).split("-");
        if (hours.length != 2) {
            return false;
        }

        try {
            int dayNumber = LocalDate.parse(date).getDayOfWeek().getValue();
            boolean dayMatches;
            if (days.contains("monday-friday")) {
                dayMatches = dayNumber >= 1 && dayNumber <= 5;
            } else if (days.contains("saturday-sunday")) {
                dayMatches = dayNumber == 6 || dayNumber == 7;
            } else {
                String[] shortNames = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"};
                dayMatches = days.contains(shortNames[dayNumber - 1]);
            }
            if (!dayMatches) {
                return false;
            }

            LocalTime start = LocalTime.parse(time);
            LocalTime end = start.plusHours(Session.DURATION_HOURS);
            boolean startOk = !start.isBefore(LocalTime.parse(hours[0]));
            boolean endOk = !end.isAfter(LocalTime.parse(hours[1]));
            return startOk && endOk;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    /** Returns the account role shown in navigation and account tables. */
    @Override
    public String getRole() {
        return "Student + Tutor";
    }

    /** Reports whether this student has an approved tutor role. */
    @Override
    public boolean isTutor() {
        return true;
    }
}
