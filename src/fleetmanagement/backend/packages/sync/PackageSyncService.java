package fleetmanagement.backend.packages.sync;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.config.Licence;
import fleetmanagement.usecases.InstallPackage;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PackageSyncService {

    @Autowired
    protected TaskRepository tasks;
    @Autowired
    protected PackageTypeRepository packageTypeRepository;
    @Autowired
    protected Licence licence;
    @Autowired
    protected InstallPackage installPackage;
    private static final Logger logger = Logger.getLogger(PackageSyncService.class);

    public PackageSyncService() {
    }

    PackageSyncService(TaskRepository tasks, PackageTypeRepository packageTypeRepository, Licence licence, InstallPackage installPackage) {
        this.tasks = tasks;
        this.packageTypeRepository = packageTypeRepository;
        this.licence = licence;
        this.installPackage = installPackage;
    }

    public void syncPackages(Vehicle sender, PackageType packageType) {

        if (!sender.autoSync || !licence.isAutoPackageSyncAvailable()) return;
        if (!packageTypeRepository.isAutoSyncEnabled(packageType)) return;

        logger.debug("Auto-sync is enabled, searching for suitable package.");

        Map<Integer, Task> latestTasks = tasks.latestTasksForEachSlot(sender, packageType);
        for (Map.Entry<Integer, Task> entry : latestTasks.entrySet()) {
            Task task = entry.getValue();
            if (!task.isCompleted() || task.isCancelled())
                continue;

            Package pkg = task.getPackage();
            String senderVersion = sender.versions.getVersion(packageType, pkg.slot);
            if (!pkg.version.equals(senderVersion)) {
                logger.debug("Vehicle version: " + senderVersion + "; latest task version: " + pkg.version
                        + "; latest task id: " + task.getId());
                logger.info("Resending package " + pkg.toString() + " to " + sender.toString());
                installPackage.startInstallation(pkg, sender, null);
            }
        }
    }
}
