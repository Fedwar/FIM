package fleetmanagement.backend.packages;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.imports.fileshare.ImportInstaller;

import java.io.File;
import java.util.Set;

public class ImportTask {
    final Set<Group> groups;
    final File packageFile;
    final ImportInstaller installer;

    public ImportTask(File packageFile, Set<Group> groups, ImportInstaller installer) {
        this.groups = groups;
        this.packageFile = packageFile;
        this.installer = installer;
    }
}
