
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class TaskManager {

    private ArrayList<Task> tasks = new ArrayList<>();
    private int nextAddOrder = 1;  
    private Random random = new Random();
    private TaskDatabase database = new TaskDatabase();   

   
    public TaskManager() {
        tasks = database.loadTasks();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getAddOrder() >= nextAddOrder) {
                nextAddOrder = tasks.get(i).getAddOrder() + 1;
            }
        }
    }

    public void close() {
        database.close();
    }

 
    private static final int MIN_ID = 10;
    private static final int MAX_ID = 99;

    // ===== ADD =====
    
    public Task addTask(String name, String description, String deadline, String priority) {
        if (tasks.size() >= MAX_ID - MIN_ID + 1) {
            return null;
        }

        int newID = generateRandomID();
        Task newTask = new Task(newID, nextAddOrder, name, description, deadline, priority);
        nextAddOrder++;
        tasks.add(newTask);     
        database.insertTask(newTask);
        return newTask;
    }

    private int generateRandomID() {
        int id = MIN_ID + random.nextInt(MAX_ID - MIN_ID + 1);
        while (isIDUsed(id)) {
            id = MIN_ID + random.nextInt(MAX_ID - MIN_ID + 1);
        }
        return id;
    }

    private boolean isIDUsed(int id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getTaskID() == id) {
                return true;
            }
        }
        return false;
    }

    // ===== EDIT =====
    public void editTask(Task task, String name, String description, String deadline, String priority) {
        task.setTaskName(name);
        task.setDescription(description);
        task.setDeadline(deadline);
        task.setPriority(priority);
        database.updateTask(task);
    }

    // ===== CHECKBOX =====
    public void setTaskDone(Task task, boolean done) {
        if (done) {
            task.setStatus("Completed");
        } else {
            task.setStatus("Pending");
        }
        database.updateTask(task);
    }

    // ---------- DELETE ----------
    public int deleteCheckedTasks() {
        int deletedCount = 0;
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (tasks.get(i).getStatus().equals("Completed")) {
                database.deleteTask(tasks.get(i).getTaskID());
                tasks.remove(i);
                deletedCount++;
            }
        }
        return deletedCount;
    }

    // ===== GET =====
    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public Task searchTask(String query) {
        if (query == null) {
            return null;
        }
        String searchText = query.trim();
        if (searchText.isEmpty()) {
            return null;
        }
        if (searchText.startsWith("#")) {
            searchText = searchText.substring(1);
        }
        if (searchText.isEmpty()) {
            return null;
        }
        String nameQuery = searchText.toLowerCase(Locale.ROOT);
        Task nameMatch = null;

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (String.valueOf(task.getTaskID()).equals(searchText)) {
                return task;
            }
            if (nameMatch == null && task.getTaskName().toLowerCase(Locale.ROOT).indexOf(nameQuery) >= 0) {
                nameMatch = task;
            }
        }
        return nameMatch;
    }

    public int countCompletedTasks() {
        int completedCount = 0;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getStatus().equals("Completed")) {
                completedCount++;
            }
        }
        return completedCount;
    }

    // ===== SORT =====
    public void sortTasks(String sortOption) {

        if (sortOption.equals("Deadline")) {
            InsertionSort.sortByDeadline(tasks);
        } 
        else if (sortOption.equals("Priority")) {
            InsertionSort.sortByPriority(tasks);
        } 
        else {
            InsertionSort.sortByTaskID(tasks);      
        }
    }

    // ===== DATE VALIDATION =====
    public String formatDeadline(String text) {
        String[] parts = text.trim().split("/");
        if (parts.length != 3) return null;

        int month, day, year;
        try {
            month = Integer.parseInt(parts[0].trim());
            day   = Integer.parseInt(parts[1].trim());
            year  = Integer.parseInt(parts[2].trim());
            
        } catch (NumberFormatException e) {
            return null;
        }

        if (year < 0 || year > 99) return null;
        if (month < 1 || month > 12) return null;
        if (day < 1 || day > daysInMonth(month, year)) return null;

        return twoDigits(month) + "/" + twoDigits(day) + "/" + twoDigits(year);
    }

    private int daysInMonth(int month, int twoDigitYear) {
        int[] days = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        int fullYear = 2000 + twoDigitYear;
        boolean leap = (fullYear % 4 == 0 && fullYear % 100 != 0) || (fullYear % 400 == 0);
        if (month == 2 && leap) return 29;
        return days[month - 1];
    }

    private String twoDigits(int n) {
        if (n < 10) return "0" + n;
        return "" + n;
    }
}
