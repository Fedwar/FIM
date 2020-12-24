package fleetmanagement.frontend.model;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.DiagnosticSummary.DiagnosticSummaryType;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.transformers.DurationFormatter;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class VehicleList implements Iterable<VehicleList.Vehicle> {

    private final List<Vehicle> vehicles;
    public VehicleGroupMap vehicleGroupMap;
    public final boolean showMapLink;
    public final boolean showReports;
    public final String selectedGroupId;

    public VehicleList(List<fleetmanagement.backend.vehicles.Vehicle> backendVehicles, Map<String, Group> backendGroups,
                       TaskRepository tasks, UserSession request, Licence licence, String selectedGroupId) {
        this.selectedGroupId = selectedGroupId;
        List<Vehicle> vehicleList = new ArrayList<>();
        ZonedDateTime now = ZonedDateTime.now();
        boolean anyPositionFound = false;
        for (fleetmanagement.backend.vehicles.Vehicle v : backendVehicles) {
            Vehicle vehicle = new Vehicle();
            vehicle.key = v.id.toString();
            vehicle.name = v.getName();
            vehicle.uic = v.uic;
            vehicle.additionalUic = v.additional_uic;
            vehicle.groupId = v.getGroupId();
            Group group = backendGroups.get(v.getGroupId());
            vehicle.groupName = group == null ? "" : group.name;
            vehicle.timeOfLastCommunication = DurationFormatter.asHumanReadable(Duration.between(now, v.lastSeen), request.getLocale());
            vehicle.connectionStatusCssClass = getConnectionStatusCssFor(v);

            Set<String> dvVersions = v.versions.getAllVersionsByType(PackageType.DataSupply);

            vehicle.dataSupplyVersions = dvVersions.isEmpty() ? "" : StringUtils.join(dvVersions, " & ");
            vehicle.runningTaskName = "";
            vehicle.showDiagnosticErrorHint = v.getDiagnosticSummary(now).type == DiagnosticSummaryType.DeviceErrors;

            Task nextTask = v.getNextTask(tasks);
            if (nextTask != null) {
                vehicle.runningTaskName = Name.of(nextTask.getPackage(), request);
                vehicle.runningTaskProgress = nextTask.getStatus().percent;
                vehicle.runningTaskPackageId = nextTask.getPackage().id.toString();
            }

            vehicleList.add(vehicle);

            if (v.liveInformation != null && v.liveInformation.position != null)
                anyPositionFound = true;
        }

        vehicleList.sort(Comparator.comparing(v -> v.name));

        showMapLink = anyPositionFound && licence.isMapAvailable();
        showReports = licence.isReportsAvailable();
        vehicles = Collections.unmodifiableList(vehicleList);
        vehicleGroupMap = new VehicleGroupMap(backendVehicles, backendGroups.values());
    }

    public List<VehicleList.Vehicle> getVehicles() {
        return selectedGroupId == null ? vehicles : getVehicles(selectedGroupId);
    }

    public VehicleGroupMap getVehicleGroupMap() {
        return vehicleGroupMap;
    }

    private List<Vehicle> getVehicles(String groupId) {
        return vehicles.stream()
                .filter(v -> Objects.equals(groupId, v.groupId))
                .sorted(Comparator.comparing(v -> v.name))
                .collect(Collectors.toList());
    }

    private String getConnectionStatusCssFor(fleetmanagement.backend.vehicles.Vehicle v) {
        if (isConnectionLost(v))
            return "connection-lost";
        if (isConnectionUnstable(v))
            return "connection-unstable";
        return "connection-ok";
    }

    public int size() {
        return vehicles.size();
    }

    @Override
    public Iterator<Vehicle> iterator() {
        return vehicles.iterator();
    }

    private boolean isConnectionUnstable(fleetmanagement.backend.vehicles.Vehicle v) {
        return v.lastSeen.isBefore(ZonedDateTime.now().minusMinutes(10));
    }

    private boolean isConnectionLost(fleetmanagement.backend.vehicles.Vehicle v) {
        return v.lastSeen.isBefore(ZonedDateTime.now().minusHours(24));
    }

    public static class Vehicle {
        public boolean showDiagnosticErrorHint;
        public String connectionStatusCssClass;
        public String statusIconTitle;

        public String key;
        public String name;
        public String uic;
        public String additionalUic;
        public String timeOfLastCommunication;
        public String dataSupplyVersions;
        public String runningTaskName;
        public int runningTaskProgress;
        public String groupId;
        public String groupName;
        public String runningTaskPackageId;
    }

}
