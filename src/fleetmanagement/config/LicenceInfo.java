package fleetmanagement.config;

import fleetmanagement.backend.packages.PackageType;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.apache.commons.lang3.ObjectUtils.*;

public class LicenceInfo {
    public final ZonedDateTime expired;
    public final int vehicleCount;
    public final boolean mapAvailable;
    public final boolean vehicleGeoAvailable;
    public final Set<PackageType> availablePackageTypes;
    public final boolean diagnosisInfo;
    public final boolean operationInfo;
    public final boolean autoPackageSync;
    public final boolean upload;
    public final boolean notifications;
    public final boolean https;
    public final boolean reports;
    public final List<String> languages;
    public final boolean vehicleIp;

    public LicenceInfo(ZonedDateTime expired, int vehicleCount, boolean mapAvailable, boolean vehicleGeoAvailable,
                       Set<PackageType> availablePackageTypes, boolean diagnosisInfo, boolean operationInfo,
                       boolean autoPackageSync, boolean upload, boolean notifications, boolean https, boolean reports,
                       List<String> languages, boolean vehicleIp) {
        this.expired = expired;
        this.vehicleCount = vehicleCount;
        this.mapAvailable = mapAvailable;
        this.vehicleGeoAvailable = vehicleGeoAvailable;
        this.availablePackageTypes = defaultIfNull(availablePackageTypes, Collections.emptySet());
        this.diagnosisInfo = diagnosisInfo;
        this.operationInfo = operationInfo;
        this.autoPackageSync = autoPackageSync;
        this.upload = upload;
        this.notifications = notifications;
        this.https = https;
        this.reports = reports;
        this.languages = defaultIfNull(languages, Collections.EMPTY_LIST);
        this.vehicleIp = vehicleIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LicenceInfo that = (LicenceInfo) o;
        return vehicleCount == that.vehicleCount &&
                mapAvailable == that.mapAvailable &&
                vehicleGeoAvailable == that.vehicleGeoAvailable &&
                diagnosisInfo == that.diagnosisInfo &&
                operationInfo == that.operationInfo &&
                autoPackageSync == that.autoPackageSync &&
                upload == that.upload &&
                notifications == that.notifications &&
                https == that.https &&
                reports == that.reports &&
                vehicleIp == that.vehicleIp &&
                Objects.equals(expired, that.expired) &&
                Objects.equals(availablePackageTypes, that.availablePackageTypes) &&
                Objects.equals(languages, that.languages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expired, vehicleCount, mapAvailable, vehicleGeoAvailable, availablePackageTypes, diagnosisInfo, operationInfo, autoPackageSync, upload, notifications, https, reports, languages, vehicleIp);
    }
};
