package fleetmanagement.backend.notifications;

import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;

import javax.mail.Multipart;
import java.text.MessageFormat;

import static fleetmanagement.backend.notifications.settings.Parameter.INDICATOR_ID;
import static fleetmanagement.backend.notifications.settings.Parameter.INVALID_VALUE;

public class IndicatorInvalidValue implements Notification {

    private final NotificationSetting notificationSetting;
    private final Vehicle vehicle;
    private final OperationData operationData;
    private Indicator indicator;
    String invalidValue;

    public IndicatorInvalidValue(NotificationSetting notificationSetting, VehicleRepository vehicleRepository, Event event) {
        this.notificationSetting = notificationSetting;
        this.operationData = (OperationData) event.getTarget();
        vehicle = vehicleRepository.tryFindById(operationData.vehicleId);
        invalidValue = notificationSetting.getParameter(INVALID_VALUE);
    }

    @Override
    public boolean needToSend() {
        String indicatorId = notificationSetting.getParameter(INDICATOR_ID);
        if (indicatorId.isEmpty())
            return false;
        indicator = operationData.getIndicator(indicatorId);
        if (indicator == null || invalidValue.isEmpty())
            return false;

        Object indicatorValue = indicator.value;

        if (indicatorValue instanceof Boolean) {
            return indicatorValue.equals(Boolean.valueOf(invalidValue));
        } else if (indicatorValue instanceof String) {
            return indicatorValue.equals(invalidValue);
        } else if (indicatorValue instanceof Double) {
            return indicatorValue.equals(Double.valueOf(invalidValue));
        } else if (indicatorValue instanceof Long) {
            return indicatorValue.equals(Long.valueOf(invalidValue));
        } else if (indicatorValue instanceof Integer) {
            return indicatorValue.equals(Integer.valueOf(invalidValue));
        }
        return false;
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
                "Vehicle: {0}\r\nIndicator: {1}\r\nValue: {2}"
                , vehicle.uic
                , indicator.id
                , indicator.value);
    }

    @Override
    public String getVehicleUic() {
        return vehicle.uic;
    }

}
