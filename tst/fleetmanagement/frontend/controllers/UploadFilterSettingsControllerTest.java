package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.vehiclecommunication.upload.filter.*;
import fleetmanagement.config.FimConfig;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UploadFilterSettingsControllerTest {

    private UploadFilterSettingsController tested;
    private TestScenarioPrefilled scenario;

    @Before
    public void setup() {
        scenario = new TestScenarioPrefilled();
        tested = new UploadFilterSettingsController(new SessionStub(), scenario.filterRepository, scenario.licence);
    }

    @Test
    public void saveUploadFilter() {
        String data = "[{'name':'logs','dir':'logs','conditions':[{'matchString':'*.log','type':'FILE_NAME'},{'matchString':'FA*','type':'GROUP_NAME'}]}]";

        tested.saveUploadFilter(data);

        UploadFilterSequence byType = scenario.filterRepository.findByType(FilterType.AD_FILTER_TYPE);
        UploadFilter filter = byType.getFilter("logs");
        UploadFilterCondition condition0 = filter.getConditions().get(0);
        UploadFilterCondition condition1 = filter.getConditions().get(1);

        assertEquals(1, byType.filters.size());
        assertEquals(2, filter.getConditions().size());

        assertEquals(ConditionType.FILE_NAME, condition0.type);
        assertEquals("*.log", condition0.matchString);
        assertEquals(".*\\.log", condition0.regex);

        assertEquals(ConditionType.GROUP_NAME, condition1.type);
        assertEquals("FA*", condition1.matchString);
        assertEquals("FA.*", condition1.regex);
    }
}