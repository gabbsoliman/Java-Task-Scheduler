# Task Scheduler: Insertion Sort Algorithm

A Java Swing desktop application whose main purpose is to **demonstrate and implement the Insertion Sort algorithm**. Tasks are added to a scheduler and can then be sorted by deadline, priority, or task ID, with every sort performed by a hand-written insertion sort (no `Collections.sort` or `Comparator`).

## Features

- Add, edit, search, and delete tasks (name, description, deadline, priority)
- Search by task name (partial match) or by ID (e.g. `#42`); the matching row is selected in the table
- Mark tasks as completed with a checkbox and delete all completed tasks at once (with a confirmation prompt)
- **Clear** button resets the input form and cancels an edit in progress
- Rows are color-coded by priority (red = High, yellow = Medium, green = Low); completed tasks are greyed out and overdue deadlines are shown in red
- Live counter at the bottom showing total, pending, and completed tasks
- Up to 90 tasks at a time (random unique Task IDs from #10 to #99)
- Sort the task list with Insertion Sort by:
  - **Deadline**: earliest to latest
  - **Priority**: High → Medium → Low
  - **Task ID**: lowest to highest (#10 → #99)
- Stable sorting: tasks that tie are kept in the order they were added
- Deadline input uses a `##/##/##` mask and is validated in `MM/DD/YY` format, including leap years
- Tasks are saved in a local SQLite database (`task_scheduler.db`), so they persist between runs
- The table is re-sorted with the selected option after every add, edit, delete, and on startup (default: Deadline)

## The Insertion Sort Algorithm

Insertion Sort builds the sorted list one item at a time. For each task, starting at the second one, it is compared with the tasks before it. Larger tasks are shifted one position to the right, and the task is inserted into the gap.

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

| Case    | Time complexity | Notes                          |
|---------|-----------------|--------------------------------|
| Best    | O(n)            | List is already sorted         |
| Average | O(n²)           |                                |
| Worst   | O(n²)           | List is in reverse order       |
| Space   | O(1)            | Sorts in place                 |

Insertion Sort is **stable**, and it works well for small lists like this one (task IDs are limited to 10–99).

## Database

Tasks are stored in a local **SQLite** database through JDBC ([TaskDatabase.java](src/TaskDatabase.java)). The `tasks` table is created on first launch. Where `task_scheduler.db` is created depends on how you run the app:

- **From the jar** (`Java Task Scheduler.jar`): in a `src` folder next to the jar (created if missing).
- **From compiled classes** (run inside `src`): in the current folder.

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
- **Add / Edit / Delete / Checkbox**: each action is written to the database immediately (`INSERT`, `UPDATE`, `DELETE` with prepared statements).
- **Sorting**: Insertion Sort runs on the in-memory list only; the database is not rewritten. Tasks are always reloaded by `add_order` (the stable tie-breaker), then sorted again by the selected option (Deadline by default) at startup.
- **Connection**: opened when the app starts and closed when the window is closed.
- **Failure**: if the database cannot be opened, the app still runs but tasks will not be saved (an error is printed to the console).
- **Reset**: delete `task_scheduler.db` to clear all saved tasks.

## Project Structure

Source files are in the [src/](src/) folder.

| File                                         | Description                                                         |
|----------------------------------------------|---------------------------------------------------------------------|
| [Main.java](src/Main.java)                       | Entry point; sets the Nimbus look and feel and launches the GUI     |
| [TaskSchedulerGUI.java](src/TaskSchedulerGUI.java) | Swing user interface (table, input form, search, sort dropdown, counter) |
| [TaskManager.java](src/TaskManager.java)         | Task logic: add, edit, delete, search, date validation, sort calls; keeps the database in sync |
| [InsertionSort.java](src/InsertionSort.java)     | **The Insertion Sort implementations**                              |
| [Task.java](src/Task.java)                       | Task model (ID, name, description, deadline, priority, status)      |
| [TaskDatabase.java](src/TaskDatabase.java)       | **SQLite persistence through JDBC** (load, insert, update, delete)  |
| `src/lib/sqlite-jdbc-3.36.0.3.jar`           | SQLite JDBC driver                                                  |
| `src/task_scheduler.db`                      | SQLite database file (created automatically)                        |
| `Java Task Scheduler.jar`                    | Ready-to-run executable jar (SQLite driver bundled inside)          |

## Requirements

- Java Development Kit (JDK) 8 or later (a JRE is enough to run the jar)
- To compile from source, the SQLite JDBC jar in `src/lib` must be on the classpath, as shown below. No separate database install is needed.

## How to Run

**Option 1: run the jar** (no compiling needed; double-click it or use):
```bash
java -jar "Java Task Scheduler.jar"
```

**Option 2: compile from source.** From the project folder, go into `src` first:

**Windows**
```bash
cd src
javac -cp ".;lib/sqlite-jdbc-3.36.0.3.jar" *.java
java -cp ".;lib/sqlite-jdbc-3.36.0.3.jar" Main
```

**macOS / Linux**
```bash
cd src
javac -cp ".:lib/sqlite-jdbc-3.36.0.3.jar" *.java
java -cp ".:lib/sqlite-jdbc-3.36.0.3.jar" Main
```

## How to Use

1. Enter a task name, description, deadline (`MM/DD/YY`, e.g. `12/25/26`), and priority, then click **Add**.
2. Add several tasks in no particular order.
3. Use the **Sort By** dropdown at the bottom to choose *Deadline*, *Priority*, or *INSERTION SORT ALGORITHM* (sort by Task ID). The table is re-sorted using Insertion Sort.
4. Use **Search** to find a task by name or ID (e.g. `#42`).
5. To change a task, select its row and click **Edit**; the form fills in and **Add** becomes **Save**. Click **Clear** to cancel.
6. Tick the checkbox to mark a task completed, then click **Delete** to remove all checked tasks (you will be asked to confirm).

## Author

GEMS #3
