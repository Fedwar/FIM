package fleetmanagement.frontend.model;

import fleetmanagement.backend.settings.SettingName;
import fleetmanagement.config.Settings;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class SettingsModelTest {

    private TestScenarioPrefilled scenario;
    private UserSession session;
    @Mock
    private Settings settings;

    @Before
    public void setup() {
        initMocks(this);
        scenario = new TestScenarioPrefilled();
        session = new SessionStub();
    }

    @Test
    public void visibilityOfOperationalDataSettingDependsOnLicence() {
        scenario.licence.operationInfo = true;
        SettingsModel tested = new SettingsModel(scenario.licence, settings);

        assertTrue(containsSetting(tested, SettingName.OPERATIONAL_DATA_LIMIT ));

        scenario.licence.operationInfo = false;
        tested = new SettingsModel(scenario.licence, settings);

        assertFalse(containsSetting(tested, SettingName.OPERATIONAL_DATA_LIMIT ));
    }

    @Test
    public void visibilityOfDiagnosisDataSettingDependsOnLicence() {
        scenario.licence.diagnosisInfo = true;
        SettingsModel tested = new SettingsModel(scenario.licence, settings);

        assertTrue(containsSetting(tested, SettingName.DIAGNOSIS_DATA_LIMIT ));

        scenario.licence.diagnosisInfo = false;
        tested = new SettingsModel(scenario.licence, settings);

        assertFalse(containsSetting(tested, SettingName.DIAGNOSIS_DATA_LIMIT ));
    }

    public boolean containsSetting(SettingsModel tested, SettingName settingName) {
        return tested.settings.stream()
                .anyMatch(setting -> setting.id.equals(settingName.toString()));
    }

}