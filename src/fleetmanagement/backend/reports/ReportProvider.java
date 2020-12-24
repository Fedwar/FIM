package fleetmanagement.backend.reports;

import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.reports.datasource.ReportDataSource;
import fleetmanagement.backend.reports.datasource.vehicles.ConnectionStatusReportDataSource;
import fleetmanagement.backend.reports.datasource.vehicles.DiagnosisReportDataSource;
import fleetmanagement.backend.reports.datasource.vehicles.OperationDataReportDataSource;
import fleetmanagement.backend.vehicles.ConnectionStatusRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReportProvider {
    @Autowired
    private VehicleRepository vehicles;
    @Autowired
    private ConnectionStatusRepository connectionStatusRepository;
    @Autowired
    private DiagnosisRepository diagnosisRepository;
    @Autowired
    private OperationDataRepository operationDataRepository;

    public ReportProvider() {
    }

    ReportProvider(
            VehicleRepository vehicles,
            ConnectionStatusRepository connectionStatusRepository,
            DiagnosisRepository diagnosisRepository,
            OperationDataRepository operationData) {
        this.vehicles = vehicles;
        this.connectionStatusRepository = connectionStatusRepository;
        this.diagnosisRepository = diagnosisRepository;
        this.operationDataRepository = operationData;
    }

    ReportDataSource getReportDataSource(Report report) {
        Map<String, String> filters = report.getFilters();
        switch (report.getReportType()) {
            case DIAGNOSIS:
                return new DiagnosisReportDataSource(diagnosisRepository, vehicles, filters);
            case OPERATION_DATA:
                return new OperationDataReportDataSource(operationDataRepository, vehicles, filters);
            case CONNECTION_STATUS:
                return new ConnectionStatusReportDataSource(vehicles, connectionStatusRepository, filters);
        }
        return null;
    }

    public void setVehicles(VehicleRepository vehicles) {
        this.vehicles = vehicles;
    }

    public void setDiagnosisRepository(DiagnosisRepository diagnosisRepository) {
        this.diagnosisRepository = diagnosisRepository;
    }

    public void setOperationDataRepository(OperationDataRepository operationDataRepository) {
        this.operationDataRepository = operationDataRepository;
    }
}