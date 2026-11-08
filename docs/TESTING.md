# Testing

The project includes standalone Java test programs and does not require JUnit or another testing library.

## Run on Windows

Open `Test.cmd` from the repository root, or run:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1
```

The script builds the application, compiles source and test files into a separate test output folder, then runs `ServiceTests` and `GuiTests`.

## Run manually on macOS or Linux

From the repository root:

```sh
mkdir -p build/test-classes
find src test -name '*.java' > build/test-sources.txt
javac --release 17 -encoding UTF-8 -d build/test-classes @build/test-sources.txt
java -cp build/test-classes aralbuddy.ServiceTests
java -cp build/test-classes aralbuddy.GuiTests
```

The GUI test requires a graphical desktop. Both test programs use separate temporary data folders and do not edit the default application workspace.

## Current coverage

| Test program | Checks | Areas covered |
| --- | --- | --- |
| `ServiceTests` | 61 | Registration, login, permissions, exact course matching, booking overlaps, rescheduling, FIFO requests, tutor promotion, profiles, attendance, ratings, courses, persistence, backups, file failures, and custom data structures |
| `GuiTests` | 29 | Role-specific navigation, tutor search, report tabs, live literal filtering, numeric sorting, sorted-row identity, and usable layouts at 960 x 680 |

File checks include damaged snapshots, unsupported versions, rejected serialized classes, missing main data with an existing backup, process locking, failed mutations, and disk-write rollback.

## Verification environment

The revised application was built and tested on Windows with Eclipse Adoptium JDK 25.0.4, targeting Java 17 with `--release 17`. Dependency inspection reported only the standard `java.base` and `java.desktop` modules.

The runnable JAR was launched twice against a separate saved workspace without application errors on stderr. Login, overview, administrator reports, and the minimum-size tutor search were also inspected through rendered Swing components. Their images are in [`assets/screenshots`](../assets/screenshots/).

The GitHub organization update moves documentation and scripts while keeping the Java application source unchanged. The relocated scripts were run again from a separate copy: all 61 service checks and 29 Swing checks passed.
