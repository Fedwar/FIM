package fleetmanagement.frontend.model;

import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.widgets.Widget;
import fleetmanagement.backend.widgets.WidgetType;
import fleetmanagement.config.Licence;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class OperationDataModel {
    public Licence licence;
    public String vehicleId;
    public String beginDate;
    public String endDate;
    public String vehicleName;
    public List<Indicator> indicators;

    public OperationDataModel(Vehicle vehicle, OperationDataRepository operationDataRepository, Licence licence) {
        this.licence = licence;
        vehicleId = vehicle.id.toString();
        vehicleName = vehicle.getName();
        LocalDate now = LocalDate.now();
        beginDate = now.withDayOfMonth(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        endDate = now.withDayOfMonth(now.lengthOfMonth()).format(DateTimeFormatter.ISO_LOCAL_DATE);
        OperationData operationData = operationDataRepository.tryFindById(vehicle.id);
        if (operationData != null)
            this.indicators = operationData.indicators.stream()
                    .map(i -> new Indicator(i))
                    .collect(Collectors.toList());
    }

    public class Indicator implements Cloneable {
        public final String id;
        public final String unit;
        public String value;
        public ZonedDateTime updated;

        public Indicator(fleetmanagement.backend.operationData.Indicator indicator) {
            this.id = indicator.id;
            this.unit = indicator.unit;
            this.value = indicator.value.toString();
            this.updated = indicator.updated;
        }
    }

    public class History {
        public final String value;
        public final ZonedDateTime timeStamp;

        public History(fleetmanagement.backend.operationData.History history) {
            this.value = history.value == null ? "" : history.value.toString();
            this.timeStamp = history.timeStamp;
        }
    }
}
