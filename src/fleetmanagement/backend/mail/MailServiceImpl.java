package fleetmanagement.backend.mail;

import fleetmanagement.backend.notifications.Notification;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.config.FimConfig;
import fleetmanagement.frontend.I18n;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
public class MailServiceImpl implements MailService {
    private static final Logger logger = Logger.getLogger(MailServiceImpl.class);
    public static final String PROPERTIES_FILE_NAME = "mail.properties";

    @Autowired
    FimConfig config;

    private Properties mailProperties;
    private Authenticator authenticator;
    private InternetAddress[] from;
    private String host;
    private String serverIp;
    private DateTimeFormatter dateTimeFormatter;

    public MailServiceImpl() {
    }

    MailServiceImpl(File dataDirectory) {
        init(readProperties(dataDirectory));
    }

    MailServiceImpl(Properties properties) {
        init(properties);
    }

    @PostConstruct
    public void init() {
        init(readProperties(config.getConfigDirectory()));
    }

    private void init(Properties properties) {
        mailProperties = initProperties(properties);
        host = mailProperties.getProperty("mail.smtp.host");
        from = buildFromAddress();
        authenticator = buildAuthenticator();
        try {
            serverIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.error("Can't get ip address.");
        }
        dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);
    }

    Authenticator buildAuthenticator() {
        String username = mailProperties.getProperty("mail.smtp.user");
        String password = mailProperties.getProperty("mail.smtp.password");

        return new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
    }

    InternetAddress[] buildFromAddress() {
        String fromEmail = mailProperties.getProperty("mail.smtp.fromEmail");
        String fromCaption = mailProperties.getProperty("mail.smtp.fromCaption");

        if (fromEmail != null)
            try {
                return new InternetAddress[]{new InternetAddress(fromEmail,
                        fromCaption != null ? fromCaption : fromEmail)};
            } catch (UnsupportedEncodingException e) {
                logger.error("\"mail.smtp.fromEmail\" address is invalid");
            }
        return null;
    }

    Properties initProperties(Properties properties) {
        properties = (Properties) properties.clone();
        String username = properties.getProperty("mail.smtp.user");
        String tls = properties.getProperty("mail.smtp.tls");
        String ssl = properties.getProperty("mail.smtp.ssl");
        String port = properties.getProperty("mail.smtp.port");

        if (username != null) {
            properties.put("mail.smtp.auth", "true");
        }

        if (tls != null) {
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.starttls.enable", "true");
        }

        if (ssl != null) {
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.socketFactory.port", port);
            properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        return properties;
    }

    private Properties readProperties(File dataDirectory) {
        Properties loaded = new Properties();
        try (InputStream is = new FileInputStream(new File(dataDirectory, PROPERTIES_FILE_NAME))) {
            loaded.load(is);
        } catch (IOException e) {
            logger.warn("Properties for mail service not found");
        }
        Properties properties = new Properties();
        for (Map.Entry<Object, Object> entry : loaded.entrySet()) {
            properties.setProperty("mail." + entry.getKey(), (String) entry.getValue());
        }
        return properties;
    }

    @Override
    public Session getSession() {
        return Session.getInstance(mailProperties, authenticator);
    }

    @Override
    public MimeMessage newMessage() {
        return new MimeMessage(getSession());
    }

    @Override
    public void send(Message message) throws MessagingException {
        if (host == null || host.isEmpty()) {
            throw new MessagingException("Mail service is not configured");
        }
        if (from != null)
            message.addFrom(from);
        Transport.send(message);
        logger.debug("Email sent to " + Arrays.stream(message.getAllRecipients())
                .map(Address::toString)
                .collect(Collectors.joining(", "))
        );
    }

    @Override
    public void send(Notification notification) {
        try {
            sendThrowingException(notification);
        } catch (MessagingException e) {
            logger.error("Can't send e-mail notification to " + notification.notification().getMailList()
                    + "\r\nCause: " + e.getMessage());
        }
    }

    @Override
    public void sendThrowingException(Notification notification) throws MessagingException {
        NotificationSetting notificationSetting = notification.notification();
        MimeMessage message = newMessage();
        String notificationType = I18n.get(Locale.getDefault(), notificationSetting.type.getResourceKey());
        String vehicleUic = notification.getVehicleUic();

        message.addRecipients(Message.RecipientType.TO, notificationSetting.getMailAddress());
        message.setSubject((vehicleUic == null ? "" : vehicleUic + ": ") + notificationType);

        String msg = MessageFormat.format(
                "Server IP address: {0}\r\nDate, time of event: {1}\r\nEvent: {2}\r\n{3}"
                , serverIp
                , dateTimeFormatter.format(ZonedDateTime.now())
                , notificationType
                , notification.mailText());

        message.setText(msg);

        Multipart multipart = notification.mailContent();
        if (multipart != null)
            message.setContent(multipart);

        send(message);
    }

    @Override
    public boolean serverAvailable() {
        try {
            Session session = getSession();
            Transport transport = session.getTransport("smtp");
            transport.connect();
            transport.close();
            return true;
        } catch (AuthenticationFailedException e) {
            logger.warn("Smtp authentication failed");
            e.printStackTrace();
        } catch (MessagingException e) {
            logger.warn("Smtp connection error");
            e.printStackTrace();
        }
        return false;
    }


    public Properties getMailProperties() {
        return mailProperties;
    }
}



