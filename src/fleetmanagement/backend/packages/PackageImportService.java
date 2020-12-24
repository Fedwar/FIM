package fleetmanagement.backend.packages;

import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.imports.fileshare.ImportInstaller;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.repositories.exception.PackageImportException;
import fleetmanagement.usecases.ImportPackage;
import gsp.testutil.Sleep;
import gsp.util.WrappedException;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class PackageImportService {
    private static final Logger logger = Logger.getLogger(PackageImportService.class);
    public static final String FIM_S_AUTO = "FIM-S AUTO";
    private Deque<ImportTask> queue = new ConcurrentLinkedDeque<>();
    private volatile Thread processingThread;
    @Autowired
    private PackageRepository packageRepository;
    @Autowired
    private ImportPackage importer;
    @Autowired
    private NotificationService notificationService;

    public PackageImportService() {
    }

    public PackageImportService(PackageRepository packageRepository, ImportPackage importer,
                                NotificationService notificationService) {
        this.importer = importer;
        this.packageRepository = packageRepository;
        this.notificationService = notificationService;
    }

    public void importPackage(File packageFile, Set<Group> groups, ImportInstaller installer) {
        queue.add(new ImportTask(packageFile, groups, installer));
        processQueue();
    }

    public void importPackage(File packageFile, ImportInstaller installer) {
        importPackage(packageFile, null, installer);
    }

    private void processQueue() {
        if (processingThread == null || !processingThread.isAlive()) {
            processingThread = new Thread(() -> {
                while (queue.peek() != null) {
                    ImportTask item = queue.poll();
                    try {
                        ArrayList<InstallTask> installTasks = process(item);
                        installImported(installTasks, item);
                        deletePackageFile(item);
                    } catch (ImportPackage.FileNotFound fileNotFound) {
                        logger.warn(fileNotFound.getMessage());
                    } catch (ImportPackage.UnknownGroup unknownGroup) {
                        logger.warn(unknownGroup.getMessage());
                    } catch (ImportPackage.FileIsBlocked fileIsBlocked) {
                        logger.warn(fileIsBlocked.getMessage());
                        queue.add(item);
                    }
                }
                processingThread = null;
            });
            processingThread.start();
        }
    }

    void installImported(ArrayList<InstallTask> installTasks, ImportTask importTask) {
        if (installTasks == null || installTasks.isEmpty())
            return;
        ImportInstaller installer = importTask.installer;
        for (InstallTask installTask : installTasks) {
            installer.installPackage(installTask);
        }
    }

    private InputStream getFileStream(Path file) {
        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(file, StandardOpenOption.READ);
            logger.debug("The file has been opened successfully");
            return inputStream;
        } catch (IOException | OverlappingFileLockException e) {
            logger.warn("The file is still blocked..." + e.toString());
            return null;
        }
    }

    private ArrayList<InstallTask> process(ImportTask importItem) throws ImportPackage.FileNotFound, ImportPackage.FileIsBlocked, ImportPackage.UnknownGroup {
        File packageFile = importItem.packageFile;
        Group group;

        if (importItem.groups == null || importItem.groups.isEmpty()) {
            throw new ImportPackage.UnknownGroup(packageFile);
        }
        group = importItem.groups.iterator().next();
        logger.debug("Processing event for group " + group.name);
        ActivityLog.groupFileMessage(ActivityLog.Operations.NEW_FILE_FOUND, group, packageFile.toString(), FIM_S_AUTO);
        logger.debug("Full name: " + packageFile.getAbsolutePath());


        if (!packageFile.exists())
            throw new ImportPackage.FileNotFound(packageFile);


        InputStream inputStream = getFileStream(packageFile.toPath());
        if (inputStream == null) {
            logger.debug("Waiting until the package file is copied");
            try {
                Sleep.msecs(1000);
            } catch (WrappedException e) {
                ActivityLog.groupFileMessage(ActivityLog.Operations.CANNOT_LOAD_FILE, group, packageFile.toString(), FIM_S_AUTO);
                logger.warn("Watch service thread interrupted while sleeping! Cancelling package import!");
                return null;
            }
            inputStream = getFileStream(packageFile.toPath());
        }

        if (inputStream == null) {
            throw new ImportPackage.FileIsBlocked(packageFile);
        }

        return importPackage(packageFile, inputStream, importItem.groups);
    }

    private void deletePackageFile(ImportTask item) {
        if (item.packageFile.delete()) {
            logger.debug("Package file deleted from the group directory");
        } else {
            logger.error("Can't delete the file from the group directory! " + item.packageFile.toString());
        }
    }

    private ArrayList<InstallTask> importPackage(File packageFile, InputStream inputStream, Set<Group> groups ) {
        ArrayList<InstallTask> packages = new ArrayList<>();
        Iterator<Group> iterator = groups.iterator();
        Group firstGroup = iterator.next();
        Package imported = importPackage(packageFile.getName(), inputStream, firstGroup, "Group: " + firstGroup.name);
        if (imported != null) {
            packages.add(new InstallTask(imported, packageFile, firstGroup));
            while (iterator.hasNext()) {
                Group group = iterator.next();
                Package duplicate = packageRepository.duplicate(imported, group);
                packages.add(new InstallTask(duplicate, packageFile, group));
            }
        }
        return packages;
    }

    private Package importPackage(String filename, InputStream inputStream, Group group, String source) {
        logger.debug("Importing package");
        Package imported = null;
        ZonedDateTime importStart = ZonedDateTime.now();
        try {
            imported = importer.importPackage(filename, inputStream, source, group, FIM_S_AUTO);
            logger.info("Imported package of type " + imported.type);
            ActivityLog.packageMessage(ActivityLog.Operations.PACKAGE_IMPORTED, group, filename, imported, FIM_S_AUTO);
        } catch (Exception e) {
            PackageImportException packageImportException = e instanceof PackageImportException
                    ? (PackageImportException) e : new PackageImportException(null, e, e.getMessage());
            PackageType packageType = packageImportException.getPackageType();
            logger.warn("Automatic file import failed for package of type " + (packageType == null ? "unknown" : packageType)
                    + ", imported file name " + filename + " : " + e.getMessage());
            logger.debug("Automatic file import failed due to: ", e);
            notificationService.processEvent(
                    Events.packageImportError(importStart, filename, packageImportException, group.name));
            ActivityLog.groupFileMessage(ActivityLog.Operations.CANNOT_IMPORT_PACKAGE, group, filename, FIM_S_AUTO);
        }
        return imported;
    }

    public void setPackageRepository(PackageRepository packageRepository) {
        this.packageRepository = packageRepository;
    }

    public void setImporter(ImportPackage importer) {
        this.importer = importer;
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
}
