package fleetmanagement.backend.packages;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.vehicles.Vehicle;
import org.apache.log4j.Logger;

public class ActivityLog {
    private static final Logger journal = Logger.getLogger("dataTransferLog");
    private static final Logger logger = Logger.getLogger(ActivityLog.class);

    public static void userMessage(Operations op, String triggered_by) {
        log(new ActivityLogEntry(op, triggered_by));
    }

    public static void groupFileMessage(Operations op, Group group, String fileName, String triggered_by) {
        log(new ActivityLogEntry(op, group, fileName, triggered_by));
    }

    public static void packageMessage(Operations op, Group group, String fileName, Package pkg, String triggered_by) {
        log(new ActivityLogEntry(op, group, fileName, pkg, triggered_by));
    }

    public static void vehicleMessage(Operations op, Package pkg, Vehicle vehicle, String triggered_by) {
        log(new ActivityLogEntry(op, pkg, vehicle, triggered_by));
    }

    public static void vehicleFilteredInfoReceived(Operations op, Vehicle vehicle, Group group, String fileName, String savePath) {
        log(new ActivityLogEntry(op, vehicle, group, fileName, savePath));
        logger.debug("Vehicle filtered file even logged. Operation is " + op.name() + ". File name is " + fileName + ". Directory is " + savePath);
    }

    public enum Operations {
        NEW_FILE_FOUND,
        OBSOLETE_FILE_DELETED,
        CANNOT_LOAD_FILE,
        PACKAGE_IMPORTED,
        PACKAGE_DELETED,
        USER_LOGIN,
        CANNOT_IMPORT_PACKAGE,
        AD_INFO_RECEIVED,
        INSTALLATION_STARTED,
        INSTALLATION_FINISHED,
        INSTALLATION_FAILED,
        INSTALLATION_CANCELLED,
        NO_LICENCE
    }

    private static void log(ActivityLogEntry entry) {
        String msg =
                entry.operationName +
                "," +
                entry.triggered_by +
                "," +
                entry.fileName +
                "," +
                entry.packageType +
                "," +
                entry.packageName +
                "," +
                entry.groupName +
                "," +
                entry.groupDir +
                "," +
                entry.vehicleName;
        journal.info(msg);
    }

    private static class ActivityLogEntry {
        private final String operationName;
        private String fileName = "";
        private String packageType = "";
        private String packageName = "";
        private String groupName = "";
        private String groupDir = "";
        private String vehicleName = "";
        private String triggered_by = "";

        ActivityLogEntry(Operations op, String triggered_by) {
            this.operationName = op.toString();
            if (triggered_by != null && !triggered_by.isEmpty())
                this.triggered_by = "TRIGGER: " + triggered_by;
        }

        ActivityLogEntry(Operations op, Group group, String fileName, String triggered_by) {
            this.operationName = op.toString();
            this.fileName = fileName;
            if (triggered_by != null && !triggered_by.isEmpty())
                this.triggered_by = "TRIGGER: " + triggered_by;
            if (group != null) {
                this.groupName = group.name;
                this.groupDir = group.dir;
            };
        }

        ActivityLogEntry(Operations op, Vehicle vehicle, Group group, String fileName, String savePath) {
            this.operationName = op.toString();
            this.fileName = fileName;
            if (group != null) {
                this.groupName = group.name;
            };
            this.groupDir = savePath;
            if (vehicle != null){
                this.vehicleName = vehicle.toString();
            }
        }

        ActivityLogEntry(Operations op, Group group, String fileName, Package pkg, String triggered_by) {
            this(op, group, fileName, triggered_by);
            if (pkg != null) {
                this.packageType = pkg.type.getTaskType();
                this.packageName = pkg.toString();
            }
        }

        ActivityLogEntry(Operations op, Package pkg, Vehicle vehicle, String triggered_by) {
            this.operationName = op.toString();
            if (pkg != null){
                this.packageType = pkg.type.getTaskType();
                this.packageName = pkg.toString();
            }
            if (vehicle != null){
                this.vehicleName = vehicle.toString();
            }
            if (triggered_by != null && !triggered_by.isEmpty())
                this.triggered_by = "TRIGGER: " + triggered_by;
        }
    }
}
