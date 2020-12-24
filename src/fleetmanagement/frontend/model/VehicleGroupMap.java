package fleetmanagement.frontend.model;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class VehicleGroupMap {

    private List<Vehicle> vehicles;
    private Collection<Group> groups;
    private Map<String, List<VehicleDTO>> vehiclesByGroup;

    public VehicleGroupMap(List<Vehicle> vehicles, Collection<Group> groups) {
        this.vehicles = new ArrayList<>(vehicles);
        this.groups = groups;
        makeVehiclesForGroups();
    }

    private void makeVehiclesForGroups() {
        vehiclesByGroup = new TreeMap<>();
        for (Group g : groups) {
            List<Vehicle> vl = getVehicles(g.id.toString());
            if (!vl.isEmpty()) {
                vehiclesByGroup.put(g.name, toDTO(vl));
                vehicles.removeAll(vl);
            }
        }
        //FIM-296 All the rest vehicles go to no group bucket to cover the case when vehicle.groupId points to
        // non-existing group
        if (!vehicles.isEmpty()) {
            vehiclesByGroup.put("", toDTO(vehicles));
            vehicles.clear();
        }
    }

    private List<Vehicle> getVehicles(String groupId) {
        return vehicles.stream()
                .filter(v -> Objects.equals(groupId, v.getGroupId()))
                .sorted(Comparator.comparing(Vehicle::getName))
                .collect(Collectors.toList());
    }

    private List<VehicleDTO> toDTO(List<Vehicle> vehicles) {
        return vehicles.stream()
                .map(v -> new VehicleDTO(v.id.toString(), v.getName()))
                .sorted(Comparator.comparing(VehicleDTO::getName))
                .collect(Collectors.toList());
    }

    public Map<String, List<VehicleDTO>> getVehiclesByGroup() {
        return vehiclesByGroup;
    }

    public boolean isEmpty() {
        return vehiclesByGroup.isEmpty();
    }

    public static class VehicleDTO {
        public String id;
        public String name;

        public VehicleDTO(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
