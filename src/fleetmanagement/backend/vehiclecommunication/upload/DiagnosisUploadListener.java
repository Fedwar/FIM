package fleetmanagement.backend.vehiclecommunication.upload;

import com.google.gson.Gson;
import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.backend.diagnosis.DeviceSnapshot.StateSnapshot;
import fleetmanagement.backend.diagnosis.ErrorCategory;
import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.vehiclecommunication.FileUploadListener;
import fleetmanagement.backend.vehiclecommunication.upload.exceptions.UploadFileNotLicenced;
import fleetmanagement.config.Licence;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class DiagnosisUploadListener implements FileUploadListener {

    private static final Logger logger = Logger.getLogger(DiagnosisFisApiUploadListener.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final Gson gson = new Gson();
    @Autowired
    private SnapshotConversionService snapshotConversion;
    @Autowired
    private Licence licence;
    @Autowired
    private NotificationService notificationService;

    public DiagnosisUploadListener() {
    }

    DiagnosisUploadListener(SnapshotConversionService snapshotConversion, Licence licence, NotificationService notificationService) {
        this.snapshotConversion = snapshotConversion;
        this.licence = licence;
        this.notificationService = notificationService;
    }

    @Override
    public boolean canHandleUploadedFile(String filename) {
        return filename.equals("diagnosis.json");
    }

    @Override
    public void onFileUploaded(UUID vehicleId, String filename, byte[] fileContent) throws UploadFileNotLicenced {
        if (!licence.isDiagnosisInfoAvailable())
            throw new UploadFileNotLicenced("Operational data not licenced");

        String data = new String(fileContent, UTF8);
        logger.debug("Received diagnosis data: " + data);

        SnapshotJson json = gson.fromJson(data, SnapshotJson.class);
        Snapshot snapshot = json.toSnapshot(vehicleId);
        Diagnosis diagnosis = snapshotConversion.integrateNewSnapshot(snapshot);
        notificationService.processEvent(Events.diagnosisUpdated(diagnosis));
    }

    @DoNotObfuscate
    static class SnapshotJson {
        List<ComponentSnapshotJson> components;

        public Snapshot toSnapshot(UUID vehicleId) {
            List<DeviceSnapshot> parsed = components.stream().map(c -> c.toComponentSnapshot()).collect(Collectors.toList());
            return new Snapshot(vehicleId, 1, ZonedDateTime.now(), parsed);
        }
    }

    @DoNotObfuscate
    private static class ComponentSnapshotJson {
        String id;
        String type;
        String name;
        String location;
        StatusJson status;
        VersionsJson versions;

        public DeviceSnapshot toComponentSnapshot() {
            return new DeviceSnapshot(id, location, name, type, versions.toVersionInfo(), status.toState());
        }
    }

    @DoNotObfuscate
    private static class VersionsJson {
        String software;
        String font;

        private VersionInfo toVersionInfo() {
            return new VersionInfo(software, font);
        }
    }

    @DoNotObfuscate
    private static class StatusJson {
        String description;
        ComponentOperationStatus operational;
        Integer code;

        public StateSnapshot toState() {
            return new StateSnapshot(description, code != null ? Integer.toString(code) : null, operational.toErrorCategory());
        }
    }

    @DoNotObfuscate
    private enum ComponentOperationStatus {
        ok(ErrorCategory.OK),
        broken(ErrorCategory.FATAL);

        private final ErrorCategory category;

        private ComponentOperationStatus(ErrorCategory category) {
            this.category = category;
        }

        public ErrorCategory toErrorCategory() {
            return category;
        }
    }

}