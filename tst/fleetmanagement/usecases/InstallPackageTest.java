package fleetmanagement.usecases;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.exception.PackageTypeNotLicenced;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.test.TestScenarioPrefilled;
import fleetmanagement.usecases.InstallPackage.StartInstallationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static fleetmanagement.TestObjectFactory.createPackage;
import static fleetmanagement.TestObjectFactory.createVehicle;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InstallPackageTest {

    private InstallPackage tested;

    private Package package1 = createPackage(PackageType.CopyStick, "v1");
    private Package package2 = createPackage(PackageType.CopyStick, "v2");
    private Vehicle vehicle1 = createVehicle("vehicle1");

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private PackageRepository packageRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private Licence licence;

    @Before
    public void setup() throws Exception {
        when(licence.isPackageTypeAvailable(any())).thenReturn(true);
        when(vehicleRepository.tryFindById(vehicle1.id)).thenReturn(vehicle1);
        when(packageRepository.tryFindById(package2.id)).thenReturn(package2);
        tested = new InstallPackage(taskRepository, packageRepository, vehicleRepository, licence);
    }

    @After
    public void teardown() throws Exception {
    }

    @Test
    public void startsTasksForPackageInstallation() throws Exception {
        StartInstallationResult result = tested.startInstallation(package1,
                Arrays.asList(vehicle1), null);

        Task task = result.startedTasks.get(0);
        assertEquals(1, result.startedTasks.size());
        assertEquals(0, result.conflictingTasks.size());
        assertEquals(1, vehicle1.getTaskIds().size());
        assertFalse(task.isCancelled());
        assertNotNull(task.getId());
        assertEquals(package1, task.getPackage());
        assertEquals(ClientStage.PENDING, task.getStatus().clientStage);
        assertEquals(0, task.getStatus().percent);
    }

    @Test
    public void detectsConflictingTasksWhenStartingNewPackageInstallation() throws Exception {
        Task previous = simulateRunningInstallation(package1, vehicle1);

        StartInstallationResult result = tested.startInstallation(package2,
                Arrays.asList(vehicle1), null);

        assertEquals(1, result.startedTasks.size());
        assertEquals(1, result.conflictingTasks.size());
        assertTrue(result.conflictingTasks.contains(previous));
    }

    private Task simulateRunningInstallation(Package pkg, Vehicle vehicle) {
        Task t = tested.startInstallation(pkg, Collections.singletonList(vehicle), null).startedTasks.get(0);
        when(taskRepository.tryFindById(t.getId())).thenReturn(t);
        return t;
    }

    @Test
    public void doesNotStartInstallationOnVehiclesThatAreAlreadyInstalling() throws Exception {
        simulateRunningInstallation(package1, vehicle1);

        StartInstallationResult result = tested.startInstallation(package1,
                Arrays.asList(vehicle1), null);

        assertEquals(1, result.startedTasks.size());
        assertEquals(0, result.conflictingTasks.size());
    }

    @Test
    public void startingAnInstallationIsIdempodent() throws Exception {
        Task conflicting = simulateRunningInstallation(package1, vehicle1);
        Task previouslyStarted = simulateRunningInstallation(package2, vehicle1);

        StartInstallationResult result = tested.startInstallation(package2,
                Arrays.asList(vehicle1), null);

        assertEquals(1, result.conflictingTasks.size());
        assertTrue(result.conflictingTasks.contains(conflicting));

        assertEquals(1, result.startedTasks.size());
        assertTrue(result.startedTasks.contains(previouslyStarted));
    }

    @Test(expected = PackageTypeNotLicenced.class)
    public void installUnlicencedPackage() throws Exception {
        when(licence.isPackageTypeAvailable(any())).thenReturn(false);

        simulateRunningInstallation(package1, vehicle1);
    }

}