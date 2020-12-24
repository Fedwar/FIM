package fleetmanagement.frontend.model;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.config.LicenceImpl;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AdminAutoSync extends Admin {
    public final Map<String, Boolean> packageTypes = new HashMap<>();

    public AdminAutoSync(PackageTypeRepository packageTypeRepository,
                         Licence licence) {
        super(licence);
        for (PackageType packageType: PackageType.values()) {
            if (packageType == PackageType.ClientConfig || packageType == PackageType.CopyStick)
                continue;
            if (licence.isPackageTypeAvailable(packageType))
                packageTypes.put(packageType.getResourceKey(), packageTypeRepository.isAutoSyncEnabled(packageType));
        }
    }
}
