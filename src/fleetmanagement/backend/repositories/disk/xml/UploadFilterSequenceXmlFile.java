package fleetmanagement.backend.repositories.disk.xml;

import fleetmanagement.backend.vehiclecommunication.upload.filter.*;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class UploadFilterSequenceXmlFile implements XmlFile<UploadFilterSequence> {
    private static final XmlSerializer serializer = new XmlSerializer(UploadFilterSequenceXml.class);
    private static final Logger logger = Logger.getLogger(UploadFilterSequenceXmlFile.class);
    private static final String fileName = "uploadFilter.xml";
    private final File file;

    public UploadFilterSequenceXmlFile(File dir) {
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

    public UploadFilterSequence load() {
        try {
            if (exists()) {
                UploadFilterSequenceXml xml = (UploadFilterSequenceXml) serializer.load(file);
                return xml.toSequence();
            }
        } catch (Exception e) {
            logger.error("Filter sequence in " + file.getParent() + " seems broken.", e);
        }
        return null;
    }

    public void save(UploadFilterSequence o) {
        createParentDirectoryIfRequired();
        UploadFilterSequenceXml meta = new UploadFilterSequenceXml(o);
        serializer.save(meta, file);
    }

    private void createParentDirectoryIfRequired() {
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
    }

    @XmlRootElement(name = "upload-filter-sequence")
    @DoNotObfuscate
    public static class UploadFilterSequenceXml {
        @XmlAttribute
        public UUID id;
        @XmlAttribute
        public String type;
        public List<UploadFilterXml> filters = new ArrayList();
        @XmlList
        public List<String> notViewedFiles = new ArrayList();

        public UploadFilterSequenceXml() {}
        public UploadFilterSequenceXml(UploadFilterSequence o) {
            this.id = o.id;
            this.type = o.type.toString();
            this.filters = o.filters.stream().map(UploadFilterXml::new).collect(toList());
            this.notViewedFiles = o.notViewedFiles;
        }

        public UploadFilterSequence toSequence() {
            List<UploadFilter> filters = this.filters.stream().map(x -> x.toUploadFilter()).collect(Collectors.toList());
            return new UploadFilterSequence(id, FilterType.valueOf(type), filters, notViewedFiles);
        }
    }

    @XmlRootElement(name = "upload-filter")
    @DoNotObfuscate
    public static class UploadFilterXml {
        @XmlAttribute
        public String name;
        @XmlAttribute
        public String description;
        @XmlAttribute
        public String dir;
        @XmlAttribute
        public String delete;
        @XmlAttribute
        public String deleteDays;
        public List<UploadFilterConditionXml> conditions = new ArrayList();

        public UploadFilterXml() {}
        public UploadFilterXml(UploadFilter o) {
            this.description = o.description;
            this.name = o.name;
            this.dir = o.dir;
            this.delete = o.delete ? "Enabled" : "Disabled";
            this.deleteDays = String.valueOf(o.deleteDays);
            this.conditions = o.getConditions().stream().map(UploadFilterConditionXml::new).collect(toList());
        }

        public UploadFilter toUploadFilter() {
            List<UploadFilterCondition> conditions = this.conditions.stream().map(x -> x.toCondition()).collect(Collectors.toList());
            return new UploadFilter (name, dir, description, delete, deleteDays, conditions);
        }
    }

    @XmlRootElement(name = "history")
    @DoNotObfuscate
    public static class UploadFilterConditionXml {
        @XmlAttribute
        public ConditionType type;
        @XmlAttribute
        public String matchString;

        public UploadFilterConditionXml() {}
        public UploadFilterConditionXml(UploadFilterCondition condition) {
            this.type = condition.type;
            this.matchString = condition.matchString;
        }

        public UploadFilterCondition toCondition() {
            return new UploadFilterCondition(type, matchString);
        }
    }

}
