package fleetmanagement.backend.notifications;

import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Test;

import javax.mail.internet.AddressException;

import static org.junit.Assert.*;

public class ServerExceptionTest {
    ServerException tested;
    TestScenario scenario;
    Vehicle vehicle;
    private NotificationSetting setting;


    @Before
    public void before() throws AddressException {
        scenario = new TestScenario();
        vehicle = scenario.addVehicle();
        setting = NotificationSetting.serverException("dev@gsp.com");
    }

    @Test
    public void notify_WhenExceptionThrown() {
        tested = new ServerException(setting, null, Events.serverException(new Exception()));
        assertTrue(tested.needToSend());
    }

}