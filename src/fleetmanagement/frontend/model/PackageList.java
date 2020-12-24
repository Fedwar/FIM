package fleetmanagement.frontend.model;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.frontend.UserSession;
import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class PackageList implements Iterable<PackageList.Category> {

    public final List<Category> categories = new ArrayList<>();
    private static final Logger logger = Logger.getLogger(PackageList.class);


    public static class Category {
        public String name;
        public String icon;
        public final List<Entry> packages = new ArrayList<>();
    }

    public static class Entry {

        private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        public final String key;
        public String version;
        public String name;
        public Boolean installationInProgress;
        public Integer installedCount;
        public Integer vehicleCount;
        public String validity;
        public String groupName;
        public String startOfPeriod;
        public String endOfPeriod;
        public Integer slot;
        public Integer validityStatus;
        public boolean downloadAvailable;

        public Entry(Package p, GroupRepository groupRepository, UserSession request) {
            this.key = p.id.toString();
            this.version = p.version;
            this.name = Name.of(p, request);
            this.validity = "";
            this.downloadAvailable = p.archive != null;
            Group group = groupRepository.tryFindById(p.groupId);
            this.groupName = (group == null ? null : group.name);
            this.startOfPeriod = p.startOfPeriod;
            this.endOfPeriod = p.endOfPeriod;
            this.slot = p.slot;
            if (p.type == PackageType.DataSupply && slot > 0)
                if (this.startOfPeriod != null && this.endOfPeriod != null) {
                    Date today = new Date();
                    try {
                        Date dateFrom = simpleDateFormat.parse(p.startOfPeriod);
                        Date dateTo = simpleDateFormat.parse(p.endOfPeriod);
                        if (today.after(dateFrom) && today.before(dateTo)) {
                            this.validityStatus = 1;
                        } else {
                            this.validityStatus = 2;
                        }
                    } catch (ParseException | NumberFormatException e) {
                        logger.warn("Can't parse validity period! " + p.startOfPeriod + " - " + p.endOfPeriod);
                        this.validityStatus = 0;
                    }
                } else {
                    this.validityStatus = 0;
                }
        }
    }

    @Override
    public Iterator<Category> iterator() {
        return categories.iterator();
    }

    public int size() {
        return categories.size();
    }
}
