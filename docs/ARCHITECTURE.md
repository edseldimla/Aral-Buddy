# Architecture and Course Integration

Aral Buddy is a Java Swing desktop application with separate model, view, controller, service, and utility packages. It uses the standard Java library only.

## Package responsibilities

| Package | Responsibility | Starting point |
| --- | --- | --- |
| `aralbuddy` | Starts the application on the Swing event thread | [Main.java](../src/aralbuddy/Main.java) |
| `model` | Holds account and tutoring records, the saved snapshot, and custom data structures | [User.java](../src/aralbuddy/model/User.java), [Session.java](../src/aralbuddy/model/Session.java) |
| `view` | Draws screens and gathers user input | [MainFrame.java](../src/aralbuddy/view/MainFrame.java), [Page.java](../src/aralbuddy/view/Page.java) |
| `controller` | Connects UI actions to background operations and refreshes screens | [AppController.java](../src/aralbuddy/controller/AppController.java) |
| `service` | Enforces business rules and saves or loads records | [AppService.java](../src/aralbuddy/service/AppService.java), [FileStore.java](../src/aralbuddy/service/FileStore.java) |
| `util` | Shares input validation and password-hashing code | [Validation.java](../src/aralbuddy/util/Validation.java), [PasswordUtil.java](../src/aralbuddy/util/PasswordUtil.java) |

## Information Management

The application organizes related information into clear record types.

| Record | Purpose | Relationships |
| --- | --- | --- |
| User | Identity, login credentials, and activation state | Parent account type for administrators and students |
| Student / Tutor | Academic profile and approved tutoring details | A tutor retains the same account ID when promoted from student |
| Subject | Course code and name | Course names are referenced by tutoring offerings, applications, and sessions |
| TutorApplication | Courses, weekly availability, reason, and review status | References the applicant's student ID |
| Session | Participants, course, topic, date, time, venue, attendance, and rating | References one student ID and one tutor ID |
| AppData | Versioned snapshot of the complete local workspace | Contains accounts, courses, applications, and sessions |

Unique IDs link related records. The service checks duplicate student numbers and emails, protects courses that are still referenced, and validates inputs before saving. Reports derive totals and attendance rates from stored session records.

Persistence is implemented through a local snapshot, a previous-save backup, restricted deserialization, and an operating-system process lock. A failed operation or file save restores the previous in-memory state. This is application-level consistency handling; the project does not claim to implement a relational database engine or SQL transactions.

## Data Structures and Algorithms

### Custom singly linked list

[`SinglyLinkedList`](../src/aralbuddy/model/SinglyLinkedList.java) connects session nodes through a `next` reference and maintains `head`, `tail`, and `size`. The service rebuilds this list from saved session order when loading or restoring a snapshot.

| Operation | Time cost | Reason |
| --- | --- | --- |
| Add a session | O(n) | The implementation first searches for a duplicate ID; linking at the tail itself is O(1) |
| Find a session by ID | O(n) | Visits nodes until a matching ID is found |
| Delete a session by ID | O(n) | Finds the node and repairs the links, including head or tail |
| Display all sessions | O(n) | Traverses every node to produce a list copy |
| Read the stored size | O(1) | Returns the maintained count |

### Custom FIFO queue

[`SessionQueue`](../src/aralbuddy/model/SessionQueue.java) maintains `front` and `rear` references. It provides first-in, first-out request handling.

| Operation | Time cost | Reason |
| --- | --- | --- |
| Enqueue | O(1) | Appends directly through the rear pointer |
| Dequeue | O(1) | Removes directly through the front pointer |
| Check whether empty | O(1) | Checks the front pointer |
| Linear search by session ID | O(n) | Visits queued nodes in order |

`AppService.requestQueue` builds an independent queue from the saved pending requests. Reading or sorting the displayed table does not consume stored requests. Responding always processes the oldest pending request. A resubmitted time is moved to the back of the request order.

Table sorting uses Swing's built-in `TableRowSorter`. It is separate from the custom linked list and queue. Record selection converts the displayed row back to the underlying model row before applying an action.

## Object-Oriented Programming

The account hierarchy is:

```text
User (abstract)
|-- Admin
`-- Student
    `-- Tutor
```

| Principle | Implementation example |
| --- | --- |
| Encapsulation | Account and session fields are private and accessed through named methods |
| Inheritance | A `Tutor` inherits the student profile and account identity |
| Abstraction | `User` defines shared account behavior; `Page` defines the shared screen structure |
| Polymorphism | Account subclasses implement role behavior, and each page implements `refresh()` |
| Composition | The controller coordinates a frame and service; screens contain shared tables and dialogs |
| Separation of responsibilities | Views handle presentation, services enforce rules, and `FileStore` owns file operations |

The model exposes record methods, but application mutations are routed through the service. `AppController` wraps these changes in `AppService.commit` so a successful UI action includes a successful save.

## Threading and program flow

`Main` creates Swing components with `SwingUtilities.invokeLater`. File loading, saving, password hashing, and password verification run through `SwingWorker`. Worker completion updates the interface on the event thread.

```text
User action
    -> View gathers input
    -> Controller starts a worker
    -> Service checks permissions and business rules
    -> FileStore writes the snapshot
    -> Controller refreshes the view or displays an error
```

No outside GUI framework, database driver, or Java dependency is required.
