package fleetmanagement.backend.vehiclecommunication.upload;

import static org.junit.Assert.*;

import fleetmanagement.backend.packages.sync.PackageSyncService;
import org.junit.*;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Objects;

public class DataSupplyVersionFileUploadListenerTest {

	private TestScenario scenario;
	private DataSupplyVersionFileUploadListener tested;
	private Vehicle vehicle;
	@Mock
	private PackageSyncService packageSyncService;

	@Before
	public void setup() throws Exception {
		MockitoAnnotations.initMocks(this);
		scenario = new TestScenario();
		vehicle = scenario.addVehicle();
		tested = new DataSupplyVersionFileUploadListener(
				scenario.vehicleRepository, packageSyncService);
	}

	@Test
	public void setsDvStatusFromUploadedJson() {
		uploadTaskJson("{'data-supply-status': [{'slot': '1', 'version': 'Slot1'}, {'slot': '2', 'version': 'Slot2'}]}");
		
		assertEquals("Slot1", vehicle.versions.getDataSupplyVersion(1));
		assertEquals("Slot2", vehicle.versions.getDataSupplyVersion(2));
		
		uploadTaskJson("{'data-supply-status': []}");
		
		assertNull(vehicle.versions.getDataSupplyVersion(1));
		assertNull(vehicle.versions.getDataSupplyVersion(2));
	}

	@Test
	public void doesNotOverwriteOtherVersions() {
		vehicle.versions.set(PackageType.Indis3MultimediaContent, "1.0");
		uploadTaskJson("{'data-supply-status': []}");
		assertEquals("1.0", vehicle.versions.get(PackageType.Indis3MultimediaContent).version);
	}

	@Test
	public void determinesIfFileCanBeHandled() {
		assertFalse(tested.canHandleUploadedFile("unknown-stuff.data"));
		assertTrue(tested.canHandleUploadedFile("dv-status.json"));
	}


	private void uploadTaskJson(String json) {
		String filename = "dv-status.json";
		tested.onFileUploaded(vehicle, filename, json.getBytes());
	}
}
