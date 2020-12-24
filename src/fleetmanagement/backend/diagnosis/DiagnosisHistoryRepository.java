package fleetmanagement.backend.diagnosis;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public interface DiagnosisHistoryRepository {
    void addHistory(UUID vehicleId, String deviceId, StateEntry stateEntry);

    void addHistory(UUID vehicleId, String deviceId, List<StateEntry> stateEntries);

    List<StateEntry> getHistoryRange(UUID vehicleId, String indicatorId, ZonedDateTime earliestReportDate, ZonedDateTime latestReportDate);

    List<StateEntry> getHistory(UUID vehicleId, String deviceId);

    StateEntry getLatestHistory(UUID vehicleId, String deviceId);

    void delete(UUID vehicleId, String deviceId, StateEntry stateEntry);

    void delete(UUID vehicleId, String deviceId, List<StateEntry> stateEntries);

    void delete(UUID vehicleId, String deviceId);

    StateEntry getOldestHistory(UUID vehicleId);

    List<StateEntry> getUnfinishedHistory(UUID vehicleId, String id);

    void reduceHistory(UUID vehicleId, ZonedDateTime toDate);

    long getDataSize(UUID id);


}
