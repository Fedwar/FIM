package fleetmanagement.backend.notifications;

import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.notifications.settings.Type;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Test;

import javax.mail.internet.AddressException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IndicatorValueRangeTest {
    IndicatorValueRange tested;
    TestScenario scenario;
    Vehicle vehicle;
    private NotificationSetting setting;
    String indicatorId = "tank1";

    @Before
    public void before() {
        scenario = new TestScenario();
        vehicle = scenario.addVehicle();
    }

    @Test
    public void doNotNotify_WhenSettingParametersEmpty() throws AddressException {
        Boolean indicatorValue = true;
        Indicator indicator = newIndicator(indicatorId, indicatorValue);
        OperationData operationData = newOperationData(indicator);
        setting = newSetting(null, null);

        tested = new IndicatorValueRange(setting, scenario.vehicleRepository, Events.operationDataUpdated(operationData));
        assertTrue(tested.needToSend());
    }

    private NotificationSetting newSetting(String lowerLimit , String upperLimit) throws AddressException {
        HashMap<Parameter, String> parameters = new HashMap<>();
        parameters.put(Parameter.INDICATOR_ID, indicatorId);
        parameters.put(Parameter.UPPER_LIMIT, upperLimit);
        parameters.put(Parameter.LOWER_LIMIT, lowerLimit);
        return new NotificationSetting(Type.INDICATOR_VALUE_RANGE,  "dev@gsp.com", parameters);
    }

    @Test
    public void doNotNotify_WhenValueInRange() throws AddressException {
        Integer indicatorValue = 5;
        Indicator indicator = newIndicator(indicatorId, indicatorValue);
        OperationData operationData = newOperationData(indicator);
        setting = newSetting(0, 5);

        tested = new IndicatorValueRange(setting, scenario.vehicleRepository, Events.operationDataUpdated(operationData));
        assertFalse(tested.needToSend());
    }

    @Test
    public void notify__WhenValueOutOfRange() throws AddressException {
        Integer indicatorValue = 6;
        Indicator indicator = newIndicator(indicatorId, indicatorValue);
        OperationData operationData = newOperationData(indicator);
        setting = newSetting(0, 5);

        tested = new IndicatorValueRange(setting, scenario.vehicleRepository, Events.operationDataUpdated(operationData));
        assertTrue(tested.needToSend());
    }

    Indicator newIndicator(String id, Object value) {
        return new Indicator(id, "unit", value, ZonedDateTime.now());
    }

    OperationData newOperationData(Indicator... indicators) {
        return new OperationData(vehicle.id, ZonedDateTime.now(), Arrays.asList(indicators));
    }

    private NotificationSetting newSetting(long lowerLimit , long upperLimit) throws AddressException {
        return NotificationSetting.indicatorValueRange(indicatorId, lowerLimit
                , upperLimit, "dev@gsp.com");
    }
}