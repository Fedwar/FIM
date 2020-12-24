package fleetmanagement.backend.vehiclecommunication.upload.filter;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.vehicles.Vehicle;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class PathComposer {

    public static final String VEHICLE = "<vehicle>";
    public static final String GROUP = "<group>";
    public static final String DATE = "<date>";
    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    public static String compose(UploadFilter filter, String vehicleName, String groupName) {
        return compose(filter.dir, vehicleName, groupName);
    }

    public static String compose(String filterDir, Vehicle vehicle, Group group) {
        return compose(filterDir, vehicle.getName()
                , (group == null ? "withoutGroup" : group.name));
    }

    public static String compose(String filterDir, String vehicleName, String groupName) {
        String template = filterDir;
        if (template.startsWith("./"))
            template = template.substring(2);
        else if (template.startsWith(".\\"))
            template = template.substring(2);
        template = template.replace(VEHICLE, vehicleName);
        template = template.replace(GROUP, groupName);
        return template.replace(DATE, ZonedDateTime.now().format(dateFormat));
    }

    public static File composeFromRoot(String filterPath, Vehicle vehicle, Group group, File root) {
        String composed = PathComposer.compose(filterPath, vehicle, group);
        return resolveRoot(composed, root);
    }

    public static File composeFromRoot(String filterPath, String vehicle, String group, File root) {
        String composed = PathComposer.compose(filterPath, vehicle, group);
        return resolveRoot(composed, root);
    }

    public static File resolveRoot(String composedPath, File root) {
        File filterDir = new File(composedPath);
        if (!filterDir.isAbsolute())
            filterDir = new File(root, composedPath);
        return filterDir;
    }

    public static String getCleanPath(UploadFilter filter) {
        return getCleanPath(filter.dir);
    }

    public static String getCleanPath(String filterDir) {
        List<String> tags = Arrays.asList(VEHICLE, GROUP, DATE);
        String cleanDir = filterDir;
        for (String tag : tags) {
            int index = cleanDir.indexOf(tag);
            if (index >= 0) {
                cleanDir = cleanDir.substring(0, index);
            }
        }
        return cleanDir;
    }

}
