import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class TaskDatabase {

    private static final String DB_URL = "jdbc:sqlite:" + findDatabaseFile();
    private Connection connection;

    private static String findDatabaseFile() {
        try {
            java.io.File location = new java.io.File(
                    TaskDatabase.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (location.isFile() && location.getName().endsWith(".jar")) {
                java.io.File srcFolder = new java.io.File(location.getParentFile(), "src");
                srcFolder.mkdirs();
                return new java.io.File(srcFolder, "task_scheduler.db").getAbsolutePath();
            }
        } catch (Exception e) {

        }
        return "task_scheduler.db";
    }

    public TaskDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            try (Statement st = connection.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS tasks ("
                        + "task_id INTEGER PRIMARY KEY, "     
                        + "add_order INTEGER NOT NULL, "
                        + "task_name TEXT NOT NULL, "
                        + "description TEXT, "
                        + "deadline TEXT NOT NULL, "          
                        + "priority TEXT NOT NULL, "
                        + "status TEXT NOT NULL)");
            }
        } catch (SQLException e) {
            System.err.println("Database error (tasks will not be saved): " + e.getMessage());
            connection = null;
        }
    }

    // ===== LOAD =====
    public ArrayList<Task> loadTasks() {
        ArrayList<Task> list = new ArrayList<>();
        if (connection == null) return list;

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM tasks ORDER BY add_order")) {
            while (rs.next()) {
                list.add(new Task(
                        rs.getInt("task_id"),
                        rs.getInt("add_order"),
                        rs.getString("task_name"),
                        rs.getString("description"),
                        rs.getString("deadline"),
                        rs.getString("priority"),
                        rs.getString("status")));
            }
        } catch (SQLException e) {
            System.err.println("Could not load tasks: " + e.getMessage());
        }
        return list;
    }

    // ===== ADD =====
    public void insertTask(Task t) {
        run("INSERT INTO tasks (task_id, add_order, task_name, description, deadline, priority, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                t.getTaskID(), t.getAddOrder(), t.getTaskName(), t.getDescription(),
                t.getDeadline(), t.getPriority(), t.getStatus());
    }

    // ===== EDIT / CHECKBOX =====
    public void updateTask(Task t) {
        run("UPDATE tasks SET task_name = ?, description = ?, deadline = ?, priority = ?, status = ? "
                + "WHERE task_id = ?",
                t.getTaskName(), t.getDescription(), t.getDeadline(), t.getPriority(),
                t.getStatus(), t.getTaskID());
    }

    // ===== DELETE =====
    public void deleteTask(int taskID) {
        run("DELETE FROM tasks WHERE task_id = ?", taskID);
    }

    // Runs one INSERT/UPDATE/DELETE with the given values.
    private void run(String sql, Object... values) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Could not close database: " + e.getMessage());
        }
    }
}
