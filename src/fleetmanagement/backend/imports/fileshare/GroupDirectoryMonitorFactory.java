package fleetmanagement.backend.imports.fileshare;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.PackageImportService;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class GroupDirectoryMonitorFactory {

    public static final int MONITOR_INTERVAL = 10;

    public static FileAlterationMonitor startMonitor(String groupDir, File monitorDir
            , ImportInstaller groupInstaller, PackageImportService packageImporter
            , GroupRepository groupRepository) throws Exception {

        FileAlterationObserver observer = new FileAlterationObserver(monitorDir);

        observer.addListener(new FileAlterationListenerAdaptor() {

            private void importPackage(File file) {
                Set<Group> groups = groupRepository.stream().filter(group -> group.dir.equals(groupDir))
                        .collect(Collectors.toSet());
                packageImporter.importPackage(file, groups, groupInstaller);
            }

            @Override
            public void onFileCreate(File file) {
                importPackage(file);
            }

            @Override
            public void onFileChange(File file) {
                importPackage(file);
            }

        });

        FileAlterationMonitor monitor = new FileAlterationMonitor(MONITOR_INTERVAL * 1000, observer);
        monitor.start();

        return monitor;
    }

};