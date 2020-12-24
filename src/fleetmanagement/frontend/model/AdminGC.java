package fleetmanagement.frontend.model;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.config.Licenced;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AdminGC extends Admin {
    public final Map<String, Boolean> packageTypes = new HashMap<>();

    public AdminGC(
            PackageTypeRepository packageTypeRepository,
            Licence licence) {
        super(licence);
        for (PackageType packageType: PackageType.values()) {
            if (licence.isPackageTypeAvailable(packageType))
                packageTypes.put(packageType.getResourceKey(), packageTypeRepository.isGCEnabled(packageType));
        }
    }

}
