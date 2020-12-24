package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class InMemoryPackageTypeRepository implements PackageTypeRepository {
    private final Set<PackageType> packageTypes = new HashSet<>();
    private final Set<PackageType> autoSync = new HashSet<>();

    @Override
    public void enableGC(PackageType packageType) {
        packageTypes.add(packageType);
    }

    @Override
    public void disableGC(PackageType packageType) {
        packageTypes.remove(packageType);
    }

    @Override
    public void enableAutoSync(PackageType packageType) {
        autoSync.add(packageType);
    }

    @Override
    public void disableAutoSync(PackageType packageType) {
        autoSync.remove(packageType);
    }

    @Override
    public boolean isGCEnabled(PackageType packageType) {
        return packageTypes.contains(packageType);
    }

    @Override
    public boolean isAutoSyncEnabled(PackageType packageType) {
        return autoSync.contains(packageType);
    }

    @Override
    public void disableAll() {
        packageTypes.clear();
    }

    @Override
    public void disableAllAutoSync() {
        autoSync.clear();
    }

    @Override
    public void save() throws IOException {

    }
}
