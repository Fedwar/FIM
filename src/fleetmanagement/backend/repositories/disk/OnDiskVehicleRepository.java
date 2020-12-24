package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.reports.datasource.vehicles.ConnectionStatus;
import fleetmanagement.backend.repositories.disk.xml.VehicleXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.backend.repositories.exception.VehicleCountExceeded;
import fleetmanagement.backend.repositories.exception.VehicleDuplicationException;
import fleetmanagement.backend.repositories.migration.DatabaseMigrations;
import fleetmanagement.backend.repositories.migration.DistinguishBetweenIndis3AndIndis5MultimediaContent;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.ConnectionStatusRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Licence;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@DoNotObfuscate
@Component
public class OnDiskVehicleRepository implements VehicleRepository {

    private static final Logger logger = Logger.getLogger(OnDiskVehicleRepository.class);
    protected final List<Vehicle> vehicles = new CopyOnWriteArrayList<>();
    @Autowired
    private Licence licence;
    @Autowired
    protected ConnectionStatusRepository connectionStatusRepository;
    @Autowired
    private TaskRepository tasks;
    @Autowired
    DiagnosisRepository diagnoses;

    private final File directory;

    @Autowired
    public OnDiskVehicleRepository(FimConfig config) {
        this.directory = config.getVehicleDirectory();
    }

    public OnDiskVehicleRepository(File directory, Licence licence, ConnectionStatusRepository connectionStatusRepository, TaskRepository tasks,
                            DiagnosisRepository diagnoses) {
        this.directory = directory;
        this.licence = licence;
        this.connectionStatusRepository = connectionStatusRepository;
        this.tasks = tasks;
        this.diagnoses = diagnoses;
    }

    @PostConstruct
    public void loadFromDisk() {
        logger.debug("Loading from disk: vehicles");
        directory.mkdirs();
        DeletionHelper.performPendingDeletes(directory);
        runDatabaseMigrations();
        loadFilesFromDisk();
        updateDiagnosticSummaries(diagnoses);
    }

    protected XmlFile<Vehicle> getXmlFile(File dir) {
        return new VehicleXmlFile(dir, tasks);
    }

    @Override
    public void insert(Vehicle vehicle) {
        if (!existsInList(vehicle)) {
            if (licence.getMaximumVehicleCount() <= vehicles.size()) {
                throw new VehicleCountExceeded("Vehicle count exceeds licenced limit of " + licence.getMaximumVehicleCount());
            }
            vehicles.add(vehicle);
            save(vehicle);
        } else
            throw new VehicleDuplicationException(vehicle.uic);
    }

    @Override
    public Vehicle update(UUID id, Consumer<Vehicle> updateConsumer) {
        Vehicle v = updateSync(id, updateConsumer);
        if (v != null) {
            save(v);
        }
        return v;
    }

    @Override
    public void insertOrUpdate(Vehicle vehicle, Consumer<Vehicle> updateConsumer) {
        Vehicle stored = insertOrUpdateSync(vehicle, updateConsumer);
        save(stored);
    }

    public synchronized Vehicle insertOrUpdateSync(Vehicle vehicle, Consumer<Vehicle> updateConsumer) {
        Vehicle stored = tryFindByUIC(vehicle.uic);
        if (stored == null) {
            if (licence.getMaximumVehicleCount() <= vehicles.size()) {
                throw new VehicleCountExceeded("Vehicle count exceeds licenced limit of " + licence.getMaximumVehicleCount());
            }
            vehicles.add(vehicle);
            stored = vehicle;
        } else {
            stored = updateVehicle(stored, updateConsumer);
        }
        return stored;
    }

    private synchronized Vehicle updateSync(UUID id, Consumer<Vehicle> updateConsumer) {
        Vehicle vehicle = tryFindById(id);
        if (vehicle != null) {
            return updateVehicle(vehicle, updateConsumer);
        } else {
            updateConsumer.accept(null);
            return null;
        }
    }

    private Vehicle updateVehicle(Vehicle vehicle, Consumer<Vehicle> updateConsumer) {
        Vehicle cloned = makeClone(vehicle);
        updateConsumer.accept(cloned);
       // vehicles.set(vehicles.indexOf(vehicle), cloned);
        vehicles.remove(vehicle);
        vehicles.add(cloned);
        return cloned;
    }

    protected Vehicle makeClone (Vehicle vehicle) {
        return  vehicle.clone();
    }

    private void save(Vehicle v) {
        persist(v);

        if (connectionStatusRepository != null)
            connectionStatusRepository.saveConnectionInfo(v);
    }

    protected void persist(Vehicle v) {
        File vehicleDir = getVehicleDirectory(v);
        if (!vehicleDir.exists() && !vehicleDir.mkdir()) {
            try {
                logger.error("Can't create vehicle data directory! " + vehicleDir.getCanonicalPath());
                return;
            } catch (IOException e) {
                logger.error("Can't make canonical path to vehicle data directory! " + vehicleDir.toString(), e);
                return;
            }
        }
        new VehicleXmlFile(vehicleDir, tasks).save(v);
    }

    @Override
    public void delete(UUID id) {
        Vehicle toRemove = tryFindById(id);
        if (toRemove != null) {
            vehicles.remove(toRemove);
            DeletionHelper.delete(getVehicleDirectory(toRemove));
        }
    }

    @Override
    public List<Vehicle> listAll() {
        return Collections.unmodifiableList(vehicles);
    }

    @Override
    public List<Vehicle> listByGroup(String groupId) {
        return vehicles.stream().filter(x -> x.getGroupId() != null && x.getGroupId().equals(groupId)).collect(Collectors.toList());
    }

    @Override
    public Vehicle tryFindByUIC(String uic) {
        return vehicles.stream().filter(x -> x.uic.equals(uic)).findFirst().orElse(null);
    }

    @Override
    public Vehicle tryFindById(UUID id) {
        return vehicles.stream().filter(x -> x.id.equals(id)).findFirst().orElse(null);
    }

    private void runDatabaseMigrations() {
        DatabaseMigrations migrations = new DatabaseMigrations();
        migrations.addMigrationStep(new DistinguishBetweenIndis3AndIndis5MultimediaContent());
        migrations.performMigrations(directory, "vehicle.xml");
    }

    private void loadFilesFromDisk() {
        for (File vehicleDir : directory.listFiles()) {
            try {
                loadVehicleFromDirectory(vehicleDir);
            } catch (Exception e) {
                logger.error("Vehicle data in " + vehicleDir + " seems broken.", e);
            }
        }
    }

    private void loadVehicleFromDirectory(File vehicleDir) {
        Vehicle v = new VehicleXmlFile(vehicleDir, tasks).load();
        vehicles.add(v);
    }

    private void updateDiagnosticSummaries(DiagnosisRepository diagnoses) {
        for (Vehicle v : vehicles) {
            Diagnosis d = diagnoses.tryFindByVehicleId(v.id);
            if (d != null)
                v.updateDiagnosticSummary(d);
        }
    }

    private File getVehicleDirectory(Vehicle vehicle) {
        return new File(directory, vehicle.id.toString());
    }

    private boolean existsInList(Vehicle vehicle) {
        return !vehicles.stream().filter(v -> v.uic.equals(vehicle.uic)).collect(Collectors.toList()).isEmpty();
    }

    public void setLicence(Licence licence) {
        this.licence = licence;
    }

    public void setConnectionStatusRepository(ConnectionStatusRepository connectionStatusRepository) {
        this.connectionStatusRepository = connectionStatusRepository;
    }

    public void setTasks(TaskRepository tasks) {
        this.tasks = tasks;
    }
}
