package fleetmanagement.backend.repositories.exception;

public class VehicleDuplicationException extends RuntimeException {

	private static final long serialVersionUID = 2132361555531690735L;

	public VehicleDuplicationException(String vehicleUic) {
		super(String.format("The vehicle with uic %s already exists.", vehicleUic));
	}
}
