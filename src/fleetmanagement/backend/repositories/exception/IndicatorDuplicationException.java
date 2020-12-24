package fleetmanagement.backend.repositories.exception;

import java.util.UUID;

public class IndicatorDuplicationException extends RuntimeException {

    private static final long serialVersionUID = -1532999829563616163L;

    public IndicatorDuplicationException(UUID id) {
        super(String.format("Task with ID %s already exists.", id.toString()));
    }
}
