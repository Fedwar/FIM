package fleetmanagement.backend.installations;

import fleetmanagement.backend.repositories.Repository;
import fleetmanagement.backend.settings.Setting;

import java.util.UUID;

public interface PackageInstallationRepository extends Repository<PackageInstallation, UUID> {

    void insertOrReplace(PackageInstallation entity);

}
