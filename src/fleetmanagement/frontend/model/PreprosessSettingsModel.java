package fleetmanagement.frontend.model;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.preprocess.PreprocessSetting;
import fleetmanagement.backend.packages.preprocess.PreprocessSettingRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.UserSession;
import gsp.util.DoNotObfuscate;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class PreprosessSettingsModel extends Admin {

    private UserSession session;
    public List<SettingModel> settings;
    public Set<PackageType> packageTypes;

    public PreprosessSettingsModel(UserSession session, Licence licence, PreprocessSettingRepository settingsRepository) {
        super(licence);
        packageTypes = licence.getPackageTypes();
        settings = settingsRepository.stream()
                .map(setting -> new SettingModel(setting, session))
                .collect(Collectors.toList());
    }

    @DoNotObfuscate
    public static class SettingModel {
        public String id;
        public PackageType packageType;
        public String command;
        public String options;
        public String fileNamePattern;
        public String packageTypeCaption;

        public SettingModel(PreprocessSetting setting, UserSession request) {
            id = setting.id.toString();
            packageType = setting.packageType;
            command = setting.command;
            options = setting.options;
            fileNamePattern = setting.fileNamePattern;
            if (packageType != null)
                packageTypeCaption = I18n.get(request, setting.packageType.getResourceKey());
        }

        public PreprocessSetting toSetting() {
            UUID uuid = null;
            if (id == null || id.isEmpty())
                uuid = UUID.randomUUID();
            else
                uuid = UUID.fromString(id);

            return new PreprocessSetting(uuid, packageType, command, options, fileNamePattern);
        }
    }

}
