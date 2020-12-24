package fleetmanagement.backend.notifications;

import fleetmanagement.backend.diagnosis.DeviceStatus;
import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.LocalizedString;
import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Test;

import javax.mail.internet.AddressException;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiagnosedDeviceErrorTest {
    DiagnosedDeviceError tested;
    TestScenario scenario;
    Vehicle vehicle;
    private DiagnosedDevice brokenDevice;
    private DiagnosedDevice healthyDevice;
    private NotificationSetting setting;
    private String brokenDeviceName;
    private String healthyDeviceName;

    @Before
    public void before() throws AddressException {
        scenario = new TestScenario();
        vehicle = scenario.addVehicle();
        brokenDeviceName = "brokenDevice";
        healthyDeviceName = "healthyDevice";
        brokenDevice = newDevice(brokenDeviceName, DeviceStatus.DEFECT);
        healthyDevice = newDevice(healthyDeviceName, DeviceStatus.OK);
    }

    private NotificationSetting newSetting(String deviceName) throws AddressException {
        return NotificationSetting.diagnosedDeviceError(deviceName, "dev@gsp.com");
    }


    @Test
    public void notify_WhenSettingHasBrokenDevice_AndDiagnosisHasBrokenDevice() throws AddressException {
        setting = newSetting(brokenDeviceName + ", " + healthyDeviceName);
        Diagnosis diagnosis = newDiagnosisWithDevices(brokenDevice, healthyDevice);

        tested = new DiagnosedDeviceError(setting, scenario.vehicleRepository, Events.diagnosisUpdated(diagnosis));
        assertTrue(tested.needToSend());
    }

    @Test
    public void doNotNotify_WhenDiagnosisHasNoBrokenDevice() throws AddressException {
        setting = newSetting(brokenDeviceName + ", " + healthyDeviceName);
        Diagnosis diagnosis = newDiagnosisWithDevices(healthyDevice);

        tested = new DiagnosedDeviceError(setting, scenario.vehicleRepository, Events.diagnosisUpdated(diagnosis));
        assertFalse(tested.needToSend());
    }

    @Test
    public void doNotNotify_WhenSettingHasNoBrokenDevice() throws AddressException {
        setting = newSetting(healthyDeviceName);
        Diagnosis diagnosis = newDiagnosisWithDevices(brokenDevice, healthyDevice);

        tested = new DiagnosedDeviceError(setting, scenario.vehicleRepository, Events.diagnosisUpdated(diagnosis));
        assertFalse(tested.needToSend());
    }

    @Test
    public void mailText_HasRequiredInfo() throws AddressException {
        String brokenDeviceName1 = "brokenDevice1";
        String brokenDeviceName2 = "brokenDevice2";
        setting = newSetting(brokenDeviceName1 + ", " + brokenDeviceName2);
        Diagnosis diagnosis = newDiagnosisWithDevices(
                newDevice(brokenDeviceName1, DeviceStatus.DEFECT),
                newDevice(brokenDeviceName2, DeviceStatus.DEFECT));
        tested = new DiagnosedDeviceError(setting, scenario.vehicleRepository, Events.diagnosisUpdated(diagnosis));
        tested.needToSend();

        String mailText = tested.mailText();

        assertTrue(mailText.contains(brokenDeviceName1));
        assertTrue(mailText.contains(brokenDeviceName2));
        assertTrue(mailText.contains(vehicle.getName()));
    }

    DiagnosedDevice newDevice(String name, DeviceStatus status) {
        return new DiagnosedDevice("id", "location", new LocalizedString(name), "type", status.toString(), null, true, null, null);
    }

    Diagnosis newDiagnosisWithDevices(DiagnosedDevice... devices) {
        return new Diagnosis(vehicle.id, ZonedDateTime.now(), Arrays.asList(devices));
    }


}