package fleetmanagement.usecases;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.vehicles.LiveInformation;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleVersions.Versioned;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.model.Name;
import fleetmanagement.frontend.model.VehicleDetails;
import fleetmanagement.frontend.model.VehicleDetails.ComponentVersion;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ListVehicleDetailsTest {

    private VehicleDetails tested;
    private TestScenarioPrefilled scenario;
    private SessionStub request;

    @Before
    public void setup() {
        request = new SessionStub();
        scenario = new TestScenarioPrefilled();
        scenario.vehicle1.setGroupId(scenario.group1.id.toString());
    }

    @Test
    public void fillsVehicleProperties() {
        createTested();

        assertEquals(scenario.vehicle1.id.toString(), tested.id);
        assertEquals(scenario.vehicle1.lastSeen, tested.lastSeen);
        assertEquals(scenario.vehicle1.getName(), tested.name);
        assertEquals(scenario.vehicle1.uic, tested.uic);
        assertEquals(scenario.vehicle1.clientVersion, tested.clientVersion);
        assertNull(tested.diagnosticError);
        assertEquals(1, tested.groupsForAssigning.entrySet().size());
    }

    @Test
    public void fillsAdditionalUic() {
        Vehicle v1 = scenario.addVehicleWithAdditionalUic("v1", "additional_uic");
        createTested(v1);

        assertEquals("additional_uic", tested.additional_uic);
    }

    @Test
    public void fillsEmptyString_WhenAdditionalUicIsNull() {
        createTested(scenario.vehicle1);

        assertNull( scenario.vehicle1.additional_uic);
        assertEquals("", tested.additional_uic);
    }

    @Test
    public void fillsAllVehicleVersions() {
        Versioned dataSupplySlot1 = new Versioned(PackageType.DataSupply, 1, "1.0");
        scenario.vehicle1.versions.add(dataSupplySlot1);
        Package content = scenario.addPackage(PackageType.Indis5MultimediaContent, "1.1");
        scenario.vehicle1.versions.set(PackageType.Indis5MultimediaContent, "1.1");
        createTested();

        ComponentVersion version1 = tested.versions.get(0);
        assertEquals(Name.of(dataSupplySlot1, request), version1.component);
        assertEquals("1.0", version1.version);
        assertEquals(scenario.package1.id.toString(), version1.packageId);

        ComponentVersion contentVersion = tested.versions.get(1);
        assertEquals(Name.of(new Versioned(PackageType.Indis5MultimediaContent, ""), request), contentVersion.component);
        assertEquals("1.1", contentVersion.version);
        assertEquals(content.id.toString(), contentVersion.packageId);
    }

    @Test
    public void addsMessage_WhenNoDataSupplyInstalled() {
        createTested();

        ComponentVersion version1 = tested.versions.get(0);
        assertEquals(I18n.get(request, "vehicle_no_data_supply_installed"), version1.version);
    }

    @Test
    public void handlesDataSupplyInWrongSlot() {
        Versioned dvSlot1 = new Versioned(PackageType.DataSupply, 1, "1.0");
        Versioned dvSlot2 = new Versioned(PackageType.DataSupply, 2, "1.0");
        Versioned dvSlot3 = new Versioned(PackageType.DataSupply, 3, "1.3");
        scenario.vehicle1.versions.add(dvSlot1);
        scenario.vehicle1.versions.add(dvSlot2);
        scenario.vehicle1.versions.add(dvSlot3);

        createTested();

        ComponentVersion version1 = tested.versions.get(0);
        assertEquals(Name.of(dvSlot1, request), version1.component);
        assertEquals("1.0", version1.version);
        assertEquals(scenario.package1.id.toString(), version1.packageId);

        ComponentVersion version2 = tested.versions.get(1);
        assertEquals(Name.of(dvSlot2, request), version2.component);
        assertEquals("1.0", version2.version);
        assertNull(version2.packageId);

        ComponentVersion version3 = tested.versions.get(2);
        assertEquals(Name.of(dvSlot3, request), version3.component);
        assertEquals("1.3", version3.version);
        assertNull(version3.packageId);
    }

    @Test
    public void fillsRunningTaskProperties() {
        Task update = scenario.addTask(scenario.vehicle1, scenario.package1);
        update.setClientStatus(ClientStage.DOWNLOADING, 12);

        createTested();

        VehicleDetails.RunningTask t = tested.runningTasks.get(0);
        assertEquals(1, tested.runningTasks.size());
        assertEquals(update.getId().toString(), t.taskId);
        assertEquals(scenario.package1.id.toString(), t.packageId);
        assertEquals("Data Supply 1.0 (Slot 1)", t.packageName);
        assertEquals(12, t.progress);
        assertEquals("Transferring files", t.status);
        assertTrue(t.taskWasStartedOnVehicle);
        assertEquals(update.getStartedAt(), t.startDate);
    }

    @Test
    public void fillsFinishedTaskProperties() {
        Task update = scenario.addTask(scenario.vehicle1, scenario.package1);
        update.setClientStatus(ClientStage.FINISHED, 100);

        createTested();

        VehicleDetails.CompletedTask t = tested.completedTasks.get(0);
        assertEquals(1, tested.completedTasks.size());
        assertEquals(update.getId().toString(), t.taskId);
        assertEquals(scenario.package1.id.toString(), t.packageId);
        assertEquals("Data Supply 1.0 (Slot 1)", t.packageName);
        assertEquals("Finished", t.status);
        assertNotNull(t.completionDate);
        assertEquals("Finished", t.statusCssClass);
    }

    @Test
    public void fillsCancelledTaskProperties() {
        Task update = scenario.addTask(scenario.vehicle1, scenario.package1);
        update.cancel();
        update.setClientStatus(ClientStage.CANCELLED, 50);

        createTested();

        VehicleDetails.CompletedTask t = tested.completedTasks.get(0);
        assertEquals("Cancelled at 50%", t.status);
        assertNotNull(t.completionDate);
        assertEquals("Cancelled", t.statusCssClass);
    }

    @Test
    public void fillsLegacyFailedTaskProperties() {
        Task update = scenario.addTask(scenario.vehicle1, scenario.package1);
        update.setClientStatus(ClientStage.CANCELLED, 50);

        createTested();

        VehicleDetails.CompletedTask t = tested.completedTasks.get(0);
        assertEquals("Failed at 50%", t.status);
        assertNotNull(t.completionDate);
        assertEquals("Failed", t.statusCssClass);
    }

    @Test
    public void fillsFailedTaskProperties() {
        Task update = scenario.addTask(scenario.vehicle1, scenario.package1);
        update.setClientStatus(ClientStage.FAILED, 50);

        createTested();

        VehicleDetails.CompletedTask t = tested.completedTasks.get(0);
        assertEquals("Failed at 50%", t.status);
        assertNotNull(t.completionDate);
        assertEquals("Failed", t.statusCssClass);
    }

    @Test
    public void estimatesCompletionIsEmptyIfNoEstimateIsAvailable() {
        scenario.addTask(scenario.vehicle1, scenario.package1);

        createTested();

        VehicleDetails.RunningTask t = tested.runningTasks.get(0);
        assertEquals("unknown", t.estimatedCompletion);
    }

    @Test
    public void translatesEstimatedCompletionIntoWords() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(Instant.now().minus(2, ChronoUnit.HOURS));
        Task task = new Task(UUID.randomUUID(), scenario.package1, scenario.vehicle1.id, ZonedDateTime.now(), null, new TaskStatus(), new ArrayList<>(), clock);
        scenario.taskRepository.insert(task);
        scenario.vehicle1.addTask(task);

        task.setClientStatus(ClientStage.DOWNLOADING, 0);
        when(clock.instant()).thenReturn(Instant.now());
        task.setClientStatus(ClientStage.DOWNLOADING, 50);

        createTested();

        VehicleDetails.RunningTask t = tested.runningTasks.get(0);
        assertEquals("2 hours from now", t.estimatedCompletion);
    }

    @Test
    public void fillsRouteInformation() {
        ZonedDateTime yesterday = ZonedDateTime.now().minusDays(1);
        scenario.vehicle1.liveInformation = new LiveInformation(null, "Berlin", "M�nchen", "RE", "1234", Collections.emptyList(), yesterday);
        createTested();
        assertEquals("RE 1234: Berlin - M�nchen", tested.routeInformation);
        assertEquals("Updated: 1 day ago", tested.lastLiveInfoUpdate);
    }

    @Test
    public void fillsPartialRouteInformation() {
        ZonedDateTime now = ZonedDateTime.now();
        scenario.vehicle1.liveInformation = new LiveInformation(null, null, "M�nchen", "RE", "1234", Collections.emptyList(), now);
        createTested();
        assertEquals("RE 1234: M�nchen", tested.routeInformation);
    }

    @Test
    public void ignoresIncompleteRouteInformation() {
        ZonedDateTime now = ZonedDateTime.now();
        scenario.vehicle1.liveInformation = new LiveInformation(null, null, "M�nchen", "RE", null, Collections.emptyList(), now);
        createTested();
        assertNull(tested.routeInformation);
    }

    @Test
    public void handlesMissingRouteInformation() {
        scenario.vehicle1.liveInformation = null;
        createTested();
        assertNull(tested.routeInformation);
        assertNull(tested.latitude);
        assertNull(tested.longitude);
    }

    @Test
    public void fillsPosition() {
        ZonedDateTime yesterday = ZonedDateTime.now().minusDays(1);
        scenario.vehicle1.liveInformation = new LiveInformation(new LiveInformation.Position(3.6758, 7.546), null, null, null, null, Collections.emptyList(), yesterday);
        createTested();
        assertEquals("3.6758", tested.latitude);
        assertEquals("7.546", tested.longitude);
    }

    @Test
    public void showsMessageWhenNoDataSupplyOnVehicle() {
        createTested();
        List<ComponentVersion> versions = getVersions(PackageType.DataSupply);

        assertEquals(1, versions.size());
        assertEquals(I18n.get(request, "vehicle_no_data_supply_installed"), versions.get(0).version);
    }

    @Test
    public void noDataSupplyMessageIsHidden_ifPackageTypeNotLicenced() {
        scenario.licence.availablePackageTypes.clear();
        createTested();
        List<ComponentVersion> versions = getVersions(PackageType.DataSupply);

        assertEquals(0, versions.size());
    }

    @Test
    public void showsInstalledDataSupplies_WhenPackageTypeNotLicenced() {
        scenario.licence.availablePackageTypes.clear();
        scenario.vehicle1.versions.setDataSupplyVersion(1, "1.0");
        createTested();
        List<ComponentVersion> versions = getVersions(PackageType.DataSupply);

        assertEquals(1, versions.size());
        assertEquals("1.0", versions.get(0).version);
    }



    @Test
    public void doesNotShowProgressBarUntilClientActuallyStartedATask() {
        scenario.addTask(scenario.vehicle1, scenario.package1);

        createTested();

        VehicleDetails.RunningTask t = tested.runningTasks.get(0);
        assertFalse(t.taskWasStartedOnVehicle);
    }

    private void createTested() {
        createTested(scenario.vehicle1);
    }
    private void createTested(Vehicle vehicle) {
        tested = new ListVehicleDetails(scenario.groupRepository, scenario.packageRepository, scenario.taskRepository,
                scenario.licence).listVehicleDetails(vehicle, request);
    }

    private List<ComponentVersion> getVersions(PackageType type) {
        return tested.versions.stream()
                .filter(v -> v.component.equals(Name.of(type, request))).collect(Collectors.toList());
    }



}
