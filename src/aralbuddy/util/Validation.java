package aralbuddy.util;

import aralbuddy.model.Session;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/** Shared rules for form values and one-hour appointments. */
public final class Validation {
    /** Small limits keep accidental pasted documents out of short form fields. */
    public static final int TEXT_LIMIT = 160;
    /** Long notes may contain several paragraphs. */
    public static final int NOTE_LIMIT = 2000;
    private Validation() { }

    /** Returns trimmed required text, or a helpful validation message. */
    public static String text(String value, String label, int limit) {
        require(value != null && !value.trim().isEmpty(), label + " is required.");
        String trimmed = value.trim();
        require(trimmed.length() <= limit, label + " must have at most " + limit + " characters.");
        require(trimmed.chars().noneMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t'),
                label + " contains an unsupported control character.");
        return trimmed;
    }

    /** Checks a practical email format without claiming that the mailbox exists. */
    public static String email(String value) {
        String email = text(value, "Email", TEXT_LIMIT);
        require(email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Enter an email such as name@example.com.");
        return email;
    }

    /** Requires a password long enough for a classroom application. */
    public static void password(String password) {
        require(password != null && password.length() >= 8 && password.length() <= 128,
                "Use a password with 8 to 128 characters.");
        require(!password.isBlank(), "The password cannot contain only spaces.");
    }

    /** Parses an exact ISO date and 24-hour time and requires a future appointment. */
    public static LocalDateTime futureSlot(String date, String time) {
        try {
            require(date != null && date.matches("\\d{4}-\\d{2}-\\d{2}"), "Use YYYY-MM-DD for the date.");
            require(time != null && time.matches("\\d{2}:\\d{2}"), "Use HH:mm for the time, for example 14:00.");
            LocalDateTime slot = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
            require(slot.isAfter(LocalDateTime.now()), "Choose a date and time in the future.");
            require(slot.toLocalDate().equals(slot.plusHours(Session.DURATION_HOURS).toLocalDate()), "The hour must end before midnight.");
            return slot;
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Enter a real date and time, for example 2026-12-14 and 14:00.");
        }
    }

    /** Raises a friendly error when a business rule is not satisfied. */
    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
