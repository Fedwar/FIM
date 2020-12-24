package fleetmanagement.frontend.controllers;


import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.backend.groups.GroupsWatcher;
import fleetmanagement.backend.settings.Setting;
import fleetmanagement.backend.settings.SettingName;
import fleetmanagement.backend.settings.SettingsRepository;
import fleetmanagement.config.Settings;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import fleetmanagement.usecases.StorageManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SettingsTest {
    private TestScenarioPrefilled scenario;
    private SettingsController tested;
    @Rule
    public TempFileRule tempFolder = new TempFileRule();
    public TempFile groupsFolder;
    @Mock
    public Settings settings;
    @Mock
    private SettingsRepository settingsRepository;
    @Captor
    private ArgumentCaptor<Setting> settingCaptor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
//        when(settingsRule.settings.getRepository()).thenReturn(settingsRepository);
        scenario = new TestScenarioPrefilled();
        groupsFolder = tempFolder.newFolder("Groups");
        tested = new SettingsController(new SessionStub(), scenario.licence,
                mock(GroupsWatcher.class), scenario.filterRepository, mock(StorageManager.class), settingsRepository);
        tested.setSettings(settings);
    }

    @After
    public void tearDown() {
    }

    @Test
    public void savesDataLimitSettings() {
        tested.saveSettings("[{'id':'DIAGNOSIS_DATA_LIMIT','value':'0.5'},{'id':'OPERATIONAL_DATA_LIMIT','value':'10'}]");
        verify(settingsRepository, times(2)).insertOrReplace(settingCaptor.capture());
        assertEquals(2, settingCaptor.getAllValues().size());

        Setting s = settingCaptor.getAllValues().get(0);
        assertEquals(SettingName.DIAGNOSIS_DATA_LIMIT, s.id());
        assertNotNull(s.getDoubleValue());
        assertEquals(0.5, s.getDoubleValue(), 0d);

        s = settingCaptor.getAllValues().get(1);
        assertEquals(SettingName.OPERATIONAL_DATA_LIMIT, s.id());
        assertNotNull(s.getDoubleValue());
        assertEquals(10d, s.getDoubleValue(), 0d);
    }

    @Test
    public void saveSettings() {
        tested.saveSettings("[{'id':'IMPORT_FOLDER_PATH','value':'import\\\\path'},{'id':'INCOMING_FOLDER_PATH','value':'incoming\\\\path'}]");

        verify(settingsRepository, times(2)).insertOrReplace(settingCaptor.capture());
        assertEquals(2, settingCaptor.getAllValues().size());

        assertEquals(SettingName.IMPORT_FOLDER_PATH, settingCaptor.getAllValues().get(0).id());
        assertEquals("import\\path", settingCaptor.getAllValues().get(0).getStringValue());

        assertEquals(SettingName.INCOMING_FOLDER_PATH, settingCaptor.getAllValues().get(1).id());
        assertEquals("incoming\\path", settingCaptor.getAllValues().get(1).getStringValue());
    }
}