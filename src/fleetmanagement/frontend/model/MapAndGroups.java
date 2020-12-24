package fleetmanagement.frontend.model;

public class MapAndGroups {

    public final MapViewModel mapView;
    public final GroupList groups;

    public MapAndGroups(MapViewModel mapView, GroupList groups) {
        this.mapView = mapView;
        this.groups = groups;
    }
}
