package fleetmanagement.backend.repositories.exception;

public class VehicleCountExceeded extends RuntimeException {
    private static final long serialVersionUID = -1532999829563616163L;

    public VehicleCountExceeded(String message) {
        super(message);
    }
}
