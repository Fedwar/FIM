package fleetmanagement.backend.repositories.disk.xml;


import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.preprocess.PreprocessSetting;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import javax.mail.internet.AddressException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.UUID;

public class PreprocessSettingXmlFile implements XmlFile<PreprocessSetting> {
    private static final XmlSerializer serializer = new XmlSerializer(NotificationXml.class);
    private static final Logger logger = Logger.getLogger(PreprocessSettingXmlFile.class);
    private static final String fileName = "preprocessSetting.xml";
    private final File file;

    public PreprocessSettingXmlFile(File dir) {
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

    public PreprocessSetting load() {
        try {
            if (exists()) {
                NotificationXml xml = (NotificationXml) serializer.load(file);
                return xml.toNotification();
            }
        } catch (Exception e) {
            logger.error("PreprocessSetting in " + file.getParent() + " seems broken.", e);
        }
        return null;
    }

    public void save(PreprocessSetting o) {
        createParentDirectoryIfRequired();
        NotificationXml meta = new NotificationXml(o);
        serializer.save(meta, file);
    }

    private void createParentDirectoryIfRequired() {
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
    }

    @XmlRootElement(name = "setting")
    @DoNotObfuscate
    public static class NotificationXml {
        @XmlAttribute
        public UUID id;
        @XmlAttribute
        public PackageType packageType;
        @XmlAttribute
        public String command;
        @XmlAttribute
        public String options;
        @XmlAttribute
        public String fileNamePattern;


        public NotificationXml() {}
        public NotificationXml(PreprocessSetting o) {
            this.id = o.id();
            this.packageType = o.packageType;
            this.command = o.command;
            this.options = o.options;
            this.fileNamePattern = o.fileNamePattern;
        }

        public PreprocessSetting toNotification() throws AddressException {
            return new PreprocessSetting(id, packageType, command, options, fileNamePattern);
        }
    }


}
