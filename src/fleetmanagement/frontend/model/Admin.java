package fleetmanagement.frontend.model;

import fleetmanagement.config.Licence;
import fleetmanagement.config.Licenced;

public class Admin implements Licenced {
    public final Licence licence;

    public Admin(Licence licence) {
        this.licence = licence;
    }

    @Override
    public Licence getLicence() {
        return licence;
    }
}
