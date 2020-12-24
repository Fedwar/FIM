package fleetmanagement.backend.vehiclecommunication.upload;

import com.google.gson.annotations.SerializedName;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.packages.sync.PackageSyncService;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.*;
import fleetmanagement.config.Licence;
import gsp.util.DoNotObfuscate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSuppliesVersionFileUploadListener extends VersionFileUploadListener {

    public DataSuppliesVersionFileUploadListener(@Autowired VehicleRepository vehicles, @Autowired PackageSyncService packageSyncService) {
        super(vehicles, packageSyncService);
    }

    @Override
    public boolean canHandleUploadedFile(String filename) {
        return filename.equals("dv-status-fis-api.json");
    }

    @DoNotObfuscate
    public class DataSupplyStatus implements DataSupplyVersionInterface {
        @SerializedName("data_supplies")
        List<Slot> slots;

        @Override
        public List<Slot> getSlots() {
            return slots;
        }
    }

    @Override
    DataSupplyVersionInterface parseJson(String data) {
        return gson.fromJson(data, DataSupplyStatus.class);
    }
}