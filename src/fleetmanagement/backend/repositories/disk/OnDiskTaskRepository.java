package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.disk.xml.TaskXmlFile;
import fleetmanagement.backend.repositories.disk.xml.TaskXmlFile.TaskXml;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.backend.repositories.exception.TaskDuplicateException;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.config.FimConfig;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

@Component
public class OnDiskTaskRepository implements TaskRepository {

    private static final Logger logger = Logger.getLogger(OnDiskTaskRepository.class);

    @Autowired
    protected PackageRepository packages;
    @Autowired
    @Setter
    private ApplicationEventPublisher eventPublisher;
    protected final List<Task> tasks = new CopyOnWriteArrayList<>();
    protected File tasksDirectory;

    @Autowired
    public OnDiskTaskRepository(FimConfig config) {
        this.tasksDirectory = config.getTasksDirectory();
    }

    protected OnDiskTaskRepository(File taskDirectory, PackageRepository packages) {
        this.tasksDirectory = taskDirectory;
        this.packages = packages;
    }

    protected XmlFile<Task> getXmlFile(File dir) {
        return new TaskXmlFile(dir);
    }

    @PostConstruct
    public void loadFromDisk() {
        logger.debug("Loading from disk: tasks");
        tasksDirectory.mkdirs();
        DeletionHelper.performPendingDeletes(tasksDirectory);

        for (File f : tasksDirectory.listFiles()) {
            try {
                loadTaskFromDirectory(f);
            } catch (Exception e) {
                logger.error("Unable to load task: " + f, e);
            }
        }
    }

    private void loadTaskFromDirectory(File f) {
        TaskXmlFile xml = new TaskXmlFile(f);
        TaskXml meta = xml.meta();
        Task t = toTask(meta);
        tasks.add(t);
    }

    @Override
    public synchronized void insert(Task t) {
        if (!existsInList(t)) {
            tasks.add(t);
            save(t);
        } else
            throw new TaskDuplicateException(t.getId());
    }

    @Override
    public synchronized void update(UUID id, Consumer<Task> update) {
        Task original = tryFindById(id);

        if (original != null) {
            Task cloned = original.clone();
            update.accept(cloned);
            tasks.remove(original);
            tasks.add(cloned);
            save(cloned);
        } else {
            update.accept(null);
        }
    }

    protected void save(Task t) {
        File taskDirectory = getTaskDirectory(t);
        taskDirectory.mkdirs();
        new TaskXmlFile(taskDirectory).save(t);
    }

    @Override
    public synchronized void delete(UUID id) {
        Task toRemove = tryFindById(id);
        if (toRemove != null) {
            tasks.remove(toRemove);
            delete(toRemove);
        }
    }

    protected void delete(Task task) {
        File taskDirectory = getTaskDirectory(task);
        DeletionHelper.delete(taskDirectory);
    }

    @Override
    public Task tryFindById(UUID id) {
        return getAllTasks().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public List<Task> getTasksByPackage(UUID packageId) {
        return getAllTasks().filter(x -> x.getPackage().id.equals(packageId)).collect(Collectors.toList());
    }

    @Override
    public List<Task> getTasksByVehicle(UUID vehicleId) {
        return getTasksStreamByVehicle(vehicleId).collect(Collectors.toList());
    }

    private Stream<Task> getTasksStreamByVehicle(UUID vehicleId) {
        return getAllTasks().filter(x -> x.getVehicleId().equals(vehicleId));
    }

    @Override
    public List<Task> getRunningTasksByPackage(UUID packageId) {
        return getAllTasks().filter(x -> x.getPackage().id.equals(packageId) && !x.isCompleted()).collect(Collectors.toList());
    }

    @Override
    public long getNumberOfRunningTasks(UUID packageId) {
        return getAllTasks().filter(x -> x.getPackage().id.equals(packageId) && !x.isCompleted()).count();
    }

    private File getTaskDirectory(Task t) {
        return new File(tasksDirectory, t.getId().toString());
    }

    private Task toTask(TaskXml meta) {
        Package pkg = packages.tryFindById(meta.packageId);

        if (pkg == null)
            throw new RuntimeException("Missing package " + meta.packageId + " for task " + meta.id);

        List<LogEntry> logs = meta.logs.stream().map(x -> x.toLogEntry()).collect(Collectors.toList());
        return new Task(meta.id, pkg, meta.vehicleId, meta.startedAt, meta.completedAt, meta.status.toTaskStatus(), logs, Clock.systemDefaultZone(), eventPublisher);
    }

    private Stream<Task> getAllTasks() {
        return tasks.stream();
    }

    private boolean existsInList(Task task) {
        return !tasks.stream().filter(t -> t.getId().equals(task.getId())).collect(Collectors.toList()).isEmpty();
    }

    @Override
    public Map<Integer, Task> latestTasksForEachSlot(Vehicle vehicle, PackageType packageType) {
        Map<Integer, List<Task>> slotTasks = getTasksStreamByVehicle(vehicle.id)
                .filter(x -> x.getPackage().type == packageType)
                .collect(groupingBy(x -> x.getPackage().slot));

        return slotTasks.entrySet().stream()
                .collect(Collectors.toMap(o -> o.getKey(), o -> o.getValue().stream()
                        .max(Comparator.comparing(
                                task -> (task.isCompleted() ? task.getCompletedAt() : ZonedDateTime.now())))
                        .orElse(null)));
    }

}
