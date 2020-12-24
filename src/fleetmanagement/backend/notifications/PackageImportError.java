package fleetmanagement.backend.notifications;

import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.frontend.I18n;
import org.apache.log4j.Logger;

import javax.mail.Multipart;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.util.Locale;

import static fleetmanagement.backend.notifications.settings.Parameter.PACKAGE_TYPE;

public class PackageImportError implements Notification {

    private static final Logger logger = Logger.getLogger(PackageImportError.class);
    private final NotificationSetting notificationSetting;
    private final Exception exception;
    private final ZonedDateTime importStart;
    private final String fileName;
    private final PackageType packageType;

    public PackageImportError(NotificationSetting notificationSetting, Event event) {
        this.notificationSetting = notificationSetting;
        this.exception = (Exception) event.getTarget();
        this.packageType = (PackageType) event.getProperties().get("packageType");
        this.importStart = (ZonedDateTime) event.getProperties().get("importStart");
        this.fileName = (String) event.getProperties().get("fileName");
    }

    @Override
    public boolean needToSend() {
        String packageType = notificationSetting.getParameter(PACKAGE_TYPE);
        return packageType.equals("notif_all_package_types")
                || PackageType.getByResourceKey(packageType) == this.packageType;
    }

    @Override
    public Multipart mailContent() {
        return null;
    }

    @Override
    public String getVehicleUic() {
        return null;
    }

    @Override
    public NotificationSetting notification() {
        return notificationSetting;
    }

    @Override
    public String mailText() {
        String message = "";
        if (packageType != null) {
            message = "Package type: " + I18n.get(Locale.getDefault(), packageType.getResourceKey()) + "\r\n";
        }
        message += MessageFormat.format(
                "Import started: {0}\r\nFilename: {1}\r\nError message: {2}\r\n"
                , importStart
                , fileName
                , exception.getMessage());
        return message;
    }

}
