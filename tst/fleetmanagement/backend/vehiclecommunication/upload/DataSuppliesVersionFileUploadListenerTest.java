package fleetmanagement.backend.vehiclecommunication.upload;

import static org.junit.Assert.*;

import fleetmanagement.backend.packages.sync.PackageSyncService;
import org.junit.*;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class DataSuppliesVersionFileUploadListenerTest {

    private TestScenario scenario;
    private DataSuppliesVersionFileUploadListener tested;
    private Vehicle vehicle;
    @Mock
    private PackageSyncService packageSyncService;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        scenario = new TestScenario();
        vehicle = scenario.addVehicle();
        tested = new DataSuppliesVersionFileUploadListener(
                scenario.vehicleRepository, packageSyncService);
    }

    @Test
    public void setsDvStatusFromUploadedJson() {
        uploadTaskJson("{'data_supplies': [" +
                "{'version': '23.1853', 'creation_date': '2018-04-20T11:00:20Z', 'slot': '1'}," +
                " {'version': '23.1902', 'creation_date': '2018-11-20T11:00:20Z', 'slot': '2'}" +
                "]}");

        assertEquals("23.1853", vehicle.versions.getDataSupplyVersion(1));
        assertEquals("23.1902", vehicle.versions.getDataSupplyVersion(2));

        uploadTaskJson("{'data_supplies': []}");

        assertNull(vehicle.versions.getDataSupplyVersion(1));
        assertNull(vehicle.versions.getDataSupplyVersion(2));
    }

    @Test
    public void setsDvEmptyStatusFromUploadedJson() {
        uploadTaskJson("{'data_supplies': [" +
                "{'slot': '1'}," +
                " {'slot': '2'}" +
                "]}");

        assertNull(vehicle.versions.getDataSupplyVersion(1));
        assertNull(vehicle.versions.getDataSupplyVersion(2));
    }

    @Test
    public void doesNotOverwriteOtherVersions() {
        vehicle.versions.set(PackageType.Indis3MultimediaContent, "1.0");
        uploadTaskJson("{'data_supplies': []}");
        assertEquals("1.0", vehicle.versions.get(PackageType.Indis3MultimediaContent).version);
    }

    @Test
    public void determinesIfFileCanBeHandled() {
        assertFalse(tested.canHandleUploadedFile("unknown-stuff.data"));
        assertTrue(tested.canHandleUploadedFile("dv-status-fis-api.json"));
    }

    private void uploadTaskJson(String json) {
        String filename = "dv-status-fis-api.json";
        tested.onFileUploaded(vehicle, filename, json.getBytes());
    }
}
