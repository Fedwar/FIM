package fleetmanagement.frontend.model;

import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.vehicles.Vehicle;
import gsp.util.DoNotObfuscate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@DoNotObfuscate
public class IndicatorHistory {
    public String vehicleName;
    public String vehicleId;
    public String indicatorId;
    public String indicatorUnit;
    public List<History> history;

    public IndicatorHistory(Vehicle vehicle, Indicator indicator, List<fleetmanagement.backend.operationData.History> history) {
        this.vehicleName = vehicle.getName();
        this.vehicleId = vehicle.id.toString();
        this.indicatorId = indicator.id;
        this.indicatorUnit = indicator.unit;
        this.history = history.stream().map(History::new).collect(Collectors.toList());
    }

    public class History {
        public final String value;
        public final ZonedDateTime timeStamp;

        public History(fleetmanagement.backend.operationData.History history) {
            this.value = history.value.toString();
            this.timeStamp = history.timeStamp;
        }

        public String getIsoLocalDate() {
            return timeStamp.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }


}
