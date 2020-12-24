package fleetmanagement.frontend.model;

import fleetmanagement.backend.vehicles.VehicleRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class GroupList implements Iterable<GroupList.Group> {

    private final List<Group> groups = new ArrayList<>();
    public final String selectedGroupId;

    public GroupList(
            List<fleetmanagement.backend.groups.Group> backendGroups,
            String selectedGroupId,
            VehicleRepository vehicleRepository) {

        for (fleetmanagement.backend.groups.Group g : backendGroups) {
            groups.add(new Group(g, vehicleRepository));
        }

        groups.sort(Comparator.comparing(g -> g.name));

        this.selectedGroupId = selectedGroupId;
    }

    public List<Group> getGroups() {
        return groups;
    }

    @Override
    public Iterator<GroupList.Group> iterator() {
        return groups.iterator();
    }

    public static class Group {
        public String key;
        public String name;
        public String dir;
        public boolean isAutoSyncEnabled;
        public boolean isEmpty;

        public Group(String key, String name, String dir, boolean isAutoSyncEnabled, boolean isEmpty) {
            this.key = key;
            this.name = name;
            this.dir = dir;
            this.isAutoSyncEnabled = isAutoSyncEnabled;
            this.isEmpty = isEmpty;
        }

        public Group(fleetmanagement.backend.groups.Group g, VehicleRepository vehicleRepository) {
            this(g.id.toString(), g.name, g.dir, g.isAutoSyncEnabled, vehicleRepository.listByGroup(g.id.toString()).isEmpty());
        }

    }
}
