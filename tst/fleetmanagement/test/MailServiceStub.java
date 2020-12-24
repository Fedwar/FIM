package fleetmanagement.test;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;
import fleetmanagement.backend.mail.MailService;
import fleetmanagement.backend.notifications.Notification;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

public class MailServiceStub extends GreenMailRule implements MailService {

    public MailServiceStub(ServerSetup[] smtpImap) {
        super(smtpImap);
    }

    @Override
    public Session getSession() {
        return getSmtp().createSession();
    }

    @Override
    public MimeMessage newMessage() {
        return new MimeMessage(getSession());
    }

    @Override
    public void send(Message message) {
        try {
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void send(Notification notification) {

    }

    @Override
    public void sendThrowingException(Notification notification) throws MessagingException {

    }

    @Override
    public boolean serverAvailable() {
        return false;
    }
}
