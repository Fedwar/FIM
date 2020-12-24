package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.repositories.Persistable;
import fleetmanagement.backend.repositories.Repository;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Component
public abstract class SqlRepository<T extends Persistable<K>, K extends Serializable> implements Repository<T, K> {

    protected PersistenceManager persistenceManager;

    public SqlRepository(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    protected abstract Class<T> getEntityClass();

    public void insertOrReplace(T object) {
        persistenceManager.session(session -> {
            session.saveOrUpdate(object);
        });
    }

    @Override
    public void insert(T object) {
        persistenceManager.session(session -> {
            session.save(object);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public T update(K id, Consumer<T> changes) {
        return persistenceManager.session(session -> {
            T s = (T) session.get(getEntityClass(), id);
            if (s != null) {
                changes.accept(s);
                session.update(s);
            }
            return s;
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void delete(K id) {
        persistenceManager.session(session -> {
            T s = (T) session.get(getEntityClass(), id);
            if (s != null) {
                session.delete(s);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> listAll() {
        return (List<T>) persistenceManager.session(session -> {
            return session.createCriteria(getEntityClass()).list();
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public T tryFindById(K id) {
        return (T) persistenceManager.session(session -> {
            return session.get(getEntityClass(), id);
        });
    }

    @Override
    public Stream<T> stream() {
        return listAll().stream();
    }
}
