package fleetmanagement;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.tasks.TaskStatus;
import fleetmanagement.backend.vehicles.Vehicle;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

public final class TestUtils {

    private TestUtils() {
    }

    public static Map<String, Group> toGroupMap(Group... groups) {
        return Stream.of(groups).collect(Collectors.toMap(g -> g.id.toString(), g -> g));
    }

    public static Task simulateInstallation(Package p, Vehicle v, int progress, TaskRepository taskRepositoryMock) {
        return simulateInstallation(p, v, progress, null, taskRepositoryMock);
    }

    public static Task simulateInstallation(Package p, Vehicle v, int progress, ApplicationEventPublisher eventPublisher,
                                            TaskRepository taskRepositoryMock) {
        Task t = new Task(p, v, eventPublisher);
        t.setClientStatus(progress == 100 ? TaskStatus.ClientStage.FINISHED : TaskStatus.ClientStage.DOWNLOADING, progress);
        t.setServerStatus(progress == 100 ? TaskStatus.ServerStatus.Finished : TaskStatus.ServerStatus.Running);
        p.startInstallation(Collections.singletonList(t));
        v.addTask(t);
        when(taskRepositoryMock.tryFindById(t.getId())).thenReturn(t);
        return t;
    }

}
