package fleetmanagement.backend.reports.datasource.vehicles;

import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.diagnosis.StateEntry;
import fleetmanagement.backend.vehicles.VehicleRepository;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DiagnosisReportDataSource extends VehiclesReportDataSource {
    private DiagnosisRepository diagnosisRepository;
    private Map<UUID, Diagnosis> diagnoses;
    //Map<vehicleName, Map<date, errorCount>>
    private Map<String, Map<String, Integer>> data = new LinkedHashMap<>();

    public DiagnosisReportDataSource(
            DiagnosisRepository diagnosisRepository,
            VehicleRepository vehicles,
            Map<String, String> filters
    ) {
        super(vehicles, filters);
        this.diagnosisRepository = diagnosisRepository;
        this.diagnoses = diagnosisRepository.mapAll();
        generateData();
    }

    public Map getData() {
        return data;
    }

    @Override
    void generateData() {
        Map<String, Integer> allErrorsCount = new HashMap<>();
        for (String vehicleId : selectedVehicles) {
            UUID uuid = UUID.fromString(vehicleId);
            Map<String, Integer> errorsCount = getErrorCount(uuid);
            Map<String, Integer> sortedErrorsCount = VehiclesReportDataSourceUtils.getSortedMapByKey(errorsCount);
            data.put(vehicles.tryFindById(uuid).getName(), sortedErrorsCount);
            for (String date : sortedErrorsCount.keySet()) {
                if (!allErrorsCount.containsKey(date))
                    allErrorsCount.put(date, 0);
                allErrorsCount.put(date, allErrorsCount.get(date) + sortedErrorsCount.get(date));
            }
        }

        Map<String, Integer> sortedAllErrorsCount = VehiclesReportDataSourceUtils.getSortedMapByKey(allErrorsCount);
        data.put("All vehicles errors count", sortedAllErrorsCount);
    }

    private Map<String, Integer> getErrorCount(UUID uuid) {
        Map<String, Integer> errorsCount = new HashMap<>();
        fillAllDatesWithZeros(errorsCount);

        Diagnosis diagnosis = diagnoses.get(uuid);
        if (diagnosis == null)
            return errorsCount;

        for (DiagnosedDevice device : diagnosis.getDevices()) {
            List<StateEntry> errors = diagnosisRepository.getDiagnosedDeviceHistory(uuid, device.getId());
            for (StateEntry error : errors) {
                if (error.end == null) {
                    if (isPeriodInRange(error.start, error.start)) {
                        putError(errorsCount, error.start);
                    }
                } else {
                    List<ZonedDateTime> datesList =
                            VehiclesReportDataSourceUtils.getDatesBetween(error.start, error.end, rangeBy);
                    if (isPeriodInRange(
                            error.start,
                            VehiclesReportDataSourceUtils.getChronoUnitEnd(error.start, rangeBy))
                    )
                        putError(errorsCount, error.start);
                    if (
                            isPeriodInRange(
                                    VehiclesReportDataSourceUtils.getChronoUnitStart(error.end, rangeBy),
                                    error.end
                            ) &
                                    VehiclesReportDataSourceUtils.getChronoUnitEnd(error.start, rangeBy)
                                            .isBefore(error.end)
                    )
                        putError(errorsCount, error.end);
                    datesList.remove(error.start);
                    datesList.remove(error.end);
                    for (ZonedDateTime time : datesList) {
                        if (isPeriodInRange(time, VehiclesReportDataSourceUtils.getChronoUnitEnd(time, rangeBy))) {
                            putError(errorsCount, time);
                        }
                    }
                }
            }
        }

        return errorsCount;
    }

    private void fillAllDatesWithZeros(Map<String, Integer> errorsCount) {
        List<ZonedDateTime> allDates =
                VehiclesReportDataSourceUtils.getDatesBetween(earliestReportDate, latestReportDate, rangeBy);
        for (ZonedDateTime time : allDates) {
            String reportFormatDate = VehiclesReportDataSourceUtils.toReportFormatDate(time, rangeBy);
            errorsCount.put(reportFormatDate, 0);
        }
    }

    private void putError(Map<String, Integer> errorsCount, ZonedDateTime time) {
        String reportFormatDate = VehiclesReportDataSourceUtils.toReportFormatDate(time, rangeBy);
        errorsCount.put(reportFormatDate, errorsCount.get(reportFormatDate) + 1);
    }

    private boolean isPeriodInRange(ZonedDateTime start, ZonedDateTime end) {
        for (ZonedDateTime time: VehiclesReportDataSourceUtils.getDatesBetween(start, end, ChronoUnit.DAYS)) {
            if (VehiclesReportDataSourceUtils.isDateInRange(time, earliestReportDate, latestReportDate))
                return true;
        }
        return false;
    }
}
