package fleetmanagement.backend.notifications;

import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.frontend.model.Logs;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Date;

public class ServerException implements Notification {
    private static final Logger logger = Logger.getLogger(ServerException.class);
    private final NotificationSetting notificationSetting;
    private Logs logs;
    private final Exception exception;


    public ServerException(NotificationSetting notificationSetting, Logs logs, Event event) {
        this.notificationSetting = notificationSetting;
        this.logs = logs;
        this.exception = (Exception) event.getTarget();
    }

    @Override
    public boolean needToSend() {
        return exception != null;
    }

    @Override
    public String mailText() {
        return exception.toString();
    }

    @Override
    public Multipart mailContent() {
        Multipart multipart = new MimeMultipart();
        try {
            MimeBodyPart messageBodyPart = new MimeBodyPart(logs.getAsZipStream());
            String fileName = "FleetManagementLogs_" + DateFormatUtils.format(new Date(), "yyyy-MM-dd_kkmm") + ".zip";
            messageBodyPart.setFileName(fileName);
            multipart.addBodyPart(messageBodyPart);
        } catch (IOException | MessagingException e) {
            logger.error("Can't create mail attachment with log files", e);
        }
        return multipart;
    }

    @Override
    public NotificationSetting notification() {
        return notificationSetting;
    }

    @Override
    public String getVehicleUic() {
        return null;
    }

}
