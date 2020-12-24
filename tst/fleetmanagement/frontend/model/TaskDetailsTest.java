package fleetmanagement.frontend.model;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.*;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.*;

public class TaskDetailsTest {
	
	private TaskDetails tested;
	private Vehicle vehicle;
	private fleetmanagement.backend.tasks.Task task;
	private SessionStub request;

	@Before
	public void setUp() throws IOException {
		TestScenario scenario = new TestScenario();
		request = new SessionStub();
		Package pkg = scenario.addPackage(PackageType.DataSupply, "1.0", 1,  "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		vehicle = scenario.addVehicle();
		task = scenario.addTask(vehicle, pkg);

		tested = new TaskDetails(task, vehicle, request);
	}
	
	@Test
	public void fillsFields() {
		assertEquals(task.getId().toString(), tested.id);
		assertEquals(vehicle.id.toString(), tested.vehicleId);
		assertEquals(vehicle.getName(), tested.vehicleDescription);
		assertEquals("Data Supply 1.0 (Slot 1)", tested.packageDescription);
	}
	
	@Test 
	public void convertsLogMessages() {
		task.addLog(new LogEntry(Severity.INFO, "bla"));
		task.addLog(new LogEntry(Severity.ERROR, "blubb"));
		
		tested = new TaskDetails(task, vehicle, request);
		
		assertEquals("Info", tested.logs.get(1).severityText);
		assertEquals("bla", tested.logs.get(1).message);
		assertFalse(tested.logs.get(1).dateTime.isEmpty());
		
		assertEquals("Error", tested.logs.get(2).severityText);
		assertEquals("blubb", tested.logs.get(2).message);
		assertFalse(tested.logs.get(2).dateTime.isEmpty());
	}

	
}