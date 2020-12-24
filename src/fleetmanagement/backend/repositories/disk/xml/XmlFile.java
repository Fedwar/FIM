package fleetmanagement.backend.repositories.disk.xml;

import java.io.File;

public interface XmlFile<T> {

    File file();

    void delete();

    boolean exists();

    T load();

    void save(T o);

}
