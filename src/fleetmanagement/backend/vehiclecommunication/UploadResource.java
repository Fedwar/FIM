package fleetmanagement.backend.vehiclecommunication;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import fleetmanagement.backend.vehiclecommunication.upload.exceptions.UploadFileNotLicenced;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.backend.webserver.UnknownVehicleRequest;
import fleetmanagement.config.Licence;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Path("upload")
public class UploadResource {

    private static final Logger logger = Logger.getLogger(UploadResource.class);
    private final List<FileUploadListener> listeners = new ArrayList<>();
    private final File unknownFilesDirectory;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private VehicleRepository vehicles;
    @Autowired
    private Licence licence;

    public UploadResource() {
        this.unknownFilesDirectory = new File(System.getProperty("java.io.tmpdir"), "FleetManagement");
    }

    UploadResource(VehicleRepository vehicles, File unknownFilesDirectory, Licence licence) {
        this.unknownFilesDirectory = unknownFilesDirectory;
        this.vehicles = vehicles;
        this.licence = licence;
    }

    @PostConstruct
    public void addListeners() {
        applicationContext.getBeansOfType(FileUploadListener.class).values().forEach(this::addListener);
    }

    void addListener(FileUploadListener listener) {
        listeners.add(listener);
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadFile(@QueryParam("uic") String vehicleUic, @FormDataParam("file") InputStream fileContent
            , @FormDataParam("file") FormDataContentDisposition contentDisposition
            , @HeaderParam("remoteAddr") String remoteAddr) throws IOException {
        byte[] data = receive(fileContent);
        String filename = contentDisposition.getFileName();
        Vehicle sender = vehicles.tryFindByUIC(vehicleUic);
        if (sender == null) {
            logger.error("Vehicle doesn't exist! " + vehicleUic);
            throw new UnknownVehicleRequest(Response.Status.BAD_REQUEST);
        }

        if (licence.isVehicleIpAvailable()) {
            sender = updateVehicleIp(sender.id, remoteAddr);
        }

        try {
            if (sender != null) {
                logger.info("Received file from vehicle " + vehicleUic + ": " + filename);
                onFileReceived(sender, filename, data);
            } else {
                logger.warn("Received file from unknown vehicle " + vehicleUic + ": " + filename);
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        } catch (UploadFileNotLicenced e) {
            logger.warn("Error receiving file: " + filename
                    + "\r\nCause: " + e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            logger.warn("Error receiving file: " + filename
                    + "\r\nCause: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok().build();
    }

    private Vehicle updateVehicleIp(UUID vehicleId, String ip) {
        Vehicle vehicle = vehicles.tryFindById(vehicleId);
        if (vehicle.ipAddress != ip) {
            return vehicles.update(vehicleId, v -> {
                v.ipAddress = ip;
            });
        }
        return vehicle;
    }

    private void onFileReceived(Vehicle sender, String fileName, byte[] data) throws UploadFileNotLicenced, IOException {
        boolean handled = false;
        for (FileUploadListener listener : listeners) {
            if (listener.canHandleUploadedFile(fileName)) {
                listener.onFileUploaded(sender.id, fileName, data);
                handled = true;
            }
        }
        if (!handled) {
            File unknownFile = new File(unknownFilesDirectory, fileName);
            FileUtils.writeByteArrayToFile(unknownFile, data);
            logger.warn("No handler for uploaded file " + fileName + ", storing as " + unknownFile.getAbsolutePath() + ".");
        }
    }

    private byte[] receive(InputStream fileContent) throws IOException {
        return IOUtils.toByteArray(fileContent);
    }

}
