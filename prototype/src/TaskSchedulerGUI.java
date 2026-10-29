import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.LocalDate;

public class TaskSchedulerGUI extends JFrame {

    // ===== Backend =====
    private TaskManager taskManager = new TaskManager();
    private Task taskBeingEdited = null;                     // null = we are adding
    private boolean isRefreshing = false;                       // stops checkbox listener during refresh

    // ===== INPUT COMPONENTS =====
    private JTextField nameField = new JTextField(20);

    private JTextField descriptionField = new JTextField(20);
    private JFormattedTextField deadlineField;   
    private JComboBox<String> priorityBox = new JComboBox<>(new String[]{"High", "Medium", "Low"});
    private JButton addButton = new JButton("Add");
    private JButton deleteButton = new JButton("Delete");
    private JButton editButton = new JButton("Edit");
    private JButton clearButton = new JButton("Clear");

    private JTextField searchField = new JTextField(20);
    private JButton searchButton = new JButton("Search");

    private JLabel taskCounter = new JLabel("Total: 0 | Pending: 0 | Completed: 0");

    // ===== TASKS TABLE COMPONENTS =====
    private DefaultTableModel tableModel;
    private JTable taskTable;

    // ===== SORT BY =====
    private JComboBox<String> sortBox =
            new JComboBox<>(new String[]{"Deadline", "Priority", "INSERTION SORT ALGORITHM"});

    public TaskSchedulerGUI() {
        setTitle("Task Scheduler - GEMS 3");
        setIconImage(createWindowIcon()); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                taskManager.close();   
            }
        });
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(245, 247, 250));

        deadlineField = createDeadlineField();
        add(createTopPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createSortPanel(), BorderLayout.SOUTH);

        connectButtons();
        connectCheckbox();
        connectSortBox();

        setSize(900, 700);
        setLocationRelativeTo(null);   
        refreshTable();
    }

    // ===== GUI ======
    private JFormattedTextField createDeadlineField() {
        JFormattedTextField field;
        try {
            MaskFormatter mask = new MaskFormatter("##/##/##");
            mask.setPlaceholderCharacter('_');   
            field = new JFormattedTextField(mask);               
        } 
        
        catch (ParseException e) {
            field = new JFormattedTextField();   
        }

        field.setColumns(10);
        field.setFocusLostBehavior(JFormattedTextField.PERSIST); 
        return field;
    }

    private JPanel createTopPanel() {
        JPanel top = new JPanel(new BorderLayout());

        JLabel title = new JLabel("TASK SCHEDULER", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setOpaque(true);
        title.setBackground(new Color(40, 70, 120));
        title.setBorder(new EmptyBorder(12, 0, 12, 0));

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 8));
        form.add(new JLabel("Task Name:"));
        form.add(nameField);
        form.add(new JLabel("Description:"));
        form.add(descriptionField);
        form.add(new JLabel("Deadline (MM/DD/YY):"));
        form.add(deadlineField);
        form.add(new JLabel("Priority:"));
        form.add(priorityBox);
        form.setBorder(new EmptyBorder(10, 60, 5, 60));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        styleButton(addButton, new Color(60, 140, 80));
        styleButton(deleteButton, new Color(180, 60, 60));
        styleButton(editButton, new Color(70, 110, 180));
        styleButton(clearButton, new Color(120, 120, 120));
        buttons.add(addButton);
        buttons.add(deleteButton);
        buttons.add(editButton);
        buttons.add(clearButton);

        JPanel inputSection = new JPanel(new BorderLayout());
        inputSection.add(form, BorderLayout.CENTER);
        inputSection.add(buttons, BorderLayout.SOUTH);
        inputSection.setBackground(new Color(235, 240, 248));
        form.setBackground(new Color(235, 240, 248));
        buttons.setBackground(new Color(235, 240, 248));
        inputSection.setBorder(new TitledBorder("New / Edit Task"));

        top.add(title, BorderLayout.NORTH);
        top.add(inputSection, BorderLayout.CENTER);
        return top;
    }

    private JPanel createTablePanel() {
        String[] columns = {"   ", "Task ID", "Task", "Description", "Deadline", "Priority", "Status"};


        tableModel = new DefaultTableModel(columns, 0) {
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Boolean.class;
                return String.class;
            }
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(27);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.setFont(new Font("SansSerif", Font.PLAIN, 13));
        taskTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        taskTable.setFillsViewportHeight(true);
        taskTable.getColumnModel().getColumn(0).setMaxWidth(50);
        taskTable.getColumnModel().getColumn(1).setMaxWidth(70);
        taskTable.getColumnModel().getColumn(2).setPreferredWidth(160);
        taskTable.getColumnModel().getColumn(3).setPreferredWidth(240);
        taskTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        taskTable.getColumnModel().getColumn(5).setPreferredWidth(90);
        taskTable.getColumnModel().getColumn(6).setPreferredWidth(100);
        taskTable.getColumnModel().getColumn(0).setCellRenderer(new CheckboxRenderer());
        for (int column = 1; column < taskTable.getColumnCount(); column++) {
            taskTable.getColumnModel().getColumn(column).setCellRenderer(new TaskCellRenderer());
        }

        JPanel panel = new JPanel(new BorderLayout());
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchPanel.add(new JLabel("Search by ID or name:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchField.setToolTipText("Enter a task ID (for example, #42) or part of a task name");
        searchButton.addActionListener(e -> searchTasks());
        searchField.addActionListener(e -> searchTasks());
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(taskTable), BorderLayout.CENTER);
        panel.setBorder(new TitledBorder("TASK LIST"));
        return panel;
    }

    private JPanel createSortPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel sortControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        sortControls.add(new JLabel("Sort By:"));
        sortControls.add(sortBox);
        panel.add(sortControls, BorderLayout.WEST);

        panel.add(taskCounter, BorderLayout.EAST);
        taskCounter.setBorder(new EmptyBorder(0, 10, 0, 15));
        panel.setBorder(new EmptyBorder(0, 10, 5, 10));
        return panel;
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setFocusPainted(false);
    }

    private Image createWindowIcon() {

        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(40, 70, 120));
        graphics.fillRoundRect(1, 1, 30, 30, 8, 8);
        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(8, 17, 14, 23);
        graphics.drawLine(14, 23, 25, 10);
        graphics.dispose();
        return image;
    }

    private void connectButtons() {
        addButton.addActionListener(e -> addOrSaveTask());
        editButton.addActionListener(e -> loadSelectedTaskForEditing());
        deleteButton.addActionListener(e -> deleteCheckedTasks());
        clearButton.addActionListener(e -> clearInputs());
    }

    private void addOrSaveTask() {
        String name = nameField.getText().trim();
        String description = descriptionField.getText().trim();
        String deadline = taskManager.formatDeadline(deadlineField.getText());
        String priority = (String) priorityBox.getSelectedItem();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a task name.");
            return;
        }
        if (deadline == null) {
            JOptionPane.showMessageDialog(this, "Please enter a valid deadline as MM/DD/YY (example: 09/20/26).");
            return;
        }

        if (taskBeingEdited == null) {
            Task added = taskManager.addTask(name, description, deadline, priority);   
            if (added == null) {
                JOptionPane.showMessageDialog(this, "The list is full (Task IDs #10 to #99 are all used). Delete some tasks first.");
                return;
            }
        } else {
            taskManager.editTask(taskBeingEdited, name, description, deadline, priority); 
        }

        clearInputs();
        refreshTable();
    }

  
    private void loadSelectedTaskForEditing() {
        int row = taskTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a task in the table first.");
            return;
        }

      
        taskBeingEdited = taskManager.getTasks().get(row);
        nameField.setText(taskBeingEdited.getTaskName());
        descriptionField.setText(taskBeingEdited.getDescription());
        deadlineField.setText(taskBeingEdited.getDeadline());
        priorityBox.setSelectedItem(taskBeingEdited.getPriority());
        addButton.setText("Save");
    }

 
    private void deleteCheckedTasks() {
        int checkedCount = taskManager.countCompletedTasks();
        if (checkedCount == 0) {
            JOptionPane.showMessageDialog(this, "Check at least one task to delete.");
            return;
        }
       
        int choice = JOptionPane.showConfirmDialog(this,
                "Delete " + checkedCount + " checked task" + (checkedCount == 1 ? "" : "s") + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        if (taskBeingEdited != null && taskBeingEdited.getStatus().equals("Completed")) {
            clearInputs();   
        }

        taskManager.deleteCheckedTasks();
        refreshTable();
    }

    private void clearInputs() {
        nameField.setText("");
        descriptionField.setText("");
        deadlineField.setValue(null);   
        priorityBox.setSelectedIndex(0);
        taskBeingEdited = null;
        addButton.setText("Add");
        taskTable.clearSelection();
    }

    
    private void connectCheckbox() {
        tableModel.addTableModelListener(e -> {
            if (isRefreshing) return;   
            if (e.getType() != TableModelEvent.UPDATE || e.getColumn() != 0) return;

            int row = e.getFirstRow();
            if (row < 0 || row >= taskManager.getTasks().size()) return;

            boolean checked = (Boolean) tableModel.getValueAt(row, 0);
            Task task = taskManager.getTasks().get(row);
            taskManager.setTaskDone(task, checked);   

           
            isRefreshing = true;
            tableModel.setValueAt(task.getStatus(), row, 6);   
            isRefreshing = false;
            updateTaskCounter();
        });
    }

    // === SORT THE COMBO BOX ===
    private void connectSortBox() {
        sortBox.addActionListener(e -> refreshTable());
    }

    private void refreshTable() {
        isRefreshing = true;

        String sortOption = (String) sortBox.getSelectedItem();
        taskManager.sortTasks(sortOption); 

        tableModel.setRowCount(0);   
        for (Task task : taskManager.getTasks()) {
            boolean isDone = task.getStatus().equals("Completed");
            tableModel.addRow(new Object[]{
                    isDone,
                    "#" + task.getTaskID(),     
                    task.getTaskName(),
                    task.getDescription(),
                    task.getDeadline(),
                    task.getPriority(),
                    task.getStatus()
            });
        }

        isRefreshing = false;
        updateTaskCounter();
    }

    private void searchTasks() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a task ID or name to search.");
            return;
        }

        Task match = taskManager.searchTask(query);
        if (match == null) {
            taskTable.clearSelection();
            JOptionPane.showMessageDialog(this, "No match found.");
            return;
        }

        int row = taskManager.getTasks().indexOf(match);
        taskTable.setRowSelectionInterval(row, row);
        taskTable.scrollRectToVisible(taskTable.getCellRect(row, 0, true));
    }

    private void updateTaskCounter() {
        int total = taskManager.getTasks().size();
        int completed = taskManager.countCompletedTasks();
        taskCounter.setText("Total: " + total + " | Pending: " + (total - completed)
                + " | Completed: " + completed);
    }

    private Color priorityColor(Task task) {
        if (task.getPriority().equals("High")) {
            return new Color(255, 225, 225);
        }
        if (task.getPriority().equals("Medium")) {
            return new Color(255, 247, 205);
        }
        return new Color(225, 245, 225);
    }

    private boolean isOverdue(Task task) {
        LocalDate today = LocalDate.now();
        int todayValue = (today.getYear() % 100) * 10000 + today.getMonthValue() * 100
                + today.getDayOfMonth();
        return task.getDeadlineValue() < todayValue;
    }

    private class TaskCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            int modelRow = table.convertRowIndexToModel(row);
            Task task = taskManager.getTasks().get(modelRow);
            if (!selected) {
                component.setBackground(priorityColor(task));
                if (task.getStatus().equals("Completed")) {
                    component.setForeground(new Color(125, 125, 125));
                } else if (table.convertColumnIndexToModel(column) == 4 && isOverdue(task)) {
                    component.setForeground(new Color(190, 35, 35));
                } else {
                    component.setForeground(table.getForeground());
                }
            }
            return component;
        }
    }

    private class CheckboxRenderer extends JCheckBox implements TableCellRenderer {
        CheckboxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            setSelected(Boolean.TRUE.equals(value));
            int modelRow = table.convertRowIndexToModel(row);
            Task task = taskManager.getTasks().get(modelRow);
            setBackground(selected ? table.getSelectionBackground() : priorityColor(task));
            return this;
        }
    }
}
