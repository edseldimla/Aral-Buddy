# Revision notes

## Original system review

The supplied ZIP contained 30 Java files in one `aralbuddy` package, with no build script, README, tests, or external dependencies. All original source files were read before editing. The original project compiled and opened on the installed JDK 25.

The purpose is to match students with peer tutors and track tutoring sessions. The main problems were memory-only data, plain text passwords, repeated screen-level rules, exact-start-only conflict checks, weak registration validation, stale booking results, rescheduling without availability checks, fixed demo dates, absolute positioning in login, and limited table navigation.

## Features preserved and improved

| Existing feature | Revised behavior |
| --- | --- |
| Registration and login | Unique email and student number, email/year/password checks, password confirmation, active-account checks |
| Administrator, student, and tutor roles | Original inheritance retained: Tutor extends Student extends User; Admin extends User |
| Tutor search and booking | Course, date, time, and optional tutor name; exact course match; full-hour availability; checks repeated when saving |
| Student/tutor session lists | Search, sorting, stable record selection, clearer actions, one-hour overlap checks |
| Rescheduling | Future date, active tutor, correct course, availability, both schedules; a resubmission moves to the back of the FIFO queue |
| Tutor request queue | Original custom FIFO queue retained; oldest pending request is answered regardless of table sort |
| Conflict handling | An invalid acceptance becomes Reschedule Needed with a reason instead of confirming an invalid booking |
| Attendance | Assigned tutor can mark completed or missed after the confirmed session's hour ends |
| Cancellation | Confirmation dialog; cancelled sessions stay in history rather than cluttering active sessions |
| Session history | Completed, missed, and cancelled records; readable details and feedback |
| Ratings and feedback | Student-only, completed-only, one rating per session, score from 1 to 5; persisted average |
| Tutor applications | Several courses/days, weekly hours, reason; pending and past review decisions visible |
| Administrator approval/rejection | Confirmed actions, full application details, same account ID/password after promotion |
| Account activation | Searchable account table; administrator accounts protected from deactivation |
| Course management | Unique codes/names, confirmed deletion, protection for courses still used by tutors or open work |
| Profile and password editing | Complete validation before mutation; optional confirmed password change; updated sidebar identity |
| Student and administrator alerts | Searchable notification tables derived from saved records |
| Reports | All session statuses, attendance rate, course demand sorted by requests, tutor workload and average ratings |
| Linked-list session storage | Custom singly linked list retained and tested, including ID search and head/tail deletion |

No existing user-facing feature was removed. The original text-only reports and alerts were replaced with searchable tables. The fixed-position login was replaced with a resizable layout. Unused old screen/helper classes were replaced by shared components.

## Added

- Automatic local saving/loading, previous-save backup, process locking, bounded file reads, restricted deserialization, and rollback on failed mutations/writes.
- Salted PBKDF2-HMAC-SHA256 password hashes instead of stored plain text passwords.
- Overview cards, live table search, numeric sorting, row tooltips, keyboard shortcuts, accessible form labels, and plain-text detail dialogs.
- Consistent navy/teal theme, one font family, custom Swing book icon and scrollbars, hover feedback, flexible layout managers, and 960 x 680 minimum window checks.
- `SwingWorker` for file access, password verification/hashing, and saves; `SwingUtilities.invokeLater` for startup.
- Runnable JAR, build/run/test scripts, public API documentation, README, presentation guide, and dependency-free regression checks.

## Design decisions for presentation

- The main added feature is persistence, because accounts and tutoring records should survive closing the program.
- The request queue is rebuilt from stored insertion order; viewing/searching/sorting it cannot consume requests.
- Completed, missed, and cancelled records are retained for history and reports.
- Attendance is recorded only after the hour ends. Use a completed demo session to demonstrate ratings immediately.
- Demo appointments are relative to the first launch date. Restarting does not move real saved appointments.
- Course names cannot contain commas because the retained tutor/application model uses comma-separated exact course names. This is validated at course creation.
- The local format is version 1; later schema changes should include an explicit migration. Original memory-only sessions have no saved file to import.
