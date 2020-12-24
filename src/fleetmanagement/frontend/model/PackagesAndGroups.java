package fleetmanagement.frontend.model;

public class PackagesAndGroups {

    public final PackageList packages;
    public final GroupList groups;

    public PackagesAndGroups(PackageList packages, GroupList groups) {
        this.packages = packages;
        this.groups = groups;
    }
}
