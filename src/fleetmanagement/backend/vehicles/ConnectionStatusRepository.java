package fleetmanagement.backend.vehicles;

import fleetmanagement.backend.reports.datasource.vehicles.ConnectionStatus;

import java.util.Map;

public interface ConnectionStatusRepository {
    void saveConnectionInfo(Vehicle vehicle);

    Map<String, ConnectionStatus> getVehicleHours(String earliestReportDate, String latestReportDate, Vehicle v);
}
