package fleetmanagement.backend.notifications;

import fleetmanagement.TestFiles;
import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.backend.events.EventType;
import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.mail.MailService;
import fleetmanagement.backend.mail.MailServiceImpl;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.NotificationSettingRepository;
import fleetmanagement.backend.notifications.settings.Type;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageSize;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.mail.MessagingException;
import java.io.File;
import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NotificationServiceTest {

    NotificationService tested;
    TestScenarioPrefilled scenario;
    NotificationSettingRepository notificationSettingRepository;
    String mailList = "dev@gsp.com";
    Vehicle vehicle;
    @Mock
    MailService mailService;
    @Mock
    DiagnosisHistoryRepository diagnosisHistoryRepository;

    @BeforeClass
    public static void beforeClass() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        scenario = new TestScenarioPrefilled();
        vehicle = scenario.vehicle1;
        notificationSettingRepository = scenario.notificationRepository;
        tested = new NotificationService(notificationSettingRepository
                ,
                scenario.vehicleRepository, scenario.taskRepository, mailService);

    }

    @Test
    public void emailSent_WhenDiagnosedDeviceError() throws MessagingException, UnknownHostException {
        String deviceName1 = "device1";
        String deviceName2 = "device2";

        NotificationSetting notificationSetting = NotificationSetting.diagnosedDeviceError(deviceName1 + ", " + deviceName2, mailList);
        notificationSettingRepository.insert(notificationSetting);

        Diagnosis diagnosis = integrateDiagnosisWithDevice(vehicle.id, deviceName2, ErrorCategory.FATAL);

        tested.processEvent(Events.diagnosisUpdated(diagnosis));

        verify(mailService).send(any(DiagnosedDeviceError.class));

    }

    @Test
    public void noEmail_WhenDiagnosedDeviceIsOK() throws MessagingException, UnknownHostException {
        String deviceName1 = "device1";
        String deviceName2 = "device2";

        Diagnosis diagnosis = integrateDiagnosisWithDevice(vehicle.id, deviceName1, ErrorCategory.OK);

        NotificationSetting notificationSetting = NotificationSetting.diagnosedDeviceError(deviceName1 + ", " + deviceName2, mailList);
        notificationSettingRepository.insert(notificationSetting);

        tested.processEvent(Events.diagnosisUpdated(diagnosis));

        verify(mailService, never()).send(any(Notification.class));

    }

    @Test
    public void emailSent_WhenDiagnosisErrorLimitExceeded() throws MessagingException, UnknownHostException {
        String deviceName1 = "device1";

        NotificationSetting notificationSetting = NotificationSetting.diagnosisMaxErrors(0, mailList);
        notificationSettingRepository.insert(notificationSetting);

        Diagnosis diagnosis = integrateDiagnosisWithDevice(vehicle.id, deviceName1, ErrorCategory.FATAL);

        tested.processEvent(Events.diagnosisUpdated(diagnosis));

        verify(mailService).send(any(DiagnosisMaxErrors.class));

    }

    @Test
    public void noEmail_WhenDiagnosisErrorLimitNotExceeded() throws MessagingException, UnknownHostException {
        String deviceName1 = "device1";
        NotificationSetting notificationSetting = NotificationSetting.diagnosisMaxErrors(1, mailList);
        notificationSettingRepository.insert(notificationSetting);

        Diagnosis diagnosis = integrateDiagnosisWithDevice(vehicle.id, deviceName1, ErrorCategory.FATAL);

        tested.processEvent(Events.diagnosisUpdated(diagnosis));

        verify(mailService, never()).send(any(Notification.class));

    }

    @Test
    public void emailSent_WhenIndicatorValueOutOfRange() throws MessagingException, UnknownHostException {
        String indicatorId = "tank1";
        String indicatorValue = "12";

        NotificationSetting notificationSetting = NotificationSetting.indicatorValueRange(indicatorId, 0, 10, mailList);
        notificationSettingRepository.insert(notificationSetting);

        scenario.addOperationData(vehicle.id, new Indicator(indicatorId, "", indicatorValue));

        OperationData operationData = scenario.operationDataRepository.tryFindById(vehicle.id);
        tested.processEvent(Events.operationDataUpdated(operationData));

        verify(mailService).send(any(IndicatorValueRange.class));

    }

    @Test
    public void noEmail_WhenIndicatorValueOK() throws MessagingException, UnknownHostException {
        String indicatorId = "tank1";
        String indicatorValue = "5";

        NotificationSetting notificationSetting = NotificationSetting.indicatorValueRange(indicatorId, 0, 10, mailList);
        notificationSettingRepository.insert(notificationSetting);

        scenario.addOperationData(vehicle.id, new Indicator(indicatorId, "", indicatorValue));


        OperationData operationData = scenario.operationDataRepository.tryFindById(vehicle.id);
        tested.processEvent(Events.operationDataUpdated(operationData));

        verify(mailService, never()).send(any(Notification.class));
    }

    @Test
    public void emailSent_WhenTaskLoggedWithError() throws MessagingException, UnknownHostException {
        NotificationSetting notificationSetting = NotificationSetting.packageInstallError(mailList);
        notificationSettingRepository.insert(notificationSetting);
        Task task = addTaskWithLog(new LogEntry(LogEntry.Severity.ERROR, "error message"));
        tested.processEvent(Events.taskLogUpdated(task));

        verify(mailService).send(any(PackageInstallError.class));
    }

    @Test
    public void noEmail_WhenTaskLoggedWithoutError() throws MessagingException, UnknownHostException {
        NotificationSetting notificationSetting = NotificationSetting.packageInstallError(mailList);
        notificationSettingRepository.insert(notificationSetting);
        Task task = addTaskWithLog(new LogEntry(LogEntry.Severity.INFO, "info message"));
        tested.processEvent(Events.taskLogUpdated(task));

        verify(mailService, never()).send(any(Notification.class));

    }

    @Test
    public void assertThatAllNotificationTypesAssignedToEvent() throws MessagingException, UnknownHostException {
        Set<Type> assignedTypes = Stream.of(EventType.values())
                .map(tested::getAssignedNotificationTypes)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        List<Type> notificationTypes = Arrays.stream(Type.values()).collect(Collectors.toList());
        //this type processed in OfflineMonitor
        notificationTypes.remove(Type.VEHICLE_OFFLINE);

        assertTrue(assignedTypes.containsAll(notificationTypes));
    }

    private Diagnosis integrateDiagnosisWithDevice(UUID vehicleId, String deviceName, ErrorCategory errorCategory) {
        DeviceSnapshot component = new DeviceSnapshot("0x10212", null, deviceName, null, new VersionInfo()
                , new DeviceSnapshot.StateSnapshot("Error", "-1", errorCategory));
        Snapshot snapshotWithTimestamp = new Snapshot(vehicleId, 1, ZonedDateTime.now(), Arrays.asList(component));

        SnapshotConversionService snapshotConversionService = new SnapshotConversionService(scenario.diagnosisRepository, diagnosisHistoryRepository, scenario.vehicleRepository);
        snapshotConversionService.integrateNewSnapshot(snapshotWithTimestamp);

        Diagnosis diagnosis = scenario.diagnosisRepository.tryFindByVehicleId(vehicleId);
        return diagnosis;
    }


    private Task addTaskWithLog(LogEntry logEntry) {
        File packageDir = TestFiles.find("sample-package");
        Package pkg = new Package(UUID.randomUUID(), PackageType.DataSupply, "1.0", packageDir, new PackageSize(0, 0),
                1, "08.09.2013 00:00:00", "01.12.2013 23:59:59");
        Task task = new Task(pkg, vehicle);
        task.addLog(logEntry);
        scenario.taskRepository.insert(task);
        return task;
    }





}