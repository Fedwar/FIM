package fleetmanagement.backend.vehiclecommunication.upload;

import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.Map;

import com.google.gson.Gson;

import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.packages.sync.PackageSyncService;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.*;
import fleetmanagement.config.Licence;
import fleetmanagement.usecases.InstallPackage;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GenericPackageVersion extends TypicalVehicleUploadListener {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private final Gson gson = new Gson();
    protected static final Logger logger = Logger.getLogger(GenericPackageVersion.class);
    private final PackageSyncService packageSyncService;

    public GenericPackageVersion(@Autowired VehicleRepository vehicles, @Autowired PackageSyncService packageSyncService) {
        super(vehicles);
        this.packageSyncService = packageSyncService;
    }

    @Override
    public boolean canHandleUploadedFile(String filename) {
        return filename.startsWith("package-version");
    }

    @Override
    public void onFileUploaded(Vehicle sender, String filename, byte[] data) {
        PackageVersionMessage version = gson.fromJson(new String(data, UTF8), PackageVersionMessage.class);
        PackageType type = PackageType.valueOf(version.type);

        if (version.version != null && version.version.isEmpty())
            version.version = null;

        sender.lastSeen = ZonedDateTime.now();
        if (type == PackageType.Indis5MultimediaContent || type == PackageType.Indis3MultimediaContent) {
            sender.versions.removeAll(PackageType.Indis5MultimediaContent);
            sender.versions.removeAll(PackageType.Indis3MultimediaContent);
        }
        sender.versions.set(type, version.version);

        packageSyncService.syncPackages(sender, type);

    }

    @DoNotObfuscate
    public static class PackageVersionMessage {
        String type;
        String version;
    }

}
