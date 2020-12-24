package fleetmanagement.backend.notifications;

import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Test;

import javax.mail.internet.AddressException;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IndicatorInvalidValueTest {
    IndicatorInvalidValue tested;
    TestScenario scenario;
    Vehicle vehicle;
    private NotificationSetting setting;
    String indicatorId = "tank1";

    @Before
    public void before() {
        scenario = new TestScenario();
        vehicle = scenario.addVehicle();
    }

    private NotificationSetting newSetting(String invalidValue) throws AddressException {
        return NotificationSetting.indicatorInvalidValue(indicatorId, invalidValue, "dev@gsp.com");
    }

    @Test
    public void notify_WhenIndicatorHasInvalidBooleanValue() throws AddressException {
        Boolean indicatorValue = true;
        Indicator indicator = newIndicator(indicatorId, indicatorValue);
        OperationData operationData = newOperationData(indicator);
        setting = newSetting("true");

        tested = new IndicatorInvalidValue(setting, scenario.vehicleRepository, Events.operationDataUpdated(operationData));
        assertTrue(tested.needToSend());
    }

    @Test
    public void doNotNotify_WhenIndicatorHasValidBooleanValue() throws AddressException {
        Boolean indicatorValue = true;
        Indicator indicator = newIndicator(indicatorId, indicatorValue);
        OperationData operationData = newOperationData(indicator);
        setting = newSetting("false");

        tested = new IndicatorInvalidValue(setting, scenario.vehicleRepository, Events.operationDataUpdated(operationData));
        assertFalse(tested.needToSend());
    }

    @Test
    public void notify_WhenIndicatorHasInvalidDoubleValue() throws AddressException {
        Double indicatorValue = 0.55;
        Indicator indicator = newIndicator(indicatorId, indicatorValue);
        OperationData operationData = newOperationData(indicator);
        setting = newSetting("0.55");

        tested = new IndicatorInvalidValue(setting, scenario.vehicleRepository, Events.operationDataUpdated(operationData));
        assertTrue(tested.needToSend());
    }

    @Test
    public void doNotNotify_WhenIndicatorHasValidDoubleValue() throws AddressException {
        Double indicatorValue = 0.55;
        Indicator indicator = newIndicator(indicatorId, indicatorValue);
        OperationData operationData = newOperationData(indicator);
        setting = newSetting("0.99");

        tested = new IndicatorInvalidValue(setting, scenario.vehicleRepository, Events.operationDataUpdated(operationData));
        assertFalse(tested.needToSend());
    }

    @Test
    public void mailText_HasRequiredInfo() throws AddressException {
        String indicatorValue = "ErrorMessage";
        Indicator indicator = newIndicator(indicatorId, indicatorValue);
        OperationData operationData = newOperationData(indicator);
        setting = newSetting("ErrorMessage");

        tested = new IndicatorInvalidValue(setting, scenario.vehicleRepository, Events.operationDataUpdated(operationData));
        tested.needToSend();

        String mailText = tested.mailText();
        assertTrue(mailText.contains(indicatorValue));
        assertTrue(mailText.contains(indicatorId));
        assertTrue(mailText.contains(vehicle.getName()));
    }

    Indicator newIndicator(String id, Object value) {
        return new Indicator(id, "unit", value, ZonedDateTime.now());
    }

    OperationData newOperationData(Indicator... indicators) {
        return new OperationData(vehicle.id, ZonedDateTime.now(), Arrays.asList(indicators));
    }

}