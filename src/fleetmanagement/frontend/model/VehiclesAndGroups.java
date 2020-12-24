package fleetmanagement.frontend.model;

public class VehiclesAndGroups {

    public final VehicleList vehicleList;
    public final GroupList groupList;

    public VehiclesAndGroups(VehicleList vehicleList, GroupList groupList) {
        this.vehicleList = vehicleList;
        this.groupList = groupList;
    }
}
