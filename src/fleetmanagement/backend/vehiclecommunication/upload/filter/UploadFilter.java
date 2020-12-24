package fleetmanagement.backend.vehiclecommunication.upload.filter;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.vehicles.Vehicle;
import gsp.util.DoNotObfuscate;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class UploadFilter {

    public String name;
    public String description;
    public List<UploadFilterCondition> conditions = new ArrayList<>();
    public String dir;
    public boolean delete;
    public int deleteDays;

    public UploadFilter() {
    }

    public UploadFilter(String name, String dir, String description, String delete, String deleteDays, List<UploadFilterCondition> conditions) {
        this(name, dir, description, delete, deleteDays);
        this.conditions = conditions;
    }

    public UploadFilter(String name, String dir, String description, String delete, String deleteDays) {
        this.description = description;
        this.name = name;
        this.dir = dir;
        this.delete = delete != null && delete.equals("Enabled");
        try {
            this.deleteDays = Integer.parseInt(deleteDays);
        } catch (NumberFormatException e) {
            this.delete = false;
            this.deleteDays = 30;
        }
    }

    public List<UploadFilterCondition> getConditions() {
        return conditions;
    }

    public void addCondition(ConditionType type, String matchString) {
        conditions.add(new UploadFilterCondition(type, matchString));
    }

    public void addCondition(UploadFilterCondition condition) {
        conditions.add(condition);
    }

    public File getAbsoluteCleanPath(File root) {
        String cleanPath = PathComposer.getCleanPath(dir);
        File filterDir = new File(cleanPath);
        if (!filterDir.isAbsolute())
            filterDir = new File(root, cleanPath);
        return filterDir;
    }

    public boolean matches(Vehicle vehicle, Group group, String fileName) {
        for (UploadFilterCondition condition : conditions) {
            switch (condition.type) {
                case VEHICLE_NAME:
                    if (vehicle == null || !condition.matches(vehicle.getName())) return false;
                    break;
                case FILE_NAME:
                    if (fileName == null || !condition.matches(fileName)) return false;
                    break;
                case GROUP_NAME:
                    if (group == null || !condition.matches(group.name)) return false;
                    break;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UploadFilter that = (UploadFilter) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(conditions, that.conditions) &&
                Objects.equals(dir, that.dir);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, conditions, dir);
    }

}
