import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.function.Consumer;

public class DatePickerPopup extends JPopupMenu {

    private final Consumer<LocalDate> onPick;
    private YearMonth shownMonth;
    private LocalDate selected;
    private final JPanel content = new JPanel(new BorderLayout(0, 8));

    public DatePickerPopup(Consumer<LocalDate> onPick) {
        this.onPick = onPick;
        setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        setBackground(Theme.SURFACE);
        content.setBackground(Theme.SURFACE);
        content.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(content);
    }

    public void showFor(Component invoker, LocalDate current) {
        LocalDate today = LocalDate.now();
        selected = current;
        shownMonth = YearMonth.from(current != null && !current.isBefore(today) ? current : today);
        rebuild();
        show(invoker, 0, invoker.getHeight() + 4);
    }

    private void rebuild() {
        content.removeAll();
        LocalDate today = LocalDate.now();

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JButton prev = arrowButton("‹");
        JButton next = arrowButton("›");
        prev.setEnabled(shownMonth.isAfter(YearMonth.from(today))); 
        prev.addActionListener(e -> { shownMonth = shownMonth.minusMonths(1); rebuild(); });
        next.addActionListener(e -> { shownMonth = shownMonth.plusMonths(1); rebuild(); });

        JLabel title = Theme.label(
                shownMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + shownMonth.getYear(),
                Font.BOLD, 14, Theme.TEXT);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(prev, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);

        // ===== DAY GRID =====
        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setOpaque(false);
        DayOfWeek[] order = {DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY};

        for (DayOfWeek d : order) {
            JLabel l = Theme.label(d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).substring(0, 2),
                    Font.BOLD, 11, Theme.MUTED);
            l.setHorizontalAlignment(SwingConstants.CENTER);
            grid.add(l);
        }

        int leadingBlanks = shownMonth.atDay(1).getDayOfWeek().getValue() % 7;   // Sunday = 0
        for (int i = 0; i < leadingBlanks; i++) grid.add(new JLabel());

        for (int day = 1; day <= shownMonth.lengthOfMonth(); day++) {
            LocalDate date = shownMonth.atDay(day);
            grid.add(dayButton(date, today));
        }

        // ===== FOOTER =====
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        footer.setOpaque(false);

        Theme.RoundButton todayBtn = new Theme.RoundButton("Today", Theme.ACCENT, Theme.RoundButton.Style.GHOST);
        todayBtn.setBorder(new EmptyBorder(5, 12, 5, 12));
        todayBtn.addActionListener(e -> pick(today));

        Theme.RoundButton tomorrowBtn = new Theme.RoundButton("Tomorrow", Theme.ACCENT, Theme.RoundButton.Style.GHOST);
        tomorrowBtn.setBorder(new EmptyBorder(5, 12, 5, 12));
        tomorrowBtn.addActionListener(e -> pick(today.plusDays(1)));
        footer.add(todayBtn);
        footer.add(tomorrowBtn);

        content.add(header, BorderLayout.NORTH);
        content.add(grid, BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);
        content.revalidate();
        pack();
        repaint();
    }

    private JButton dayButton(LocalDate date, LocalDate today) {
        boolean past = date.isBefore(today);
        boolean isToday = date.equals(today);
        boolean isSelected = date.equals(selected);

        JButton b = new JButton(String.valueOf(date.getDayOfMonth())) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected) {
                    g2.setColor(Theme.ACCENT);
                    g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                } 
                
                else if (!past && getModel().isRollover()) {
                    g2.setColor(Theme.SELECTION);
                    g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                }
                
                if (isToday && !isSelected) {
                    g2.setColor(Theme.ACCENT);
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(34, 30));
        b.setFont(Theme.font(isToday ? Font.BOLD : Font.PLAIN, 13));
        if (past) {
            b.setEnabled(false);
            b.setToolTipText("You cannot select past dates");
        } 
        else {
            b.setForeground(isSelected ? Color.WHITE : Theme.TEXT);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> pick(date));
        }
        return b;
    }

    private JButton arrowButton(String text) {
        Theme.RoundButton b = new Theme.RoundButton(text, Theme.TEXT, Theme.RoundButton.Style.GHOST);
        b.setFont(Theme.font(Font.BOLD, 18));
        b.setBorder(new EmptyBorder(0, 10, 2, 10));
        return b;
    }

    private void pick(LocalDate date) {
        setVisible(false);
        onPick.accept(date);
    }
}
