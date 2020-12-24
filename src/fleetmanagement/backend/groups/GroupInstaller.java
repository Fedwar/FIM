package fleetmanagement.backend.groups;

import fleetmanagement.backend.imports.fileshare.ImportInstaller;
import fleetmanagement.backend.packages.InstallTask;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.usecases.DeletePackage;
import fleetmanagement.usecases.InstallPackage;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class GroupInstaller implements ImportInstaller {

    private static final Logger logger = Logger.getLogger(GroupInstaller.class);

    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private PackageRepository packageRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private PackageTypeRepository packageTypeRepository;
    @Autowired
    private InstallPackage installPackage;
    @Autowired
    private DeletePackage deletePackage;

    public GroupInstaller() {
    }

    public GroupInstaller(VehicleRepository vehicleRepository, PackageRepository packageRepository,
                   TaskRepository taskRepository,
                   PackageTypeRepository packageTypeRepository, InstallPackage installPackage, DeletePackage deletePackage) {
        this.vehicleRepository = vehicleRepository;
        this.packageRepository = packageRepository;
        this.taskRepository = taskRepository;
        this.packageTypeRepository = packageTypeRepository;
        this.installPackage = installPackage;
        this.deletePackage = deletePackage;
    }

    public synchronized void assignPackageToGroup(Package original, Group group) {
        if (!packageRepository.isGroupContainsPackageDuplicate(original, group)) {
            Package copy = packageRepository.duplicate(original, group);
            installPackage(new InstallTask(copy, null, group));
        } else {
            logger.info("Package " + original.toString() +
                    " or it's duplicate has already assigned to group " + group.name);
        }
    }

    public void removePackageFromGroup(Package pkg, Group group, String triggered_by) {
        if (pkg == null)
            return;
        List<Package> duplicates = packageRepository.getDuplicates(pkg);
        if (duplicates.size() == 1) {
            Package lastPackage = duplicates.get(0);
            if (lastPackage.groupId.equals(group.id))
                lastPackage.groupId = null;
        } else {
            packageRepository.listByGroupId(group.id).stream()
                    .filter(packageWithSameGroup -> packageWithSameGroup.isDuplicate(pkg))
                    .forEach(duplicate -> deletePackage.deleteById(duplicate.id, triggered_by));
        }
    }

    public void removeAllPackagesByGroupId(UUID groupId) {
        packageRepository.listByGroupId(groupId).forEach(pkg -> {
            if (packageRepository.getDuplicates(pkg).size() > 1)
                packageRepository.delete(pkg.id);
        });
    }

    public void assignVehicles(Group group, List<Vehicle> vehiclesAssigned) {
        List<Package> groupPackages = packageRepository.listByGroupId(group.id);

        vehiclesAssigned.forEach(vehicle -> vehicleRepository.update(vehicle.id, v -> {
                if (v == null)
                    return;
                v.setGroupId(group.id.toString());
            }));
        groupPackages.forEach(p -> installPackage(p, vehiclesAssigned));
    }

    @Override
    public void installPackage(InstallTask task) {
        Package imported = task.pkg;
        Set<Group> groups = task.groups;
        for (Group group : groups) {
            logger.debug("Installing package to vehicles of the group");

            List<Vehicle> vehicles = vehicleRepository.listByGroup(group.id.toString());
            installPackage(imported, vehicles);

            collectGarbage(imported, group);
        }
    }

    private void installPackage(Package imported, List<Vehicle> vehicles) {
        InstallPackage.StartInstallationResult results = installPackage.startInstallation(imported, vehicles, null);

        for (Task t : results.conflictingTasks) {
            t.cancel();
        }
    }


    private void collectGarbage(Package imported, Group group) {
        logger.debug("Collecting garbage");

        if (imported == null || !packageTypeRepository.isGCEnabled(imported.type))
            return;

        for (Package pkg : packageRepository.listByType(imported.type)) {
            if (pkg.groupId == null || pkg.id.equals(imported.id))
                continue;

            if (!pkg.groupId.equals(group.id))
                continue;

            if (pkg.type.equals(PackageType.DataSupply) && !pkg.slot.equals(imported.slot))
                continue;

            packageRepository.delete(pkg.id);
            for (Task t : taskRepository.getTasksByPackage(pkg.id)) {
                taskRepository.delete(t.getId());

                vehicleRepository.update(t.getVehicleId(), v -> {
                    if (v != null)
                        v.removeTask(t);
                });
            }
        }
    }
}
