package fleetmanagement.backend.repositories.disk.xml;

import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.notifications.settings.Type;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import javax.mail.internet.AddressException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.Map;
import java.util.UUID;

public class NotificationXmlFile implements XmlFile<NotificationSetting> {
    private static final XmlSerializer serializer = new XmlSerializer(NotificationXml.class);
    private static final Logger logger = Logger.getLogger(NotificationXmlFile.class);
    private static final String fileName = "notification.xml";
    private final File file;

    public NotificationXmlFile(File dir) {
        this.file = new File(dir, fileName);
    }

    @Override
    public File file() {
        return file;
    }

    public void delete() {
        file.delete();
    }

    public boolean exists() {
        return file.exists();
    }

    public NotificationSetting load() {
        try {
            if (exists()) {
                NotificationXml xml = (NotificationXml) serializer.load(file);
                return xml.toNotification();
            }
        } catch (Exception e) {
            logger.error("NotificationSetting in " + file.getParent() + " seems broken.", e);
        }
        return null;
    }

    public void save(NotificationSetting o) {
        createParentDirectoryIfRequired();
        NotificationXml meta = new NotificationXml(o);
        serializer.save(meta, file);
    }

    private void createParentDirectoryIfRequired() {
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
    }

    @XmlRootElement(name = "notification")
    @DoNotObfuscate
    public static class NotificationXml {
        @XmlAttribute
        public UUID id;
        @XmlAttribute
        public Type type;
        @XmlAttribute
        public String mailList;
        @XmlElement
        public Map<Parameter, String> parameters;


        public NotificationXml() {}
        public NotificationXml(NotificationSetting o) {
            this.id = o.id();
            this.type = o.type;
            this.mailList = o.getMailList();
            this.parameters = o.getParameters();
        }

        public NotificationSetting toNotification() throws AddressException {
            return new NotificationSetting(id, type, mailList, parameters);
        }
    }


}
