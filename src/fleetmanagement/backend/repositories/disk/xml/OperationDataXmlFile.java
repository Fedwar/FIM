package fleetmanagement.backend.repositories.disk.xml;

import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.operationData.ValueClassConverter;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.File;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class OperationDataXmlFile implements XmlFile<OperationData> {
    private static final XmlSerializer serializer = new XmlSerializer(OperationDataXml.class);
    private static final Logger logger = Logger.getLogger(UploadFilterSequenceXmlFile.class);
    private static final String fileName = "operationData.xml";
    private final File file;

    public OperationDataXmlFile(File directory) {
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

    public OperationData load() {
        try {
            if (exists()) {
                OperationDataXml xml = (OperationDataXml) serializer.load(file);
                UUID vehicleId = UUID.fromString(file.getParentFile().getName());
                return xml.toOperationData(vehicleId);
            }
        } catch (Exception e) {
            logger.error("OperationData in " + file.getParent() + " seems broken.", e);
        }
        return null;
    }

    public void save(OperationData o) {
        createParentDirectoryIfRequired();
        OperationDataXml meta = new OperationDataXml(o);
        serializer.save(meta, file);
    }

    private void createParentDirectoryIfRequired() {
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
    }

    @XmlRootElement(name = "operation-data")
    @DoNotObfuscate
    public static class OperationDataXml {
        @XmlAttribute(name = "vehicle-id")
        public UUID vehicleId;
        @XmlAttribute
        @XmlJavaTypeAdapter(XmlToZonedDateTime.class)
        public ZonedDateTime updated;
        @Deprecated
        @XmlElementWrapper(name = "indicatorNames")
        @XmlElement(name = "indicator")
        public List<IndicatorXml> legacyList;
        @XmlElementWrapper(name = "indicators")
        @XmlElement(name = "indicator")
        public List<IndicatorXml> indicators = new ArrayList<>();

        public OperationDataXml() {
        }

        public OperationDataXml(OperationData o) {
            this.updated = o.updated;
            this.vehicleId = o.vehicleId;
            this.indicators = o.indicators.stream().map(IndicatorXml::new).collect(toList());
        }

        public OperationData toOperationData(UUID vehicleId) {
            if (indicators.isEmpty())
                indicators = emptyIfNull(legacyList);
            List<Indicator> indicatorsList = indicators.stream().map(x -> x.toIndicator()).collect(Collectors.toList());
            return new OperationData(vehicleId, updated, indicatorsList);
        }
    }

    @DoNotObfuscate
    public static class IndicatorXml {
        @XmlAttribute
        public String id;
        @XmlAttribute
        public String unit;
        @XmlAttribute
        public String value;
        @XmlAttribute
        public String valueClass;
        @XmlAttribute(name = "last-updated")
        @XmlJavaTypeAdapter(XmlToZonedDateTime.class)
        public ZonedDateTime updated;
        //history migrated to OperationDataHistoryRepository
        @Deprecated
        @XmlElementWrapper(name = "history")
        @XmlElement(name = "entry")
        public List<HistoryXml> history;

        public IndicatorXml() {
        }

        public IndicatorXml(Indicator o) {
            this.updated = o.updated;
            this.id = o.id;
            this.unit = o.unit;
            this.value = o.value.toString();
            this.valueClass = o.value.getClass().getName();
            if (o.getHistory() != null && !o.getHistory().isEmpty())
                this.history = o.getHistory().stream().map(HistoryXml::new).collect(toList());
        }

        public Indicator toIndicator() {
            if (history == null) history = new ArrayList<>();
            return new Indicator(id, unit, new ValueClassConverter(value, valueClass).getValue(), updated
                    , history.stream().map(HistoryXml::toHistory).collect(toList()));

        }
    }

    //history migrated to OperationDataHistoryRepository
    @Deprecated
    @XmlRootElement(name = "history")
    @DoNotObfuscate
    public static class HistoryXml {
        @XmlAttribute
        public String value;
        @XmlAttribute
        public String valueClass;

        @XmlJavaTypeAdapter(ZonedDateTimeXmlAdapter.class)
        @XmlAttribute
        public ZonedDateTime timeStamp;

        public HistoryXml() {
        }

        public HistoryXml(History history) {
            this.value = history.value.toString();
            this.valueClass = history.value.getClass().getName();
            this.timeStamp = history.timeStamp;
        }

        public History toHistory() {
            return new History(new ValueClassConverter(value, valueClass).getValue(), timeStamp);
        }

    }

}
