package fleetmanagement.backend.vehiclecommunication.upload;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.preprocess.Preprocessor;
import fleetmanagement.backend.packages.sync.PackageSyncService;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus;
import org.apache.commons.io.FileUtils;
import org.junit.*;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class GenericPackageVersionTest {
	private TestScenario scenario;
	private GenericPackageVersion tested;
	private Vehicle vehicle;
	@Mock
	private PackageSyncService packageSyncService;
	
	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		scenario = new TestScenario();
		vehicle = scenario.addVehicle();
		tested = new GenericPackageVersion(scenario.vehicleRepository, packageSyncService);
	}
	
	@Test
	public void setsVersionFromUploadedText() {
		uploadVersion("V1.1");
		assertEquals("V1.1", vehicle.versions.get(PackageType.OebbDigitalContent).version);
		
		uploadVersion("");
		assertNull(vehicle.versions.get(PackageType.OebbDigitalContent));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void detectsUnknownPackageTypes() {
		String json = "{ \"type\": \"Unknown\", \"version\": \"1.0\"}";
		tested.onFileUploaded(vehicle, "package-version-digital-content.json", json.getBytes());
	}
	
	@Test
	public void determinesIfFileCanBeHandled() {
		assertFalse(tested.canHandleUploadedFile("unknown-stuff.data"));
		assertTrue(tested.canHandleUploadedFile("package-version-digital-content.json"));
	}
	
	private void uploadVersion(String version) {
		String filename = "package-version-digital-content.json";
		String json = "{ \"type\": \"OebbDigitalContent\", \"version\": \"" + version + "\"}";
		tested.onFileUploaded(vehicle, filename, json.getBytes());
	}

	@Test
	public void installsPackageIfVehiclePackageVersionIsDifferent() throws IOException, InterruptedException {
		Package pkg = scenario.addPackage(PackageType.OebbDigitalContent, "v2");
		addTask(vehicle, pkg, ZonedDateTime.now(), TaskStatus.ServerStatus.Finished);
		uploadVersion("v1");

		assertEquals(1, scenario.taskRepository.getTasksByVehicle(vehicle.id).size());
	}
	@Test
	public void autoSyncRuns() throws IOException, InterruptedException {
		Package pkg = scenario.addPackage(PackageType.OebbDigitalContent, "v2");
		addTask(vehicle, pkg, ZonedDateTime.now(), TaskStatus.ServerStatus.Finished);
		uploadVersion("v1");

		assertEquals(1, scenario.taskRepository.getTasksByVehicle(vehicle.id).size());
	}




	Task addTask(Vehicle vehicle, Package pkg,  ZonedDateTime completedAt, TaskStatus.ServerStatus status) {
		Task task = new Task(UUID.randomUUID(), pkg, vehicle.id, null, completedAt, new TaskStatus(), Collections.emptyList(), null );
		task.setServerStatus(status);
		scenario.taskRepository.insert(task);
		return task;
	}

}
