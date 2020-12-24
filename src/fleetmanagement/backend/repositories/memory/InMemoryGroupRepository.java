package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.repositories.disk.OnDiskGroupRepository;

import java.io.File;

public class InMemoryGroupRepository extends OnDiskGroupRepository {

    public InMemoryGroupRepository() {
        super((File)null);
    }

    @Override
    public void loadFromDisk() {}

    @Override
    protected File getDirectory(Group persistable) {
        return null;
    }

    @Override
    protected void persist(Group group) {}
}
