package fleetmanagement.backend.installations;

import fleetmanagement.backend.repositories.Persistable;
import fleetmanagement.backend.tasks.Task;
import gsp.util.DoNotObfuscate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@DoNotObfuscate
public class PackageInstallation implements Persistable<UUID> {

    @Id
    private UUID id;
    @Column
    private String taskIds;
    @Column
    private Date startDatetime;
    @Column
    private Date endDatetime;
    @Transient
    private List<Task> conflictingTasks = new ArrayList<>();

    public PackageInstallation() {
    }

    public PackageInstallation(UUID id) {
        this.id = id;
    }

    public PackageInstallation(UUID id, List<Task> tasks) {
        this.id = id;
        setTasks(tasks);
    }

    @Override
    public UUID id() {
        return id;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(String taskIds) {
        this.taskIds = taskIds;
    }

    public List<UUID> getTasks() {
        return Stream.of(getTaskIds().split(","))
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    void setTasks(List<Task> tasks) {
        setTaskIds(compact(tasks));
    }

    public void addTasks(List<Task> tasks) {
        if (this.taskIds == null || this.taskIds.isEmpty()) {
            setTasks(tasks);
        } else {
            setTaskIds(getTaskIds() + "," + compact(tasks));
        }
    }

    private String compact(List<Task> tasks) {
        return tasks.stream().map(Task::getId).map(UUID::toString).collect(Collectors.joining(","));
    }

    public Date getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(Date startDatetime) {
        this.startDatetime = startDatetime;
    }

    public Date getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(Date endDatetime) {
        this.endDatetime = endDatetime;
    }

    public List<Task> getConflictingTasks() {
        return conflictingTasks;
    }

    public void setConflictingTasks(List<Task> conflictingTasks) {
        this.conflictingTasks = conflictingTasks;
    }

    @Override
    public PackageInstallation clone() {
        try {
            return (PackageInstallation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
