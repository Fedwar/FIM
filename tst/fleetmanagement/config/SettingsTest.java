package fleetmanagement.config;

import fleetmanagement.backend.settings.Setting;
import fleetmanagement.backend.settings.SettingName;
import fleetmanagement.backend.settings.SettingsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class SettingsTest {

    private Settings settings;
    @Mock
    private SettingsRepository settingsRepository;
    @Mock
    FimConfig config;

    private Setting operationalDataLimitSetting = new Setting(SettingName.OPERATIONAL_DATA_LIMIT, 10d);
    private Setting diagnosisDataLimitSetting = new Setting(SettingName.DIAGNOSIS_DATA_LIMIT, 20d);
    private Setting importFolderPathSetting = new Setting(SettingName.IMPORT_FOLDER_PATH, "import");
    private Setting incomingFolderPathSetting = new Setting(SettingName.INCOMING_FOLDER_PATH, "incoming");

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        settings = new Settings(config, settingsRepository);
    }

    @Test
    public void testAllSet() {
        when(settingsRepository.listAll()).thenReturn(Arrays.asList(operationalDataLimitSetting,
                diagnosisDataLimitSetting, importFolderPathSetting, incomingFolderPathSetting));

        settings.refresh();

        assertEquals(operationalDataLimitSetting.getDoubleValue(), settings.getOperationalDataLimit());
        assertEquals(diagnosisDataLimitSetting.getDoubleValue(), settings.getDiagnosisDataLimit());
        assertEquals(importFolderPathSetting.getStringValue(), settings.getImportFolderPath());
        assertEquals(incomingFolderPathSetting.getStringValue(), settings.getIncomingFolderPath());

        Map<SettingName, Setting> map = settings.get();

        assertEquals(4, map.size());
        assertEquals(operationalDataLimitSetting.getDoubleValue(), map.get(SettingName.OPERATIONAL_DATA_LIMIT).getDoubleValue());
        assertEquals(diagnosisDataLimitSetting.getDoubleValue(), map.get(SettingName.DIAGNOSIS_DATA_LIMIT).getDoubleValue());
        assertEquals(importFolderPathSetting.getStringValue(), map.get(SettingName.IMPORT_FOLDER_PATH).getStringValue());
        assertEquals(incomingFolderPathSetting.getStringValue(), map.get(SettingName.INCOMING_FOLDER_PATH).getStringValue());
    }

    @Test
    public void testEmpty() {
        final String importFolder = "import2";
        final String incomingFolder = "incoming2";
        when(config.getGroupImport()).thenReturn(importFolder);
        when(config.getFilterIncoming()).thenReturn(incomingFolder);

        settings.refresh();

        assertNull(settings.getOperationalDataLimit());
        assertNull(settings.getDiagnosisDataLimit());
        assertEquals(importFolder, settings.getImportFolderPath());
        assertEquals(incomingFolder, settings.getIncomingFolderPath());

        Map<SettingName, Setting> map = settings.get();

        assertEquals(2, map.size());
    }

    @Test
    public void testPartial() {
        when(settingsRepository.listAll()).thenReturn(Arrays.asList(
                diagnosisDataLimitSetting, importFolderPathSetting));

        settings.refresh();

        assertNull(settings.getOperationalDataLimit());
        assertEquals(diagnosisDataLimitSetting.getDoubleValue(), settings.getDiagnosisDataLimit());
        assertEquals(importFolderPathSetting.getStringValue(), settings.getImportFolderPath());
        assertNull(settings.getIncomingFolderPath());

        Map<SettingName, Setting> map = settings.get();

        assertEquals(2, map.size());
        assertEquals(diagnosisDataLimitSetting.getDoubleValue(), map.get(SettingName.DIAGNOSIS_DATA_LIMIT).getDoubleValue());
        assertEquals(importFolderPathSetting.getStringValue(), map.get(SettingName.IMPORT_FOLDER_PATH).getStringValue());
    }
}