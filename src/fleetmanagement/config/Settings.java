package fleetmanagement.config;

import fleetmanagement.backend.settings.Setting;
import fleetmanagement.backend.settings.SettingName;
import fleetmanagement.backend.settings.SettingsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class Settings {

    private Double operationalDataLimit;
    private Double diagnosisDataLimit;
    private String importFolderPath;
    private String incomingFolderPath;
    @Autowired
    private FimConfig config;
    @Autowired
    private SettingsRepository repository;

    public Settings() {
    }

    public Settings(FimConfig config, SettingsRepository repository) {
        this.config = config;
        this.repository = repository;
    }

    @PostConstruct
    public void refresh() {
        List<Setting> settings = repository.listAll();
        if (settings != null) {
            Map<SettingName, Setting> map = settings.stream().collect(Collectors.toMap(Setting::id, s -> s));
            set(map);
        }
    }

    protected void set(Map<SettingName, Setting> settings) {
        operationalDataLimit = Optional.ofNullable(settings.get(SettingName.OPERATIONAL_DATA_LIMIT))
                .map(Setting::getDoubleValue).orElse(null);
        diagnosisDataLimit = Optional.ofNullable(settings.get(SettingName.DIAGNOSIS_DATA_LIMIT))
                .map(Setting::getDoubleValue).orElse(null);
        importFolderPath = Optional.ofNullable(settings.get(SettingName.IMPORT_FOLDER_PATH))
                .map(Setting::getStringValue).orElse(null);
        incomingFolderPath = Optional.ofNullable(settings.get(SettingName.INCOMING_FOLDER_PATH))
                .map(Setting::getStringValue).orElse(null);
    }

    public Map<SettingName, Setting> get() {
        return Stream.of(
                toSetting(SettingName.OPERATIONAL_DATA_LIMIT, getOperationalDataLimit()),
                toSetting(SettingName.DIAGNOSIS_DATA_LIMIT, getDiagnosisDataLimit()),
                toSetting(SettingName.IMPORT_FOLDER_PATH, getImportFolderPath()),
                toSetting(SettingName.INCOMING_FOLDER_PATH, getIncomingFolderPath())
        ).filter(Objects::nonNull)
                .collect(Collectors.toMap(Setting::id, s -> s));
    }

    private Setting toSetting(SettingName name, Object value) {
        if (value != null) {
            return new Setting(name, value);
        }
        return null;
    }

    public Double getOperationalDataLimit() {
        return operationalDataLimit;
    }

    public Double getDiagnosisDataLimit() {
        return diagnosisDataLimit;
    }

    public String getImportFolderPath() {
        return importFolderPath == null ? config.getGroupImport() : importFolderPath;
    }

    public String getIncomingFolderPath() {
        return incomingFolderPath == null ? config.getFilterIncoming() : incomingFolderPath;
    }

    public void setRepository(SettingsRepository repository) {
        this.repository = repository;
    }
}
