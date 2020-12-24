package fleetmanagement.backend.tasks;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.vehicles.Vehicle;

import java.util.*;
import java.util.function.Consumer;

public interface TaskRepository {
	void insert(Task t);
	void update(UUID id, Consumer<Task> update);
	void delete(UUID id);
	Task tryFindById(UUID id);
	List<Task> getTasksByPackage(UUID packageId);
	List<Task> getTasksByVehicle(UUID vehicleId);
	List<Task> getRunningTasksByPackage(UUID packageId);
	long getNumberOfRunningTasks(UUID packageId);
	Map<Integer, Task> latestTasksForEachSlot(Vehicle vehicle, PackageType packageType);
}