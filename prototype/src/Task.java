public class Task {

    private int taskID;       
    private int addOrder;     
    private String taskName;
    private String description;
    private String deadline;   
    private String priority;   
    private String status;     

    public Task(int taskID, int addOrder, String taskName, String description, String deadline, String priority) {
        this.taskID = taskID;
        this.addOrder = addOrder;
        this.taskName = taskName;
        this.description = description;
        this.deadline = deadline;
        this.priority = priority;
        this.status = "Pending";
    }

    public Task(int taskID, int addOrder, String taskName, String description, String deadline, String priority, String status) {
        this(taskID, addOrder, taskName, description, deadline, priority);
        this.status = status;
    }

    // ===== GETTERS =====
    public int getTaskID()       { return taskID; }
    public int getAddOrder()     { return addOrder; }
    public String getTaskName()  { return taskName; }
    public String getDescription() { return description; }
    public String getDeadline()  { return deadline; }
    public String getPriority()  { return priority; }
    public String getStatus()    { return status; }

    // ====== SETTERS =====
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public void setDescription(String description) { this.description = description; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setStatus(String status)     { this.status = status; }

    public int getDeadlineValue() {
        String[] parts = deadline.split("/");
        int month = Integer.parseInt(parts[0]);
        int day   = Integer.parseInt(parts[1]);
        int year  = Integer.parseInt(parts[2]);
        return year * 10000 + month * 100 + day;
    }

    public int getPriorityValue() {
        if (priority.equals("High"))   return 1;
        if (priority.equals("Medium")) return 2;
        return 3;   
    }
}

