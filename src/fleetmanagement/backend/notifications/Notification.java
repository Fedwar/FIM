package fleetmanagement.backend.notifications;

import fleetmanagement.backend.notifications.settings.NotificationSetting;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import java.io.IOException;

public interface Notification {

    boolean needToSend();

    NotificationSetting notification();

    String mailText();

    Multipart mailContent();

    String getVehicleUic();

}
