package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFileRule;
import fleetmanagement.backend.settings.Setting;
import fleetmanagement.backend.settings.SettingName;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SettingsRepositoryTest {

    private SettingsSqlRepository tested;

    @Rule
    public TempFileRule dataFolder = new TempFileRule();

    @Before
    public void setUp() {
        tested = new SettingsSqlRepository(dataFolder);
    }

    private void assertSettingIsPersisted(Setting setting) {
        SettingsSqlRepository verifier = new SettingsSqlRepository(dataFolder);
        Setting loaded = verifier.tryFindById(setting.id());

        assertNotNull(loaded);
        assertEquals(setting.id(), loaded.id());
        assertEquals(setting.getStringValue(), loaded.getStringValue());
    }

    @Test
    public void persistsStringSetting() {
        Setting setting = new Setting(SettingName.OPERATIONAL_DATA_LIMIT, "enabled");
        tested.insert(setting);

        assertSettingIsPersisted(setting);
    }

    @Test
    public void persistsDoubleSetting() {
        Setting setting = new Setting(SettingName.OPERATIONAL_DATA_LIMIT, 95.0);
        tested.insert(setting);

        assertSettingIsPersisted(setting);
    }

    @Test
    public void persistsLongSetting() {
        Setting setting = new Setting(SettingName.OPERATIONAL_DATA_LIMIT, new Long(10));
        tested.insert(setting);

        assertSettingIsPersisted(setting);
    }

    @Test
    public void deletesSetting() {
        Setting settingString = new Setting(SettingName.OPERATIONAL_DATA_LIMIT, "enabled");
        tested.insert(settingString);
        tested.delete(settingString.id());
        tested = new SettingsSqlRepository(dataFolder);

        assertEquals(Collections.emptyList(), tested.listAll());
    }


}