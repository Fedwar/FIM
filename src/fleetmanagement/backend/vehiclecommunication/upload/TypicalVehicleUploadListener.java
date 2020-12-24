package fleetmanagement.backend.vehiclecommunication.upload;

import fleetmanagement.backend.vehiclecommunication.FileUploadListener;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;

import java.util.UUID;

public abstract class TypicalVehicleUploadListener implements FileUploadListener {
	
	protected VehicleRepository vehicles;

	public TypicalVehicleUploadListener() {
	}

	TypicalVehicleUploadListener(VehicleRepository vehicles) {
		this.vehicles = vehicles;
	}

	public void setVehicles(VehicleRepository vehicles) {
		this.vehicles = vehicles;
	}

	@Override
	public abstract boolean canHandleUploadedFile(String filename);

	@Override
	public void onFileUploaded(UUID vehicleId, String filename, byte[] data) {
		vehicles.update(vehicleId, v -> {
			if (v == null)
				return;
			
			onFileUploaded(v, filename, data);
		});
	}
	
	public abstract void onFileUploaded(Vehicle vehicle, String filename, byte[] data);



}
