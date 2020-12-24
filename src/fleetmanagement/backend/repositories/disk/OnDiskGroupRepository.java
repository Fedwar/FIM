package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.repositories.disk.xml.GroupXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.backend.repositories.exception.GroupDuplicationException;
import fleetmanagement.config.FimConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.UUID;

@Component
public class OnDiskGroupRepository extends GenericOnDiskRepository<Group, UUID> implements GroupRepository {

    private static final Logger logger = Logger.getLogger(OnDiskGroupRepository.class);

    @Autowired
    public OnDiskGroupRepository(FimConfig config) {
        super(config.getGroupsDirectory());
    }

    public OnDiskGroupRepository(File directory) {
        super(directory);
    }

    @Override
    @PostConstruct
    public void loadFromDisk() {
        logger.debug("Loading from disk: groups");
        super.loadFromDisk();
    }

    @Override
    public synchronized void insert(Group group) {
        if (groupExists(group.name)) {
            throw new GroupDuplicationException(group.name);
        }
        if (group.id == null) {
            group.id = UUID.randomUUID();
        }
        super.insert(group);
    }

    @Override
    protected XmlFile<Group> getXmlFile(File dir) {
        return new GroupXmlFile(dir);
    }

    protected boolean groupExists(String groupName) {
        return persistables.stream().anyMatch(g -> g.name.equals(groupName));
    }

}
