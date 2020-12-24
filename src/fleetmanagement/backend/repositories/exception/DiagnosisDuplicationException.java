package fleetmanagement.backend.repositories.exception;

import java.util.UUID;

public class DiagnosisDuplicationException extends RuntimeException {

	private static final long serialVersionUID = -580779835577113465L;

	public DiagnosisDuplicationException(UUID vehicleId) {
		super(String.format("Vehicle %s already has a diagnosis saved.", vehicleId));
	}
}
