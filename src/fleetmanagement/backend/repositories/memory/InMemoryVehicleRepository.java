package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.repositories.disk.DeletionHelper;
import fleetmanagement.backend.repositories.disk.OnDiskVehicleRepository;
import fleetmanagement.backend.repositories.disk.xml.VehicleXmlFile;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.ConnectionStatusRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.config.Licence;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class InMemoryVehicleRepository extends OnDiskVehicleRepository {

	public InMemoryVehicleRepository(Licence licence, ConnectionStatusRepository connectionStatusRepository, TaskRepository tasks) {
		super(null, licence, connectionStatusRepository, tasks, null);
	}

	@Override
	public void loadFromDisk() {}

	@Override
	protected void persist(Vehicle v) {	}

	//TODO: remove this method and fix all tests
	@Override
	protected Vehicle makeClone (Vehicle vehicle) {
		return vehicle;
	}

	@Override
	public void delete(UUID id) {
		Vehicle toRemove = tryFindById(id);
		if (toRemove != null) {
			vehicles.remove(toRemove);
		}
	}

}