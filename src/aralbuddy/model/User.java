package aralbuddy.model;

/** Shared account identity and salted password hash for each application role. */
public abstract class User implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String idNumber;
    private String name;
    private String email;
    private String password;
    private boolean active = true;

    /** Creates a User record from validated values. */
    public User(int id, String idNumber, String name, String email, String password) {
        this.id = id;
        this.idNumber = idNumber;
        this.name = name;
        this.email = email;
        this.password = aralbuddy.util.PasswordUtil.hash(password);
    }

    protected User(User other) {
        this.id = other.id;
        this.idNumber = other.idNumber;
        this.name = other.name;
        this.email = other.email;
        this.password = other.password;
        this.active = other.active;
    }

    /** Returns the stable record ID used by related records. */
    public int getId() {
        return id;
    }

    /** Returns the unique student or administrator number. */
    public String getIdNumber() {
        return idNumber;
    }

    /** Returns the display name. */
    public String getName() {
        return name;
    }

    /** Returns the registered email address. */
    public String getEmail() {
        return email;
    }

    /** Reports whether this account can log in and make new requests. */
    public boolean isActive() {
        return active;
    }

    /** Updates the display name after service validation. */
    public void setName(String name) {
        this.name = name;
    }

    /** Changes the account activation state after administrator review. */
    public void setActive(boolean active) {
        this.active = active;
    }

    /** Checks a password against the stored salted hash. */
    public boolean checkPassword(String password) {
        return aralbuddy.util.PasswordUtil.matches(password, this.password);
    }

    /** Replaces the salted password hash after service validation. */
    public void changePassword(String password) {
        this.password = aralbuddy.util.PasswordUtil.hash(password);
    }

    /** Returns the display label implemented by each concrete account role. */
    public abstract String getRole();

    /** Reports whether this account has administrator permissions. */
    public boolean isAdmin() {
        return false;
    }

    /** Reports whether this student has an approved tutor role. */
    public boolean isTutor() {
        return false;
    }

    /** Returns the program and year label for account tables. */
    public String getProgramText() {
        return "";
    }
}
