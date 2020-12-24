package fleetmanagement.backend.packages;

import java.io.IOException;

public interface PackageTypeRepository {
    void enableGC(PackageType packageType);
    void disableGC(PackageType packageType);
    void enableAutoSync(PackageType packageType);
    void disableAutoSync(PackageType packageType);
    boolean isGCEnabled(PackageType packageType);
    boolean isAutoSyncEnabled(PackageType packageType);
    void disableAll();
    void disableAllAutoSync();
    void save() throws IOException;
}
