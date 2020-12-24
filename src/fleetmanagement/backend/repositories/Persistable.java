package fleetmanagement.backend.repositories;

public interface Persistable<T> extends Cloneable {

    T id();

    Object clone();

}
