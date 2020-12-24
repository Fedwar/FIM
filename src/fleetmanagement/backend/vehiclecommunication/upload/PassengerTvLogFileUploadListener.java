package fleetmanagement.backend.vehiclecommunication.upload;

import fleetmanagement.backend.vehiclecommunication.FileUploadListener;
import fleetmanagement.backend.vehiclecommunication.FilteredUploadResource;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class PassengerTvLogFileUploadListener implements FileUploadListener {

    private static final Logger logger = Logger.getLogger(PassengerTvLogFileUploadListener.class);
    private final Pattern pattern = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}_[0-9]{12}_\\w{2}\\d{3}\\.csv", Pattern.CASE_INSENSITIVE);
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private FilteredUploadResource filteredUploadResource;

    PassengerTvLogFileUploadListener(VehicleRepository vehicleRepository, FilteredUploadResource filteredUploadResource) {
        this.vehicleRepository = vehicleRepository;
        this.filteredUploadResource = filteredUploadResource;
    }

    @Override
    public boolean canHandleUploadedFile(String filename) {
        return pattern.matcher(filename).matches();
    }

    @Override
    public void onFileUploaded(UUID vehicleId, String filename, byte[] data) {
        Vehicle vehicle = vehicleRepository.tryFindById(vehicleId);
        filteredUploadResource.onFileReceived(vehicle, filename, data);
    }

}
