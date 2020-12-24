package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.installations.PackageInstallation;
import fleetmanagement.backend.installations.PackageInstallationRepository;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.disk.xml.PackageXmlFile;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.backend.repositories.exception.PackageTypeNotLicenced;
import fleetmanagement.backend.repositories.migration.AddMissingPackageSource;
import fleetmanagement.backend.repositories.migration.DatabaseMigrations;
import fleetmanagement.backend.repositories.migration.RenameIndis5MultimediaContent;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskCompleteEvent;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Licence;
import gsp.testutil.Sleep;
import gsp.util.WrappedException;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Component
public class OnDiskPackageRepository extends GenericOnDiskRepository<Package, UUID> implements PackageRepository,
        ApplicationListener<TaskCompleteEvent> {

    private static final String FILES_SUBDIR = "files";
    private static final Logger logger = Logger.getLogger(OnDiskPackageRepository.class);
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    @Setter
    private PackageInstallationRepository packageInstallationRepository;
    @Autowired
    private Licence licence;

    @Autowired
    public OnDiskPackageRepository(FimConfig config) {
        super(config.getPackagesDirectory());
    }

    public OnDiskPackageRepository(File directory, TaskRepository taskRepository, Licence licence) {
        super(directory);
        this.taskRepository = taskRepository;
        this.licence = licence;
    }

    @PostConstruct
    @Override
    public void loadFromDisk() {
        logger.debug("Loading from disk: packages");
        directory.mkdirs();
        DeletionHelper.performPendingDeletes(directory);
        runDatabaseMigrations();

        for (File pkgDir : directory.listFiles()) {
            try {
                loadPackageFromDirectory(pkgDir);
            } catch (Exception e) {
                logger.error("Package in " + pkgDir + " seems broken.", e);
            }
        }
    }

    @Override
    protected XmlFile<Package> getXmlFile(File dir) {
        return new PackageXmlFile(dir);
    }

    private void runDatabaseMigrations() {
        DatabaseMigrations migrations = new DatabaseMigrations();
        migrations.addMigrationStep(new RenameIndis5MultimediaContent());
        migrations.addMigrationStep(new AddMissingPackageSource());
        migrations.performMigrations(directory, "package.xml");
    }

    @Override
    public synchronized void insert(Package pkg) {
        if (!licence.isPackageTypeAvailable(pkg.type)) {
            throw new PackageTypeNotLicenced(pkg.type);
        }
        assignFilesSubDir(pkg);
        persist(pkg);
        persistables.add(pkg);
    }

    @Override
    protected void persist(Package pkg) {
        if (pkg.installation != null) {
            packageInstallationRepository.insertOrReplace(pkg.installation);
        }
        super.persist(pkg);
    }

    @Override
    public Package duplicate(Package pkg, Group group) {
        return duplicate(pkg, group == null ? null : group.id);
    }

    private Package duplicate(Package pkg, UUID groupId) {
        UUID copyUuid = UUID.randomUUID();
        Package copy = new Package(
                copyUuid,
                pkg.type,
                pkg.version,
                null,
                pkg.size,
                pkg.slot,
                pkg.startOfPeriod,
                pkg.endOfPeriod
        );
        copy.groupId = groupId;
        copy.archive = copyArchive(pkg.archive, copy);
        insert(copy);
        if (pkg.path != null) {
            try {
                FileUtils.copyDirectory(pkg.path, copy.path);
            } catch (IOException e) {
                logger.error("Can not copy " + pkg.path + " to " + copy.path);
            }
        }
        return copy;
    }

    @Override
    public List<Package> getDuplicates(Package pkg) {
        return listAll().stream().filter(p -> p.isDuplicate(pkg)).collect(toList());
    }

    @Override
    public boolean isGroupContainsPackageDuplicate(Package pkg, Group group) {
        return listByGroupId(group.id).stream().anyMatch(p -> p.isDuplicate(pkg));
    }

    private File copyArchive(File archive, Package pkg) {
        if (archive != null) {
            try {
                File target = new File(getDirectory(pkg), archive.getName());
                FileUtils.copyFile(archive, target);
                return target;
            } catch (IOException e) {
                throw new WrappedException(e);
            }
        }
        return null;
    }

    protected void assignFilesSubDir(Package pkg) {
        try {
            File pkgDir = getDirectory(pkg);
            File pkgFilesDir = new File(pkgDir, FILES_SUBDIR);
            pkgDir.mkdirs();
            if (pkg.path != null) {
                rename(pkg.path, pkgFilesDir);
            }
            pkg.path = pkgFilesDir;
            if (pkg.archive != null) {
                File target = new File(getDirectory(pkg), pkg.archive.getName());
                if (!pkg.archive.equals(target)) {
                    rename(pkg.archive, target);
                    pkg.archive = target;
                }
            }
        } catch (Exception e) {
            throw new WrappedException(e);
        }
    }

    @Override
    public synchronized List<Package> listByType(PackageType type) {
        return persistables.stream().filter(p -> p.type == type).collect(toList());
    }

    private void rename(File src, File dest) throws IOException {
        Path source = src.getAbsoluteFile().toPath();
        Path destination = dest.getAbsoluteFile().toPath();

        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Sleep.msecs(1000);
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void loadPackageFromDirectory(File pkgDir) {
        Package pkg = getXmlFile(pkgDir).load();
        if (pkg != null) {
            pkg.path = new File(pkgDir, FILES_SUBDIR);
            if (pkg.archive != null) {
                pkg.archive = new File(getDirectory(pkg), pkg.archive.getName());
            }
            if (pkg.installation != null) {
                pkg.installation = packageInstallationRepository.tryFindById(pkg.installation.id());
            }
            persistables.add(pkg);
        }
    }

    @Override
    public List<Package> listByGroupId(UUID groupId) {
        return persistables.stream().filter(p -> p.groupId != null && p.groupId.equals(groupId)).collect(toList());
    }

    @Override
    protected boolean existsInList(Package pack) {
        return persistables.stream().anyMatch(p -> p.version.equals(pack.version) && p.type.equals(pack.type));
    }

    @Override
    public void onApplicationEvent(TaskCompleteEvent taskCompleteEvent) {
        final Package pkg = tryFindById(taskCompleteEvent.getTask().getPackage().id());
        logger.debug("Task " + taskCompleteEvent.getTask().getId() + " is complete, verifying package installation process");
        synchronized (pkg) {
            if (pkg.installation != null) {
                PackageInstallation installation = pkg.installation;
                if (isInstallationComplete(installation, taskCompleteEvent.getTask())) {
                    logger.debug("Package " + pkg.type + " version " + pkg.version + " installation complete");
                    update(pkg, p -> p.installation = null);
                    installation.setEndDatetime(new Date());
                    packageInstallationRepository.insertOrReplace(installation);
                }
            }
        }
    }

    private boolean isInstallationComplete(PackageInstallation installation, Task t) {
        return installation.getTasks().stream()
                .filter(id -> !id.equals(t.getId()))
                .map(taskRepository::tryFindById)
                .filter(Objects::nonNull)
                .allMatch(Task::isCompleted);
    }
}
