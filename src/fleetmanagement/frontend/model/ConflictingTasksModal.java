package fleetmanagement.frontend.model;

import java.util.*;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.vehicles.*;
import fleetmanagement.frontend.UserSession;

public class ConflictingTasksModal {

	public String packageKey;
	public List<ConflictingTask> conflicting = new ArrayList<>();
	
	public ConflictingTasksModal(Package p, List<Task> conflictingTasks, VehicleRepository vehicles, UserSession request) {
		this.packageKey = p.id.toString();
		
		for (Task t : conflictingTasks) {
			ConflictingTask c = new ConflictingTask();
			c.taskId = t.getId().toString();
			c.packageName = Name.of(t.getPackage(), request);
			c.vehicle = getVehicleName(vehicles, t);
			conflicting.add(c);
		}
	}

	private String getVehicleName(VehicleRepository vehicles, Task t) {
		Vehicle vehicle = vehicles.tryFindById(t.getVehicleId());
		return vehicle == null ? "" : vehicle.getName();
	}
	
	public static class ConflictingTask {
		public String taskId;
		public String vehicle;
		public String packageName;
	}
}
