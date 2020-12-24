package fleetmanagement.backend.repositories.migration;

import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.DiagnosisHistoryRepository;
import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.operationData.OperationDataHistoryRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MigrateDiagnosisHistoryToSQLite {

    final DiagnosisHistoryRepository historyRepository;
    final Collection<Diagnosis> diagnosisList;

    public MigrateDiagnosisHistoryToSQLite(DiagnosisHistoryRepository historyRepository, Collection<Diagnosis> diagnosisList) {
        this.historyRepository = historyRepository;
        this.diagnosisList = diagnosisList;
    }

    public List<Diagnosis> migrate() {
        List<Diagnosis> migrationList = new ArrayList();
        for (Diagnosis diagnosis : diagnosisList) {
            boolean hasHistory = false;
            for (DiagnosedDevice device : diagnosis.getDevices()) {
                if (device.getErrorHistory().getEntries().size() > 0) {
                    historyRepository.addHistory(diagnosis.getVehicleId(), device.getId(), device.getErrorHistory().getEntries());
                    device.getErrorHistory().clear();
                    hasHistory = true;
                }
            }
            if (hasHistory)
                migrationList.add(diagnosis);
        }
        return migrationList;
    }

}
