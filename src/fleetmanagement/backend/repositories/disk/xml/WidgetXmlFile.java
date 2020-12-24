package fleetmanagement.backend.repositories.disk.xml;

import fleetmanagement.backend.operationData.ValueClassConverter;
import fleetmanagement.backend.widgets.Widget;
import fleetmanagement.backend.widgets.WidgetType;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.UUID;

public class WidgetXmlFile implements XmlFile<Widget> {
    private static final Logger logger = Logger.getLogger(WidgetXmlFile.class);
    private static final XmlSerializer serializer = new XmlSerializer(WidgetXml.class);
    private static final String fileName = "widget.xml";
    private final File file;

    public WidgetXmlFile(File directory) {
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

    public Widget load() {
        try {
            if (exists()) {
                WidgetXml widgetXml = (WidgetXml) serializer.load(file);
                return widgetXml.toWidget();
            }
        } catch (Exception e) {
            logger.error("Widget in " + file.getParent() + " seems broken.", e);
        }
        return null;
    }

    public void save(Widget widget) {
        createParentDirectoryIfRequired();
        WidgetXml meta = new WidgetXml(widget);
        serializer.save(meta, file);
    }

    private void createParentDirectoryIfRequired() {
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
    }

    @XmlRootElement(name = "widget")
    @DoNotObfuscate
    private static class WidgetXml {
        @XmlAttribute
        public UUID id;
        @XmlAttribute
        public String indicatorId;
        @XmlAttribute
        public String maxValue;
        @XmlAttribute
        public String maxValueClass;
        @XmlAttribute
        public String minValue;
        @XmlAttribute
        public String minValueClass;
        @XmlAttribute
        public String type;

        public WidgetXml() {
        }

        public WidgetXml(Widget o) {
            id = o.id;
            indicatorId = o.indicatorId;
            if ( o.maxValue != null) {
                maxValue = o.maxValue.toString();
                maxValueClass = o.minValue.getClass().getName();
            }
            if ( o.minValue != null) {
                minValue = o.minValue.toString();
                minValueClass = o.minValue.getClass().getName();
            }
            type = o.type.name();
        }

        public Widget toWidget() {
            return new Widget(id
                    , indicatorId
                    , new ValueClassConverter(maxValue, maxValueClass).getValue()
                    , new ValueClassConverter(minValue, minValueClass).getValue()
                    , WidgetType.valueOf(type));
        }
    }

    public File getFile() {
        return file;
    }
}
