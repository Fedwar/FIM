package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.DiagnosisHistoryRepository;
import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.diagnosis.StateEntry;
import fleetmanagement.backend.repositories.disk.xml.DiagnosisXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.backend.repositories.exception.DiagnosisDuplicationException;
import fleetmanagement.backend.repositories.migration.MigrateDiagnosisHistoryToSQLite;
import fleetmanagement.config.FimConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class OnDiskDiagnosisRepository implements DiagnosisRepository {
    private static final Logger logger = Logger.getLogger(OnDiskDiagnosisRepository.class);

    protected final Map<UUID, Diagnosis> diagnoses = new ConcurrentHashMap<>();
    private final File directory;
    @Autowired
    private DiagnosisHistoryRepository historyRepository;

    @Autowired
    public OnDiskDiagnosisRepository(FimConfig fimConfig) {
        this.directory = fimConfig.getVehicleDirectory();
    }

    public OnDiskDiagnosisRepository(File directory, DiagnosisHistoryRepository historyRepository) {
        this.directory = directory;
        this.historyRepository = historyRepository;
    }

    protected XmlFile<Diagnosis> getXmlFile(File dir) {
        return new DiagnosisXmlFile(dir);
    }

    @Override
    public List<Diagnosis> listAll() {
        return new ArrayList<>(diagnoses.values());
    }

    @Override
    public Map<UUID, Diagnosis> mapAll() {
        return diagnoses;
    }

    @PostConstruct
    public void loadFromDisk() {
        logger.debug("Loading from disk: diagnosis");
        directory.mkdirs();

        for (File vehicleDir : directory.listFiles()) {
            try {
                loadDiagnosisFromDirectory(vehicleDir);
            } catch (Exception e) {
                logger.error("Diagnosis data in " + vehicleDir + " seems broken.", e);
            }
        }

        migrate();
    }

    private void migrate() {
        List<Diagnosis> migratedList = new MigrateDiagnosisHistoryToSQLite(historyRepository, diagnoses.values()).migrate();
        for (Diagnosis diagnosis : migratedList) {
            diagnoses.put(diagnosis.getVehicleId(), diagnosis);
            save(diagnosis);
        }
    }

    private void loadDiagnosisFromDirectory(File vehicleDir) {
        UUID vehicleId = UUID.fromString(vehicleDir.getName());
        XmlFile<Diagnosis> diagnosisXml = getDiagnosisXmlFile(vehicleId);
        if (!diagnosisXml.exists())
            return;

        diagnoses.put(vehicleId, diagnosisXml.load());
    }

    @Override
    public synchronized void insert(Diagnosis diagnosis) {
        if (!existsInMap(diagnosis)) {
            diagnoses.put(diagnosis.getVehicleId(), diagnosis);
            save(diagnosis);
        } else
            throw new DiagnosisDuplicationException(diagnosis.getVehicleId());
    }

    @Override
    public synchronized void delete(UUID vehicleId) {
        Diagnosis toDelete = tryFindByVehicleId(vehicleId);
        if (toDelete != null) {
            diagnoses.remove(vehicleId);
            XmlFile<Diagnosis> diagnosisXml = getDiagnosisXmlFile(toDelete);
            diagnosisXml.delete();
        }
    }

    @Override
    public Diagnosis tryFindByVehicleId(UUID vehicleId) {
        return diagnoses.get(vehicleId);
    }

    @Override
    public synchronized void update(UUID vehicleId, Consumer<Diagnosis> update) {
        Diagnosis original = tryFindByVehicleId(vehicleId);
        if (original != null) {
            Diagnosis cloned = original.clone();
            update.accept(cloned);
            diagnoses.put(cloned.getVehicleId(), cloned);
            save(cloned);
        } else {
            update.accept(null);
        }
    }

    @Override
    public List<StateEntry> getDiagnosedDeviceHistory(UUID vehicleId, String deviceId) {
        return historyRepository.getHistory(vehicleId, deviceId);
    }

    @Override
    public StateEntry getLatestDeviceHistoryRecord(UUID vehicleId, String deviceId) {
        return historyRepository.getLatestHistory(vehicleId, deviceId);
    }

    @Override
    public List<StateEntry> getDeviceHistoryRange(UUID vehicleId, String deviceId, ZonedDateTime start) {
        return historyRepository.getHistoryRange(vehicleId, deviceId, start, start);
    }


    @Override
    public void insertDeviceHistory(UUID vehicleId, String id, StateEntry state) {
        historyRepository.addHistory(vehicleId, id, state);
    }

    @Override
    public void insertDeviceHistory(UUID vehicleId, String id, List<StateEntry> states) {
        if (!states.isEmpty())
            historyRepository.addHistory(vehicleId, id, states);
    }

    @Override
    public void deleteDeviceHistory(UUID vehicleId, String id) {
        historyRepository.delete(vehicleId, id);
    }

    @Override
    public void deleteDeviceHistory(UUID vehicleId, String id, StateEntry state) {
        historyRepository.delete(vehicleId, id, state);
    }

    private void save(Diagnosis diagnosis) {
        XmlFile<Diagnosis> diagnosisXml = getDiagnosisXmlFile(diagnosis);
        diagnosisXml.save(diagnosis);
    }

    private XmlFile<Diagnosis> getDiagnosisXmlFile(Diagnosis diagnosis) {
        return getDiagnosisXmlFile(diagnosis.getVehicleId());
    }

    private XmlFile<Diagnosis> getDiagnosisXmlFile(UUID vehicleId) {
        return getXmlFile(new File(directory, vehicleId.toString()));
    }

    private boolean existsInMap(Diagnosis diagnosis) {
        return diagnoses.containsKey(diagnosis.getVehicleId());
    }
}
