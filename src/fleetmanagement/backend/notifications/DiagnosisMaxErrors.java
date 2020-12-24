package fleetmanagement.backend.notifications;

import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;

import javax.mail.Multipart;
import java.text.MessageFormat;

public class DiagnosisMaxErrors implements Notification {

    private final NotificationSetting notificationSetting;
    private final Vehicle vehicle;
    private final Diagnosis diagnosis;
    private final String errorLimit;
    private int brokenCount;

    public DiagnosisMaxErrors(NotificationSetting notificationSetting, VehicleRepository vehicleRepository, Event event) {
        this.notificationSetting = notificationSetting;
        this.diagnosis = (Diagnosis) event.getTarget();
        this.vehicle = vehicleRepository.tryFindById(this.diagnosis.getVehicleId());
        this.errorLimit = notificationSetting.getParameter(Parameter.ERROR_LIMIT);
    }

    @Override
    public boolean needToSend() {
        if (errorLimit.isEmpty())
            return false;
        brokenCount = diagnosis.countBrokenDevices();
        return brokenCount > Integer.valueOf(errorLimit);
    }

    @Override
    public Multipart mailContent() {
        return null;
    }

    @Override
    public String getVehicleUic() {
        return vehicle.uic;
    }

    @Override
    public NotificationSetting notification() {
        return notificationSetting;
    }

    @Override
    public String mailText() {
        return MessageFormat.format(
                "Vehicle: {0}\r\nBroken device count: {1}\r\nBroken device limit: {2}"
                , vehicle.uic
                , brokenCount, errorLimit);
    }
}
