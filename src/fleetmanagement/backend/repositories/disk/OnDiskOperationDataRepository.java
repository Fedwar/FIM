package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.operationData.OperationDataHistoryRepository;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.operationData.OperationDataSnapshot;
import fleetmanagement.backend.repositories.disk.xml.OperationDataXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.backend.repositories.migration.MigrateIndicatorsHistoryToSQLite;
import fleetmanagement.config.FimConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Component
public class OnDiskOperationDataRepository extends GenericOnDiskRepository<OperationData, UUID> implements OperationDataRepository {

    public static final Logger logger = Logger.getLogger(OnDiskOperationDataRepository.class);

    @Autowired
    private OperationDataHistoryRepository historyRepository;

    @Autowired
    public OnDiskOperationDataRepository(FimConfig config) {
        super(config.getVehicleDirectory());
    }

    protected OnDiskOperationDataRepository(File directory, OperationDataHistoryRepository historyRepository) {
        super(directory);
        this.historyRepository = historyRepository;
    }

    @Override
    protected XmlFile<OperationData> getXmlFile(File dir) {
        return new OperationDataXmlFile(dir);
    }

    @PostConstruct
    @Override
    public void loadFromDisk() {
        logger.debug("Loading from disk: operations");
        super.loadFromDisk();
        migrate();
    }

    private void migrate() {
        List<OperationData> migratedList =  new MigrateIndicatorsHistoryToSQLite(historyRepository, persistables).migrate();
        for (OperationData operationData : migratedList) {
            persist(operationData);
        }
    }

    @Override
    public void insert(OperationData operationData) {
        super.insert(operationData);
        historyRepository.addHistory(operationData.vehicleId, operationData.indicators);
    }

    @Override
    public OperationData update(UUID id, Consumer<OperationData> changes) {
        throw new UnsupportedOperationException();
    }

    private OperationData updateOrInsert(UUID vehicleId, Consumer<OperationData> updateConsumer) {
        OperationData operationData = tryFindById(vehicleId);
        if (operationData == null) {
            operationData = new OperationData(vehicleId);
            updateConsumer.accept(operationData);
            persist(operationData);
            persistables.add(operationData);
            return operationData;
        } else {
            return update(operationData, updateConsumer);
        }
    }

    @Override
    public void delete(UUID vehicleId) {
        OperationData toDelete = tryFindById(vehicleId);
        if (toDelete != null) {
            persistables.remove(toDelete);
            OperationDataXmlFile operationDataXmlFile = new OperationDataXmlFile(getDirectory(toDelete));
            operationDataXmlFile.delete();
            historyRepository.delete(toDelete.vehicleId, toDelete.indicators);
        }
    }

    public OperationData integrateSnapshot(UUID vehicleId, OperationDataSnapshot snapshot) {
        OperationData operationData = updateOrInsert(vehicleId, o -> {
            o.updated = snapshot.created;
            for (Indicator indicator : snapshot.indicators) {
                o.setIndicatorValue(indicator.id, indicator.value, indicator.unit);
            }
        });
        historyRepository.addHistory(vehicleId, snapshot.indicators);
        return operationData;
    }

    @Override
    public List<History> getIndicatorHistory(UUID vehicleId, String indicatorId) {
        return historyRepository.getHistory(vehicleId,  indicatorId);
    }

    @Override
    public List<History> getIndicatorHistoryRange(UUID vehicleId, String indicatorId, ZonedDateTime beginDate, ZonedDateTime endDate, ChronoUnit timeUnit) {
        return historyRepository.getHistoryRange(vehicleId,  indicatorId, beginDate, endDate, timeUnit);
    }

    @Override
    public Map<String, List<History>> getIndicatorsHistoryRange(UUID vehicleId, List<String> indicatorIdList, ZonedDateTime beginDate, ZonedDateTime endDate) {
        return historyRepository.getHistoryRange(vehicleId,  indicatorIdList, beginDate, endDate);
    }

    @Override
    public List<History> getHistory(UUID vehicleId, String indicatorId, int limit) {
        return historyRepository.getHistory(vehicleId,  indicatorId, limit);
    }

}
