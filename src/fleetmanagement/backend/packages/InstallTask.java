package fleetmanagement.backend.packages;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.packages.Package;

import java.io.File;
import java.util.Collections;
import java.util.Set;

public class InstallTask {
    public final Set<Group> groups;
    public final File packageFile;
    public final Package pkg;

    public InstallTask(Package pkg, File packageFile, Set<Group> groups) {
        this.groups = groups;
        this.packageFile = packageFile;
        this.pkg = pkg;
    }
    public InstallTask(Package pkg, File packageFile, Group group) {
        this(pkg, packageFile, Collections.singleton(group));
    }
}
