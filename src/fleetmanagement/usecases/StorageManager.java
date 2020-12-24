package fleetmanagement.usecases;

import fleetmanagement.backend.diagnosis.DiagnosisHistoryRepository;
import fleetmanagement.backend.operationData.OperationDataHistoryRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.config.Settings;
import gsp.util.Timers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class StorageManager {

    @Autowired
    private DiagnosisHistoryRepository diagnosisHistory;
    @Autowired
    private OperationDataHistoryRepository operationDataHistory;
    @Autowired
    private VehicleRepository vehiclesRepository;
    @Autowired
    private Licence licence;
    @Autowired
    private Settings settings;

    private static final double threshold = 0.95;
    private ScheduledExecutorService timer;
    private int reduceDelay;
    private TimeUnit timeUnit;
    private static final int DEFAULT_DELAY = 1;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.DAYS;

    public void start() {
        start(DEFAULT_DELAY, DEFAULT_TIME_UNIT);
    }

    public void start(int reduceDelay, TimeUnit timeUnit) {
        this.reduceDelay = reduceDelay;
        this.timeUnit = timeUnit;
        if (timer != null)
            stop();
        timer = Timers.newTimer("StorageManager");
        timer.scheduleWithFixedDelay(this::reduce, 0, reduceDelay, timeUnit);
    }

    public void restart() {
        stop();
        start(reduceDelay, timeUnit);
    }

    public void stop() {
        if (timer != null) {
            timer.shutdown();
        }
        timer = null;
    }

    public StorageManager() {
    }

    StorageManager(DiagnosisHistoryRepository diagnosisHistory,
                          OperationDataHistoryRepository operationDataHistory, VehicleRepository vehiclesRepository,
                          Licence licence) {
        this.diagnosisHistory = diagnosisHistory;
        this.operationDataHistory = operationDataHistory;
        this.vehiclesRepository = vehiclesRepository;
        this.licence = licence;
    }

    public Double getLimitInBytes(Double limit) {
        if (limit != null) {
            limit = limit * 1073741824;
        }
        return limit;
    }

    boolean isOperationalDataOverflow() {
        Double limit = getLimitInBytes(settings.getOperationalDataLimit());
        if (limit == null || limit == 0.0) {
            return false;
        }

        long sum = vehiclesRepository.listAll().stream()
                .map(vehicle -> operationDataHistory.getDataSize(vehicle.id))
                .collect(Collectors.summarizingLong(Long::longValue))
                .getSum();

        return sum > limit * threshold;
    }

    boolean isDiagnosisDataOverflow() {
        Double limit = getLimitInBytes(settings.getDiagnosisDataLimit());
        if (limit == null || limit == 0.0) {
            return false;
        }

        long sum = vehiclesRepository.listAll().stream()
                .map(vehicle -> diagnosisHistory.getDataSize(vehicle.id))
                .collect(Collectors.summarizingLong(Long::longValue))
                .getSum();

        return sum > limit * threshold;
    }

    public void reduce() {
        if (licence.isDiagnosisInfoAvailable())
            reduceDiagnosisData();
        if (licence.isOperationInfoAvailable())
            reduceOperationalData();
    }

    public void reduceDiagnosisData() {
        while (isDiagnosisDataOverflow()) {
            List<Vehicle> vehicles = vehiclesRepository.listAll();

            ZonedDateTime oldestDate = vehicles.stream()
                    .map(v -> diagnosisHistory.getOldestHistory(v.id))
                    .filter(Objects::nonNull)
                    .map(stateEntry -> stateEntry.start)
                    .sorted()
                    .findFirst().orElse(null);

            if (oldestDate != null) {
                oldestDate = oldestDate.plusDays(1).toLocalDate().atStartOfDay(oldestDate.getZone());
                diagnosisHistory.getHistory(vehiclesRepository.listAll().get(0).id,
                        "device1").stream().collect(Collectors.toList());
                for (Vehicle vehicle : vehicles) {
                    diagnosisHistory.reduceHistory(vehicle.id, oldestDate);
                }
            } else {
                break;
            }
        }
    }

    void reduceOperationalData() {
        while (isOperationalDataOverflow()) {
            List<Vehicle> vehicles = vehiclesRepository.listAll();

            ZonedDateTime oldestDate = vehicles.stream()
                    .map(v -> operationDataHistory.getOldestHistory(v.id))
                    .filter(Objects::nonNull)
                    .map(history -> history.timeStamp)
                    .sorted()
                    .findFirst().orElse(null);

            if (oldestDate != null) {
                oldestDate = oldestDate.plusDays(1).toLocalDate().atStartOfDay(oldestDate.getZone());
                for (Vehicle vehicle : vehicles) {
                    operationDataHistory.reduceHistory(vehicle.id, oldestDate);
                }
            } else {
                break;
            }
        }
    }

//    private List<LocalDate> getOldDates() {
//        List<Vehicle> vehicles = vehiclesRepository.listAll();
//
//        Stream<LocalDate> oldestOperationDate = vehicles.stream()
//                .map(v -> operationDataHistory.getOldestHistory(v.id))
//                .filter(Objects::nonNull)
//                .map(history -> history.timeStamp)
//                .map(ZonedDateTime::toLocalDate);
//
//        Stream<LocalDate> oldestDiagnosisDate = vehicles.stream()
//                .map(v -> diagnosisHistory.getOldestHistory(v.id))
//                .filter(Objects::nonNull)
//                .map(stateEntry -> stateEntry.start)
//                .map(ZonedDateTime::toLocalDate);
//
//        return Stream.concat(oldestOperationDate, oldestDiagnosisDate)
//                .sorted()
//                .collect(Collectors.toList());
//
//
//    }


    public void setDiagnosisHistory(DiagnosisHistoryRepository diagnosisHistory) {
        this.diagnosisHistory = diagnosisHistory;
    }

    public void setOperationDataHistory(OperationDataHistoryRepository operationDataHistory) {
        this.operationDataHistory = operationDataHistory;
    }

    public void setVehiclesRepository(VehicleRepository vehiclesRepository) {
        this.vehiclesRepository = vehiclesRepository;
    }

    public void setLicence(Licence licence) {
        this.licence = licence;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}
