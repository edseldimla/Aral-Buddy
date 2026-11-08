package aralbuddy.model;

/** Course code and name used by the current booking catalog. */
public class Subject implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private String code;
    private String name;

    /** Creates a Subject record from validated values. */
    public Subject(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /** Returns the unique course code. */
    public String getCode() {
        return code;
    }

    /** Returns the display name. */
    public String getName() {
        return name;
    }
}
