package fleetmanagement.backend.vehiclecommunication.upload.filter;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.repositories.Persistable;
import fleetmanagement.backend.vehicles.Vehicle;
import gsp.util.WrappedException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UploadFilterSequence implements Persistable<UUID> {

    public final UUID id;
    public final FilterType type;
    public final List<UploadFilter> filters;
    public List<String> notViewedFiles;

    public UploadFilterSequence(UUID id, FilterType type, List<UploadFilter> filters, List<String> notViewedFiles) {
        this.id = id;
        this.type = type;
        this.filters = filters;
        for (UploadFilter filter : filters) {
            filter.conditions.stream().forEach(UploadFilterCondition::init);
        }
        this.notViewedFiles = notViewedFiles;
    }

    public UploadFilterSequence(FilterType type, List<UploadFilter> filters) {
        this(UUID.randomUUID(), type, filters, new ArrayList<>());
    }

    public UploadFilterSequence(FilterType type) {
        this(UUID.randomUUID(), type, new ArrayList(), new ArrayList<>());
    }

    public void addFilter(UploadFilter filter) {
        try {
            File filterDir = PathComposer.composeFromRoot(filter.dir, "vehicle", "group"
                    , Files.createTempDirectory("").toFile());
            isValidFile(filterDir);
            filter.conditions.stream().forEach(UploadFilterCondition::init);
        } catch (Exception e) {
            throw new Error(filter.dir);
        }
        filters.add(filter);
    }

    public boolean isValidFile(File destination) throws Exception {
        if (destination != null) {
            if (destination.isFile() || destination.isDirectory()) {
                return true;
            } else if (destination.mkdirs()) {
                return true;
            }
        }
        throw new Exception();
    }

    public UploadFilter getFilter(String name) {
        return filters.stream().filter(f -> (f.name.equals(name))).findFirst().orElse(null);
    }

    public UploadFilter getFilterByDirectory(String dir) {
        return filters.stream().filter(f -> (isChild(f, dir)))
                .findFirst().orElse(null);
    }

    private boolean isChild(UploadFilter filter, String child) {
        Path parentPath = new File(PathComposer.getCleanPath(filter)).toPath();
        Path childPath = new File(child).toPath();
        return childPath.startsWith(parentPath);
    }

    public UploadFilter match(Vehicle vehicle, Group group, String fileName) {
        for (UploadFilter filter : filters) {
            if (filter.matches(vehicle, group, fileName)) {
                return filter;
            }
        }
        return null;
    }


    @Override
    public UUID id() {
        return id;
    }

    @Override
    public UploadFilterSequence clone() {
        try {
            return (UploadFilterSequence) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new WrappedException(e);
        }
    }
}
