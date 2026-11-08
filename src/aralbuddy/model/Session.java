package aralbuddy.model;

/** One-hour tutoring request, attendance outcome, and optional student feedback. */
public class Session implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    /** Every booked session occupies exactly this many hours. */
    public static final int DURATION_HOURS = 1;

    /** Display label for the requested workflow state. */
    public static final String REQUESTED = "Requested";
    /** Display label for the confirmed workflow state. */
    public static final String CONFIRMED = "Confirmed";
    /** Display label for the reschedule needed workflow state. */
    public static final String RESCHEDULE_NEEDED = "Reschedule Needed";
    /** Display label for the completed workflow state. */
    public static final String COMPLETED = "Completed";
    /** Display label for the cancelled workflow state. */
    public static final String CANCELLED = "Cancelled";
    /** Display label for the missed workflow state. */
    public static final String MISSED = "Missed";

    private int id, studentId, tutorId, rating;
    private String subject, topic, date, time, venue, status = REQUESTED, feedback = "";
    private boolean rated;

    /** Creates a Session record from validated values. */
    public Session(int id, int studentId, int tutorId, String subject, String topic,
            String date, String time, String venue) {
        this.id = id;
        this.studentId = studentId;
        this.tutorId = tutorId;
        this.subject = subject;
        this.topic = topic;
        this.date = date;
        this.time = time;
        this.venue = venue;
    }

    /** Returns the stable record ID used by related records. */
    public int getId() {
        return id;
    }

    /** Returns the requesting student account ID. */
    public int getStudentId() {
        return studentId;
    }

    /** Returns the assigned tutor account ID. */
    public int getTutorId() {
        return tutorId;
    }

    /** Returns the course name saved with this session. */
    public String getSubject() {
        return subject;
    }

    /** Returns the lesson or topic requested by the student. */
    public String getTopic() {
        return topic;
    }

    /** Returns the session date in YYYY-MM-DD format. */
    public String getDate() {
        return date;
    }

    /** Returns the session start time using the 24-hour clock. */
    public String getTime() {
        return time;
    }

    /** Returns the meeting location or online meeting link. */
    public String getVenue() {
        return venue;
    }

    /** Returns the current workflow status. */
    public String getStatus() {
        return status;
    }

    /** Reports whether the student has already submitted a rating. */
    public boolean isRated() {
        return rated;
    }

    /** Returns the submitted score, or zero before a rating. */
    public int getRating() {
        return rating;
    }

    /** Returns the optional student feedback. */
    public String getFeedback() {
        return feedback;
    }

    /** Updates the date after the service validates the whole reschedule. */
    public void setDate(String date) {
        this.date = date;
    }

    /** Updates the start time after the service validates availability. */
    public void setTime(String time) {
        this.time = time;
    }

    /** Applies a workflow transition that the service has authorized. */
    public void setStatus(String status) {
        this.status = status;
    }

    /** Stores one validated rating and its feedback. */
    public void rate(int rating, String feedback) {
        this.rating = rating;
        this.feedback = feedback;
        this.rated = true;
    }

    /** Reports whether the account is either participant in this session. */
    public boolean involves(int userId) {
        return studentId == userId || tutorId == userId;
    }

    /** Returns the other participant for a user involved in this session. */
    public int getOtherPersonId(int userId) {
        if (studentId == userId) {
            return tutorId;
        }
        return studentId;
    }

    /** Returns the participant role shown in session tables. */
    public String getRoleOf(int userId) {
        if (studentId == userId) {
            return "Tutee";
        }
        return "Tutor";
    }

    /** Reports whether the session belongs in completed, missed, or cancelled history. */
    public boolean isHistory() {
        return status.equals(COMPLETED) || status.equals(MISSED) || status.equals(CANCELLED);
    }

    /** Returns a compact course, schedule, and status description. */
    public String getSummary() {
        return subject + " - " + date + " " + time + " (" + status + ")";
    }
}
