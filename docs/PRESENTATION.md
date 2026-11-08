# Teacher demonstration

## Before presenting

1. Check `java -version` and run `Run.cmd` on the actual presentation computer. Java 17 or newer is required.
2. Run `Test.cmd` with a JDK installed. It uses temporary data, so your presentation accounts stay untouched.
3. For a fresh demonstration, use a new folder: `java -Daralbuddy.dataDir=presentation-data -jar dist/AralBuddy.jar`. Reuse that folder when demonstrating restart persistence.
4. Keep this project extracted. Do not run it directly inside a ZIP viewer.

## A short demonstration

1. Log in as **Juan**. Show the overview, current sessions, and searchable history.
2. Open **Find a tutor**. Keep the next-Monday default, choose **Programming 1**, use **10:00**, and search. Select Maria and send a request with a topic and venue.
3. Log out and log in as **Maria**. Open **Tutor requests**. Explain that the queue always answers the oldest pending request. Demo requests may be ahead of your new request. Accept them in order.
4. Return to Juan and show the saved status in **My sessions**. In **Session history**, show the existing completed/missed examples and rating details.
5. Log in as the **administrator**. Review Liza's pending application, inspect student accounts and courses, then show all three report tabs.
6. Log out and log in as **Liza**. Her approved tutor role is now available under the same email/password.
7. Close and reopen the program with the same data folder. Show that the new booking and approval remain. This is the main new feature.

All student demo passwords are `password123`. Admin is `admin@aralbuddy.edu` / `admin1234`.

## Explain the code in this order

1. **Main** starts Swing on the event thread.
2. **Model** contains accounts and tutoring records. Explain encapsulation, User abstraction, Admin/Student/Tutor inheritance, and role polymorphism.
3. **AppController** receives screen actions and uses `SwingWorker` for slow operations.
4. **AppService** checks permissions and business rules. `commit` either saves the whole change or restores the previous state.
5. **FileStore** handles local snapshots, backup, and the process lock.
6. **View** contains layout managers, shared tables, and dialogs. The GUI does not write files directly.
7. **SinglyLinkedList** stores session nodes; **SessionQueue** uses front/rear pointers for FIFO requests. Their behavior is covered by the tests.

## Questions you may be asked

- **Why Swing?** It is part of Java, meets the project rules, and supports a complete desktop GUI without extra libraries.
- **Why persistence?** The original application lost new accounts, approvals, sessions, and ratings every time it closed.
- **How are overlapping schedules handled?** A new start must be before the other hour's end, and the other start must be before the new end. Adjacent hours are allowed.
- **Why a worker?** Disk writes and password hashing should not block the Swing event thread.
- **What if saving fails?** The form stays open, a friendly error is shown, and the prior in-memory state is restored.
- **Can two people use separate computers together?** No. This version stores data on one computer. A networked system would need a shared service and a different storage design.
- **Is it production security?** It improves local password storage and validation, but it is still a classroom desktop application with known demo accounts and a local, unencrypted data file.
