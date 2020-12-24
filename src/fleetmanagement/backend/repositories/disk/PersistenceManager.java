package fleetmanagement.backend.repositories.disk;

import org.hibernate.Session;

import java.sql.Connection;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

public interface PersistenceManager {

    void transaction(Object vehicleId, Consumer<Session> action);

    void session(Object vehicleId, Consumer<Session> action);

    void connect(Object vehicleId, Consumer<Connection> action);

    <T> T connect(Object vehicleId, Function<Connection, T> action);

    <T> T session(Object vehicleId, Function<Session, T> action);

    default void transaction(Consumer<Session> action) {
        transaction(null, action);
    }

    default void session(Consumer<Session> action) {
        session(null, action);
    }

    default void connect(Consumer<Connection> action) {
        connect(null, action);
    }

    default <T> T connect(Function<Connection, T> action) {
        return connect(null, action);
    }

    default <T> T session(Function<Session, T> action) {
        return session(null, action);
    }

}
