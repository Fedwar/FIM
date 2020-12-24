package fleetmanagement.backend.vehicles;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.mail.MailService;
import fleetmanagement.backend.mail.MailServiceImpl;
import fleetmanagement.backend.notifications.VehiclesOffline;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.NotificationSettingRepository;
import fleetmanagement.backend.notifications.settings.Type;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.mail.internet.AddressException;
import java.time.ZonedDateTime;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class OfflineMonitorTest {

    private TestScenario scenario;
    String mailList = "dev@gsp.com";
    OfflineMonitor tested;
    NotificationSettingRepository notificationSettings;
    MailService mailService;
    int offlineDelay = 3;

    @BeforeClass
    public static void beforeClass() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Before
    public void before() throws AddressException {
        scenario = new TestScenario();
        mailService = mock(MailServiceImpl.class);
        notificationSettings = scenario.notificationRepository;
        tested = new OfflineMonitor(scenario.vehicleRepository, scenario.groupRepository, notificationSettings,
                mailService);
    }

    @Test
    public void sendsNotification_IfVehicleIsOffline() throws AddressException {
        addNotificationSetting(offlineDelay);
        addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay+1));
        tested.check();

        verify(mailService).send(any(VehiclesOffline.class));
    }

    @Test
    public void sendsNotification_IfVehicleInGroupIsOffline() throws AddressException {
        Vehicle vehicle = addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay + 1));
        Group group = scenario.addGroup("group");
        vehicle.setGroupId(group.id.toString());
        addNotificationSetting(offlineDelay, group.name, null);
        tested.check();

        verify(mailService).send(any(VehiclesOffline.class));
    }

    @Test
    public void sendsNotification_IfVehicleNameSameAsInSetting() throws AddressException {
        Vehicle vehicle = addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay + 1));
        addNotificationSetting(offlineDelay, null, vehicle.uic);

        tested.check();

        verify(mailService).send(any(VehiclesOffline.class));
    }

    @Test
    public void sendsNotification_IfSettingForAllVehicles() throws AddressException {
        Vehicle vehicle = addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay + 1));
        addNotificationSettingAllVehicles(offlineDelay);

        tested.check();

        verify(mailService).send(any(VehiclesOffline.class));
    }

    @Test
    public void noNotification_IfVehicleNameDiffersFromSetting() throws AddressException {
        addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay + 1));
        addNotificationSetting(offlineDelay, null, "different uic");

        tested.check();

        verify(mailService, never()).send(any(VehiclesOffline.class));
    }

    @Test
    public void noNotification_IfVehicleOfflineTime_LessThanSettingDelay() throws AddressException {
        addNotificationSetting(offlineDelay);
        addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay-1));
        tested.check();

        verify(mailService, never()).send(any(VehiclesOffline.class));
    }

    @Test
    public void noNotification_WhenNoSettings() throws AddressException {
        notificationSettings.findByType(Type.VEHICLE_OFFLINE).stream()
                .forEach(s -> notificationSettings.delete(s.id));
        addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay+1));
        tested.check();

        verify(mailService, never()).send(any(VehiclesOffline.class));
    }

    @Test
    public void works_WhenSettingChanged() throws AddressException {
        addNotificationSetting(offlineDelay);
        addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay-1));
        tested.check();
        clearSettings();
        addNotificationSetting(offlineDelay-2);
        tested.check();

        verify(mailService, times(1)).send(any(VehiclesOffline.class));
    }

    @Test
    public void doesNotRepeatNotification() throws AddressException {
        addNotificationSetting(offlineDelay);
        addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay+1));
        tested.check();
        tested.check();

        verify(mailService, times(1)).send(any(VehiclesOffline.class));
    }

    @Test
    public void repeatNotification_IfOfflineVehiclesCountChanged() throws AddressException {
        addNotificationSetting(offlineDelay);
        addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay+1));
        tested.check();
        addVehicle(ZonedDateTime.now().minusMinutes(offlineDelay+2));
        tested.check();

        verify(mailService, times(2)).send(any(VehiclesOffline.class));
    }

    Vehicle addVehicle(ZonedDateTime lastSeen) {
        Vehicle vehicle = scenario.addVehicle();
        vehicle.lastSeen = lastSeen;
        return vehicle;
    }

    NotificationSetting addNotificationSetting(int delay) throws AddressException {
        NotificationSetting notificationSetting = NotificationSetting.vehicleOffline(mailList, delay,5, null, null, null);
        notificationSettings.insert(notificationSetting);
        return notificationSetting;
    }

    NotificationSetting addNotificationSettingAllVehicles(int delay) throws AddressException {
        NotificationSetting notificationSetting = NotificationSetting
                .vehicleOffline(mailList, delay,5, null, null, "true");
        notificationSettings.insert(notificationSetting);
        return notificationSetting;
    }

    NotificationSetting addNotificationSetting(int delay, String groupName, String vehicleName) throws AddressException {
        NotificationSetting notificationSetting = NotificationSetting
                .vehicleOffline(mailList, delay,5, groupName, vehicleName, null);
        notificationSettings.insert(notificationSetting);
        return notificationSetting;
    }

    void clearSettings() {
        notificationSettings.findByType(Type.VEHICLE_OFFLINE).stream()
                .forEach(s -> notificationSettings.delete(s.id));
    }


}