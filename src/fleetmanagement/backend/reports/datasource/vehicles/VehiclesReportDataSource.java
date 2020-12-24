package fleetmanagement.backend.reports.datasource.vehicles;

import fleetmanagement.backend.reports.datasource.ReportDataSource;
import fleetmanagement.backend.vehicles.VehicleRepository;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

abstract class VehiclesReportDataSource implements ReportDataSource {
    VehicleRepository vehicles;
    ZonedDateTime earliestReportDate;
    ZonedDateTime latestReportDate;
    List<String> selectedVehicles;
    ChronoUnit rangeBy;

    VehiclesReportDataSource(VehicleRepository vehicles, Map<String, String> filters) {
        this.vehicles = vehicles;
        ZonedDateTime reportDate1 = ZonedDateTime.parse(filters.get("earliestReportDate") + "T00:00:00-00:00");
        ZonedDateTime reportDate2 = ZonedDateTime.parse(filters.get("latestReportDate") + "T23:59:59-00:00");
        earliestReportDate = reportDate1.isBefore(reportDate2) ? reportDate1 : reportDate2;
        latestReportDate = reportDate1.isBefore(reportDate2) ? reportDate2 : reportDate1;

        selectedVehicles = Arrays.asList(filters.get("selectedVehicles").split(","));
        rangeBy = VehiclesReportDataSourceUtils.getChronoUnit(filters.get("rangeBy"));
    }

    abstract void generateData();
}
