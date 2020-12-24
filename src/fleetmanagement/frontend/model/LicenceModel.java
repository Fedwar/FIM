package fleetmanagement.frontend.model;

import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.config.Licenced;

public class LicenceModel extends Admin {
    public final int currentNumberOfVehicles;

    public LicenceModel(Licence licence, VehicleRepository vehicleRepository) {
        super(licence);
        currentNumberOfVehicles = vehicleRepository.listAll().size();
    }

}
