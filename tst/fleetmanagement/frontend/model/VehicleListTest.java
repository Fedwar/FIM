package fleetmanagement.frontend.model;

import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.backend.diagnosis.DeviceSnapshot.StateSnapshot;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.tasks.TaskStatus.ServerStatus;
import fleetmanagement.backend.vehicles.LiveInformation;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.*;

public class VehicleListTest {

	private TestScenarioPrefilled scenario;
	private Vehicle v1;
	private fleetmanagement.frontend.model.VehicleList.Vehicle renderedVehicle;
	private SessionStub request;
	private SnapshotConversionService snapshotConversionService;

	@Mock
	private DiagnosisHistoryRepository diagnosisHistoryRepository;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		request = new SessionStub();
		scenario = new TestScenarioPrefilled();
		v1 = scenario.vehicle1;
		snapshotConversionService = new SnapshotConversionService(scenario.diagnosisRepository, diagnosisHistoryRepository, scenario.vehicleRepository);
	}
	
	@Test
	public void returnsEmptyStringForVehicleWithoutDatasupply() {
		renderVehicleList(scenario.vehicle1);
		
		assertEquals("", renderedVehicle.dataSupplyVersions);
	}
	
	@Test
	public void concatenatesVersionsOfMultipleDatasupplies() {
		v1.versions.setDataSupplyVersion(1, "v1");
		v1.versions.setDataSupplyVersion(2, "v2");
		
		renderVehicleList(v1);
		
		assertEquals("v1 & v2", renderedVehicle.dataSupplyVersions);
	}
	
	@Test
	public void containsNextUnfinishedTaskIfAny() {
		renderVehicleList(v1);
		assertEquals("", renderedVehicle.runningTaskName);
		
		Task t1 = scenario.addTask(v1, scenario.package1);
		t1.setClientStatus(ClientStage.DOWNLOADING, 25);
		renderVehicleList(v1);
		assertEquals("Data Supply 1.0 (Slot 1)", renderedVehicle.runningTaskName);
		assertEquals(25, renderedVehicle.runningTaskProgress);
		
		t1.setServerStatus(ServerStatus.Finished);
		renderVehicleList(v1);
		assertEquals("", renderedVehicle.runningTaskName);
	}
	
	@Test
	public void reportsConnectionOk() {
		simulateRecentConnection(v1);
		
		renderVehicleList(v1);
		
		assertEquals("connection-ok", renderedVehicle.connectionStatusCssClass);
	}
	
	@Test
	public void reportsConnectionUnstable() {
		simulateUnstableConnection(v1);
		
		renderVehicleList(v1);
		
		assertEquals("connection-unstable", renderedVehicle.connectionStatusCssClass);
	}
	
	@Test
	public void reportsConnectionLost() {
		simulateLostConnection(v1);		
		
		renderVehicleList(v1);
		
		assertEquals("connection-lost", renderedVehicle.connectionStatusCssClass);
	}
	
	@Test
	public void displaydiagnosticError() {
		simulateDiagnosticError(v1);

		renderVehicleList(v1);
		
		assertTrue(renderedVehicle.showDiagnosticErrorHint);
	}

	@Test
	public void displaysHumanReadableTimespanSinceLastCommunication() {
		simulateLostConnection(v1);

		renderVehicleList(v1);

		assertEquals("3 days ago", renderedVehicle.timeOfLastCommunication);
	}

	@Test
	public void hideMapLinkWhenNotLicenced() {
		scenario.licence.mapAvailable = true;
		scenario.vehicle1.liveInformation = new LiveInformation(new LiveInformation.Position(12.3, 45.6), "Berlin Hbf", "M�nchen Hbf", "RE", "12345", new ArrayList<>(), null);

		scenario.licence.mapAvailable = false;
		VehicleList tested = new VehicleList(Collections.singletonList(scenario.vehicle1), new HashMap<String, Group>(),
				scenario.taskRepository, request, scenario.licence, null);

		assertFalse("map link visible, but not licenced", tested.showMapLink);
	}

	private void simulateUnstableConnection(Vehicle v) {
		v.lastSeen = ZonedDateTime.now().minusMinutes(11);
	}

	private void simulateLostConnection(Vehicle v) {
		v.lastSeen = ZonedDateTime.now().minusDays(3);
	}

	private void simulateRecentConnection(Vehicle v) {
		v.lastSeen = ZonedDateTime.now();
	}

	private void simulateDiagnosticError(Vehicle v) {
		Diagnosis d = scenario.diagnosisRepository.tryFindByVehicleId(v.id);
		Snapshot snapshot = new Snapshot(v.id, 1, ZonedDateTime.now(), Arrays.asList(
				new DeviceSnapshot("id", "location", "name", "type",
						new VersionInfo(), new StateSnapshot("Error", "-1", ErrorCategory.FATAL)))
		);
		snapshotConversionService.integrateNewSnapshot(snapshot);
		v.updateDiagnosticSummary(d);
	}

	private void renderVehicleList(Vehicle vehicle) {
		VehicleList tested = new VehicleList(Collections.singletonList(vehicle), new HashMap<String, Group>(),
				scenario.taskRepository, request, scenario.licence, null);
		renderedVehicle = tested.getVehicles().get(0);
	}
}
