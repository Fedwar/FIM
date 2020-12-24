package fleetmanagement.backend.packages;

import fleetmanagement.backend.installations.PackageInstallation;
import fleetmanagement.backend.repositories.Persistable;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import gsp.util.WrappedException;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class Package implements Persistable<UUID> {
    public final PackageType type;
    public final UUID id;
    public final String version;
    public File path;
    public File archive;
    public final PackageSize size;
    public final Integer slot;
    public String source;
    public UUID groupId;
    public final String startOfPeriod;
    public final String endOfPeriod;
    public PackageInstallation installation;

    public Package(UUID id, PackageType type, String version, File path, PackageSize size,
                   Integer slot, String startOfPeriod, String endOfPeriod) {
        this.id = id;
        this.type = type;
        this.version = version;
        this.path = path;
        this.size = size;
        this.slot = defaultIfNull(slot, 0);
        this.startOfPeriod = startOfPeriod;
        this.endOfPeriod = endOfPeriod;

        if (type != PackageType.DataSupply && this.slot != 0)
            throw new RuntimeException(String.format("Package with type %s trying to install in slot %s." +
                    " Only Data Supply can have multiple slots", type, slot));
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public Package clone() {
        try {
            return (Package) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new WrappedException(e);
        }
    }

    public boolean isInstalledOn(Vehicle v, TaskRepository tasks) {
        return type.isInstalledOn(this, v, tasks);
    }

    public long numberOfRunningTasks(TaskRepository taskRepository) {
        return taskRepository.getNumberOfRunningTasks(id);
    }

    @Override
    public String toString() {
        return type + " " + version + " (" + id + ")";
    }

    public boolean isDuplicate(Package pkg) {
        if (pkg == null)
            return false;
        return this.type.equals(pkg.type) && this.version.equals(pkg.version) && this.slot.equals(pkg.slot);
    }

    public synchronized void startInstallation(List<Task> tasks) {
        if (installation == null) {
            installation = new PackageInstallation(UUID.randomUUID(), tasks);
            installation.setStartDatetime(new Date());
        } else {
            installation.addTasks(tasks);
        }
    }

}
