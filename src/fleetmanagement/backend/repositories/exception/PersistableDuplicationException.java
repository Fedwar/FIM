package fleetmanagement.backend.repositories.exception;

import fleetmanagement.backend.repositories.Persistable;

public class PersistableDuplicationException extends RuntimeException {

    private static final long serialVersionUID = -1532999829563616163L;

    public PersistableDuplicationException(Persistable o) {
        super(String.format("Object %s with ID %s already exists.", o.getClass(), o.id().toString() ));
    }
}
