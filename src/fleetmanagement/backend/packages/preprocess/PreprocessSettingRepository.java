package fleetmanagement.backend.packages.preprocess;

import fleetmanagement.backend.repositories.Repository;

import java.util.UUID;

public interface PreprocessSettingRepository extends Repository<PreprocessSetting, UUID> {

    void insertOrReplace(PreprocessSetting setting);

}