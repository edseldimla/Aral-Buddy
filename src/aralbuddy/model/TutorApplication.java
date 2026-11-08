package aralbuddy.model;

/** Student application with its requested courses, availability, and review decision. */
public class TutorApplication implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    /** Display label for the pending workflow state. */
    public static final String PENDING = "Pending";
    /** Display label for the approved workflow state. */
    public static final String APPROVED = "Approved";
    /** Display label for the rejected workflow state. */
    public static final String REJECTED = "Rejected";

    private int id, studentId;
    private String subjects, availability, reason, status = PENDING;

    /** Creates a TutorApplication record from validated values. */
    public TutorApplication(int id, int studentId, String subjects, String availability, String reason) {
        this.id = id;
        this.studentId = studentId;
        this.subjects = subjects;
        this.availability = availability;
        this.reason = reason;
    }

    /** Returns the stable record ID used by related records. */
    public int getId() {
        return id;
    }

    /** Returns the requesting student account ID. */
    public int getStudentId() {
        return studentId;
    }

    /** Returns the exact course names separated by a comma and space. */
    public String getSubjects() {
        return subjects;
    }

    /** Returns the weekly days and available time range. */
    public String getAvailability() {
        return availability;
    }

    /** Returns the getReason value. */
    public String getReason() {
        return reason;
    }

    /** Returns the current workflow status. */
    public String getStatus() {
        return status;
    }

    /** Reports whether an administrator still needs to review the application. */
    public boolean isPending() {
        return status.equals(PENDING);
    }

    /** Records an approval after the account has been promoted to tutor. */
    public void approve() {
        status = APPROVED;
    }

    /** Records a rejection while retaining the application history. */
    public void reject() {
        status = REJECTED;
    }
}
