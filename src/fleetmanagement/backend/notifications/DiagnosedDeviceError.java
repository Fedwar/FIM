package fleetmanagement.backend.notifications;

import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.events.Event;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import javax.mail.Multipart;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiagnosedDeviceError implements Notification {

    private final NotificationSetting notificationSetting;
    private final Vehicle vehicle;
    private final Diagnosis diagnosis;
    private final List<String> deviceNames;
    private MultiValuedMap<String, DiagnosedDevice> broken = new ArrayListValuedHashMap<>();

    public DiagnosedDeviceError(NotificationSetting notificationSetting, VehicleRepository vehicleRepository, Event event) {
        this.notificationSetting = notificationSetting;
        this.diagnosis = (Diagnosis) event.getTarget();
        vehicle = vehicleRepository.tryFindById(diagnosis.getVehicleId());

        String deviceList = notificationSetting.getParameter(Parameter.DEVICE_NAME);
        deviceNames = Arrays.stream(deviceList.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @Override
    public boolean needToSend() {
        for (DiagnosedDevice brokenDevice : diagnosis.getBrokenDevices()) {
            for (String deviceName : deviceNames) {
                if (brokenDevice.getName().getLocaleMap().values().contains(deviceName)) {
                    broken.put(deviceName, brokenDevice);
                }
            }
        }
        return broken.size() > 0;
    }

    @Override
    public NotificationSetting notification() {
        return notificationSetting;
    }

    @Override
    public String mailText() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Vehicle: " + vehicle.uic)
                .append("\r\nBroken devices:");
        for (Map.Entry<String, DiagnosedDevice> entry : broken.entries()) {
            stringBuilder.append("\r\nName:").append(entry.getKey()).append(", id:").append(entry.getValue().getId());
        }
        return stringBuilder.toString();
    }

    @Override
    public Multipart mailContent() {
        return null;
    }

    @Override
    public String getVehicleUic() {
        return vehicle.uic;
    }

}
