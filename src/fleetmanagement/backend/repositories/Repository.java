package fleetmanagement.backend.repositories;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface Repository<T extends Persistable<K>, K> {

    void insert(T object);

    T update(K id, Consumer<T> update);

    void delete(K id);

    T tryFindById(K id);

    Stream<T> stream();

    List<T> listAll();

}
