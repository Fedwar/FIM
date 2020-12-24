package fleetmanagement.backend.vehicles;

import fleetmanagement.backend.reports.datasource.vehicles.ConnectionStatus;
import gsp.util.DoNotObfuscate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@DoNotObfuscate
public interface VehicleRepository {

    void insert(Vehicle vehicle);

    void insertOrUpdate(Vehicle vehicle, Consumer<Vehicle> updateConsumer);

    Vehicle update(UUID id, Consumer<Vehicle> update);

    void delete(UUID id);

    List<Vehicle> listAll();

    List<Vehicle> listByGroup(String groupId);

    Vehicle tryFindByUIC(String uic);

    Vehicle tryFindById(UUID id);

}