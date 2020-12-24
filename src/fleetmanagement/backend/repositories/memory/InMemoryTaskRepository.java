package fleetmanagement.backend.repositories.memory;

import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.disk.OnDiskTaskRepository;
import fleetmanagement.backend.repositories.disk.xml.TaskXmlFile;
import fleetmanagement.backend.repositories.exception.TaskDuplicateException;
import fleetmanagement.backend.tasks.DateTimeDescTaskComparator;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InMemoryTaskRepository extends OnDiskTaskRepository {

    public InMemoryTaskRepository(PackageRepository packages) {
        super(null, packages);
    }

    @Override
    protected void save(Task t) {
    }

    @Override
    protected void delete(Task t) {
    }
}
