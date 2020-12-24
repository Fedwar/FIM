package fleetmanagement.backend.vehiclecommunication.upload;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.operationData.OperationDataSnapshot;
import fleetmanagement.backend.vehiclecommunication.FileUploadListener;
import fleetmanagement.backend.vehiclecommunication.upload.exceptions.UploadFileNotLicenced;
import fleetmanagement.config.Licence;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.UUID;

@Component
public class OperationDataUploadListener implements FileUploadListener {

    private static final Logger logger = Logger.getLogger(OperationDataUploadListener.class);
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final Gson gson = new Gson();
    @Autowired
    private Licence licence;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private OperationDataRepository operationDataRepository;

    public OperationDataUploadListener() {
    }

    OperationDataUploadListener(OperationDataRepository operationDataRepository, Licence licence, NotificationService notificationService) {
        this.operationDataRepository = operationDataRepository;
        this.licence = licence;
        this.notificationService = notificationService;
    }

    @Override
    public boolean canHandleUploadedFile(String filename) {
        return filename.equals("operation-data.json");
    }

    @Override
    public void onFileUploaded(UUID vehicleId, String filename, byte[] data) throws UploadFileNotLicenced {
        if (!licence.isOperationInfoAvailable())
            throw new UploadFileNotLicenced("Operational data not licenced");

        String jsonString = new String(data, UTF8);
        OperationDataSnapshot snapshot = null;

        try {
            snapshot = new OperationDataSnapshot(gson.fromJson(jsonString, LinkedTreeMap.class));
        } catch (Exception e) {
            logger.error("Invalid json format."
                    + "\r\n" + e.getLocalizedMessage()
                    + "\r\n" + "Json data: " + jsonString, e);
        }

        if (snapshot != null) {
            operationDataRepository.integrateSnapshot(vehicleId, snapshot);
            notificationService.processEvent(Events.operationDataUpdated(operationDataRepository.tryFindById(vehicleId)));
        }

    }



}
