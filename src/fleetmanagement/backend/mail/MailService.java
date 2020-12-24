package fleetmanagement.backend.mail;

import fleetmanagement.backend.notifications.Notification;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public interface MailService {
    Session getSession();

    MimeMessage newMessage();

    void send(Message message) throws MessagingException;

    void send(Notification notification);

    void sendThrowingException(Notification notification) throws MessagingException;

    boolean serverAvailable();
}
