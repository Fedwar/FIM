package fleetmanagement.backend.repositories.disk.xml;

import fleetmanagement.backend.groups.Group;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.UUID;

public class GroupXmlFile implements XmlFile<Group> {

    private static final Logger logger = Logger.getLogger(GroupXmlFile.class);
    private static final XmlSerializer serializer = new XmlSerializer(GroupXml.class);
    private static final String fileName = "group.xml";
    private final File file;

    public GroupXmlFile(File directory) {
        this.file = new File(directory, fileName);
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

    public Group load() {
        try {
            if (exists()) {
                GroupXml meta = (GroupXml)serializer.load(file);
                Group group = new Group(meta.id, meta.name, meta.dir, meta.isAutoSyncEnabled);
                return group;
            }
        } catch (Exception e) {
            logger.error("Group in " + file.getParent() + " seems broken.", e);
        }
        return null;
    }

    public void save(Group group) {
        createParentDirectoryIfRequired();
        GroupXml meta = new GroupXml();

        meta.formatVersion = 1;
        meta.id = group.id;
        meta.name = group.name;
        meta.dir = group.dir;
        meta.isAutoSyncEnabled = group.isAutoSyncEnabled;

        serializer.save(meta, file);
    }

    private void createParentDirectoryIfRequired() {
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
    }

    @XmlRootElement(name="group")
    @DoNotObfuscate
    private static class GroupXml {
        @XmlAttribute(name="format-version") int formatVersion;
        @XmlAttribute public UUID id;
        @XmlAttribute public String name;
        @XmlAttribute public String dir;
        @XmlAttribute public boolean isAutoSyncEnabled;
    }

    public File getFile() {
        return  file;
    }
}
