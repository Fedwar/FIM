package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.settings.Setting;
import fleetmanagement.backend.settings.SettingName;
import fleetmanagement.backend.settings.SettingsRepository;
import fleetmanagement.config.FimConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class SettingsSqlRepository extends SqlRepository<Setting, SettingName> implements SettingsRepository {

    public static final String DB_NAME = "settings.db";
    private static final Class<Setting> PERSIST_CLASS = Setting.class;

    @Autowired
    public SettingsSqlRepository(FimConfig config) {
        this(config.getSettingsDirectory());
    }

    public SettingsSqlRepository(File directory) {
        super(new HibernateSQLitePersistenceManager(directory, PERSIST_CLASS, DB_NAME));
        directory.mkdirs();
    }

    public SettingsSqlRepository(PersistenceManager persistenceManager) {
        super(persistenceManager);
    }

    @Override
    protected Class<Setting> getEntityClass() {
        return PERSIST_CLASS;
    }
}
