package fleetmanagement.backend.notifications;

import fleetmanagement.backend.mail.MailService;
import fleetmanagement.backend.mail.MailServiceImpl;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.NotificationSettingRepository;
import fleetmanagement.backend.vehicles.OfflineMonitor;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Test;

import javax.mail.internet.AddressException;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class VehiclesOfflineTest {

    private TestScenario scenario;
    String mailList = "dev@gsp.com";

    int offlineDelay = 3;

    @Before
    public void before() throws AddressException {
        scenario = new TestScenario();

    }

    @Test
    public void notificationContainsAllOfflineVehicles() throws AddressException {
        Vehicle vehicle1 = addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay + 1));
        Vehicle vehicle2 = addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay + 1));
        Vehicle vehicle3 = addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay - 1));
        NotificationSetting notificationSetting = NotificationSetting
                .vehicleOffline(mailList, offlineDelay,5, null, vehicle1.uic, null);
        VehiclesOffline tested = new VehiclesOffline(notificationSetting, scenario.vehicleRepository.listAll(), Collections.emptyList());

        assertEquals(1,tested.getOfflineVehicles().size());
        assertTrue(tested.getOfflineVehicles().contains(vehicle1));
        assertEquals(1,tested.getOtherOfflineVehicles().size());
        assertTrue(tested.getOtherOfflineVehicles().contains(vehicle2));
    }


    Vehicle addVehicle(ZonedDateTime lastSeen) {
        Vehicle vehicle = scenario.addVehicle();
        vehicle.lastSeen = lastSeen;
        return vehicle;
    }

}