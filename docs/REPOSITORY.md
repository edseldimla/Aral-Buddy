# GitHub Repository Details

## Suggested repository name

```text
aralbuddy
```

## Project title

```text
Aral Buddy: A Peer Tutoring Matching and Scheduling System
```

## About / short description

Copy this into the repository's **Description** or **About** field:

```text
Java Swing peer tutoring and scheduling system. Our integrated final project for Information Management, DSA, and OOP in the first semester of second-year BSIT, with tutor matching, session management, custom data structures, and local persistence.
```

## Suggested topics

```text
java
java-swing
desktop-application
peer-tutoring
scheduling
student-project
final-project
bsit
information-management
data-structures
algorithms
oop
mvc
linked-list
fifo-queue
```

## Longer project description

Aral Buddy is our group's integrated final project for Information Management, Data Structures and Algorithms, and Object-Oriented Programming during the first semester of our second year in the Bachelor of Science in Information Technology program. It is a Java Swing desktop application that helps students find peer tutors, request and manage one-hour study sessions, and maintain a record of their learning activities. Tutors can process requests through a custom FIFO queue, while administrators review tutor applications, manage accounts and courses, and view activity reports. The project brings together organized information handling, custom linked data structures, object-oriented design, and local file persistence in one practical system.

## Upload through the GitHub website

1. Create a repository named `aralbuddy`, or use your group's preferred name.
2. Paste the short About description above. This project already includes a README and `.gitignore`, so use those files when creating the repository.
3. Upload the **contents** of the `AralBuddy` folder from the GitHub-ready ZIP. `README.md`, `src/`, `test/`, `docs/`, `assets/`, and `scripts/` should appear at the repository root.
4. Include `.gitignore`, `.gitattributes`, and `.editorconfig`. Files starting with a dot may need to be selected separately in a file picker.
5. Keep generated `build/`, `dist/`, and saved `.dat` files out of the upload. The clean ZIP does not include them.
6. Add the suggested topics through the repository's About settings.

Upload the extracted project contents rather than uploading only the ZIP file. This lets GitHub display the README, source code, and screenshots directly.

## Upload with Git

From the project root, after creating the repository on GitHub:

```sh
git init
git add .
git status
git commit -m "Add Aral Buddy integrated BSIT final project"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/aralbuddy.git
git push -u origin main
```

Replace `YOUR-USERNAME` with the account or organization that owns the repository. No remote is configured and no files are published by this preparation package.
