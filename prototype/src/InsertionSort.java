import java.util.ArrayList;

public class InsertionSort {

    // DEADLINE: Sort earliest deadline -> latest deadline
    public static void sortByDeadline(ArrayList<Task> list) {
        for (int i = 1; i < list.size(); i++) {
            Task currentTask = list.get(i);        
            int j = i - 1;

            while (j >= 0 && isDeadlineAfter(list.get(j), currentTask)) {
                list.set(j + 1, list.get(j));     
                j--;
            }

            list.set(j + 1, currentTask);           
        }
    }

    // 2) PRIORITY: Sort High -> Medium -> Low
    public static void sortByPriority(ArrayList<Task> list) {
        for (int i = 1; i < list.size(); i++) {
            Task currentTask = list.get(i);
            int j = i - 1;

            while (j >= 0 && isPriorityAfter(list.get(j), currentTask)) {
                list.set(j + 1, list.get(j));
                j--;
            }

            list.set(j + 1, currentTask);
        }
    }

    // 3) "Insertion Sort Algorithm" Sort by Task ID (#10 -> #99). 
    public static void sortByTaskID(ArrayList<Task> list) {
        for (int i = 1; i < list.size(); i++) {
            Task currentTask = list.get(i);
            int j = i - 1;

            while (j >= 0 && list.get(j).getTaskID() > currentTask.getTaskID()) {
                list.set(j + 1, list.get(j));
                j--;
            }

            list.set(j + 1, currentTask);
        }
    }

    private static boolean isDeadlineAfter(Task previous, Task current) {
        if (previous.getDeadlineValue() != current.getDeadlineValue()) {
            return previous.getDeadlineValue() > current.getDeadlineValue();
        }
        return previous.getAddOrder() > current.getAddOrder();
    }

    private static boolean isPriorityAfter(Task previous, Task current) {
        if (previous.getPriorityValue() != current.getPriorityValue()) {
            return previous.getPriorityValue() > current.getPriorityValue();
        }
        return previous.getAddOrder() > current.getAddOrder();  
    }
}
