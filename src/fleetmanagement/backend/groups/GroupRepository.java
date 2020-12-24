package fleetmanagement.backend.groups;

import fleetmanagement.backend.repositories.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public interface GroupRepository extends Repository<Group, UUID> {

    default Map<String, Group> mapAll() {
        return stream().collect(Collectors.toMap(g -> g.id.toString(), g -> g));
    }

    default Optional<Group> findByName(String groupName) {
        return stream().filter(group -> group.name.equals(groupName)).findFirst();
    }
}
