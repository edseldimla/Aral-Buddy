# Usage and Data Recovery

## Session rules

- Every tutoring session lasts one hour.
- Enter dates as `YYYY-MM-DD` and start times as 24-hour `HH:mm`, such as `14:00`.
- New requests and rescheduled requests must be in the future.
- The entire hour must fit the tutor's approved weekly availability.
- A confirmed session cannot overlap another confirmed session involving either participant. Adjacent hours are allowed.
- Only the requesting student can reschedule an eligible request or cancel their open session.
- The assigned tutor can record completed or missed attendance after the scheduled hour ends.
- Only the requesting student can rate a completed session, and each session can be rated once.
- Cancelled sessions stay in history for reference and reporting.

The computer's local date and time are used. For a quick demonstration, keep the default next-Monday search date and use an available weekday hour.

## Tables and keyboard shortcuts

Click a table heading to sort its rows. Search filters visible records by text across the table's columns. Wide tables can be scrolled horizontally, and hovering over a cell shows its full text.

| Shortcut | Action |
| --- | --- |
| `Ctrl+1` to `Ctrl+9` | Open an available sidebar page |
| `Ctrl+F` | Focus the current table's search field |
| `Ctrl+R` | Refresh the current page |
| `Ctrl+L` | Log out |
| `F1` | Open help and show the exact saved-data location |
| `Enter` | Submit the current form |
| `Escape` | Close a form when it is not saving |

## Saved workspace

On Windows, the default data folder is `%LOCALAPPDATA%\AralBuddy`. On macOS and Linux, it is `~/.aralbuddy`.

| File | Purpose |
| --- | --- |
| `aralbuddy.dat` | Current successful saved workspace |
| `aralbuddy.dat.bak` | Previous successful workspace snapshot |
| `aralbuddy.lock` | Supports the operating-system lock for the data folder |

Only one application instance may use a data folder at a time. The lock file may remain after closing; its presence alone does not mean the operating-system lock is still held.

To use a separate workspace, supply a data-folder property:

```sh
java -Daralbuddy.dataDir=demo-data -jar dist/AralBuddy.jar
```

Demo records are seeded only in a new data folder. Saved appointments retain their original dates on later launches.

## Recover from a damaged main file

1. Close every Aral Buddy window using the affected data folder.
2. Open that data folder and keep a separate copy of the damaged `aralbuddy.dat`.
3. If `aralbuddy.dat.bak` exists, copy it to `aralbuddy.dat` in the same folder.
4. Reopen the application and check the recovered records.

The backup represents the previous save, so the latest successful edit may not be included. A damaged main file, or a missing main file with an existing backup, does not trigger an automatic demo reset.

## Scope

This version runs on one computer. It has no shared online database, cross-computer synchronization, or outgoing email notifications. Passwords are hashed, while the full local workspace file is not encrypted. The demo accounts are intended for academic demonstrations.
