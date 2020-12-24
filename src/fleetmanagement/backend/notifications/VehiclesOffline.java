package fleetmanagement.backend.notifications;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.vehicles.Vehicle;

import javax.mail.Multipart;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static fleetmanagement.backend.notifications.settings.Parameter.*;

public class VehiclesOffline implements Notification {

    private final NotificationSetting notificationSetting;
    private final List<Vehicle> offlineVehicles;
    private final List<Vehicle> otherOfflineVehicles;
    private final Integer timeout;
    private final String groupName;
    private final String noFilter;
    private final String vehicleName;
    private final Integer repeatDelay;

    public VehiclesOffline(NotificationSetting notificationSetting, List<Vehicle> allVehicles, List<Group> groups) {
        this.notificationSetting = notificationSetting;
        vehicleName = notificationSetting.getParameter(VEHICLE_NAME);
        groupName = notificationSetting.getParameter(GROUP_NAME);
        noFilter = notificationSetting.getParameter(ALL_VEHICLES);
        String parameter = notificationSetting.getParameter(Parameter.VEHICLE_OFFLINE_TIMEOUT);
        timeout = parameter.isEmpty() ? 0 :Integer.valueOf(parameter);
        parameter = notificationSetting.getParameter(REPEAT_DELAY);
        repeatDelay = parameter.isEmpty() ? 0 :Integer.valueOf(parameter);

        if (timeout == 0) {
            offlineVehicles = Collections.EMPTY_LIST;
            otherOfflineVehicles = Collections.EMPTY_LIST;
        } else {
            ZonedDateTime now = ZonedDateTime.now();
            List<Vehicle> allOfflineVehicles = allVehicles.stream()
                    .filter(vehicle -> vehicle.lastSeen.plusMinutes(timeout).isBefore(now))
                    .collect(Collectors.toList());
            if (!noFilter.isEmpty()) {
                offlineVehicles = allOfflineVehicles.stream().collect(Collectors.toList());
            } else if (!vehicleName.isEmpty()) {
                offlineVehicles = allOfflineVehicles.stream()
                        .filter(vehicle -> vehicle.uic.equals(vehicleName))
                        .collect(Collectors.toList());
            } else if (!groupName.isEmpty()) {
                Group group = groups.stream()
                        .filter(g -> g.name.equals(groupName))
                        .findFirst().orElse(null);
                if (group == null) {
                    offlineVehicles = Collections.EMPTY_LIST;
                } else {
                    offlineVehicles = allOfflineVehicles.stream()
                            .filter(vehicle -> vehicle.getGroupId() != null && vehicle.getGroupId().equals(group.id.toString()))
                            .collect(Collectors.toList());
                }
            } else {
                offlineVehicles = allOfflineVehicles.stream().collect(Collectors.toList());
            }

            allOfflineVehicles.removeAll(offlineVehicles);
            otherOfflineVehicles = allOfflineVehicles;
        }
    }

    public Integer getTimeout() {
        return timeout;
    }

    public List<Vehicle> getOfflineVehicles() {
        return offlineVehicles;
    }

    public List<Vehicle> getOtherOfflineVehicles() {
        return otherOfflineVehicles;
    }

    @Override
    public boolean needToSend() {
        return !offlineVehicles.isEmpty();
    }

    @Override
    public Multipart mailContent() {
        return null;
    }

    @Override
    public NotificationSetting notification() {
        return notificationSetting;
    }

    @Override
    public String mailText() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Offline vehicles:\r\n");

        for (Vehicle offlineVehicle : offlineVehicles) {
            stringBuilder.append(offlineVehicle.uic);
//            if (offlineVehicle.additional_uic != null && offlineVehicle.additional_uic.isEmpty()) {
//                stringBuilder.append(" (").append(offlineVehicle.additional_uic).append(")");
//            }
            stringBuilder.append("\r\n");
        }
        if (!otherOfflineVehicles.isEmpty()) {
            stringBuilder.append("\r\n");
            stringBuilder.append("Other vehicles offline:\r\n");
            for (Vehicle offlineVehicle : otherOfflineVehicles) {
                stringBuilder.append(offlineVehicle.uic).append("\r\n");
            }
        }

        return stringBuilder.toString();
    }

    @Override
    public String getVehicleUic() {
        return "";
    }

}
