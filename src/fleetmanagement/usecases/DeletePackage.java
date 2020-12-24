package fleetmanagement.usecases;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.ActivityLog;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DeletePackage {

	@Autowired
	private PackageRepository packages;
	@Autowired
	private TaskRepository tasks;
	@Autowired
	private VehicleRepository vehicles;
	@Autowired
	private GroupRepository groups;

	public DeletePackage() {
	}

	public DeletePackage(PackageRepository packages, TaskRepository tasks, VehicleRepository vehicles, GroupRepository groups) {
		this.packages = packages;
		this.tasks = tasks;
		this.vehicles = vehicles;
		this.groups = groups;
	}

	public void deleteById(UUID id, String triggered_by) {
		Package toDelete = packages.tryFindById(id);
		if (toDelete != null) {
			Group group = groups.tryFindById(toDelete.groupId);
			ActivityLog.packageMessage(ActivityLog.Operations.PACKAGE_DELETED, group, "",
					toDelete, triggered_by);
			packages.delete(id);

			for (Task t : tasks.getTasksByPackage(id)) {
				tasks.delete(t.getId());

				vehicles.update(t.getVehicleId(), v -> {
					if (v != null)
						v.removeTask(t);
				});
			}
		}
	}

}
