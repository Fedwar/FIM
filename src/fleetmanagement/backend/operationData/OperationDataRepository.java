package fleetmanagement.backend.operationData;


import fleetmanagement.backend.repositories.Repository;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public interface OperationDataRepository extends Repository<OperationData, UUID> {

    OperationData integrateSnapshot(UUID vehicleId, OperationDataSnapshot snapshot);

    List<History> getIndicatorHistory(UUID vehicleId, String indicatorId);

    List<History> getIndicatorHistoryRange(UUID vehicleId, String indicatorId, ZonedDateTime beginDate, ZonedDateTime endDate, ChronoUnit timeUnit);

    Map<String, List<History>> getIndicatorsHistoryRange(UUID vehicleId, List<String> indicatorIdList, ZonedDateTime beginDate, ZonedDateTime endDate);

    List<History> getHistory(UUID vehicleId, String indicatorId, int limit);


}