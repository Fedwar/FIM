package fleetmanagement.backend.mail;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import fleetmanagement.TempFileRule;
import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.DiagnosedDeviceError;
import fleetmanagement.backend.notifications.Notification;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.frontend.I18n;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Properties;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class MailServiceImplTest {

    private MailServiceImpl tested;
    Properties properties;
    final static String smptPort = "3725";
    final static String fromCaption = "GSP_Server";
    final static String fromEmail = "Dev@gsp.com";

    @Rule
    public final GreenMailRule mailService = new GreenMailRule(new ServerSetup(Integer.valueOf(smptPort), (String)null, "smtp"));
    @Rule
    public TempFileRule tempFolder = new TempFileRule();

    @Before
    public void before() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    public void fileConstructor() throws IOException {
        String propertiesString = "smtp.user=devUser\n" +
                "smtp.password=devPassword\n" +
                "smtp.host=localhost\n" +
                "smtp.port=" + smptPort + "\n" +
                "smtp.fromCaption=" + fromCaption + "\n" +
                "smtp.fromEmail=" + fromEmail;

        File file = tempFolder.newFile(MailServiceImpl.PROPERTIES_FILE_NAME);
        FileUtils.writeStringToFile(file, propertiesString);
        tested = new MailServiceImpl(tempFolder);

        Properties mailProperties = tested.getMailProperties();
        assertEquals("devUser", mailProperties.getProperty("mail.smtp.user"));
        assertEquals("devPassword", mailProperties.getProperty("mail.smtp.password"));
        assertEquals("localhost", mailProperties.getProperty("mail.smtp.host"));
        assertEquals(smptPort, mailProperties.getProperty("mail.smtp.port"));
        assertEquals("true", mailProperties.getProperty("mail.smtp.auth"));
    }

    @Test
    public void propertiesAddedIfSSLEnabled() throws UnknownHostException {
        Properties properties = defaultProperties();
        properties.setProperty("mail.smtp.ssl", "true");
        tested = new MailServiceImpl(properties);

        Properties mailProperties = tested.getMailProperties();
        assertEquals("javax.net.ssl.SSLSocketFactory", mailProperties.getProperty("mail.smtp.socketFactory.class"));
        assertEquals(smptPort, mailProperties.getProperty("mail.smtp.socketFactory.port"));
        assertEquals("true", mailProperties.getProperty("mail.smtp.auth"));
    }


    @Test
    public void messageHasProperSenderAndRecipient() throws MessagingException, IOException {
        tested = new MailServiceImpl(defaultProperties());
        Message msg = tested.newMessage();
        msg.setText(GreenMailUtil.random());
        InternetAddress sender = new InternetAddress(fromEmail,
                fromCaption != null ? fromCaption : fromEmail);
        String address = "bar@example.com";
        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(address));

        tested.send(msg);

        assertEquals(1, mailService.getReceivedMessagesForDomain(address).length);
        MimeMessage msgReceived = mailService.getReceivedMessagesForDomain(address)[0];
        assertEquals(1, msgReceived.getFrom().length);
        assertEquals(sender, msgReceived.getFrom()[0]);
    }

    @Test(expected = MessagingException.class)
    public void sendMailWhenServiceNotSetup() throws MessagingException, UnknownHostException {
        tested = new MailServiceImpl(new Properties());
        Message msg = tested.newMessage();

        try {
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress("bar@example.com"));
            msg.setText(GreenMailUtil.random());
        } catch (AddressException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        tested.send(msg);
    }

    @Test
    public void serverAvailable() throws IOException {
        tested = new MailServiceImpl(defaultProperties());

        assertTrue(tested.serverAvailable());
    }

    @Test
    public void serverUnavailableWhenNotAuthorized() throws IOException {
        Properties properties = defaultProperties();
        properties.setProperty("mail.smtp.user", "devUser");
        properties.setProperty("mail.smtp.password", "devPassword");
        tested = new MailServiceImpl(properties);

        assertFalse(tested.serverAvailable());
    }

    @Test
    public void notificationMessageContainsRequiredData() throws IOException, MessagingException {
        TestScenarioPrefilled scenario = new TestScenarioPrefilled();
        NotificationSetting setting = NotificationSetting.diagnosedDeviceError("", "dev@gsp.com");
        Notification notification = new DiagnosedDeviceError(setting, scenario.vehicleRepository, Events.diagnosisUpdated(scenario.diagnosis1));
        tested = new MailServiceImpl(defaultProperties());
        tested.send(notification);

        assertEquals(1, mailService.getReceivedMessages().length);
        MimeMessage msgReceived = mailService.getReceivedMessages()[0];
        assertTrue(msgReceived.getSubject().contains(notification.getVehicleUic()));
        assertTrue(msgReceived.getSubject().contains(I18n.get(Locale.getDefault(), setting.type.getResourceKey())));
        assertMessageContains(msgReceived, InetAddress.getLocalHost().getHostAddress());
        assertMessageContains(msgReceived, I18n.get(Locale.getDefault(), setting.type.getResourceKey()));
        assertMessageContains(msgReceived, new SimpleDateFormat("EEEEEEEEEEE, MMMMMMMMMMM d").format(msgReceived.getReceivedDate()));
    }


    void assertMessageContains(MimeMessage msgReceived, String text) {
        assertTrue("Message doesn't contain \"" + text + "\"", GreenMailUtil.getBody(msgReceived).contains(text));
    }

    Properties defaultProperties() {
        properties = new Properties();
        properties.setProperty("mail.smtp.host", "localhost");
        properties.setProperty("mail.smtp.port", smptPort);
        properties.setProperty("mail.smtp.fromCaption", fromCaption);
        properties.setProperty("mail.smtp.fromEmail", fromEmail);
        return properties;
    }

}