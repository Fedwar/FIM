package fleetmanagement.backend.repositories.disk.xml;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.*;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.*;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.backend.diagnosis.VersionInfo.VersionType;
import fleetmanagement.backend.packages.Package;
import gsp.util.DoNotObfuscate;

public class DiagnosisXmlFile implements XmlFile<Diagnosis> {
    private static final XmlSerializer serializer = new XmlSerializer(DiagnosisXml.class);
    private static final String fileName = "diagnosis.xml";
    private final File file;

    public DiagnosisXmlFile(File directory) {
        this.file = new File(directory, fileName);
    }

    @Override
    public File file() {
        return file;
    }

    @Override
    public void delete() {
        file.delete();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public Diagnosis load() {
        DiagnosisXml xml = (DiagnosisXml) serializer.load(file);
        UUID vehicleId = UUID.fromString(file.getParentFile().getName());
        return xml.toDiagnosis(vehicleId);
    }

    @Override
    public void save(Diagnosis p) {
        createParentDirectoryIfRequired();
        saveDiagnosisAsXml(p);
    }

    private void saveDiagnosisAsXml(Diagnosis d) {
        DiagnosisXml xml = new DiagnosisXml(d);
        serializer.save(xml, file);
    }

    private void createParentDirectoryIfRequired() {
        File parent = file.getParentFile();
        if (!parent.exists())
            parent.mkdirs();
    }

    @XmlRootElement
    @DoNotObfuscate
    public static class DiagnosisXml {
        @XmlAttribute(name = "last-updated")
        @XmlJavaTypeAdapter(XmlToZonedDateTime.class)
        public ZonedDateTime lastUpdated;
        @XmlElementWrapper(name = "devices")
        @XmlElement(name = "device")
        public List<DiagnosedDeviceXml> devices = new ArrayList<>();

        public DiagnosisXml() {
        }

        public DiagnosisXml(Diagnosis d) {
            lastUpdated = d.getLastUpdated();
            devices = d.getDevices().stream().map(DiagnosedDeviceXml::new).collect(toList());
        }

        public Diagnosis toDiagnosis(UUID vehicleId) {
            return new Diagnosis(vehicleId, lastUpdated, devices.stream().map(x -> x.toDiagnosedDevice()).collect(toList()));
        }
    }

    @DoNotObfuscate
    public static class DiagnosedDeviceXml {
        @XmlAttribute
        public String id;
        @XmlAttribute
        public String location;
        @XmlAttribute(name = "name")
        public String legacyName;
        public LocalizedStringXml name;
        @XmlAttribute
        public String type;
        @XmlAttribute
        public String status;
        @XmlAttribute
        public boolean disabled;
        @XmlElement(name = "versions")
        public VersionInfoXml versions = new VersionInfoXml();
        @XmlElement(name = "states")
        public StatesXml states;
        @XmlElement(name = "error-history")
        public StatesXml errorHistory = new StatesXml();

        public DiagnosedDeviceXml() {
        }

        public DiagnosedDeviceXml(DiagnosedDevice c) {
            this.id = c.getId();
            this.location = c.getLocation();
            this.name = new LocalizedStringXml(c.getName());
            this.type = c.getType();
            this.disabled = c.isDisabled();
            this.versions = new VersionInfoXml(c);
            this.status = c.getStatus();
            this.states = new StatesXml(c.getCurrentState());
            this.errorHistory = new StatesXml(c.getErrorHistory());
        }

        public DiagnosedDevice toDiagnosedDevice() {
            List<StateEntry> stateEntries;
            if (states == null) {
                ErrorHistory errorHistory = this.errorHistory.toErrorHistory();
                StateEntry lastEntry = errorHistory.getEntries().stream()
                        .filter(stateEntry -> stateEntry.end == null)
                        .findFirst().orElse(null);
                stateEntries = lastEntry == null ? emptyList() : singletonList(lastEntry);
            } else {
                stateEntries = states.toStatesList();
            }

            return new DiagnosedDevice(id, location
                    , (name == null ? new LocalizedString(legacyName) : name.toLocalizedString())
                    , type
                    , status
                    , stateEntries
                    , disabled
                    , versions.toVersionInfo()
                    , this.errorHistory.toErrorHistory()
            );
        }
    }

    @DoNotObfuscate
    public static class StatesXml {
        @XmlElement(name = "status")
        public List<StatusXml> entries = new ArrayList<>();

        public StatesXml() {
        }

        public StatesXml(ErrorHistory e) {
            entries = e.getEntries().stream().map(StatusXml::new).collect(toList());
        }

        public StatesXml(List<StateEntry> e) {
            entries = e.stream().map(StatusXml::new).collect(toList());
        }

        public ErrorHistory toErrorHistory() {
            return new ErrorHistory(entries.stream().map(x -> x.toStatusEntry()).collect(toList()));
        }

        public List<StateEntry> toStatesList() {
            return entries.stream().map(x -> x.toStatusEntry()).collect(toList());
        }
    }

    @DoNotObfuscate
    public static class StatusXml {
        @XmlAttribute
        @XmlJavaTypeAdapter(XmlToZonedDateTime.class)
        public ZonedDateTime start;
        @XmlAttribute
        @XmlJavaTypeAdapter(XmlToZonedDateTime.class)
        public ZonedDateTime end;
        @XmlAttribute
        public String code;
        @XmlAttribute
        public ErrorCategory category;
        @XmlAttribute(name = "message")
        public String legacyMessage;
        public LocalizedStringXml message;

        public StatusXml() {
        }

        public StatusXml(StateEntry e) {
            this.start = e.start;
            this.end = e.end;
            this.code = e.code;
            this.category = e.category;
            this.message = new LocalizedStringXml(e.message);
        }

        public StateEntry toStatusEntry() {
            return new StateEntry(start, end, code, category
                    , (message == null ? new LocalizedString(legacyMessage) : message.toLocalizedString()));
        }
    }

    @DoNotObfuscate
    public static class VersionInfoXml {

        @XmlElement
        public HashMap<String, String> versions = new HashMap<>();

        @XmlAttribute
        public String software;
        @XmlAttribute
        public String fontware;

        public VersionInfoXml() {
        }

        public VersionInfoXml(DiagnosedDevice d) {
            this.versions.putAll(d.getVersionsInfo().getAll());
        }

        public VersionInfo toVersionInfo() {
            if (software != null)
                versions.put(VersionType.Software.toString(), software);
            if (fontware != null)
                versions.put(VersionType.Fontware.toString(), fontware);
            return new VersionInfo(versions);
        }
    }
}
