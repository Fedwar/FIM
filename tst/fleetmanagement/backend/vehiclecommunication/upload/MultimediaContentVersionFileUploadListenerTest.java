package fleetmanagement.backend.vehiclecommunication.upload;

import static org.junit.Assert.*;

import fleetmanagement.backend.packages.sync.PackageSyncService;
import org.junit.*;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MultimediaContentVersionFileUploadListenerTest {

    private TestScenario scenario;
    private MultimediaContentVersionFileUploadListener tested;
    private Vehicle vehicle;
    @Mock
    private PackageSyncService packageSyncService;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        scenario = new TestScenario();
        vehicle = scenario.addVehicle();
        tested = new MultimediaContentVersionFileUploadListener(scenario.vehicleRepository, packageSyncService);
    }

    @Test
    public void setsVersionFromUploadedText() {
        uploadVersion("V1.1");
        assertEquals("V1.1", vehicle.versions.get(PackageType.Indis5MultimediaContent).version);

        uploadVersion("");
        assertNull(vehicle.versions.get(PackageType.Indis5MultimediaContent));
    }

    @Test
    public void distinguishesBetweenIndis3AndIndis5BasedOnVehicleName() {
        vehicle = scenario.addVehicle("FAL 1234");
        uploadVersion("V1.1");
        assertEquals("V1.1", vehicle.versions.get(PackageType.Indis3MultimediaContent).version);

        vehicle = scenario.addVehicle("ENNO 1234");
        uploadVersion("V1.2");
        assertEquals("V1.2", vehicle.versions.get(PackageType.Indis5MultimediaContent).version);
    }

    @Test
    public void determinesIfFileCanBeHandled() {
        assertFalse(tested.canHandleUploadedFile("unknown-stuff.data"));
        assertTrue(tested.canHandleUploadedFile("multimedia-content-version.json"));
    }

    private void uploadVersion(String version) {
        String filename = "multimedia-content-version.json";
        String json = "{ \"multimedia-content-version\": \"" + version + "\"}";
        tested.onFileUploaded(vehicle, filename, json.getBytes());
    }

}
