# Task Scheduler: Insertion Sort Algorithm

A Java Swing desktop application whose main purpose is to **demonstrate and implement the Insertion Sort algorithm**. Tasks are added to a scheduler and can then be sorted by status, deadline, priority, or task ID. Every sort uses a hand-written insertion sort (no `Collections.sort` or `Comparator`).

## Features

- Add, edit, search, and delete tasks (name, description, deadline, priority)
- Edit a task with the **Edit** button or by double-clicking its row
- Search by task name (partial match) or by ID (e.g. `#42`); the matching row is selected in the table
- Mark tasks as completed with a checkbox, or use the **select-all checkbox** in the table header to mark or unmark every task at once
- **Delete Checked** removes all completed tasks at once (with a confirmation prompt)
- **Clear** resets the input form; while editing it becomes **Cancel**
- Deadline input:
  - typed with a `##/##/##` mask and validated in `MM/DD/YY` format, including leap years
  - or picked from a **calendar popup** (with *Today* / *Tomorrow* shortcuts)
  - **past dates are not allowed**; a live hint under the field shows the full date and how many days are left
- Priority and status shown as colored badges (red = High, yellow = Medium, green = Low); completed tasks are greyed out and struck through; deadlines are tagged **Overdue** (red), **Today**, or **Tomorrow**
- Summary cards at the top: **Total**, **Pending**, **Completed**, and **Overdue** tasks
- Up to 90 tasks at a time (random unique Task IDs from #10 to #99)
- Sort the task list with Insertion Sort by:
  - **Status**: Pending → Completed (default)
  - **Deadline**: earliest to latest
  - **Priority**: High → Medium → Low
  - **INSERTION SORT ALGORITHM** (Task ID): lowest to highest (#10 → #99)
- Stable sorting: tasks that tie are kept in the order they were added
- Tasks are saved in a local SQLite database (`tasks.db`), so they persist between runs
- The table is re-sorted with the selected option after every add, edit, checkbox change, and delete, and on startup
- The input form scrolls on small screens, so nothing gets cut off

## The Insertion Sort Algorithm

Insertion Sort builds the sorted list one item at a time. Starting from the second task, each task is compared with the tasks before it. Larger tasks are shifted one position to the right, and the task is inserted into the gap.

From [InsertionSort.java](src/InsertionSort.java):

```java
public static void sortByTaskID(ArrayList<Task> list) {
    for (int i = 1; i < list.size(); i++) {
        Task currentTask = list.get(i);
        int j = i - 1;

        while (j >= 0 && list.get(j).getTaskID() > currentTask.getTaskID()) {
            list.set(j + 1, list.get(j));   // shift right
            j--;
        }

        list.set(j + 1, currentTask);       // insert in place
    }
}
```

The other sorts use the same loop with a different comparison:

| Sort option              | Order                  | Tie-breaker                        |
|--------------------------|------------------------|------------------------------------|
| Status                   | Pending → Completed    | Earliest deadline, then add order  |
| Deadline                 | Earliest → latest      | Add order                          |
| Priority                 | High → Medium → Low    | Add order                          |
| INSERTION SORT ALGORITHM | Task ID #10 → #99      | (IDs are unique)                   |

| Case    | Time complexity | Notes                          |
|---------|-----------------|--------------------------------|
| Best    | O(n)            | List is already sorted         |
| Average | O(n²)           |                                |
| Worst   | O(n²)           | List is in reverse order       |
| Space   | O(1)            | Sorts in place                 |

Insertion Sort is **stable**, and it works well for small lists like this one (task IDs are limited to 10–99).

## Database

Tasks are stored in a local **SQLite** database through JDBC ([TaskDatabase.java](src/TaskDatabase.java)). The `tasks` table is created on first launch. The database file is `tasks.db`, and where it is created depends on how you run the app:

- **From the jar** (`Java Task Scheduler.jar`): in a `src` folder next to the jar (created if missing).
- **From source in the project folder** (e.g. VS Code): `src/tasks.db`.
- **From source inside `src`**: `tasks.db` in that folder.

In this project all three are the same file, `src/tasks.db`, so the jar and the source code share the same tasks.

| Column        | Type                  | Description                                              |
|---------------|-----------------------|----------------------------------------------------------|
| `task_id`     | INTEGER, PRIMARY KEY  | Task ID (10–99)                                          |
| `add_order`   | INTEGER, NOT NULL     | Order the task was added; used to keep sorting stable    |
| `task_name`   | TEXT, NOT NULL        | Task name                                                |
| `description` | TEXT                  | Task description                                         |
| `deadline`    | TEXT, NOT NULL        | Deadline in `MM/DD/YY` format                            |
| `priority`    | TEXT, NOT NULL        | `High`, `Medium`, or `Low`                               |
| `status`      | TEXT, NOT NULL        | `Pending` or `Completed`                                 |

How the database is used:

- **Load**: on startup, all tasks are read in `add_order` order.
- **Add / Edit / Delete / Checkbox / Select all**: each action is written to the database immediately (`INSERT`, `UPDATE`, `DELETE` with prepared statements).
- **Sorting**: Insertion Sort runs on the in-memory list only; the database is not rewritten. Tasks are always reloaded by `add_order` (the stable tie-breaker), then sorted again by the selected option at startup.
- **Driver**: the SQLite driver is loaded automatically from `src/lib`, so no classpath setup is needed. The jar has the driver built in.
- **Connection**: opened when the app starts and closed when the app exits.
- **Failure**: if the database cannot be opened, the app still runs, shows a **Database Error** message, and tasks are not saved.
- **Viewing**: `tasks.db` is a standard SQLite file and can be opened in any SQLite viewer (e.g. the VS Code *Database Client* extension).
- **Reset**: delete `tasks.db` to clear all saved tasks; an empty one is created on the next launch.

## Project Structure

Source files are in the [src/](src/) folder.

| File                                               | Description                                                         |
|----------------------------------------------------|---------------------------------------------------------------------|
| [Main.java](src/Main.java)                         | Entry point; applies the system look and feel, opens the database, and launches the GUI |
| [TaskSchedulerGUI.java](src/TaskSchedulerGUI.java) | Swing user interface (summary cards, input form, table, search, sort, select-all) |
| [TaskManager.java](src/TaskManager.java)           | Task logic: add, edit, delete, search, select all, date validation, sort calls; keeps the database in sync |
| [InsertionSort.java](src/InsertionSort.java)       | **The Insertion Sort implementations**                              |
| [Task.java](src/Task.java)                         | Task model (ID, name, description, deadline, priority, status)      |
| [TaskDatabase.java](src/TaskDatabase.java)         | **SQLite persistence through JDBC** (load, insert, update, delete)  |
| [DatePickerPopup.java](src/DatePickerPopup.java)   | Calendar popup for picking a deadline (past days disabled)          |
| [Theme.java](src/Theme.java)                       | Shared colors, fonts, and custom-drawn components (rounded cards, buttons, logo) |
| `src/lib/sqlite-jdbc-3.36.0.3.jar`                 | SQLite JDBC driver                                                  |
| `src/tasks.db`                            | SQLite database file (created automatically)                        |
| `Java Task Scheduler.jar`                          | Ready-to-run executable jar (SQLite driver bundled inside)          |
| [../docs/](../docs/)                               | System documentation (see below)                                    |

## Requirements

- **Java 17 or later**. A JDK is needed to compile; a JRE is enough to run the jar.
- No separate database install is needed.

## How to Run

**Option 1: run the jar** (no compiling needed). Double-click `Java Task Scheduler.jar`, or use:
```bash
java -jar "Java Task Scheduler.jar"
```

**Option 2: compile from source.** From the project folder (the SQLite driver in `src/lib` is found automatically):

**Windows**
```bash
javac -d out src\*.java
java -cp out Main
```

**macOS / Linux**
```bash
javac -d out src/*.java
java -cp out Main
```

## How to Build the Jar

After changing the code, rebuild the jar from the project folder (Windows). If `jar` is not recognized, use its full path, e.g. `"C:\Program Files\Java\jdk-25\bin\jar.exe"`.

```bash
:: 1. compile for Java 17+
javac --release 17 -encoding UTF-8 -d build\classes src\*.java

:: 2. add the SQLite driver
cd build\classes
jar xf ..\..\src\lib\sqlite-jdbc-3.36.0.3.jar
del META-INF\MANIFEST.MF
cd ..\..

:: 3. package and clean up
jar --create --file "Java Task Scheduler.jar" --main-class Main -C build\classes .
rmdir /s /q build
```

Saved tasks are not affected; they live in `src\tasks.db`, not in the jar.

## How to Use

1. Enter a task name, description, deadline (`MM/DD/YY`, e.g. `12/25/26`, or pick it with the calendar button), and priority, then click **Add Task**.
2. Add several tasks in no particular order.
3. Use the **Sort by** dropdown (top right of the table) to choose *Status*, *Deadline*, *Priority*, or *INSERTION SORT ALGORITHM* (sort by Task ID). The table is re-sorted using Insertion Sort.
4. Use **Search** to find a task by name or ID (e.g. `#42`).
5. To change a task, double-click its row (or select it and click **Edit**). The form fills in and **Add Task** becomes **Save Changes**. Click **Cancel** to stop editing.
6. Tick a task's checkbox to mark it completed, or tick the checkbox in the table header to mark **all** tasks (tick it again to unmark all).
7. Click **Delete Checked** to remove all completed tasks (you will be asked to confirm).

## Documentation

- [SYSTEM_INFRASTRUCTURE.md](../docs/SYSTEM_INFRASTRUCTURE.md): how the system is built, with code explanations
- [SYSTEM_BLUEPRINT.md](../docs/SYSTEM_BLUEPRINT.md): step-by-step guide to programming the system yourself

## Author

GEMS #3
