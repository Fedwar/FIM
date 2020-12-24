package fleetmanagement.config;

import java.io.File;
import java.util.UUID;

public class FimConfig {

    private String groupImport;
    private String filterIncoming;
    public String dataDirectory;
    public int frontendPort;
    public int[] backendPort;
    public boolean httpsByDefault;

    public FimConfig() {
    }

    public FimConfig(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public FimConfig(String groupImport, String filterIncoming, String dataDirectory, int frontendPort,
                     int[] backendPort, boolean httpsByDefault) {
        this.groupImport = groupImport;
        this.filterIncoming = filterIncoming;
        this.dataDirectory = dataDirectory;
        this.frontendPort = frontendPort;
        this.backendPort = backendPort;
        this.httpsByDefault = httpsByDefault;
    }

    public String getGroupImport() {
        return groupImport;
    }

    public String getFilterIncoming() {
        return filterIncoming;
    }

    public File getDataDirectory() {
        return new File(dataDirectory);
    }

    public File getVehicleDirectory() {
        return new File(dataDirectory, "vehicles");
    }

    public File getVehicleDirectory(UUID vehicleId) {
        return new File(getVehicleDirectory(), vehicleId.toString());
    }

    public File getPackagesDirectory() {
        return new File(dataDirectory, "packages");
    }

    public File getTasksDirectory() {
        return new File(dataDirectory, "tasks");
    }

    public File getGroupsDirectory() {
        return new File(dataDirectory, "groups");
    }

    public File getUploadFiltersDirectory() {
        return new File(dataDirectory, "uploadFilters");
    }

    public File getNotificationsDirectory() {
        return new File(dataDirectory, "notifications");
    }

    public File getConfigDirectory() {
        return new File(dataDirectory, "config");
    }

    public File getAccountsDirectory() {
        return new File(dataDirectory, "accounts");
    }

    public File getWidgetsDirectory() {
        return new File(dataDirectory, "widgets");
    }

    public File getBackendDirectory() {
        return new File(dataDirectory, "backend");
    }

    public File getSettingsDirectory() {
        return new File(dataDirectory, "settings");
    }

}
