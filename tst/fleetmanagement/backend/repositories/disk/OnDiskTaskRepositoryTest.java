package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.exception.TaskDuplicateException;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.vehicles.Vehicle;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static fleetmanagement.TestObjectFactory.createPackage;
import static fleetmanagement.TestObjectFactory.createVehicle;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OnDiskTaskRepositoryTest {

    private OnDiskTaskRepository tested;
    private OnDiskTaskRepository toRead;
    
    private Package package1 = createPackage(PackageType.DataSupply, "v1");
    private Package package2 = createPackage(PackageType.CopyStick, "v1");
    private Vehicle vehicle1 = createVehicle("vehicle1");
    private Vehicle vehicle2 = createVehicle("vehicle2");

    @Mock
    private PackageRepository packageRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Rule
    public TempFileRule tempDir = new TempFileRule();


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        tested = new OnDiskTaskRepository(tempDir, packageRepository);
        tested.setEventPublisher(eventPublisher);
        toRead = new OnDiskTaskRepository(tempDir, packageRepository);
        toRead.setEventPublisher(eventPublisher);
    }

    @Test
    public void listsTasksByPackage() {
        Task task1 = new Task(package1, vehicle1, null);
        Task task2 = new Task(package1, vehicle2, null);
        Task irrelevantTask = new Task(package2, vehicle1, null);
        tested.insert(task1);
        tested.insert(task2);
        tested.insert(irrelevantTask);

        assertEquals(Arrays.asList(task1, task2), tested.getTasksByPackage(package1.id));
    }

    @Test
    public void findsTaskById() {
        Task task = new Task(package1, vehicle1, null);
        tested.insert(task);

        assertEquals(task, tested.tryFindById(task.getId()));
        assertNull(tested.tryFindById(UUID.randomUUID()));
    }

    @Test
    public void collectsTasks() {
        Task t = new Task(package1, vehicle1, null);

        tested.insert(t);

        assertEquals(t, tested.tryFindById(t.getId()));
    }

    @Test(expected = TaskDuplicateException.class)
    public void doesNotStoreDuplicateTasks() {
        Task t = new Task(package1, vehicle1, null);

        tested.insert(t);
        tested.insert(t);
    }

    @Test
    public void survivesNonExistingTaskDirectory() throws IOException {
        File nonExistingDirectory = new File("non-existing");
        try {
            OnDiskTaskRepository tested = new OnDiskTaskRepository(nonExistingDirectory, packageRepository);
            tested.loadFromDisk();
        } finally {
            FileUtils.deleteDirectory(nonExistingDirectory);
        }
    }

    @Test
    public void storesTasksOnDisk() {
        Task t = new Task(package1, vehicle1, null);
        t.setClientStatus(ClientStage.DOWNLOADING, 50);
        t.addLog(new LogEntry(Severity.WARNING, "Hallo Welt"));
        tested.insert(t);

        when(packageRepository.tryFindById(package1.id)).thenReturn(package1);
        toRead.loadFromDisk();

        Task loaded = toRead.tryFindById(t.getId());
        assertNotNull(loaded);
        assertEquals(t.getId(), loaded.getId());
        assertEquals(t.getPackage().id, loaded.getPackage().id);
        assertEquals(t.getVehicleId(), loaded.getVehicleId());
        assertEquals(t.getStartedAt(), loaded.getStartedAt());
        assertEquals(t.getCompletedAt(), loaded.getCompletedAt());
        assertEquals(t.getStatus(), loaded.getStatus());
        assertEquals(t.getLogMessages(), loaded.getLogMessages());
        assertEquals(eventPublisher,loaded.getEventPublisher());
    }

    @Test
    public void deletesTasksFromDisk() {
        Task t = new Task(package1, vehicle1);
        File taskDir = new File(tempDir, t.getId().toString());

        tested.insert(t);
        assertTrue(taskDir.exists());

        tested.delete(t.getId());
        assertFalse(taskDir.exists());
    }

    @Test
    public void updatesTasksInMemory() throws IOException {
        Task t = new Task(package1, vehicle1);
        tested.insert(t);

        tested.update(t.getId(), updated -> {
            updated.cancel();
        });

        assertFalse(t.isCancelled());
        assertTrue(tested.tryFindById(t.getId()).isCancelled());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doesNotCrashWhenModifyingNonExistantTask() throws IOException {
        Consumer<Task> updateCall = mock(Consumer.class);

        tested.update(UUID.randomUUID(), updateCall);

        verify(updateCall).accept(null);
    }

    @Test
    public void latestTasks_ReturnsAllSlots() throws IOException {
        addTask(0);
        addTask(2);
        addTask(5);
        Map<Integer, Task> slotMap = tested.latestTasksForEachSlot(vehicle1, PackageType.DataSupply);

        assertEquals(3, slotMap.size());
        assertNotNull(slotMap.get(0));
        assertNotNull(slotMap.get(2));
        assertNotNull(slotMap.get(5));
    }

    @Test
    public void latestTasks_ReturnsSpecifiedVehicleTasks() throws IOException {
        addTask(0, vehicle1);
        addTask(2, vehicle2);
        addTask(5, vehicle1);
        Map<Integer, Task> slotMap = tested.latestTasksForEachSlot(vehicle1, PackageType.DataSupply);

        assertEquals(2, slotMap.size());
        assertNotNull(slotMap.get(0));
        assertNotNull(slotMap.get(5));
    }

    @Test
    public void ifTaskNotCompleted_ThenItIsLatest() throws IOException {
        addTask(0, ZonedDateTime.now().minusSeconds(1), TaskStatus.ServerStatus.Finished);
        Task expected = addTask(0, null, null);
        Map<Integer, Task> slotMap = tested.latestTasksForEachSlot(vehicle1, PackageType.DataSupply);

        assertEquals(expected.getId(), slotMap.get(0).getId());
    }

    @Test
    public void returnsLatestTask() throws IOException {
        addTask(0, ZonedDateTime.now().minusDays(1), TaskStatus.ServerStatus.Finished);
        addTask(0, ZonedDateTime.now().minusHours(1), TaskStatus.ServerStatus.Finished);
        Task expected = addTask(0, ZonedDateTime.now(), TaskStatus.ServerStatus.Finished);
        Map<Integer, Task> slotMap = tested.latestTasksForEachSlot(vehicle1, PackageType.DataSupply);

        assertSame(slotMap.get(0), expected);
    }

    void addTask(Integer slot, Vehicle vehicle) {
        Package pkg = createPackage(PackageType.DataSupply, "", slot, "", "");
        tested.insert(new Task(pkg, vehicle, null));
    }

    void addTask(Integer slot) {
        addTask(slot, vehicle1);
    }

    Task addTask(Integer slot, ZonedDateTime completedAt, TaskStatus.ServerStatus status) {
        Package pkg = createPackage(PackageType.DataSupply, "", slot, "", "");
        Task task = new Task(UUID.randomUUID(), pkg, vehicle1.id, null, completedAt, new TaskStatus(), Collections.emptyList(), null , null);
        task.setServerStatus(status);
        tested.insert(task);
        return task;
    }


    private FileOutputStream open(File file) throws FileNotFoundException {
        return new FileOutputStream(file);
    }


    @Test
    public void noException_WhenXmlFileIsEmpty() throws IOException {
        TempFile folder = tempDir.newFolder(UUID.randomUUID().toString());
        String xmlFile = tested.getXmlFile(folder).file().getName();
        folder.newFile(xmlFile);

        tested.loadFromDisk();
    }

    @Test
    public void loosesTaskCompletionWhenTaskIsLoadFromDisk() {
        Clock c = mock(Clock.class);
        Task t = new Task(UUID.randomUUID(), package1, vehicle1.id, ZonedDateTime.now(), null,
                new TaskStatus(), new ArrayList<>(), c, eventPublisher);
        when(c.instant()).thenReturn(Instant.now().minus(5, ChronoUnit.SECONDS));
        t.setClientDownloading();
        when(c.instant()).thenReturn(Instant.now() );
        t.setClientStatus(ClientStage.DOWNLOADING, 10);

        assertNotNull(t.getEstimatedCompletionDate());

        tested.insert(t);

        when(packageRepository.tryFindById(package1.id)).thenReturn(package1);
        toRead.loadFromDisk();
        Task loaded = toRead.tryFindById(t.getId());

        assertNull(loaded.getEstimatedCompletionDate());
    }

}
