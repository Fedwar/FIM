package fleetmanagement.backend.tasks;

import java.util.Comparator;

public class DateTimeDescTaskComparator implements Comparator<Task> {

    @Override
    public int compare(Task o1, Task o2) {
        if (o1.getCompletedAt().equals(o2.getCompletedAt()))
            return 0;
        if (o1.getCompletedAt().isBefore(o2.getCompletedAt()))
            return 1;
        return -1;
    }
}
