package fleetmanagement.backend.vehiclecommunication.upload;

import static org.junit.Assert.*;

import org.junit.*;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus.*;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;

public class TaskStatusFileUploadListenerTest {

	private TestScenario scenario;
	private TaskStatusFileUploadListener tested;
	private Vehicle vehicle;
	private Package pkg;
	private Task task;
	
	@Before
	public void setup() throws Exception {
		scenario = new TestScenario();
		vehicle = scenario.addVehicle();
		pkg = scenario.addPackage(PackageType.DataSupply, "1.0", 1,  "08.09.2013 00:00:00", "01.12.2013 23:59:59");
		task = scenario.addTask(vehicle, pkg);
		tested = new TaskStatusFileUploadListener(scenario.taskRepository, scenario.vehicleRepository, null);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void setsTaskStatusFromUploadedJson() {
		task = uploadTaskJson("{'progress': {'percent': 54, 'stage': 'DOWNLOADING'}}");

		assertEquals(ClientStage.DOWNLOADING, task.getStatus().clientStage);
		assertEquals(54, task.getStatus().percent);
	}
	
	@Test
	public void marksTaskAsFinished() {
		task = uploadTaskJson("{'progress': {'percent': '100', 'stage': 'FINISHED'}}");
		
		assertEquals(ServerStatus.Finished, task.getStatus().serverStatus);
	}
	
	@Test
	public void marksTaskAsCancelled() {
		task.cancel();
		task = uploadTaskJson("{'progress': {'percent': '100', 'stage': 'CANCELLED'}}");
		
		assertEquals(ServerStatus.Cancelled, task.getStatus().serverStatus);
	}
	
	@Test
	public void marksTaskAsFailedForLegacyFailMessagesWithStatusCancelled() {
		task = uploadTaskJson("{'progress': {'percent': '100', 'stage': 'CANCELLED'}}");
		
		assertEquals(ServerStatus.Failed, task.getStatus().serverStatus);
	}
	
	@Test
	public void recognizesInitialStageNameWhichIsDifferentInServerAndClient() {
		task = uploadTaskJson("{'progress': {'percent': '1', 'stage': 'INITIAL'}}");
		
		assertEquals(ClientStage.INITIALIZING, task.getStatus().clientStage);
	}
	
	@Test
	public void addsLogEntryWhenStatusChanges() {
		int initialLogCount = task.getLogMessages().size();

		task = uploadTaskJson("{'progress': {'percent': 54, 'stage': 'DOWNLOADING'}}");
		
		assertEquals(initialLogCount + 1, task.getLogMessages().size());

		task = uploadTaskJson("{'progress': {'percent': 54, 'stage': 'DOWNLOADING'}}");
		
		assertEquals(initialLogCount + 1, task.getLogMessages().size());
	}
	
	@Test
	public void determinesIfFileCanBeHandlesd() {
		assertFalse(tested.canHandleUploadedFile("unknown-stuff.data"));
		assertTrue(tested.canHandleUploadedFile("task-status_" + task.getId() + ".json"));
	}
	
	private Task uploadTaskJson(String json) {
		String filename = "task-status_" + task.getId() + ".json";
		tested.onFileUploaded(vehicle.id, filename, json.getBytes());
		return scenario.taskRepository.tryFindById(task.getId());
	}
}
