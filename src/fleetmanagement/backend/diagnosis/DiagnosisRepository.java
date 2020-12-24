package fleetmanagement.backend.diagnosis;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public interface DiagnosisRepository {

    void insert(Diagnosis diagnosis);

    void delete(UUID vehicleId);

    Diagnosis tryFindByVehicleId(UUID vehicleId);

    void update(UUID vehicleId, Consumer<Diagnosis> update);

    List<Diagnosis> listAll();

    Map<UUID, Diagnosis> mapAll();

    List<StateEntry> getDiagnosedDeviceHistory(UUID vehicleId, String deviceId);

    StateEntry getLatestDeviceHistoryRecord(UUID vehicleId, String deviceId);

    List<StateEntry> getDeviceHistoryRange(UUID vehicleId, String deviceId, ZonedDateTime start);

    void insertDeviceHistory(UUID vehicleId, String id, StateEntry state);

    void insertDeviceHistory(UUID vehicleId, String id, List<StateEntry> states);

    void deleteDeviceHistory(UUID vehicleId, String id);

    void deleteDeviceHistory(UUID vehicleId, String id, StateEntry state);
}