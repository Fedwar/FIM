package fleetmanagement.frontend.model;


import fleetmanagement.backend.settings.SettingName;
import fleetmanagement.config.Licence;
import fleetmanagement.config.Settings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static fleetmanagement.backend.settings.SettingName.DIAGNOSIS_DATA_LIMIT;
import static fleetmanagement.backend.settings.SettingName.OPERATIONAL_DATA_LIMIT;
import static fleetmanagement.backend.settings.SettingName.values;

public class SettingsModel extends Admin {
    public final List<Setting> settings = new ArrayList<>();

    public SettingsModel(Licence licence, Settings settings) {
        super(licence);

        Map<SettingName, fleetmanagement.backend.settings.Setting> map = settings.get();
        for (SettingName settingName : values()) {
            if (settingName == OPERATIONAL_DATA_LIMIT && !licence.isOperationInfoAvailable())
                continue;
            if(settingName == DIAGNOSIS_DATA_LIMIT && !licence.isDiagnosisInfoAvailable())
                continue;
            this.settings.add(new Setting(settingName, map.get(settingName)));
        }
    }

    public static class Setting {
        public String id;
        public String name;
        public Object value;

        public Setting(SettingName settingName, fleetmanagement.backend.settings.Setting setting) {
            id = settingName.toString();
            name = settingName.getResourceKey();
            if (setting != null)
                if (setting.id() == DIAGNOSIS_DATA_LIMIT
                        || setting.id() == OPERATIONAL_DATA_LIMIT) {
                    if (setting.getDoubleValue() != null) {
                        value = setting.getDoubleValue().toString()
                                .replace(".", ",");
                    }
                } else
                    value = setting.getValue();
        }
    }
}
