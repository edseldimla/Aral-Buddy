package aralbuddy.model;

import java.io.Serializable;
import java.util.ArrayList;


/** Versioned local snapshot; collection order also preserves the request queue. */
public final class AppData implements Serializable {
    private static final long serialVersionUID = 1L;
    /** Format version checked before a saved file is used. */
    public int version = 1;
    /** Accounts, including student, tutor, and administrator subclasses. */
    public final ArrayList<User> users = new ArrayList<>();
    /** Courses currently open for booking. */
    public final ArrayList<Subject> subjects = new ArrayList<>();
    /** Applications and their review decisions. */
    public final ArrayList<TutorApplication> applications = new ArrayList<>();
    /** Session records kept in insertion order. */
    public final ArrayList<Session> sessions = new ArrayList<>();
}
