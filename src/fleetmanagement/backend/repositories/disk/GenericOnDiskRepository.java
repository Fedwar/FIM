package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.repositories.Persistable;
import fleetmanagement.backend.repositories.Repository;
import fleetmanagement.backend.repositories.disk.xml.XmlFile;
import fleetmanagement.backend.repositories.exception.PersistableDuplicationException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Stream;

public abstract class GenericOnDiskRepository<T extends Persistable<K>, K> implements Repository<T, K> {

    protected final List<T> persistables = new CopyOnWriteArrayList<>();
    protected File directory;

    public GenericOnDiskRepository(File directory) {
        this.directory = directory;
    }

    public void loadFromDisk() {
        directory.mkdirs();
        DeletionHelper.performPendingDeletes(directory);

        for (File dir : directory.listFiles()) {
            T persistable = getXmlFile(dir).load();
            if (persistable != null)
                persistables.add(persistable);
        }
    }

    protected abstract XmlFile<T> getXmlFile(File dir);

    public void insert(T object) {
        if (!existsInList(object)) {
            persist(object);
            persistables.add(object);
        } else
            throw new PersistableDuplicationException(object);
    }

    public T update(K id, Consumer<T> changes) {
        T object = tryFindById(id);
        return update(object, changes);
    }

    protected T update(T object, Consumer<T> changes) {
        if (object != null) {
            T cloned = (T) object.clone();
            changes.accept(cloned);
            persistables.set(persistables.indexOf(object), cloned);
            persist(cloned);
            return cloned;
        } else {
            changes.accept(null);
        }
        return object;
    }

    protected File getDirectory(T persistable) {
        return new File(directory, persistable.id().toString());
    }

    protected void persist(T object) {
        getXmlFile(getDirectory(object)).save(object);
    }

    @Override
    public void delete(K id) {
        T toRemove = tryFindById(id);

        if (toRemove != null) {
            persistables.remove(toRemove);
            DeletionHelper.delete(getDirectory(toRemove));
        }
    }

    @Override
    public Stream<T> stream() {
        return persistables.stream();
    }

    @Override
    public List<T> listAll() {
        return new ArrayList<>(persistables);
    }

    @Override
    public T tryFindById(K id) {
        return persistables.stream().filter(x -> x.id().equals(id)).findFirst().orElse(null);
    }

    protected boolean existsInList(T object) {
        return persistables.stream().anyMatch(x -> x.id().equals(object.id()));
    }

}
