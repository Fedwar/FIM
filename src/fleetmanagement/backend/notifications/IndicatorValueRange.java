package fleetmanagement.backend.notifications;

import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;

import javax.mail.Multipart;
import java.text.MessageFormat;

import static fleetmanagement.backend.notifications.settings.Parameter.*;

public class IndicatorValueRange implements Notification {

    private final NotificationSetting notificationSetting;
    private final Vehicle vehicle;
    private final OperationData operationData;
    private Indicator indicator;
    long upperLimit;
    long lowerLimit;


    public IndicatorValueRange(NotificationSetting notificationSetting, VehicleRepository vehicleRepository, Event event) {
        this.notificationSetting = notificationSetting;
        this.operationData = (OperationData) event.getTarget();
        vehicle = vehicleRepository.tryFindById(operationData.vehicleId);
    }

    @Override
    public boolean needToSend() {
        String indicatorId = notificationSetting.getParameter(INDICATOR_ID);
        if (indicatorId.isEmpty())
            return false;
        indicator = operationData.getIndicator(indicatorId);
        if (indicator == null)
            return false;
        String upperLimit = notificationSetting.getParameter(UPPER_LIMIT);
        String lowerLimit = notificationSetting.getParameter(LOWER_LIMIT);
        try {
            long indicatorValue = Long.parseLong(indicator.value.toString());
            boolean inRange = true;
            if (inRange && !lowerLimit.isEmpty())
                inRange = indicatorValue >= Long.parseLong(lowerLimit);
            if (inRange && !upperLimit.isEmpty())
                inRange = indicatorValue <= Long.parseLong(upperLimit);
            return !inRange;
        } catch (NumberFormatException e) {
            return true;
        }

    }

    @Override
    public Multipart mailContent() {
        return null;
    }

    @Override
    public NotificationSetting notification() {
        return notificationSetting;
    }

    @Override
    public String mailText() {
        return MessageFormat.format(
                "Vehicle: {0}\r\nIndicator: {1}\r\nValue: {2}\r\nRange: {3}-{4}"
                , vehicle.uic
                , indicator.id
                , indicator.value
                , String.valueOf(lowerLimit)
                , String.valueOf(upperLimit));
    }

    @Override
    public String getVehicleUic() {
        return vehicle.uic;
    }

}
