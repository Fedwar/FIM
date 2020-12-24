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

public class TestNotification implements Notification {

    private final NotificationSetting notificationSetting;

    public TestNotification(NotificationSetting notificationSetting) {
        this.notificationSetting = notificationSetting;
    }

    @Override
    public boolean needToSend() {
        return true;
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
        return "This is test notification";
    }

    @Override
    public String getVehicleUic() {
        return null;
    }

}
