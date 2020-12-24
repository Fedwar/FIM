package fleetmanagement.backend.vehiclecommunication.upload;

import com.google.gson.Gson;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.Snapshot;
import fleetmanagement.backend.diagnosis.SnapshotConversionService;
import fleetmanagement.backend.diagnosis.SnapshotFisApiJson;
import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.vehiclecommunication.FileUploadListener;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.UUID;

@Component
public class DiagnosisFisApiUploadListener implements FileUploadListener {

    private static final Logger logger = Logger.getLogger(DiagnosisFisApiUploadListener.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final Gson gson = new Gson();
    @Autowired
    private SnapshotConversionService snapshotConversion;
    @Autowired
    private NotificationService notificationService;

    public DiagnosisFisApiUploadListener() {
    }

    DiagnosisFisApiUploadListener(SnapshotConversionService snapshotConversion, NotificationService notificationService) {
        this.snapshotConversion = snapshotConversion;
        this.notificationService = notificationService;
    }

    @Override
    public boolean canHandleUploadedFile(String filename) {
        return filename.equals("diagnosis-fis-api.json");
    }

    @Override
    public void onFileUploaded(UUID vehicleId, String filename, byte[] fileContent) {
        String data = new String(fileContent, UTF8);
        //logger.debug("Received FisApi diagnosis data: " + data);
        SnapshotFisApiJson json = gson.fromJson(data, SnapshotFisApiJson.class);
        Snapshot snapshot = json.toSnapshot(vehicleId);

        Diagnosis diagnosis = snapshotConversion.integrateNewSnapshot(snapshot);
        notificationService.processEvent(Events.diagnosisUpdated(diagnosis));
    }

}