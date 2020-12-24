package fleetmanagement.frontend.model;

import fleetmanagement.FleetManagement;

public class About {
    public final String softwareVersion;

    public About() {
        this.softwareVersion = FleetManagement.getVersion();
    }
}
