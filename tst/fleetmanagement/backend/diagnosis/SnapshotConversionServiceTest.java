package fleetmanagement.backend.diagnosis;

import com.google.gson.Gson;
import fleetmanagement.Dates;
import fleetmanagement.TempFileRule;
import fleetmanagement.backend.diagnosis.DeviceSnapshot.StateSnapshot;
import fleetmanagement.backend.diagnosis.VersionInfo.VersionType;
import fleetmanagement.backend.repositories.disk.DiagnosisHistorySQLiteRepository;
import fleetmanagement.backend.repositories.disk.OnDiskDiagnosisRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenario;
import gsp.configuration.LocalFiles;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.formula.functions.T;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SnapshotConversionServiceTest {

    private SnapshotConversionService tested;
    private Diagnosis diagnosis;
    private UUID vehicleId = UUID.randomUUID();
    private VehicleRepository vehicles;
    private SessionStub session;
    private TestScenario scenario;
    private DiagnosisRepository diagnosisRepository;
    private String deviceId;
    private final Gson gson = new Gson();
    ZonedDateTime timestamp;

    @Rule
    public TempFileRule tempFolder = new TempFileRule();

    private DiagnosisHistoryRepository diagnosisHistoryRepository;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        timestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        scenario = new TestScenario();
        session = new SessionStub();
        diagnosis = new Diagnosis(vehicleId);
        vehicles = scenario.vehicleRepository;
        vehicles.insert(new Vehicle(vehicleId, "123456", null, "name", "1.0", ZonedDateTime.now(), null, false, 1, null));
        diagnosisHistoryRepository = new DiagnosisHistorySQLiteRepository(tempFolder);
        diagnosisRepository = new OnDiskDiagnosisRepository(tempFolder, diagnosisHistoryRepository);
        diagnosisRepository.insert(diagnosis);
        tested = new SnapshotConversionService(diagnosisRepository, diagnosisHistoryRepository, vehicles);
        deviceId = "0x10212";
    }

    @Test
    public void createsDiagnosisForPreviouslyUnknownVehicle() {
        diagnosisRepository.delete(vehicleId);
        tested.integrateNewSnapshot(createSnapshot());

        assertNotNull(diagnosisRepository.tryFindByVehicleId(vehicleId));
    }

    @Test
    public void doesNotCreateDiagnosisForAlreadyKnownVehicle() {
        tested.integrateNewSnapshot(createSnapshot());

        assertEquals(1, diagnosisRepository.listAll().size());
    }

    @Test
    public void writesUpdatedDiagnosisToRepository() {
        DiagnosisRepository repository = mock(DiagnosisRepository.class);
        tested = new SnapshotConversionService(repository, diagnosisHistoryRepository, vehicles);
        when(repository.tryFindByVehicleId(vehicleId)).thenReturn(diagnosis);

        tested.integrateNewSnapshot(createSnapshot());

        verify(repository).update(eq(vehicleId), any());
    }

    @Test
    public void takesLastUpdatedFromSnapshotTimestamp() {
        ZonedDateTime snapshotTimestamp = ZonedDateTime.now();
        diagnosis = tested.integrateNewSnapshot(createSnapshotWithTimestamp(snapshotTimestamp));

        assertEquals(snapshotTimestamp, diagnosis.getLastUpdated());
    }

    @Test
    public void createsPreviouslyUnknownComponent() {
        diagnosis = integrateSnapshotWithComponent(deviceId, "location", "name", "type");

        assertEquals(1, diagnosis.getDevices().size());
        DiagnosedDevice component = diagnosis.getDevices().get(0);
        assertEquals(deviceId, component.getId());
        assertEquals("location", component.getLocation());
        assertEquals("name", component.getName().get(session.getAcceptableLanguages()));
        assertEquals("type", component.getType());
    }

    @Test
    public void updatesAlreadyKnownComponent() {
        integrateSnapshotWithComponent(deviceId, "oldLocation", "oldName", "oldType");
        diagnosis = integrateSnapshotWithComponent(deviceId, "location", "name", "type");

        assertEquals(1, diagnosis.getDevices().size());
        DiagnosedDevice component = diagnosis.getDevices().get(0);
        assertEquals("location", component.getLocation());
        assertEquals("name", component.getName().get(session.getAcceptableLanguages()));
        assertEquals("type", component.getType());
    }

    @Test
    public void marksVanishedComponentsAsDisabled() {
        diagnosis = integrateSnapshotWithComponent(deviceId);
        Snapshot snapshotWithoutComponents = createSnapshot();
        diagnosis = tested.integrateNewSnapshot(snapshotWithoutComponents);

        assertEquals(1, diagnosis.getDevices().size());
        assertTrue(diagnosis.getDevices().get(0).isDisabled());
    }

    @Test
    public void removesDisabledMarkFromRediscoveredComponents() {
        diagnosis = integrateSnapshotWithComponent(deviceId);
        diagnosis.getDevice(deviceId).disable();
        diagnosis = integrateSnapshotWithComponent(deviceId);

        assertFalse(diagnosis.getDevices().get(0).isDisabled());
    }

    @Test
    public void addsVersionInfoToPreviouslyUnknownComponent() {
        integrateSnapshotWithVersionedComponent(deviceId, "sw-1.0", null);
        diagnosis = diagnosisRepository.tryFindByVehicleId(vehicleId);

        DiagnosedDevice component = diagnosis.getDevices().get(0);
        assertEquals("sw-1.0", component.getVersion(VersionType.Software));
        assertNull(component.getVersion(VersionType.Fontware));
    }

    @Test
    public void updatesVersions() {
        integrateSnapshotWithComponent(deviceId);
        integrateSnapshotWithVersionedComponent(deviceId, null, "fw-1.0");
        diagnosis = diagnosisRepository.tryFindByVehicleId(vehicleId);

        List<DiagnosedDevice> diagnosedComponents = diagnosis.getDevices();
        assertEquals("fw-1.0", diagnosedComponents.get(0).getVersion(VersionType.Fontware));
        assertNull(diagnosedComponents.get(0).getVersion(VersionType.Software));
    }

    @Test
    public void entersNewErrorsIntoErrorHistory() {
        ZonedDateTime timestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        diagnosis = tested.integrateNewSnapshot(createSnapshotWithComponent(deviceId, timestamp, error()));

        StateEntry currentState = diagnosisRepository.getLatestDeviceHistoryRecord(vehicleId, deviceId);

        assertFalse(currentState.isOk());
        assertEquals("-1", currentState.code);
        assertEquals("Error", currentState.message.get(session.getAcceptableLanguages()));
        assertEquals(ErrorCategory.FATAL, currentState.category);
        assertEquals(timestamp, currentState.start);
    }

    @Test
    public void endsPreviousErrorWhenDeviceBecomesOk() {
        ZonedDateTime now = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        tested.integrateNewSnapshot(createSnapshotWithComponent(deviceId, now.minusDays(1), error()));
        diagnosis = tested.integrateNewSnapshot(createSnapshotWithComponent(deviceId, now, ok()));

        StateEntry previousState = diagnosisRepository.getDeviceHistoryRange(vehicleId, deviceId, now.minusDays(1)).get(0);
        StateEntry currentState = diagnosis.getDevices().get(0).getCurrentState().get(0);
        assertTrue(currentState.isOk());
        assertEquals(null, currentState.code);
        assertEquals("", currentState.message.get(session.getAcceptableLanguages()));
        assertEquals(ErrorCategory.OK, currentState.category);
        assertEquals(now, currentState.start);
        assertEquals(now, previousState.end);
    }

    @Test
    public void prolonguesCurrentErrorThatReoccursInSnapshot() {
        ZonedDateTime firstOccurrence = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        tested.integrateNewSnapshot(createSnapshotWithComponent(deviceId, firstOccurrence, error()));
        diagnosis = tested.integrateNewSnapshot(createSnapshotWithComponent(deviceId, firstOccurrence.plus(1, ChronoUnit.DAYS), error()));

        List<StateEntry> history = diagnosisRepository.getDiagnosedDeviceHistory(vehicleId, deviceId);
        assertEquals(1, history.size());
        assertEquals("Error", history.get(0).message.get(session.getAcceptableLanguages()));
        assertEquals(firstOccurrence, history.get(0).start);
        assertNull(history.get(0).end);
    }

    private StateSnapshot error(String code) {
        return new StateSnapshot("Error", code, ErrorCategory.FATAL);
    }

    private StateSnapshot error() {
        return error("-1");
    }

    private StateSnapshot ok() {
        return new StateSnapshot("", null, ErrorCategory.OK);
    }

    private Snapshot createSnapshot(DeviceSnapshot... components) {
        return new Snapshot(vehicleId, 1, ZonedDateTime.now(), Arrays.asList(components));
    }

    private Snapshot createSnapshotWithTimestamp(ZonedDateTime timestamp, DeviceSnapshot... components) {
        return new Snapshot(vehicleId, 1, timestamp, Arrays.asList(components));
    }

    private Diagnosis integrateSnapshotWithComponent(String id) {
        return integrateSnapshotWithComponent(id, "Front-Door", "INDIS5 1", "INDIS5", new VersionInfo(), ok());
    }

    private Diagnosis integrateSnapshotWithComponent(String id, String location, String name, String type) {
        return integrateSnapshotWithComponent(id, location, name, type, new VersionInfo(), ok());
    }

    private Diagnosis integrateSnapshotWithVersionedComponent(String componentId, String softwareVersion, String fontVersion) {
        VersionInfo versions = new VersionInfo(softwareVersion, fontVersion);
        return integrateSnapshotWithComponent(componentId, "Front-Door", "INDIS5 1", "INDIS5", versions, ok());
    }

    private Diagnosis integrateSnapshotWithComponent(String id, String location, String name, String type, VersionInfo version, StateSnapshot status) {
        DeviceSnapshot component = new DeviceSnapshot(id, location, name, type, version, status);
        Snapshot snapshot = createSnapshot(component);
        return tested.integrateNewSnapshot(snapshot);
    }

    private Snapshot createSnapshotWithComponent(String componentId, ZonedDateTime timestamp, StateSnapshot status) {
        DeviceSnapshot component = deviceSnapshot(componentId, status);
        return createSnapshotWithTimestamp(timestamp, component);
    }

    private Snapshot snapshot(int version, ZonedDateTime timestamp, DeviceSnapshot... components) {
        return new Snapshot(vehicleId, version, timestamp, Arrays.asList(components));
    }

    private Snapshot snapshot(int version, DeviceSnapshot... components) {
        return new Snapshot(vehicleId, version, timestamp, Arrays.asList(components));
    }

    private DeviceSnapshot deviceSnapshot(String componentId, StateSnapshot... states) {
        return new DeviceSnapshot(componentId, null, new LocalizedString("name")
                , null, new VersionInfo(), "Status"
                , toList(states)
                , null);
    }

    private DeviceSnapshot deviceSnapshotWithHistory(String componentId, StateSnapshot... history) {
        return new DeviceSnapshot(componentId, null, new LocalizedString("name"),
                null, new VersionInfo(), "Status", null
                , toList(history));
    }

    public <T> List<T> toList(T[] array) {
        return Arrays.stream(array)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }



    @Test
    public void StatesHasBeginDate() throws IOException {
        File jsonFile = LocalFiles.find("test-resources/diagnonsis/diagnosis-fis-api.json");
        SnapshotFisApiJson json = gson.fromJson(FileUtils.readFileToString(jsonFile, "UTF-8"), SnapshotFisApiJson.class);
        Snapshot snapshot = json.toSnapshot(vehicleId);

        Diagnosis diagnosis = tested.integrateNewSnapshot(snapshot);

        DiagnosedDevice device = diagnosis.getDevice("781ex");

        assertNotNull(device.getCurrentState().get(0).start);
        assertNotNull(device.getCurrentState().get(0).start.toString());
    }

    @Test
    public void deviceHasStateFromLatestSnapshot() throws IOException {
        Snapshot snapshot = createSnapshotWithComponent(deviceId, ZonedDateTime.now().minusDays(1), error());
        Snapshot snapshot1 = createSnapshotWithComponent(deviceId, ZonedDateTime.now(), ok());

        tested.integrateNewSnapshot(snapshot);
        diagnosis = tested.integrateNewSnapshot(snapshot1);

        DiagnosedDevice device = diagnosis.getDevice(deviceId);

        Dates.assertEquals(ZonedDateTime.now(), device.getCurrentState().get(0).start, ChronoUnit.MINUTES);
        assertEquals(ErrorCategory.OK, device.getCurrentState().get(0).category);

    }

    @Test
    public void integrationOfEqualStatesDoesNotUpdateStartDate() {
        ZonedDateTime firstDate = ZonedDateTime.now().minusDays(1);
        Snapshot snapshot = createSnapshotWithComponent(deviceId, firstDate, ok());
        Snapshot snapshot1 = createSnapshotWithComponent(deviceId, firstDate.plusDays(1), ok());

        tested.integrateNewSnapshot(snapshot);
        diagnosis = tested.integrateNewSnapshot(snapshot1);

        DiagnosedDevice device = diagnosis.getDevice(deviceId);

        Dates.assertEquals(firstDate, device.getCurrentState().get(0).start, ChronoUnit.MINUTES);
    }

    @Test
    public void integratesSnapshotWithoutState() {
        ZonedDateTime firstDate = ZonedDateTime.now().minusDays(1);
        Snapshot snapshot = createSnapshotWithComponent(deviceId, firstDate, null);

        diagnosis = tested.integrateNewSnapshot(snapshot);

        DiagnosedDevice device = diagnosis.getDevice(deviceId);

        Dates.assertEquals(firstDate, device.getCurrentState().get(0).start, ChronoUnit.MINUTES);
    }

    @Test
    public void integratesMultipleSnapshotWithoutState1() {
        ZonedDateTime firstDate = ZonedDateTime.now().minusDays(1);
        Snapshot snapshot = createSnapshotWithComponent(deviceId, firstDate, null);
        Snapshot snapshot1 = createSnapshotWithComponent(deviceId, firstDate.plusDays(1), null);

        tested.integrateNewSnapshot(snapshot);
        diagnosis = tested.integrateNewSnapshot(snapshot1);

        DiagnosedDevice device = diagnosis.getDevice(deviceId);

        Dates.assertEquals(firstDate, device.getCurrentState().get(0).start, ChronoUnit.MINUTES);
    }

    @Test
    public void createsHistoryRecordForEachStateV2() {
        ZonedDateTime timestamp = ZonedDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        Snapshot snapshot = snapshot(2, timestamp, deviceSnapshot(deviceId, error("-1"), error("42")));
        tested.integrateNewSnapshot(snapshot);
        List<StateEntry> history = diagnosisRepository.getDiagnosedDeviceHistory(vehicleId, deviceId);

        assertEquals(2, history.size());
        assertEquals("-1", history.get(0).code);
        assertEquals("42", history.get(1).code);
    }

    @Test
    public void createsHistoryRecordForDeviceStateV2() {
        Snapshot snapshot = snapshot(2, deviceSnapshot(deviceId, error("-1")));
        tested.integrateNewSnapshot(snapshot);
        List<StateEntry> history = diagnosisRepository.getDiagnosedDeviceHistory(vehicleId, deviceId);

        assertEquals(1, history.size());
        assertEquals("-1", history.get(0).code);
        assertEquals("Error", history.get(0).message.get(session.getAcceptableLanguages()));
        assertEquals(timestamp, history.get(0).start);
        assertNull(history.get(0).end);
    }

    @Test
    public void createsHistoryRecordIfDifferentHistoryExistsV2() {
        diagnosisHistoryRepository.addHistory(vehicleId, deviceId, error("42").toStateEntry().startingAt(ZonedDateTime.now()));
        Snapshot snapshot = snapshot(2, deviceSnapshot(deviceId, error("-1")));
        tested.integrateNewSnapshot(snapshot);
        List<StateEntry> history = diagnosisRepository.getDiagnosedDeviceHistory(vehicleId, deviceId);

        assertEquals(2, history.size());
    }

    @Test
    public void doesNotCreateHistoryRecordIfSimilarRecordExistsV2() {
        diagnosisHistoryRepository.addHistory(vehicleId, deviceId, error("-1").toStateEntry().startingAt(ZonedDateTime.now()));
        Snapshot snapshot = snapshot(2, deviceSnapshot(deviceId, error("-1")));
        tested.integrateNewSnapshot(snapshot);
        List<StateEntry> history = diagnosisRepository.getDiagnosedDeviceHistory(vehicleId, deviceId);

        assertEquals(1, history.size());
    }

    @Test
    public void historyRecordFinished_IfThereIsNoSimilarStateV2() {
        diagnosisHistoryRepository.addHistory(vehicleId, deviceId, error("42").toStateEntry().startingAt(ZonedDateTime.now()));
        Snapshot snapshot = snapshot(2, deviceSnapshot(deviceId, error("-1")));
        tested.integrateNewSnapshot(snapshot);
        List<StateEntry> history = diagnosisHistoryRepository.getUnfinishedHistory(vehicleId, deviceId);

        assertEquals(1, history.size());
        assertEquals("-1", history.get(0).code);
    }

    @Test
    public void statusHistoryImported_IfAvailableV2() {
        StateSnapshot historyState = new StateSnapshot(new LocalizedString("Error"), "-1", ErrorCategory.FATAL, timestamp.minusDays(1), timestamp);
        Snapshot snapshot = snapshot(2, deviceSnapshotWithHistory(deviceId, historyState));
        tested.integrateNewSnapshot(snapshot);
        List<StateEntry> history = diagnosisRepository.getDiagnosedDeviceHistory(vehicleId, deviceId);

        assertEquals(1, history.size());
        assertEquals("-1", history.get(0).code);
    }


}
