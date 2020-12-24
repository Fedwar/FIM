package fleetmanagement.config;


import fleetmanagement.backend.packages.PackageType;
import gsp.util.DoNotObfuscate;

import java.util.List;
import java.util.Set;

@DoNotObfuscate
public interface Licence {

    String getInstallationSeed();

    boolean isExpired();

    boolean isMapAvailable();

    boolean isDiagnosisInfoAvailable();

    boolean isOperationInfoAvailable();

    boolean isVehicleGeoAvailable();

    boolean isAutoPackageSyncAvailable();

    boolean isUploadAvailable();

    boolean isNotificationsAvailable();

    boolean isHttpsAvailable();

    boolean isReportsAvailable();

    boolean isPackageTypeAvailable(PackageType packageType);

    void update();

    boolean update(String command);

    int getMaximumVehicleCount();

    String getExpirationDate();

    Set<PackageType> getPackageTypes();

    void saveLicenceToFile(String licence);

    boolean isLoaded();

    public List<String> getLanguages();

    boolean isVehicleIpAvailable();
}
