package fleetmanagement.frontend.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fleetmanagement.backend.groups.GroupsWatcher;
import fleetmanagement.backend.settings.Setting;
import fleetmanagement.backend.settings.SettingName;
import fleetmanagement.backend.settings.SettingsRepository;
import fleetmanagement.backend.vehiclecommunication.upload.filter.FilterSequenceRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.config.Settings;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.SettingsModel;
import fleetmanagement.frontend.security.webserver.ConfigRoleRequired;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.usecases.StorageManager;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static fleetmanagement.backend.settings.SettingName.IMPORT_FOLDER_PATH;
import static fleetmanagement.backend.settings.SettingName.INCOMING_FOLDER_PATH;

@Path("settings")
@ConfigRoleRequired
@Component
public class SettingsController extends FrontendController {

    private static final Logger logger = Logger.getLogger(SettingsController.class);

    @Autowired
    private Settings settings;
    private Licence licence;
    private final SettingsRepository settingsRepository;
    private final GroupsWatcher watcher;
    private final FilterSequenceRepository filterSequenceRepository;
    private final StorageManager storageManager;

    @Autowired
    public SettingsController(UserSession request, Licence licence, GroupsWatcher watcher, FilterSequenceRepository filterSequenceRepository,
                              StorageManager storageManager, SettingsRepository settingsRepository) {
        super(request);
        this.licence = licence;
        this.settingsRepository = settingsRepository;
        this.watcher = watcher;
        this.filterSequenceRepository = filterSequenceRepository;
        this.storageManager = storageManager;
    }

    @GET
    public ModelAndView<SettingsModel> showNotifications() {
        return new ModelAndView<>("admin-settings.html",
                new SettingsModel(licence, settings));
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveSettings(String data) {
        Type listType = new TypeToken<ArrayList<SettingJson>>() {}.getType();
        List<SettingJson> settings;

        try {
            settings = new Gson().fromJson(data, listType);
        } catch (Exception e) {
            logger.error("Json parsing error", e);
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(i18n("settings_save_error"))
                    .build();
        }
        Map<SettingName, Setting> changed = getChanges(settings);
        Map<SettingName, String> errors = validate(changed);
        if (!errors.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(errors)
                    .build();
        }

        saveSettings(changed.values());
        this.settings.refresh();
        if (changed.containsKey(IMPORT_FOLDER_PATH)) {
            watcher.restart();
        }
        if (changed.containsKey(INCOMING_FOLDER_PATH)) {
            filterSequenceRepository.createFilterDirectories();
        }

        storageManager.restart();

        return Response.status(Response.Status.OK)
                .entity(i18n("settings_save_success"))
                .build();
    }

    private Map<SettingName, Setting> getChanges(List<SettingJson> settings) {
        Map<SettingName, Setting> existing = this.settings.get();
        Map<SettingName, Setting> changed = new LinkedHashMap<>();
        for (SettingJson settingJson : settings) {
            Setting setting = settingJson.toSetting();
            Setting oldSetting = existing.get(setting.id());
            if (!isSettingsEquals(oldSetting, setting)) {
                changed.put(setting.id(), setting);
            }
        }
        return changed;
    }

    private boolean isSettingsEquals(Setting oldSetting, Setting newSetting) {
        Object oldValue = "";
        if (oldSetting != null ) {
            oldValue = oldSetting.getValue();
        }

        Object newValue = "";
        if (newSetting != null ) {
            newValue = newSetting.getValue();
        }

        return oldValue.equals(newValue);
    }

    private Map<SettingName, String> validate(Map<SettingName, Setting> changed) {
        Map<SettingName, String> result = new HashMap<>();
        SettingName[] fileNames = new SettingName[] {IMPORT_FOLDER_PATH, INCOMING_FOLDER_PATH};
        for (SettingName s : fileNames) {
            if (changed.containsKey(s)) {
                if (changed.get(s).getStringValue().isEmpty()) {
                    result.put(s, i18n("settings.validation.required"));
                    continue;
                }
                java.nio.file.Path target = null;
                try {
                    target = Paths.get(changed.get(s).getStringValue());
                } catch (Exception e) {
                    logger.info("Validation: invalid path " + s + " " + target + ", " + e.toString());
                    result.put(s, i18n("settings.validation.invalid-path"));
                    continue;
                }
                if (Files.exists(target)) {
                    if (!Files.isDirectory(target)) {
                        result.put(s, i18n("settings.validation.not-a-directory"));
                    }
                    if (!Files.isWritable(target)) {
                        result.put(s, i18n("settings.validation.no-write-permissions"));
                    }
                } else if (Files.notExists(target)) {
                    try {
                        logger.debug("Creating directory: " + target);
                        Files.createDirectories(target);
                        if (Files.exists(target)) {
                            logger.debug("Directory created: " + target);
                        } else {
                            logger.info("Validation: directory " + target + " not created, but no error thrown");
                            result.put(s, i18n("settings.validation.cannot-create"));
                        }
                    } catch (IOException e) {
                        logger.info("Validation: couldn't create directories for " + s + " due to " + e.toString());
                        result.put(s, i18n("settings.validation.cannot-create"));
                    }
                } else {
                    result.put(s, i18n("settings.validation.no-permission"));
                }
            }
        }
        return result;
    }

    private void saveSettings(Collection<Setting> settings) {
        for (Setting setting : settings) {
            if (setting.getStringValue() != null && setting.getStringValue().isEmpty()) {
                settingsRepository.delete(setting.id());
            } else {
                if (setting.id() == SettingName.DIAGNOSIS_DATA_LIMIT
                        || setting.id() == SettingName.OPERATIONAL_DATA_LIMIT) {
                    if (setting.getLongValue() != null) {
                        setting.setDoubleValue(setting.getLongValue().doubleValue());
                        setting.setLongValue(null);
                    }
                    if (setting.getStringValue() != null) {
                        String value = setting.getStringValue().replace(",", ".");
                        setting.setDoubleValue(Double.parseDouble(value));
                        setting.setStringValue(null);
                    }
                }
                settingsRepository.insertOrReplace(setting);
            }
        }
    }

    @DoNotObfuscate
    public static class SettingJson {
        public String id;
        public Object value;

        Setting toSetting() {
            try {
                return new Setting(SettingName.valueOf(id), Long.parseLong(value.toString()));
            } catch (NumberFormatException ignored) {
            }
            try {
                return new Setting(SettingName.valueOf(id), Double.parseDouble(value.toString()));
            } catch (NumberFormatException ignored) {
            }
            return new Setting(SettingName.valueOf(id), value);
        }
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}