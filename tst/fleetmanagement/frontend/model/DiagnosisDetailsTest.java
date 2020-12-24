package fleetmanagement.frontend.model;

import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.backend.diagnosis.DeviceSnapshot.StateSnapshot;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.DiagnosisDetails.DeviceGroup;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.time.ZonedDateTime.now;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiagnosisDetailsTest {

    private DiagnosisDetails tested;
    private UserSession session;
    private TestScenarioPrefilled scenario;
    private UUID vehicleId;

    @Mock
    private DiagnosisHistoryRepository diagnosisHistoryRepository;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        session = new SessionStub();
        scenario = new TestScenarioPrefilled();
        vehicleId = scenario.vehicle1.id;
    }

    @Test
    public void countsWorkingAndDefectiveDevices() {
        addDeviceWithStatus(vehicleId, ErrorCategory.FATAL);
        addDeviceWithStatus(vehicleId, ErrorCategory.OK);
        addDeviceWithStatus(vehicleId, ErrorCategory.TRIVIAL);
        addDeviceWithStatus(vehicleId, ErrorCategory.WARNING);
        Diagnosis diagnosis = scenario.diagnosisRepository.tryFindByVehicleId(vehicleId);

        tested = new DiagnosisDetails(diagnosis, scenario.vehicle1, scenario.licence, session);

        assertEquals(1, tested.defectiveDevicesCount);
        assertEquals(3, tested.operationalDevicesCount);
    }

    @Test
    public void groupsDevicesByType() {
        addDeviceWithStatus(vehicleId, ErrorCategory.OK);
        addDeviceWithStatus(vehicleId, ErrorCategory.OK);
        Diagnosis diagnosis = scenario.diagnosisRepository.tryFindByVehicleId(vehicleId);
        tested = new DiagnosisDetails(diagnosis, scenario.vehicle1, scenario.licence, session);

        assertEquals(1, tested.groups.size());
        DeviceGroup group = tested.groups.get(0);
        assertTrue(group.containsMultipleDevices());
        assertEquals(0, group.devicesWithErrors);
        assertEquals("type", group.deviceType);
        assertTrue(group.versions.isEmpty());
    }

    @Test
    public void detectsIfDeviceGroupContainsErrors() {
        addDeviceWithStatus(vehicleId, ErrorCategory.OK);
        addDeviceWithStatus(vehicleId, ErrorCategory.SEVERE);
        Diagnosis diagnosis = scenario.diagnosisRepository.tryFindByVehicleId(vehicleId);
        tested = new DiagnosisDetails(diagnosis, scenario.vehicle1, scenario.licence, session);

        DeviceGroup group = tested.groups.get(0);
        assertEquals(1, group.devicesWithErrors);
    }

    @Test
    public void detectsIfDevicesHaveDifferentSoftwareVersions() {
        addDeviceWithVersion(vehicleId, "1.0");
        addDeviceWithVersion(vehicleId, "2.0");
        Diagnosis diagnosis = scenario.diagnosisRepository.tryFindByVehicleId(vehicleId);
        tested = new DiagnosisDetails(diagnosis, scenario.vehicle1, scenario.licence, session);

        DeviceGroup group = tested.groups.get(0);
        assertEquals(Collections.singletonList("various") , group.versions);
    }

    @Test
    public void printsTimeSinceLastDiagnosisUpdate() {
        ZonedDateTime oneHourAgo = now().minus(1, ChronoUnit.HOURS);
        tested = new DiagnosisDetails(createDiagnosisWithTimestamp(oneHourAgo), scenario.vehicle1, scenario.licence, session);
        assertEquals("1 hour ago", tested.lastUpdated);
    }

    @Test
    public void printsTimeSinceCurrentStatusBegan() {
        ZonedDateTime twoDaysAgo = now().minus(2, ChronoUnit.DAYS);
        addDeviceWithStatus(vehicleId, ErrorCategory.FATAL, twoDaysAgo);
        Diagnosis diagnosis = scenario.diagnosisRepository.tryFindByVehicleId(vehicleId);
        tested = new DiagnosisDetails(diagnosis, scenario.vehicle1, scenario.licence, session);

        assertEquals("2 days ago", tested.groups.get(0).devices.get(0).currentState.get(0).currentStatusSince);
    }

    @Test
    public void logErrorWhenNoDeviceNameOnAnyLanguage() {
        DiagnosedDevice diagnosedDevice = createDevice(Locale.JAPAN, Locale.CHINA);

        DiagnosisDetails.Device device = generateDiagnosisDetails(diagnosedDevice);

        assertEquals("-", device.name);
        assertEquals("", device.currentState.get(0).message);
    }

    @Test
    public void deviceNameInEnglish_WhenNoAceptableLanguages() {
        session.setAcceptableLanguages(Arrays.asList(Locale.KOREA, Locale.GERMAN));
        DiagnosedDevice diagnosedDevice = createDevice(Locale.JAPAN, Locale.CHINA, Locale.ENGLISH);

        DiagnosisDetails.Device device = generateDiagnosisDetails(diagnosedDevice);

        assertEquals("en", device.name);
        assertEquals("", device.currentState.get(0).message);
    }

    @Test
    public void deviceNameOnFirstAcceptableLanguage() {
        session.setAcceptableLanguages(Arrays.asList(Locale.KOREA, Locale.GERMAN, Locale.ENGLISH));
        DiagnosedDevice diagnosedDevice = createDevice(Locale.JAPAN, Locale.ENGLISH, Locale.GERMAN);

        DiagnosisDetails.Device device = generateDiagnosisDetails(diagnosedDevice);

        assertEquals("de", device.name);
        assertEquals("de", device.currentState.get(0).message);
    }

    @Test
    public void logErrorWhenDeviceNameIsEmpty() {
        Diagnosis diagnosis = new Diagnosis(vehicleId);
        DiagnosedDevice diagnosedDevice = createDevice(Locale.JAPAN);
        diagnosis.getDevices().add(diagnosedDevice);

        tested = new DiagnosisDetails(diagnosis, scenario.vehicle1, scenario.licence, session);

        DeviceGroup group = tested.groups.get(0);
        assertEquals(1, group.devicesWithErrors);
    }

    DiagnosisDetails.Device generateDiagnosisDetails(DiagnosedDevice diagnosedDevice) {
        Diagnosis diagnosis = new Diagnosis(vehicleId);
        diagnosis.getDevices().add(diagnosedDevice);
        tested = new DiagnosisDetails(diagnosis, scenario.vehicle1, scenario.licence, session);
        DeviceGroup group = tested.groups.get(0);
        return group.devices.get(0);
    }

    private Diagnosis createDiagnosisWithTimestamp(ZonedDateTime timestamp) {
        Diagnosis diagnosis = new Diagnosis(scenario.vehicle1.id, timestamp, Collections.EMPTY_LIST);
        return diagnosis;
    }

    private void addDeviceWithStatus(UUID vehicleId, ErrorCategory status) {
        addDeviceWithStatus(vehicleId, status, ZonedDateTime.now());
    }

    private void addDeviceWithStatus(UUID vehicleId, ErrorCategory status, ZonedDateTime statusBeganAt) {
        addDevice(vehicleId, status, statusBeganAt, new VersionInfo());
    }

    private void addDeviceWithVersion(UUID vehicleId, String version) {
        addDevice(vehicleId, ErrorCategory.OK, ZonedDateTime.now(), new VersionInfo(version, null));
    }

    private void addDevice(UUID vehicleId, ErrorCategory status, ZonedDateTime statusBeganAt, VersionInfo version) {
        DeviceSnapshot component = new DeviceSnapshot(
                UUID.randomUUID().toString(), "location", "name", "type",
                version,
                new StateSnapshot("description", "code", status));
        Snapshot snapshot = new Snapshot(vehicleId, 1, statusBeganAt, Collections.singletonList(component));
        SnapshotConversionService snapshotConversionService = new SnapshotConversionService(scenario.diagnosisRepository, diagnosisHistoryRepository, scenario.vehicleRepository);
        snapshotConversionService.integrateNewSnapshot(snapshot);
    }

    private DiagnosedDevice createDevice(Locale... locale) {
        Map<String, String> localeMap = new HashMap<>();
        Arrays.stream(locale).forEach(l -> localeMap.put(l.getLanguage(), l.getLanguage()));

        StateEntry newStatus = new StateEntry(ZonedDateTime.now(), null, "-1", ErrorCategory.FATAL, new LocalizedString(localeMap));
        ErrorHistory errorHistory = new ErrorHistory(Collections.singletonList(newStatus));
        VersionInfo versionInfo = new VersionInfo("v1", "v1");
        return new DiagnosedDevice("id", "location", new LocalizedString(localeMap), "type", null, Collections.singletonList(newStatus), true, versionInfo, errorHistory);
    }
}
