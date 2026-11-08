package aralbuddy.model;

/** Administrator account with permission to review tutors and manage the catalog. */
public class Admin extends User {
    private static final long serialVersionUID = 1L;

    /** Creates a Admin record from validated values. */
    public Admin(int id, String idNumber, String name, String email, String password) {
        super(id, idNumber, name, email, password);
    }

    /** Returns the account role shown in navigation and account tables. */
    @Override
    public String getRole() {
        return "Administrator";
    }

    /** Reports whether this account has administrator permissions. */
    @Override
    public boolean isAdmin() {
        return true;
    }
}
