import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

public class TaskDatabase {

    private static final String FILE_NAME = "tasks.db";

    private final Path file = findDataFile();
    private Connection connection;

    private static Path findDataFile() {
        try {
            File location = new File(
                    TaskDatabase.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (location.isFile() && location.getName().endsWith(".jar")) {
                File srcFolder = new File(location.getParentFile(), "src");
                srcFolder.mkdirs();
                return new File(srcFolder, FILE_NAME).toPath().toAbsolutePath();
            }
        }

        catch (Exception e) {
            // fall through to the working-directory lookup below
        }

        File srcFolder = new File("src");

        if (srcFolder.isDirectory()) {
            return new File(srcFolder, FILE_NAME).toPath().toAbsolutePath();
        }

        return new File(FILE_NAME).toPath().toAbsolutePath();
    }

    public TaskDatabase() {
        try {
            Driver driver = loadSqliteDriver();
            connection = driver.connect("jdbc:sqlite:" + file, new Properties());
            createTable();
        }
        catch (Exception e) {
            System.err.println("Database error (tasks will not be saved): " + e.getMessage());
            close();
            connection = null;
        }
    }

    public boolean isConnected() {
        return connection != null;
    }

    // ===== DRIVER =====
    private Driver loadSqliteDriver() throws Exception {
        try {
            return (Driver) Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
        }
        catch (ClassNotFoundException notOnClasspath) {
            // look for the JAR below
        }

        File jar = findSqliteJar();
        if (jar == null) {
            throw new IllegalStateException("sqlite-jdbc JAR not found. Put it in " + file.getParent() + File.separator + "lib");
        }
        URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, TaskDatabase.class.getClassLoader());
        return (Driver) Class.forName("org.sqlite.JDBC", true, loader).getDeclaredConstructor().newInstance();
    }

    private File findSqliteJar() {
        File[] folders = {
                new File(file.getParent().toFile(), "lib"),                 
                new File(file.getParent().toFile().getParentFile(), "lib"), 
                new File("lib")
        };
        for (File folder : folders) {
            File[] jars = folder.listFiles((dir, name) -> name.startsWith("sqlite-jdbc") && name.endsWith(".jar"));
            if (jars != null && jars.length > 0) return jars[0];
        }
        return null;
    }

    // ===== TABLE SETUP =====
    private void createTable() throws SQLException {
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
        }
        catch (SQLException e) {
            System.err.println("Could not load tasks: " + e.getMessage());
        }
        return list;
    }

    // ===== ADD =====
    public void insertTask(Task t) {
        run("INSERT OR REPLACE INTO tasks (task_id, add_order, task_name, description, deadline, priority, status) "
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

    private void run(String sql, Object... values) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setObject(i + 1, values[i]);
            }
            ps.executeUpdate();
        }
        catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        }
        catch (SQLException e) {
            System.err.println("Could not close database: " + e.getMessage());
        }
    }
}
