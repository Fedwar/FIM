package fleetmanagement.backend.diagnosis;

import fleetmanagement.backend.vehicles.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Component
public class SnapshotConversionService {

    @Autowired
    private DiagnosisRepository diagnoses;
    @Autowired
    private DiagnosisHistoryRepository historyRepository;
    @Autowired
    private VehicleRepository vehicles;

    public SnapshotConversionService() {
    }

    public SnapshotConversionService(DiagnosisRepository repository, DiagnosisHistoryRepository diagnosisHistoryRepository, VehicleRepository vehicles) {
        this.diagnoses = repository;
        this.historyRepository = diagnosisHistoryRepository;
        this.vehicles = vehicles;
    }

    public Diagnosis integrateNewSnapshot(Snapshot snapshot) {
        Diagnosis diagnosis = updateDiagnosis(snapshot);
        updateDiagnosticSummary(diagnosis);
        return diagnosis;
    }

    private Diagnosis updateDiagnosis(Snapshot snapshot) {
        if (vehicles.tryFindById(snapshot.vehicleId) == null) {
            diagnoses.delete(snapshot.vehicleId);
            return null;
        }

        if (diagnoses.tryFindByVehicleId(snapshot.vehicleId) == null) {
            Diagnosis diagnosis = new Diagnosis(snapshot.vehicleId);
            integrate(diagnosis, snapshot);
            diagnoses.insert(diagnosis);
        } else {
            diagnoses.update(snapshot.vehicleId, diag -> {
                integrate(diag, snapshot);
            });
        }
        return diagnoses.tryFindByVehicleId(snapshot.vehicleId);
    }

    private void updateDiagnosticSummary(Diagnosis diagnosis) {
        vehicles.update(diagnosis.getVehicleId(), v -> {
            if (v != null)
                v.updateDiagnosticSummary(diagnosis);
        });
    }

    private void integrate(Diagnosis diagnosis, Snapshot snapshot) {
        diagnosis.setLastUpdated(snapshot.timestamp);
        integrateExistingDevices(diagnosis, snapshot);
        disableMissingDevices(diagnosis, snapshot);
    }

    private void integrateExistingDevices(Diagnosis diagnosis, Snapshot snapshot) {
        for (DeviceSnapshot deviceSnapshot : snapshot.devices) {
            if (snapshot.version == 1) {
                integrateDeviceV1(diagnosis, deviceSnapshot, snapshot.timestamp);
            } else {
                integrateDeviceV2(diagnosis, deviceSnapshot, snapshot.timestamp);
            }
        }
    }

    protected void integrateDeviceV1(Diagnosis diagnosis, DeviceSnapshot deviceSnapshot, ZonedDateTime timestamp) {
        DeviceSnapshot.StateSnapshot state = deviceSnapshot.states.stream().findFirst().orElse(null);
        StateEntry newState;
        if (state == null)
            newState = new StateEntry(timestamp, null, "N/A", null, null);
        else
            newState = new StateEntry(timestamp, null, state.code, state.type, state.description);

        StateEntry lastState = diagnoses.getLatestDeviceHistoryRecord(diagnosis.getVehicleId(), deviceSnapshot.id);
        if (lastState == null) {
            diagnoses.insertDeviceHistory(diagnosis.getVehicleId(), deviceSnapshot.id, newState);
        } else {
            if (lastState.isStatusEquivalent(newState)) {
                newState = lastState.clone();
           } else {
                diagnoses.deleteDeviceHistory(diagnosis.getVehicleId(), deviceSnapshot.id, lastState);
                diagnoses.insertDeviceHistory(diagnosis.getVehicleId(), deviceSnapshot.id, asList(newState, lastState.endingAt(timestamp)));
            }
        }

        DiagnosedDevice diagnosedDevice = new DiagnosedDevice(deviceSnapshot.id, deviceSnapshot.location, deviceSnapshot.name, deviceSnapshot.type
                , deviceSnapshot.status, Collections.singletonList(newState), false, deviceSnapshot.versions, null);
        diagnosis.updateDevice(diagnosedDevice);
    }


    protected void integrateDeviceV2(Diagnosis diagnosis, DeviceSnapshot deviceSnapshot, ZonedDateTime timestamp) {
        String deviceId = deviceSnapshot.id;
        UUID vehicleId = diagnosis.getVehicleId();
        List<StateEntry> states = deviceSnapshot.states.stream().map(DeviceSnapshot.StateSnapshot::toStateEntry)
                .collect(Collectors.toList());


        List<StateEntry> unfinishedHistory = historyRepository.getUnfinishedHistory(vehicleId, deviceId);
        //adding history record for states that does not have one
        ListIterator<StateEntry> stateIterator = states.listIterator();
        while (stateIterator.hasNext()) {
            StateEntry stateEntry =  stateIterator.next();
            StateEntry unfinished = unfinishedHistory.stream()
                    .filter(s -> s.isStatusEquivalent(stateEntry))
                    .findFirst().orElse(null);
            if (unfinished == null) {
                historyRepository.addHistory(vehicleId, deviceId, stateEntry.startingAt(timestamp));
                stateIterator.set(stateEntry.startingAt(timestamp));
            } else {
                unfinishedHistory.remove(unfinished);
                stateIterator.set(stateEntry.startingAt(unfinished.start));
            }
        }

        //closing history records that should be closed
        for (StateEntry unfinished : unfinishedHistory) {
            historyRepository.delete(vehicleId, deviceId, unfinished);
            historyRepository.addHistory(vehicleId, deviceId, unfinished.endingAt(timestamp));
        }

        //adding history records from snapshot
        List<StateEntry> history = deviceSnapshot.statesHistory.stream()
                .map(DeviceSnapshot.StateSnapshot::toStateEntry)
                .collect(Collectors.toList());
        for (StateEntry historyState : history) {
            historyRepository.addHistory(vehicleId, deviceId, historyState);
        }

        DiagnosedDevice diagnosedDevice = new DiagnosedDevice(deviceId, deviceSnapshot.location,
                deviceSnapshot.name, deviceSnapshot.type, deviceSnapshot.status, states,
                false, deviceSnapshot.versions, null);

        diagnosis.updateDevice(diagnosedDevice);

    }

    private void disableMissingDevices(Diagnosis diagnosis, Snapshot snapshot) {
        diagnosis.getDevices().stream().filter(x -> !snapshot.hasDevice(x.getId())).forEach(DiagnosedDevice::disable);
    }

}
