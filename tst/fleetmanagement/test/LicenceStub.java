package fleetmanagement.test;

import fleetmanagement.config.Licence;
import fleetmanagement.backend.packages.PackageType;


import java.time.Period;
import java.time.ZonedDateTime;
import java.util.*;

public class LicenceStub implements Licence {

    public ZonedDateTime expired = ZonedDateTime.now().plus(Period.ofYears(1));
    public int maxVehicleCount = 10000;
    public boolean mapAvailable = true;
    public boolean vehicleGeo = true;
    public Set<PackageType> availablePackageTypes = new HashSet();
    public boolean diagnosisInfo = true;
    public boolean operationInfo = true;
    public boolean autoPackageSync = true;
    public boolean upload = true;
    public boolean notifications = true;
    public boolean https = true;
    public boolean reports = true;
    public boolean vehicleIp = true;
    public LicenceStub() {
        availablePackageTypes.addAll(Arrays.asList(PackageType.values()));
    }

    @Override
    public String getInstallationSeed() {
        return null;
    }

    @Override
    public boolean isExpired() {
        return expired != null && expired.isBefore(ZonedDateTime.now());
    }

    @Override
    public boolean isMapAvailable() {
        return mapAvailable;
    }

    @Override
    public boolean isDiagnosisInfoAvailable() {
        return diagnosisInfo;
    }

    @Override
    public boolean isOperationInfoAvailable() {
        return operationInfo;
    }

    @Override
    public boolean isVehicleGeoAvailable() {
        return vehicleGeo;
    }

    @Override
    public boolean isAutoPackageSyncAvailable() {
        return autoPackageSync;
    }

    @Override
    public boolean isUploadAvailable() {
        return upload;
    }

    @Override
    public boolean isNotificationsAvailable() {
        return notifications;
    }

    @Override
    public boolean isHttpsAvailable() {
        return https;
    }

    @Override
    public boolean isReportsAvailable() {
        return reports;
    }

    @Override
    public boolean isPackageTypeAvailable(PackageType packageType) {
        return availablePackageTypes.contains(packageType);
    }

    @Override
    public void update() {}

    @Override
    public boolean update(String command) {return true;}

    @Override
    public int getMaximumVehicleCount() {
        return maxVehicleCount;
    }

    @Override
    public String getExpirationDate() {
        return null;
    }

    @Override
    public Set<PackageType> getPackageTypes() {
        return availablePackageTypes;
    }

    @Override
    public void saveLicenceToFile(String licence) {}

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public List<String> getLanguages() {
        return Arrays.asList("de", "es", "fr", "pl", "cs", "ru");
    }

    @Override
    public boolean isVehicleIpAvailable() {
        return vehicleIp;
    }


}
