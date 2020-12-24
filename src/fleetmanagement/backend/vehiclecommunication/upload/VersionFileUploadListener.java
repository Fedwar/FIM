package fleetmanagement.backend.vehiclecommunication.upload;

import com.google.gson.Gson;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.packages.sync.PackageSyncService;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.usecases.InstallPackage;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.Map;

public abstract class VersionFileUploadListener extends TypicalVehicleUploadListener {

    private PackageSyncService packageSyncService;

    public VersionFileUploadListener() {
    }

    VersionFileUploadListener(VehicleRepository vehicles, PackageSyncService packageSyncService) {
        super(vehicles);
        this.packageSyncService = packageSyncService;
    }

    public void setPackageSyncService(PackageSyncService packageSyncService) {
        this.packageSyncService = packageSyncService;
    }

    protected static final Logger logger = Logger.getLogger(DataSupplyVersionFileUploadListener.class);
    protected static final Charset UTF8 = Charset.forName("UTF-8");
    protected final Gson gson = new Gson();


    abstract DataSupplyVersionInterface parseJson(String data);

    @Override
    public void onFileUploaded(Vehicle sender, String filename, byte[] fileContent) {
        String data = new String(fileContent, UTF8);
        logger.debug("Handling DV status. Data = " + data);
        PackageType packageType = PackageType.DataSupply;

        DataSupplyVersionInterface status = parseJson(data);

        sender.versions.removeAll(packageType);

        if (status != null && status.getSlots() != null)
            for (Slot slot : status.getSlots()) {
                sender.versions.set(packageType, slot.version, slot.slot,
                        slot.validity_begin, slot.validity_end, slot.active);
            }

        packageSyncService.syncPackages(sender, packageType);

    }

    @DoNotObfuscate
    public class Slot {
        int slot;
        String version;
        String validity_begin;
        String validity_end;
        Boolean active;
    }
}
