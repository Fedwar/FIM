package fleetmanagement.backend.operationData;



import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OperationDataHistoryRepository {

    void addHistory(UUID vehicleId, Indicator indicator) throws SQLException;

    void addHistory(UUID vehicleId, List<Indicator> indicators);

    List<History> getHistory(UUID vehicleId, String indicatorId);

    void delete(UUID vehicleId, Indicator indicator);

    void delete(UUID vehicleId, List<Indicator> indicators);

    void addHistory(UUID vehicleId, Map<Indicator, List<History>> historyMap);

    List<History> getHistoryRange(UUID vehicleId, String indicatorId, ZonedDateTime beginDate, ZonedDateTime endDate, ChronoUnit timeUnit);

    List<History> getHistoryRange(UUID vehicleId, String indicatorId, ZonedDateTime beginDate, ZonedDateTime endDate);

    Map<String, List<History>> getHistoryRange(UUID vehicleId, List<String> indicatorIdList, ZonedDateTime beginDate, ZonedDateTime endDate);

    History getOldestHistory(UUID vehicleId);

    List<History> getHistory(UUID vehicleId, String indicatorId, int limit);

    void reduceHistory(UUID vehicleId, ZonedDateTime toDate);

    long getDataSize(UUID id);


}