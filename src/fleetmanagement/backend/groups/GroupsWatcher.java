package fleetmanagement.backend.groups;

import fleetmanagement.backend.imports.fileshare.GroupDirectoryMonitorFactory;
import fleetmanagement.backend.imports.fileshare.ImportInstaller;
import fleetmanagement.backend.packages.PackageImportService;
import fleetmanagement.config.Settings;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class GroupsWatcher {

    private static final Logger logger = Logger.getLogger(GroupsWatcher.class);
    private final Map<File, FileAlterationMonitor> monitors = new ConcurrentHashMap();
    @Autowired
    private PackageImportService packageImporter;
    @Autowired
    private ImportInstaller groupInstaller;
    @Autowired
    private GroupRepository groups;
    @Autowired
    private Settings settings;
    private File rootDirectory;

    public GroupsWatcher() {
    }

    public GroupsWatcher(PackageImportService packageImporter, ImportInstaller groupInstaller, GroupRepository groups) {
        this.packageImporter = packageImporter;
        this.groupInstaller = groupInstaller;
        this.groups = groups;
    }

    public File getGroupDir(Group group) {
        String groupDir = group.dir;
        if (groupDir == null || groupDir.isEmpty())
            return null;
        return new File(rootDirectory, groupDir);
    }

    public boolean verifyDir(File groupDir, Group group) {
        logger.debug("Verifying group directory: " + groupDir.getAbsolutePath());
        if (!groupDir.exists()) {
            if (groupDir.mkdirs()) {
                logger.info("Successfully created group directory. Group: " + group.name + " Directory: " + groupDir.getAbsolutePath());
            } else {
                logger.error("Can't create group directory! Group: " + group.name + " Directory: " + groupDir.getAbsolutePath());
                return false;
            }
        } else {
            if (groupDir.isDirectory()) {
                logger.info("Group directory already exists and can be used. Group: " + group.name + " Directory: " + groupDir.getAbsolutePath());
            } else {
                logger.error("Can't create group directory because there is a file with the same name! Group: " + group.name + " Directory: " + groupDir.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    public synchronized List<File> watchDir(Group group) {
        File groupDir = getGroupDir(group);
        if (groupDir == null) {
            logger.debug("No directory defined in group \"" + group.name + "\"");
            return null;
        }

        if (!verifyDir(groupDir, group))
            return null;

        FileAlterationMonitor monitor = monitors.get(groupDir);

        if (monitor == null) {
            logger.info("Registering directory: " + groupDir.getAbsolutePath());
            try {
                monitor = GroupDirectoryMonitorFactory
                        .startMonitor(group.dir, groupDir, groupInstaller, packageImporter, groups);
                monitors.put(groupDir, monitor);
            } catch (Exception e) {
                logger.warn("Failed to register the directory: " + groupDir.getAbsolutePath());
                logger.debug(e);
            }
        }

        List<File> result = installExistingPackages(group);

        return result;
    }

    public List<File> installExistingPackages(Group group) {
        logger.debug("Installing existing packages");
        List<File> result = new LinkedList<>();
        File groupDir = getGroupDir(group);
        File[] directoryListing = groupDir.listFiles();

        Set<Group> groupSet = groups.stream().filter(g -> g.dir.equals(groupDir))
                .collect(Collectors.toSet());

        if (directoryListing != null) {
            for (File child : directoryListing) {
                packageImporter.importPackage(child, groupSet, groupInstaller);
            }
        } else {
            logger.warn("Can't find group directory although it was verified! " + groupDir);
        }
        return result;
    }

    public synchronized boolean stopWatching(Group group) {
        logger.debug("Stop watching for directory of group " + group.name);
        Set<Group> groupSet = groups.stream().filter(g -> g.dir.equals(group.dir))
                .collect(Collectors.toSet());

        if (groupSet.size() <= 1) {
            File groupDir = getGroupDir(group);
            FileAlterationMonitor monitor = monitors.remove(groupDir);
            if (monitor != null)
                stopMonitor(monitor);
        }
        return true;
    }

    private void stopMonitor(FileAlterationMonitor monitor) {
        try {
            monitor.stop(1); //milliseconds
        } catch (Exception e) {
            logger.error("Can't stop group directory monitor");
            logger.debug(e);
        }
    }

    public synchronized void start() {
        initRootDirectory();
        startListening();
    }

    public synchronized void restart() {
        initRootDirectory();
        shutDownListener();
        startListening();
    }

    private synchronized void startListening() {
        List<File> filesToDelete = new LinkedList<>();

        for (Group group : groups.listAll()) {
            List<File> groupFiles = watchDir(group);
            if (groupFiles == null)
                logger.error("Can't watch for directory (" + group.dir + ") of group \"" + group.name);
            else
                filesToDelete.addAll(groupFiles);
        }

        for (File f : filesToDelete) {
            if (!f.delete()) {
                try {
                    logger.warn("Can't delete package file! " + f.getCanonicalPath());
                } catch (IOException e) {
                    logger.warn("Can't print canonical path of the package file. Can't delete the package file!");
                }
            }
        }
    }

    public void initRootDirectory() {
        String groupImportDir = settings.getImportFolderPath();
        rootDirectory = new File(groupImportDir);
    }

    public void shutDownListener() {
        logger.info("Stopping watching service");
        Collection<FileAlterationMonitor> values = monitors.values();
        monitors.clear();
        for (FileAlterationMonitor fileAlterationMonitor : values) {
            stopMonitor(fileAlterationMonitor);
        }
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }
}
