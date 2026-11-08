package aralbuddy.model;

/** Student profile shared by learners and approved peer tutors. */
public class Student extends User {
    private static final long serialVersionUID = 1L;

    private String program;
    private String yearLevel;

    /** Creates a Student record from validated values. */
    public Student(int id, String idNumber, String name, String email, String password,
            String program, String yearLevel) {
        super(id, idNumber, name, email, password);
        this.program = program;
        this.yearLevel = yearLevel;
    }

    protected Student(Student other) {
        super(other);
        this.program = other.program;
        this.yearLevel = other.yearLevel;
    }

    /** Returns the degree program. */
    public String getProgram() {
        return program;
    }

    /** Returns the stored year level from 1 to 4. */
    public String getYearLevel() {
        return yearLevel;
    }

    /** Returns a readable year level label. */
    public String getYearLevelText() {
        if ("1".equals(yearLevel)) {
            return "1st year";
        }
        if ("2".equals(yearLevel)) {
            return "2nd year";
        }
        if ("3".equals(yearLevel)) {
            return "3rd year";
        }
        if ("4".equals(yearLevel)) {
            return "4th year";
        }
        return yearLevel;
    }

    /** Applies the profile fields that the service has already validated. */
    public void updateProfile(String name, String program, String yearLevel) {
        setName(name);
        this.program = program;
        this.yearLevel = yearLevel;
    }

    /** Returns the account role shown in navigation and account tables. */
    @Override
    public String getRole() {
        return "Student";
    }

    /** Returns the program and year label for account tables. */
    @Override
    public String getProgramText() {
        return program + ", " + getYearLevelText();
    }
}
