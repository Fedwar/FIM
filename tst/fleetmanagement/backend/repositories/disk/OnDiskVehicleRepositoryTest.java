package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.backend.diagnosis.DeviceSnapshot.StateSnapshot;
import fleetmanagement.backend.diagnosis.ErrorCategory;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.sync.PackageSyncService;
import fleetmanagement.backend.repositories.exception.VehicleCountExceeded;
import fleetmanagement.backend.repositories.exception.VehicleDuplicationException;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.DiagnosticSummary;
import fleetmanagement.backend.vehicles.DiagnosticSummary.DiagnosticSummaryType;
import fleetmanagement.backend.vehicles.LiveInformation;
import fleetmanagement.backend.vehicles.LiveInformation.NextStation;
import fleetmanagement.backend.vehicles.LiveInformation.Position;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.LicenceStub;
import fleetmanagement.test.TestScenario;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static fleetmanagement.backend.vehicles.VehicleVersions.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class OnDiskVehicleRepositoryTest {

    private OnDiskVehicleRepository tested;
    private DiagnosisRepository diagnoses;
    private TaskRepository tasks;
    private LicenceStub licence;
    private TestScenario scenario;


    @Rule
    public TempFileRule tempDir = new TempFileRule();

    @Mock
    private DiagnosisHistoryRepository diagnosisHistoryRepository;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        scenario = new TestScenario();
        licence = scenario.licence;
        diagnoses = scenario.diagnosisRepository;
        tasks = scenario.taskRepository;
        tested = new OnDiskVehicleRepository(tempDir, licence, scenario.connectionStatusRepository, tasks, scenario.diagnosisRepository);
    }

    @Test
    public void addsVehicles() throws IOException {
        addVehicle("123456", "231685798", "Vehicle 123456", "1.2.34567.0");

        tested = new OnDiskVehicleRepository(tempDir, licence, scenario.connectionStatusRepository, tasks, scenario.diagnosisRepository);
        tested.loadFromDisk();

        Vehicle added = tested.listAll().get(0);
        assertEquals(1, tested.listAll().size());
        assertEquals("123456", added.uic);
        assertEquals("231685798", added.additional_uic);
        assertEquals("Vehicle 123456", added.getName());
        assertEquals("1.2.34567.0", added.clientVersion);
        assertTrue(Duration.between(ZonedDateTime.now(), added.lastSeen).abs().toMillis() < 1000);
    }

    @Test(expected = VehicleDuplicationException.class)
    public void doesNotAddDuplicateVehicle() {
        addVehicle("123456", "Vehicle 1", "1.2.34567.0");
        addVehicle("123456", "Vehicle 2", "1.2.34567.0");

        assertEquals(1, tested.listAll().size());
    }

    @Test
    public void findsVehiclesByUic() {
        addVehicle("123456", "Vehicle 123456", "1.2.34567.0");

        Vehicle added = tested.tryFindByUIC("123456");
        assertNotNull(added);
    }

    @Test
    public void findsVehiclesById() {
        Vehicle added = addVehicle("123456", "Vehicle 123456", "1.2.34567.0");

        Vehicle found = tested.tryFindById(added.id);

        assertSame(found, added);
    }

    @Test
    public void returnsNullForUnknownUICs() {
        Vehicle added = tested.tryFindByUIC("unknown");
        assertNull(added);
    }

    @Test
    public void loadsStateFromDisk() throws IOException {
        Vehicle previous = new Vehicle("123456", null,"Vehicle 123456", "1.2.34567.0", ZonedDateTime.now(), null, false, 1);
        previous.versions.add(new Versioned(PackageType.DataSupply, 1, "v.1"));
        previous.versions.add(new Versioned(PackageType.DataSupply, 2, "v.2"));
        previous.versions.add(new Versioned(PackageType.DataSupply, 3, "v.3"));
        tested.insert(previous);

        tested = new OnDiskVehicleRepository(tempDir, licence, scenario.connectionStatusRepository, tasks, scenario.diagnosisRepository);
        tested.loadFromDisk();

        assertEquals(1, tested.listAll().size());
        Vehicle loaded = tested.listAll().get(0);
        assertEquals("123456", loaded.uic);
        assertEquals("Vehicle 123456", loaded.getName());
        assertEquals("1.2.34567.0", loaded.clientVersion);
        assertEquals(previous.lastSeen, loaded.lastSeen);
        for (int i = 1; i <= 3; i++) {
            assertEquals(previous.versions.getDataSupplyVersion(i), loaded.versions.getDataSupplyVersion(i));
        }
    }

    @Test
    public void updatesVehicles() throws IOException {
        Vehicle previous = addVehicle("123456", "Vehicle 123456", "1.0");

        tested.update(previous.id, v -> {
            v.clientVersion = "2.0";
        });

        assertEquals("1.0", previous.clientVersion);
        assertEquals("2.0", tested.tryFindByUIC(previous.uic).clientVersion);
    }

    @Test
    public void savesVehicleChangesOnDisk() throws IOException {
        NextStation s1 = new NextStation("s1", "pa1", "ea1");
        NextStation s2 = new NextStation("s2", "pa2", null);
        NextStation s3 = new NextStation("s3", null, null);
        Vehicle previous = addVehicle("123456", "Vehicle 123456", "1.2.34567.0");

        ZonedDateTime liveInfoTimestamp = ZonedDateTime.parse("2015-10-09T17:59:00+01:00");
        tested.update(previous.id, v -> {
            v.lastSeen = ZonedDateTime.parse("2007-12-03T10:15:30+01:00");
            v.versions.setDataSupplyVersion(1, "1.0");
            List<NextStation> nextStations = Arrays.asList(s1, s2, s3);
            v.liveInformation = new LiveInformation(new Position(12.3, 45.6), "Berlin Hbf", "M�nchen Hbf", "RE", "12345", nextStations, liveInfoTimestamp);
        });

        tested = new OnDiskVehicleRepository(tempDir, licence, scenario.connectionStatusRepository, tasks, scenario.diagnosisRepository);
        tested.loadFromDisk();

        Vehicle vehicle = tested.listAll().get(0);
        assertEquals(ZonedDateTime.parse("2007-12-03T10:15:30+01:00"), vehicle.lastSeen);
        assertEquals("1.0", vehicle.versions.getDataSupplyVersion(1));
        LiveInformation loadedLiveInfo = vehicle.liveInformation;
        assertEquals(12.3, loadedLiveInfo.position.latitude, 0.01);
        assertEquals(45.6, loadedLiveInfo.position.longitude, 0.01);

        assertEquals(liveInfoTimestamp, loadedLiveInfo.received);
        assertEquals("RE", loadedLiveInfo.trainType);
        assertEquals("12345", loadedLiveInfo.tripNumber);
        assertEquals("Berlin Hbf", loadedLiveInfo.startStation);
        assertEquals("M�nchen Hbf", loadedLiveInfo.destinationStation);
        assertEquals("s1", loadedLiveInfo.nextStations.get(0).name);
        assertEquals("pa1", loadedLiveInfo.nextStations.get(0).plannedArrival);
        assertEquals("ea1", loadedLiveInfo.nextStations.get(0).estimatedArrival);
        assertEquals("s2", loadedLiveInfo.nextStations.get(1).name);
        assertEquals("pa2", loadedLiveInfo.nextStations.get(1).plannedArrival);
        assertEquals("s3", loadedLiveInfo.nextStations.get(2).name);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doesNotCrashWhenModifyingNonExistantVehicle() throws IOException {
        Consumer<Vehicle> updateCall = mock(Consumer.class);

        tested.update(UUID.randomUUID(), updateCall);

        verify(updateCall).accept(null);
    }

    @Test
    public void setsMissingLiveInformationTimestamp() throws IOException {
        Vehicle previous = addVehicle("123456", "Vehicle 123456", "1.2.34567.0");

        tested.update(previous.id, v -> {
            v.liveInformation = new LiveInformation(null, "Start", "Destination", null, null, Collections.emptyList(), null);
        });
        tested = new OnDiskVehicleRepository(tempDir, licence, scenario.connectionStatusRepository, tasks, scenario.diagnosisRepository);
        tested.loadFromDisk();

        Vehicle vehicle = tested.listAll().get(0);
        assertNotNull(vehicle.liveInformation.received);
    }

    @Test
    public void emptyLiveInformationStaysEmpty() throws IOException {
        Vehicle previous = addVehicle("123456", "Vehicle 123456", "1.2.34567.0");

        tested.update(previous.id, v -> {
            v.liveInformation = null;
        });
        tested = new OnDiskVehicleRepository(tempDir, licence, scenario.connectionStatusRepository, tasks, scenario.diagnosisRepository);
        tested.loadFromDisk();

        Vehicle vehicle = tested.listAll().get(0);
        assertNull(vehicle.liveInformation);
    }

    @Test
    public void deletesVehicle() throws IOException {
        Vehicle added = addVehicle("123456", "Vehicle 123456", "1.2.34567.0");

        tested.delete(added.id);
        assertTrue(tested.listAll().isEmpty());

        tested.loadFromDisk();
        assertTrue(tested.listAll().isEmpty());
    }

    @Test
    public void loadsLegacyXmlFile() throws IOException {
        FileUtils.copyFileToDirectory(TestFiles.find("legacy-database-files/vehicle.xml"), tempDir.append("129875a9-cfb7-435c-bb57-e85ffd76e3bb"));

        tested.loadFromDisk();

        Vehicle v = tested.tryFindById(UUID.fromString("129875a9-cfb7-435c-bb57-e85ffd76e3bb"));
        assertEquals("1.0", v.versions.get(PackageType.Indis5MultimediaContent).version);
    }

    @Test
    public void updatesDiagnosticSummaryUponLoad() throws IOException {
        Vehicle vehicle = addVehicle("123456", "Vehicle 123456", "1.2.34567.0");
        addDiagnosticError(diagnoses, vehicle);

        tested = new OnDiskVehicleRepository(tempDir, licence, scenario.connectionStatusRepository, tasks, scenario.diagnosisRepository);
        tested.loadFromDisk();

        DiagnosticSummary summary = tested.tryFindById(vehicle.id).getDiagnosticSummary(ZonedDateTime.now());
        assertEquals(DiagnosticSummaryType.DeviceErrors, summary.type);
    }

    @Test(expected = VehicleCountExceeded.class)
    public void vehicleCountExceeded() throws IOException {
        licence.maxVehicleCount = 1;

        addVehicle("123", "Vehicle 123", "1.2.34567.0");
        addVehicle("456", "Vehicle 456", "1.2.34567.0");
    }

    private void addDiagnosticError(DiagnosisRepository diagnoses, Vehicle vehicle) {
        Snapshot snapshot = new Snapshot(vehicle.id, 1, ZonedDateTime.now(),
                Arrays.asList(new DeviceSnapshot("id", "location", "name", "type",
                        new VersionInfo(), new StateSnapshot("Error", "-1", ErrorCategory.FATAL)))
        );
        SnapshotConversionService snapshotConversionService = new SnapshotConversionService(diagnoses, diagnosisHistoryRepository, tested);
        snapshotConversionService.integrateNewSnapshot(snapshot);
    }

    private Vehicle addVehicle(String uic, String name, String clientVersion) {
        Vehicle v = new Vehicle(uic, null, name, clientVersion, ZonedDateTime.now(), null, false, 1);
        tested.insert(v);
        return v;
    }

    private Vehicle addVehicle(String uic, String additional_uic, String name, String clientVersion) {
        Vehicle v = new Vehicle(uic, additional_uic, name, clientVersion, ZonedDateTime.now(), null, false, 1);
        tested.insert(v);
        return v;
    }


    @Test
    public void noException_WhenXmlFileIsEmpty() throws Exception {
        TempFile folder = tempDir.newFolder(UUID.randomUUID().toString());
        String xmlFile = tested.getXmlFile(folder).file().getName();
        folder.newFile(xmlFile);

        tested.loadFromDisk();
    }
}
