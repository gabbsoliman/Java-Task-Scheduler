import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.MaskFormatter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.Map;

public class TaskSchedulerGUI extends JFrame {

    // ===== BACKEND =====
    private final TaskManager taskManager;
    private Task taskBeingEdited = null;                    
    private boolean isRefreshing = false;                   

    // ===== INPUT COMPONENTS =====
    private final Theme.HintTextField nameField = new Theme.HintTextField("e.g. Write code");
    private final JTextArea descriptionArea = new JTextArea(3, 20);
    private JFormattedTextField deadlineField;
    private final PriorityToggle highToggle   = new PriorityToggle("High", Theme.DANGER);
    private final PriorityToggle mediumToggle = new PriorityToggle("Medium", Theme.WARNING);
    private final PriorityToggle lowToggle    = new PriorityToggle("Low", Theme.SUCCESS);

    private final Theme.RoundButton addButton =
            new Theme.RoundButton("Add Task", Theme.PRIMARY, Theme.RoundButton.Style.FILLED);
    private final Theme.RoundButton clearButton =
            new Theme.RoundButton("Clear", Theme.MUTED, Theme.RoundButton.Style.OUTLINE);
    private final Theme.RoundButton editButton =
            new Theme.RoundButton("Edit", Theme.ACCENT, Theme.RoundButton.Style.OUTLINE);
    private final Theme.RoundButton deleteButton =
            new Theme.RoundButton("Delete Checked", Theme.DANGER, Theme.RoundButton.Style.OUTLINE);

    private final JLabel formTitle = Theme.label("New Task", Font.BOLD, 17, Theme.TEXT);
    private final JLabel formSubtitle = Theme.label("Fill in the details below.", Font.PLAIN, 12, Theme.MUTED);
    private final JLabel deadlineHint = Theme.label(" ", Font.PLAIN, 11, Theme.MUTED);
    private final JLabel formError = Theme.label(" ", Font.PLAIN, 12, Theme.DANGER);

    private final Theme.HintTextField searchField = new Theme.HintTextField("Search by #ID or task name");
    private final Theme.RoundButton searchButton =
            new Theme.RoundButton("Search", Theme.ACCENT, Theme.RoundButton.Style.FILLED);

    // ===== STAT CARDS =====
    private final JLabel totalValue = statValue(Theme.TEXT);
    private final JLabel pendingValue = statValue(Theme.ACCENT);
    private final JLabel completedValue = statValue(Theme.SUCCESS);
    private final JLabel overdueValue = statValue(Theme.DANGER);

    // ===== TASKS TABLE COMPONENTS =====
    private DefaultTableModel tableModel;
    private JTable taskTable;

    // ===== SORT BY =====
    private final JComboBox<String> sortBox =
            new JComboBox<>(new String[]{"Status", "Deadline", "Priority", "INSERTION SORT ALGORITHM"});

    public TaskSchedulerGUI(TaskManager taskManager) {
        this.taskManager = taskManager;

        setTitle("Task Scheduler - GEMS 3");
        setIconImage(Theme.windowIcon());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        deadlineField = createDeadlineField();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(createHeader(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(18, 22, 20, 22));
        body.add(createStatsRow(), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(16, 0));
        main.setOpaque(false);
        main.add(createFormCard(), BorderLayout.WEST);
        main.add(createTableCard(), BorderLayout.CENTER);
        body.add(main, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);
        setContentPane(root);

        connectButtons();
        connectCheckbox();
        connectSortBox();
        connectDeadlineHint();
        setSelectedPriority("High");

        setSize(1200, 760);
        setMinimumSize(new Dimension(1020, 640));
        setLocationRelativeTo(null);
        refreshTable();
    }

    // ===== LAYOUT =====
    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.PRIMARY, getWidth(), 0, Theme.PRIMARY_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setBorder(new EmptyBorder(12, 22, 12, 22));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        left.add(Theme.logo(44, Color.WHITE, Theme.PRIMARY));
        left.add(Theme.label(" TASK SCHEDULER ", Font.BOLD, 40, Color.WHITE));

        header.add(left, BorderLayout.WEST);
        return header;
    }

    private JPanel createStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.add(statCard("Total tasks", totalValue, Theme.PRIMARY));
        row.add(statCard("Pending", pendingValue, Theme.ACCENT));
        row.add(statCard("Completed", completedValue, Theme.SUCCESS));
        row.add(statCard("Overdue", overdueValue, Theme.DANGER));
        return row;
    }

    private JPanel statCard(String title, JLabel value, Color accent) {
        Theme.RoundedPanel card = Theme.card(new BorderLayout(14, 0), 0);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        JComponent bar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 5, getHeight(), 5, 5);
                g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(5, 10));
        JPanel text = new JPanel(new BorderLayout(0, 2));
        text.setOpaque(false);
        text.add(Theme.label(title, Font.PLAIN, 12, Theme.MUTED), BorderLayout.NORTH);
        text.add(value, BorderLayout.CENTER);
        card.add(bar, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private static JLabel statValue(Color color) {
        return Theme.label("0", Font.BOLD, 26, color);
    }

    private JPanel createFormCard() {
        Theme.RoundedPanel card = Theme.card(new BorderLayout(), 20);
        card.setPreferredSize(new Dimension(330, 10));

        FormPanel form = new FormPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setFont(Theme.font(Font.PLAIN, 14));

        Theme.RoundButton calendarButton = new Theme.RoundButton("", Theme.ACCENT, Theme.RoundButton.Style.GHOST);
        calendarButton.setIcon(new CalendarIcon(Theme.ACCENT));
        calendarButton.setBorder(new EmptyBorder(4, 8, 4, 8));
        calendarButton.setToolTipText("Pick a date");
        Theme.RoundedPanel deadlineBox = Theme.inputBox(deadlineField, calendarButton);
        DatePickerPopup picker = new DatePickerPopup(date -> {
            deadlineField.setText(TaskManager.fromDate(date));
            deadlineField.requestFocusInWindow();
        });
        calendarButton.addActionListener(e -> {
            String current = taskManager.formatDeadline(deadlineField.getText());
            picker.showFor(deadlineBox, current == null ? null : TaskManager.toDate(current));
        });

        JPanel priorityRow = new JPanel(new GridLayout(1, 3, 8, 0));
        priorityRow.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (PriorityToggle t : new PriorityToggle[]{highToggle, mediumToggle, lowToggle}) {
            group.add(t);
            priorityRow.add(t);
        }

        addLeft(form, formTitle);
        form.add(Box.createVerticalStrut(2));
        addLeft(form, formSubtitle);
        form.add(Box.createVerticalStrut(18));
        addLeft(form, fieldLabel("Task name"));
        form.add(Box.createVerticalStrut(6));
        addLeft(form, Theme.inputBox(nameField, null));
        form.add(Box.createVerticalStrut(14));
        addLeft(form, fieldLabel("Description"));
        form.add(Box.createVerticalStrut(6));
        addLeft(form, Theme.inputBox(descriptionArea, null));
        form.add(Box.createVerticalStrut(14));
        addLeft(form, fieldLabel("Deadline (MM/DD/YY)"));
        form.add(Box.createVerticalStrut(6));
        addLeft(form, deadlineBox);
        form.add(Box.createVerticalStrut(5));
        addLeft(form, deadlineHint);
        form.add(Box.createVerticalStrut(12));
        addLeft(form, fieldLabel("Priority"));
        form.add(Box.createVerticalStrut(6));
        addLeft(form, priorityRow);
        form.add(Box.createVerticalStrut(10));
        addLeft(form, formError);
        form.add(Box.createVerticalGlue());

        addButton.setFont(Theme.font(Font.BOLD, 14));
        addLeft(form, addButton);
        form.add(Box.createVerticalStrut(8));
        addLeft(form, clearButton);

        for (Component c : form.getComponents()) {
            if (c instanceof JComponent && !(c instanceof Box.Filler)) {
                JComponent jc = (JComponent) c;
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, jc.getPreferredSize().height));
            }
        }
        formError.setVerticalAlignment(SwingConstants.TOP);
        formError.setPreferredSize(new Dimension(250, 34));
        formError.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JScrollPane scroll = new JScrollPane(form,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", e -> {
            if (e.getNewValue() instanceof Component) {
                Component focused = (Component) e.getNewValue();
                if (SwingUtilities.isDescendingFrom(focused, form)) {
                    Rectangle r = SwingUtilities.convertRectangle(focused.getParent(), focused.getBounds(), form);
                    form.scrollRectToVisible(r);
                }
            }
        });
        return card;
    }

    private static class FormPanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 16; }
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(16, visible.height - 32);
        }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport && getParent().getHeight() > getPreferredSize().height;
        }
    }

    private void addLeft(JPanel panel, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(c);
    }

    private JLabel fieldLabel(String text) {
        return Theme.label(text, Font.BOLD, 12, Theme.TEXT);
    }

    private JPanel createTableCard() {
        Theme.RoundedPanel card = Theme.card(new BorderLayout(0, 12), 18);

        JPanel toolbar = new JPanel(new BorderLayout(12, 0));
        toolbar.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleBox.setOpaque(false);
        titleBox.add(Theme.label("My Tasks", Font.BOLD, 17, Theme.TEXT));

        searchButton.setBorder(new EmptyBorder(6, 14, 6, 14));
        JPanel searchBox = Theme.inputBox(searchField, searchButton);
        searchBox.setPreferredSize(new Dimension(340, 40));
        searchField.setToolTipText("Enter a task ID (for example, #42) or part of a task name");
        searchButton.addActionListener(e -> searchTasks());
        searchField.addActionListener(e -> searchTasks());

        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        sortPanel.setOpaque(false);
        sortBox.setFont(Theme.font(Font.PLAIN, 13));
        sortBox.setBackground(Theme.SURFACE);
        sortBox.setFocusable(false);
        sortBox.setPreferredSize(new Dimension(230, 36));
        sortBox.setToolTipText("Status: Pending on top, Completed at the bottom");
        sortPanel.add(Theme.label("Sort by", Font.BOLD, 12, Theme.MUTED));
        sortPanel.add(sortBox);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        left.add(titleBox);
        left.add(searchBox);
        toolbar.add(left, BorderLayout.WEST);
        toolbar.add(sortPanel, BorderLayout.EAST);

        // ===== TABLE =====
        String[] columns = {"", "ID", "Task", "Description", "Deadline", "Priority", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Boolean.class;
                return String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };

        taskTable = new JTable(tableModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) paintEmptyState((Graphics2D) g, this);
            }
        };
        taskTable.setRowHeight(42);
        taskTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskTable.setFont(Theme.font(Font.PLAIN, 13));
        taskTable.setForeground(Theme.TEXT);
        taskTable.setBackground(Theme.SURFACE);
        taskTable.setSelectionBackground(Theme.SELECTION);
        taskTable.setSelectionForeground(Theme.TEXT);
        taskTable.setFillsViewportHeight(true);
        taskTable.setShowVerticalLines(false);
        taskTable.setShowHorizontalLines(true);
        taskTable.setGridColor(new Color(237, 240, 245));
        taskTable.setIntercellSpacing(new Dimension(0, 1));
        taskTable.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = taskTable.getTableHeader();
        header.setDefaultRenderer(new HeaderRenderer());
        header.setPreferredSize(new Dimension(10, 38));
        header.setBackground(Theme.HEADER_BG);
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (taskTable.convertColumnIndexToModel(header.columnAtPoint(e.getPoint())) == 0) {
                    toggleSelectAll();
                }
            }
        });

        taskTable.getColumnModel().getColumn(0).setMaxWidth(46);
        taskTable.getColumnModel().getColumn(0).setMinWidth(46);
        taskTable.getColumnModel().getColumn(1).setMaxWidth(64);
        taskTable.getColumnModel().getColumn(1).setMinWidth(56);
        taskTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        taskTable.getColumnModel().getColumn(2).setMinWidth(160);
        taskTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        taskTable.getColumnModel().getColumn(4).setPreferredWidth(175);
        taskTable.getColumnModel().getColumn(4).setMinWidth(165);
        taskTable.getColumnModel().getColumn(5).setPreferredWidth(95);
        taskTable.getColumnModel().getColumn(5).setMinWidth(90);
        taskTable.getColumnModel().getColumn(6).setPreferredWidth(135);
        taskTable.getColumnModel().getColumn(6).setMinWidth(130);

        taskTable.getColumnModel().getColumn(0).setCellRenderer(new CheckboxRenderer());
        TaskCellRenderer textRenderer = new TaskCellRenderer();
        for (int column = 1; column <= 4; column++) {
            taskTable.getColumnModel().getColumn(column).setCellRenderer(textRenderer);
        }
        BadgeRenderer badge = new BadgeRenderer();
        taskTable.getColumnModel().getColumn(5).setCellRenderer(badge);
        taskTable.getColumnModel().getColumn(6).setCellRenderer(badge);

        taskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && taskTable.columnAtPoint(e.getPoint()) != 0) {
                    loadSelectedTaskForEditing();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(taskTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        scroll.getViewport().setBackground(Theme.SURFACE);

        // ===== FOOTER =====
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(Theme.label("Tick a box to mark a task complete  •  Double-click a row to edit",
                Font.PLAIN, 12, Theme.MUTED), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(deleteButton);
        footer.add(actions, BorderLayout.EAST);

        card.add(toolbar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private void paintEmptyState(Graphics2D g, JTable table) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle view = table.getVisibleRect();
        int cx = view.x + view.width / 2;
        int cy = view.y + view.height / 2 - 30;
        Theme.paintLogo(g2, cx - 24, cy - 48, 48, Theme.SELECTION, Theme.ACCENT);
        g2.setFont(Theme.font(Font.BOLD, 16));
        g2.setColor(Theme.TEXT);
        String title = "No Scheduled Task yet";
        g2.drawString(title, cx - g2.getFontMetrics().stringWidth(title) / 2, cy + 26);
        g2.setFont(Theme.font(Font.PLAIN, 13));
        g2.setColor(Theme.MUTED);
        String sub = "Schedule your tasks and be productive";
        g2.drawString(sub, cx - g2.getFontMetrics().stringWidth(sub) / 2, cy + 48);
        g2.dispose();
    }

    private JFormattedTextField createDeadlineField() {
        JFormattedTextField field;
        try {
            MaskFormatter mask = new MaskFormatter("##/##/##");
            mask.setPlaceholderCharacter('_');
            field = new JFormattedTextField(mask);
        } catch (ParseException e) {
            field = new JFormattedTextField();
        }

        field.setColumns(10);
        field.setFocusLostBehavior(JFormattedTextField.PERSIST);
        return field;
    }

    // ===== ACTIONS =====
    private void connectButtons() {
        addButton.addActionListener(e -> addOrSaveTask());
        editButton.addActionListener(e -> loadSelectedTaskForEditing());
        deleteButton.addActionListener(e -> deleteCheckedTasks());
        clearButton.addActionListener(e -> clearInputs());
    }

    private void addOrSaveTask() {
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String deadline = taskManager.formatDeadline(deadlineField.getText());
        String priority = getSelectedPriority();

        if (name.isEmpty()) {
            showFormError("Please enter a task name.");
            nameField.requestFocusInWindow();
            return;
        }
        if (deadline == null) {
            showFormError("Enter a valid deadline as MM/DD/YY (example: "
                    + TaskManager.fromDate(LocalDate.now().plusDays(7)) + ").");
            deadlineField.requestFocusInWindow();
            return;
        }

        // ===== PAST DATE RESTRICTION =====
        boolean keepingOldDeadline = taskBeingEdited != null && deadline.equals(taskBeingEdited.getDeadline());
        if (taskManager.isBeforeToday(deadline) && !keepingOldDeadline) {
            showFormError("The deadline can't be a past date. Pick today or later.");
            deadlineField.requestFocusInWindow();
            return;
        }

        try {
            if (taskBeingEdited == null) {
                Task added = taskManager.addTask(name, description, deadline, priority);
                if (added == null) {
                    showFormError("The list is full (Task IDs #10 to #99 are all used). Delete some tasks first.");
                    return;
                }
            } 
            else {
                taskManager.editTask(taskBeingEdited, name, description, deadline, priority);
            }
        } catch (IllegalArgumentException ex) {
            showFormError(ex.getMessage());
            return;
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
        descriptionArea.setText(taskBeingEdited.getDescription());
        deadlineField.setText(taskBeingEdited.getDeadline());
        setSelectedPriority(taskBeingEdited.getPriority());
        formTitle.setText("Edit Task #" + taskBeingEdited.getTaskID());
        formSubtitle.setText("Change the details, then save.");
        addButton.setText("Save Changes");
        clearButton.setText("Cancel");
        showFormError(" ");
        nameField.requestFocusInWindow();
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

        if (taskBeingEdited != null && taskBeingEdited.isCompleted()) {
            clearInputs();
        }

        taskManager.deleteCheckedTasks();
        refreshTable();
    }

    private void clearInputs() {
        nameField.setText("");
        descriptionArea.setText("");
        deadlineField.setValue(null);
        setSelectedPriority("High");
        taskBeingEdited = null;
        formTitle.setText("New Task");
        formSubtitle.setText("Fill in the details below.");
        addButton.setText("Add Task");
        clearButton.setText("Clear");
        showFormError(" ");
        taskTable.clearSelection();
    }

    private void showFormError(String message) {
        formError.setText(message.trim().isEmpty() ? " "
                : "<html><div style='width:225px'>" + message + "</div></html>");
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

            SwingUtilities.invokeLater(this::refreshTable);
        });
    }

    // === SORT THE COMBO BOX ===
    private void connectSortBox() {
        sortBox.addActionListener(e -> refreshTable());
    }

    private void connectDeadlineHint() {
        deadlineField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateDeadlineHint(); }
            public void removeUpdate(DocumentEvent e)  { updateDeadlineHint(); }
            public void changedUpdate(DocumentEvent e) { updateDeadlineHint(); }
        });
        updateDeadlineHint();
    }

    private void updateDeadlineHint() {
        String deadline = taskManager.formatDeadline(deadlineField.getText());
        if (deadline == null) {
            deadlineHint.setForeground(Theme.MUTED);
            deadlineHint.setText("Enter date number or use the calendar. (No past dates)");
            return;
        }

        LocalDate date = TaskManager.toDate(deadline);
        long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date);
        boolean keepingOld = taskBeingEdited != null && deadline.equals(taskBeingEdited.getDeadline());

        if (days < 0 && !keepingOld) {
            deadlineHint.setForeground(Theme.DANGER);
            deadlineHint.setText("That date has already passed. Choose today or later.");
        } 
        else {
            deadlineHint.setForeground(days < 0 ? Theme.DANGER : Theme.SUCCESS);
            deadlineHint.setText(date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                    + "  •  " + relativeDays(days));
        }
    }

    private String relativeDays(long days) {
        if (days == 0) return "Today";
        if (days == 1) return "Tomorrow";
        if (days < 0) return (-days) + " day" + (days == -1 ? "" : "s") + " overdue";
        return "in " + days + " days";
    }

    private void refreshTable() {
        isRefreshing = true;

        String sortOption = (String) sortBox.getSelectedItem();
        taskManager.sortTasks(sortOption);

        tableModel.setRowCount(0);
        for (Task task : taskManager.getTasks()) {
            tableModel.addRow(new Object[]{
                    task.isCompleted(),
                    "#" + task.getTaskID(),
                    task.getTaskName(),
                    task.getDescription(),
                    task.getDeadline(),
                    task.getPriority(),
                    task.getStatus()
            });
        }

        isRefreshing = false;
        taskTable.repaint();
        taskTable.getTableHeader().repaint();
        updateTaskCounter();
    }

    private void toggleSelectAll() {
        if (taskManager.getTasks().isEmpty()) return;
        taskManager.setAllTasksDone(!taskManager.areAllTasksDone());
        refreshTable();
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
        totalValue.setText(String.valueOf(total));
        pendingValue.setText(String.valueOf(total - completed));
        completedValue.setText(String.valueOf(completed));
        overdueValue.setText(String.valueOf(taskManager.countOverdueTasks()));
    }

    // ===== PRIORITY TOGGLE HELPERS =====
    private String getSelectedPriority() {
        if (mediumToggle.isSelected()) return "Medium";
        if (lowToggle.isSelected()) return "Low";
        return "High";
    }

    private void setSelectedPriority(String priority) {
        if (priority.equals("Medium")) mediumToggle.setSelected(true);
        else if (priority.equals("Low")) lowToggle.setSelected(true);
        else highToggle.setSelected(true);
    }

    private boolean isOverdue(Task task) {
        return !task.isCompleted() && task.getDeadlineDate().isBefore(LocalDate.now());
    }

    private static Color priorityColor(String priority) {
        if (priority.equals("High")) return Theme.DANGER;
        if (priority.equals("Medium")) return Theme.WARNING;
        return Theme.SUCCESS;
    }

    // ===== CUSTOM COMPONENTS / RENDERERS =====
    private static class PriorityToggle extends JToggleButton {
        private final Color color;

        PriorityToggle(String text, Color color) {
            super(text);
            this.color = color;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFont(Theme.font(Font.BOLD, 13));
            setBorder(new EmptyBorder(8, 6, 8, 6));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addItemListener(e -> setForeground(isSelected() ? color : Theme.MUTED));
            setForeground(Theme.MUTED);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (isSelected()) {
                g2.setColor(Theme.withAlpha(color, 28));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, w - 3, h - 3, 10, 10);
            } 
            else {
                g2.setColor(getModel().isRollover() ? Theme.HEADER_BG : Theme.SURFACE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
                g2.setColor(Theme.BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class CalendarIcon implements Icon {
        private final Color color;
        CalendarIcon(Color color) { 
            this.color = color; 
        }
        public int getIconWidth()  { 
            return 18; 
        }
        public int getIconHeight() { 
            return 18; 
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(x + 2, y + 3, 14, 13, 4, 4);
            g2.drawLine(x + 2, y + 7, x + 16, y + 7);
            g2.drawLine(x + 6, y + 1, x + 6, y + 4);
            g2.drawLine(x + 12, y + 1, x + 12, y + 4);
            g2.fillRect(x + 5, y + 10, 2, 2);
            g2.fillRect(x + 8, y + 10, 2, 2);
            g2.fillRect(x + 11, y + 10, 2, 2);
            g2.dispose();
        }
    }

    private class HeaderRenderer extends DefaultTableCellRenderer {
        private final JCheckBox selectAllBox = new JCheckBox();

        HeaderRenderer() {
            selectAllBox.setHorizontalAlignment(CENTER);
            selectAllBox.setOpaque(true);
            selectAllBox.setBackground(Theme.HEADER_BG);
            selectAllBox.setBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER));
            selectAllBox.setBorderPainted(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            if (table.convertColumnIndexToModel(column) == 0) {
                selectAllBox.setSelected(taskManager.areAllTasksDone());
                selectAllBox.setEnabled(!taskManager.getTasks().isEmpty());
                
                return selectAllBox;
            }
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, false, false, row, column);
            l.setText(value == null ? "" : value.toString().toUpperCase());
            l.setFont(Theme.font(Font.BOLD, 11));
            l.setForeground(Theme.MUTED);
            l.setBackground(Theme.HEADER_BG);
            l.setOpaque(true);

            int modelColumn = table.convertColumnIndexToModel(column);
            l.setHorizontalAlignment(modelColumn == 0 || modelColumn == 4 ? CENTER : LEFT);  
            l.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDER), new EmptyBorder(0, 12, 0, 12)));
            return l;
        }
    }

    private class TaskCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, selected, false, row, column);
            Task task = taskManager.getTasks().get(table.convertRowIndexToModel(row));
            int modelColumn = table.convertColumnIndexToModel(column);

            l.setBorder(new EmptyBorder(0, 12, 0, 12));
            l.setBackground(selected ? Theme.SELECTION : Theme.SURFACE);
            l.setFont(Theme.font(modelColumn == 2 ? Font.BOLD : Font.PLAIN, 13));
            l.setForeground(Theme.TEXT);
            l.setToolTipText(null);

            l.setHorizontalAlignment(modelColumn == 4 ? CENTER : LEFT);

            if (modelColumn == 1) {
                l.setForeground(Theme.MUTED);
            } 
            else if (modelColumn == 3) {
                l.setForeground(Theme.MUTED);
                String d = task.getDescription();
                if (d != null && !d.isEmpty()) l.setToolTipText(d);
            } 
            else if (modelColumn == 4) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), task.getDeadlineDate());
                String tag = "";
                if (!task.isCompleted()) {
                    if (days < 0) { tag = "  •  Overdue"; l.setForeground(Theme.DANGER); }
                    else if (days == 0) { tag = "  •  Today"; l.setForeground(Theme.WARNING); }
                    else if (days == 1) { tag = "  •  Tomorrow"; }
                }
                l.setText(task.getDeadline() + tag);
            }

            if (task.isCompleted()) {
                l.setForeground(new Color(150, 156, 168));
                if (modelColumn == 2) {
                    Map<TextAttribute, Object> attrs = new java.util.HashMap<>();
                    attrs.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
                    l.setFont(l.getFont().deriveFont(attrs));
                }
            }
            return l;
        }
    }

    private class BadgeRenderer extends JComponent implements TableCellRenderer {
        private String text = "";
        private Color color = Theme.MUTED;
        private boolean selected;
        private boolean faded;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            Task task = taskManager.getTasks().get(table.convertRowIndexToModel(row));
            int modelColumn = table.convertColumnIndexToModel(column);
            this.selected = selected;
            this.text = value == null ? "" : value.toString();
            this.faded = false;

            if (modelColumn == 5) {
                color = priorityColor(text);
                faded = task.isCompleted();
            } 
            else {
                if (task.isCompleted()) {
                    color = Theme.SUCCESS;
                    text = "✓ Completed";
                } 
                else if (isOverdue(task)) {
                    color = Theme.DANGER;
                    text = "Pending";
                } 
                else {
                    color = Theme.ACCENT;
                    text = "Pending";
                }
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(selected ? Theme.SELECTION : Theme.SURFACE);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (faded) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2.setFont(Theme.font(Font.BOLD, 12));

            FontMetrics fm = g2.getFontMetrics();
            int pillW = fm.stringWidth(text) + 22;
            int pillH = 24;
            int x = 12;
            int y = (getHeight() - pillH) / 2;

            g2.setColor(Theme.withAlpha(color, 30));
            g2.fillRoundRect(x, y, pillW, pillH, pillH, pillH);
            g2.setColor(color);
            g2.drawString(text, x + 11, y + (pillH - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    private class CheckboxRenderer extends JCheckBox implements TableCellRenderer {
        CheckboxRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setOpaque(true);
            setBorderPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            setSelected(Boolean.TRUE.equals(value));
            setBackground(selected ? Theme.SELECTION : Theme.SURFACE);
            return this;
        }
    }
}
