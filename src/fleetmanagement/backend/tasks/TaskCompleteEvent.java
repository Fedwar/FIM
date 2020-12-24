package fleetmanagement.backend.tasks;

import org.springframework.context.ApplicationEvent;

public class TaskCompleteEvent extends ApplicationEvent {

    private Task task;

    public TaskCompleteEvent(Object source, Task task) {
        super(source);
        this.task = task;
    }

    public Task getTask() {
        return task;
    }
}
