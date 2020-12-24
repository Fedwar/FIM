package fleetmanagement.test;

import fleetmanagement.backend.accounts.AccountRepository;
import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.DiagnosisHistoryRepository;
import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.notifications.settings.NotificationSettingRepository;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.operationData.OperationDataHistoryRepository;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.operationData.OperationDataSnapshot;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageSize;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.packages.preprocess.Preprocessor;
import fleetmanagement.backend.repositories.disk.DiagnosisHistorySQLiteRepository;
import fleetmanagement.backend.repositories.disk.OperationDataHistorySQLiteRepository;
import fleetmanagement.backend.repositories.memory.InMemoryAccountRepository;
import fleetmanagement.backend.repositories.memory.InMemoryDiagnosisRepository;
import fleetmanagement.backend.repositories.memory.InMemoryGroupRepository;
import fleetmanagement.backend.repositories.memory.InMemoryNotificationSettingRepository;
import fleetmanagement.backend.repositories.memory.InMemoryOperationDataRepository;
import fleetmanagement.backend.repositories.memory.InMemoryPackageRepository;
import fleetmanagement.backend.repositories.memory.InMemoryPackageTypeRepository;
import fleetmanagement.backend.repositories.memory.InMemoryTaskRepository;
import fleetmanagement.backend.repositories.memory.InMemoryUploadFilterSequenceRepository;
import fleetmanagement.backend.repositories.memory.InMemoryVehicleRepository;
import fleetmanagement.backend.repositories.memory.InMemoryWidgetRepository;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.FilterSequenceRepository;
import fleetmanagement.backend.vehicles.ConnectionStatusRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.backend.widgets.WidgetRepository;
import fleetmanagement.config.Settings;
import fleetmanagement.usecases.DeletePackage;
import fleetmanagement.usecases.InstallPackage;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public class TestScenario {
    public ConnectionStatusRepository connectionStatusRepository;
    public PackageRepository packageRepository;
    public VehicleRepository vehicleRepository;
    public GroupRepository groupRepository;
    public TaskRepository taskRepository;
    public DiagnosisRepository diagnosisRepository;
    public PackageTypeRepository packageTypeRepository;
    public OperationDataHistoryRepository operationDataHistoryRepository;
    public DiagnosisHistoryRepository diagnosisHistoryRepository;
    public OperationDataRepository operationDataRepository;
    public FilterSequenceRepository filterRepository;
    public NotificationSettingRepository notificationRepository;
    public InstallPackage installPackage;
    public DeletePackage deletePackage;
    public AccountRepository accountRepository;
    public WidgetRepository widgetRepository;
    @Mock
    public Preprocessor preprocessor;
    @Mock
    public NotificationService notificationService;
    @Mock
    public Settings settings;

    public final LicenceStub licence;

    public TestScenario() {
        MockitoAnnotations.initMocks(this);
        licence = new LicenceStub();
        packageRepository = new InMemoryPackageRepository(taskRepository, licence);
        taskRepository = new InMemoryTaskRepository(packageRepository);
        connectionStatusRepository = mock(ConnectionStatusRepository.class);
        vehicleRepository = new InMemoryVehicleRepository(licence, connectionStatusRepository, taskRepository);
        groupRepository = new InMemoryGroupRepository();
        diagnosisHistoryRepository = mock(DiagnosisHistorySQLiteRepository.class);
        diagnosisRepository = new InMemoryDiagnosisRepository(diagnosisHistoryRepository);
        packageTypeRepository = new InMemoryPackageTypeRepository();
        operationDataHistoryRepository = mock(OperationDataHistorySQLiteRepository.class);
        operationDataRepository = new InMemoryOperationDataRepository(operationDataHistoryRepository);
        filterRepository = new InMemoryUploadFilterSequenceRepository(settings);
        notificationRepository = new InMemoryNotificationSettingRepository();
        installPackage = new InstallPackage(taskRepository, packageRepository, vehicleRepository, licence);
        deletePackage = new DeletePackage(packageRepository, taskRepository, vehicleRepository, groupRepository);
        accountRepository = new InMemoryAccountRepository();
        widgetRepository = new InMemoryWidgetRepository();
    }

    public Vehicle addVehicleWithGroup(String name, String groupId) {
        Vehicle v = new Vehicle(name, null, name, "1.2.34567.0", ZonedDateTime.now(), groupId, false, 1);
        vehicleRepository.insert(v);
        return v;
    }

    public Vehicle addVehicle(String name) {
        Vehicle v = new Vehicle(name, null, name, "1.2.34567.0", ZonedDateTime.now(), null, false, 1);
        vehicleRepository.insert(v);
        return v;
    }

    public Vehicle addVehicleWithAdditionalUic(String name, String additional_uic) {
        Vehicle v = new Vehicle(name, additional_uic, name, "1.2.34567.0", ZonedDateTime.now(), null, false, 1);
        vehicleRepository.insert(v);
        return v;
    }

    public Vehicle addVehicle() {
        int vehicleIndex = vehicleRepository.listAll().size() + 1;
        return addVehicle("Vehicle " + vehicleIndex);
    }

    public Package addPackage(PackageType type, String version) {
        return addPackage(type, version, null, null, null);
    }

    public Package addPackage(PackageType type, String version, Integer slot, String startPeriod, String endPeriod) {
        Package pkg = new Package(UUID.randomUUID(), type, version, null,
                new PackageSize(2, 1024), slot, startPeriod, endPeriod);
        pkg.source = "Source: Unit test";
        packageRepository.insert(pkg);
        return pkg;
    }

    public Task addTask(Vehicle vehicle, Package pkg) {
        InstallPackage workflow = new InstallPackage(taskRepository, packageRepository, vehicleRepository, licence);
        Task task = workflow.startInstallation(pkg, Collections.singletonList(vehicle), null).startedTasks.get(0);
        return task;
    }

    public Diagnosis addDiagnosis(Vehicle vehicle) {
        Diagnosis d = new Diagnosis(vehicle.id);
        diagnosisRepository.insert(d);
        return d;
    }

    public Diagnosis addDiagnosis(Vehicle vehicle, List<DiagnosedDevice> devices) {
        Diagnosis d = new Diagnosis(vehicle.id, ZonedDateTime.now(), devices);
        diagnosisRepository.insert(d);
        return d;
    }

    public Diagnosis addDiagnosis(Vehicle vehicle, ZonedDateTime lastUpdated, List<DiagnosedDevice> devices) {
        Diagnosis d = new Diagnosis(vehicle.id, lastUpdated, devices);
        diagnosisRepository.insert(d);
        return d;
    }

    public Group addGroup(String name) {
        return addGroup(name, null, false);
    }

    public Group addGroup(String name, String dir, boolean isAutoSyncEnabled) {
        Group g = new Group(name, dir, isAutoSyncEnabled);
        groupRepository.insert(g);
        return g;
    }

    public OperationData addOperationData(OperationDataRepository operationDataRepository, UUID vehicleId, ZonedDateTime time, Indicator... indicators) {
        OperationDataSnapshot operationDataSnapshot = new OperationDataSnapshot();
        operationDataSnapshot.created = time;
        for (Indicator indicator : indicators) {
            indicator.updated = operationDataSnapshot.created;
            operationDataSnapshot.indicators.add(indicator);
        }
        return operationDataRepository.integrateSnapshot(vehicleId, operationDataSnapshot);
    }

    public OperationData addOperationData(OperationDataRepository operationDataRepository, UUID vehicleId, Indicator... indicators) {
        return addOperationData(operationDataRepository, vehicleId, ZonedDateTime.now(), indicators);
    }

    public OperationData addOperationData(UUID vehicleId, Indicator... indicators) {
        return addOperationData(operationDataRepository, vehicleId, indicators);
    }

    public OperationData addOperationData(UUID vehicleId, Indicator indicator) {
        return addOperationData(vehicleId, new Indicator[]{indicator});
    }

    public Vehicle getVehicle(Vehicle vehicle) {
        return getVehicle(vehicle.id);
    }

    public Vehicle getVehicle(UUID vehicleId) {
        return vehicleRepository.tryFindById(vehicleId);
    }

}
