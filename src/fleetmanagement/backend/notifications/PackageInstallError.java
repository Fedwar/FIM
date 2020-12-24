package fleetmanagement.backend.notifications;

import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.I18n;
import org.apache.log4j.Logger;

import javax.mail.Multipart;
import java.text.MessageFormat;
import java.util.Locale;

public class PackageInstallError implements Notification {

    private static final Logger logger = Logger.getLogger(PackageInstallError.class);
    private final NotificationSetting notificationSetting;
    private final Vehicle vehicle;
    private final Task task;

    public PackageInstallError(NotificationSetting notificationSetting, VehicleRepository vehicleRepository, Event event) {
        this.notificationSetting = notificationSetting;
        this.task = (Task) event.getTarget();
        this.vehicle = vehicleRepository.tryFindById(task.getVehicleId());

    }

    @Override
    public boolean needToSend() {
        LogEntry lastLogEntry = task.getLogMessages().get(task.getLogMessages().size() - 1);
        if (lastLogEntry.severity != LogEntry.Severity.ERROR) {
            logger.debug("Last log entry severity is not ERROR, email notification is not required.");
        }
        return lastLogEntry.severity == LogEntry.Severity.ERROR;
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
                "Vehicle: {0}\r\nPackage type: {1}\r\nPackage version: {2}\r\n"
                , vehicle.uic
                , I18n.get(Locale.getDefault(), task.getPackage().type.getResourceKey())
                , task.getPackage().version);
    }

    @Override
    public String getVehicleUic() {
        return vehicle.uic;
    }

}
