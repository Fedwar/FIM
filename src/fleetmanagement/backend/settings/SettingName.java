package fleetmanagement.backend.settings;

import gsp.util.DoNotObfuscate;

@DoNotObfuscate
public enum SettingName {

    OPERATIONAL_DATA_LIMIT("setting_operational_data_limit"),
    DIAGNOSIS_DATA_LIMIT("setting_diagnosis_data_limit"),
    IMPORT_FOLDER_PATH("settings_import_folder_path"),
    INCOMING_FOLDER_PATH("settings_incoming_folder_path");

    private final String resourceKey;

    SettingName(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public String getResourceKey() {
        return resourceKey;
    };

    public static SettingName getByResourceKey(String resourceKey) {
        for (SettingName type : values()) {
            if (type.getResourceKey().equals(resourceKey)) {
                return type;
            }
        }
        return null;
    }
}
