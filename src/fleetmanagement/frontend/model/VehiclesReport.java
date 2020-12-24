package fleetmanagement.frontend.model;

import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.UserSession;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VehiclesReport {
    public final List<Group> groups;
    public final Set<String> indicatorNames;

    public VehiclesReport(UserSession session, GroupRepository groups, VehicleRepository vehicleRepository, OperationDataRepository operationDataRepository) {
        this.groups = groups.stream()
                .map(g -> new Group(g, vehicleRepository))
                .filter(g -> !g.vehicles.isEmpty())
                .collect(Collectors.toList());
        this.groups.add(new Group(I18n.get(session , "group_no_group"), vehicleRepository.listAll().stream()
                .filter(vehicle -> vehicle.getGroupId() == null)
                .map(Vehicle::new)
                .collect(Collectors.toList()) ));
        indicatorNames = operationDataRepository.stream()
                .flatMap(o -> o.indicators.stream())
                .map(i -> i.id)
                .collect(Collectors.toSet());
    }

    public static class Group {
        public String id;
        public String name;
        public List<Vehicle> vehicles;

        public Group(String name, List<Vehicle> vehicles) {
            this.id = name;
            this.name = name;
            this.vehicles = vehicles;
        }

        public Group(fleetmanagement.backend.groups.Group g, VehicleRepository vehicleRepository) {
            this.name = g.name;
            this.id = g.id.toString();
            vehicles = vehicleRepository.listByGroup(g.id.toString()).stream()
                    .map(Vehicle::new)
                    .collect(Collectors.toList());

        }
    }

    public static class Vehicle {
        public String id;
        public String name;

        public Vehicle(fleetmanagement.backend.vehicles.Vehicle v) {
            this.id = v.id.toString();
            this.name = v.getName();
        }
    }
}