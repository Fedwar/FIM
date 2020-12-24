package fleetmanagement.backend.repositories.migration;

import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.operationData.OperationDataHistoryRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MigrateIndicatorsHistoryToSQLite {

    final OperationDataHistoryRepository historyRepository;
    final List<OperationData> operationDataList;

    public MigrateIndicatorsHistoryToSQLite(OperationDataHistoryRepository historyRepository, List<OperationData> operationDataList) {
        this.historyRepository = historyRepository;
        this.operationDataList = operationDataList;
    }

    public List<OperationData> migrate() {
        List<OperationData> migrationList = new ArrayList();
        HashMap<Indicator, List<History>> indicatorsHistory = new HashMap<>();
        for (OperationData operationData : operationDataList) {
            indicatorsHistory.clear();
            for (Indicator indicator : operationData.indicators) {
                List<History> history = indicator.getHistory();
                if (history != null && !history.isEmpty()) {
                    indicatorsHistory.put(indicator, new ArrayList<>(history));
                    indicator.clearHistory();
                }
            }
            if (!indicatorsHistory.isEmpty()) {
                historyRepository.addHistory(operationData.vehicleId, indicatorsHistory);
                migrationList.add(operationData);
            }
        }
        return migrationList;
    }


}
