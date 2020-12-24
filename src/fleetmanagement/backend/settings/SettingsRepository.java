package fleetmanagement.backend.settings;

import fleetmanagement.backend.repositories.Repository;

public interface SettingsRepository extends Repository<Setting, SettingName> {

    void insertOrReplace(Setting setting);

}
