package fleetmanagement.usecases;

import fleetmanagement.backend.installations.PackageInstallation;
import fleetmanagement.backend.packages.ActivityLog;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.repositories.exception.PackageTypeNotLicenced;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class InstallPackage {

	private static final Logger logger = Logger.getLogger(InstallPackage.class);

	@Autowired
	private TaskRepository taskRepository;
	@Autowired
	private PackageRepository packageRepository;
	@Autowired
	private VehicleRepository vehicleRepository;
	@Autowired
	private Licence licence;
	@Autowired
	@Setter
	private ApplicationEventPublisher eventPublisher;

	public InstallPackage() {
	}

	public InstallPackage(TaskRepository taskRepository, PackageRepository packageRepository,
						  VehicleRepository vehicleRepository, Licence licence) {
		this.taskRepository = taskRepository;
		this.packageRepository = packageRepository;
		this.vehicleRepository = vehicleRepository;
		this.licence = licence;
	}

	public StartInstallationResult startInstallation(Package pkg, Vehicle vehicle, String triggered_by) {
		return startInstallation(pkg, Collections.singletonList(vehicle), triggered_by);
	}

	public StartInstallationResult startInstallation(Package pkg, List<Vehicle> vehicles, String triggered_by) {
		StartInstallationResult result = new StartInstallationResult();

		for (Vehicle v : vehicles) {
			ActivityLog.vehicleMessage(ActivityLog.Operations.INSTALLATION_STARTED, pkg, v, triggered_by);
			start(pkg, v, result);
		}

		if (result.startedTasks.size() > 0) {
			packageRepository.update(pkg.id, p ->
					p.startInstallation(result.startedTasks));
		}

		if (result.conflictingTasks.size() > 0) {
			PackageInstallation i = packageRepository.tryFindById(pkg.id).installation;
			if (i != null) {
				i.setConflictingTasks(result.conflictingTasks);
			}
		}

		return result;
	}

	private StartInstallationResult start(Package pkg, Vehicle vehicle, StartInstallationResult result) {
		logger.info("Installing package " + pkg.toString() + " to " + vehicle.toString());
		if (result == null) {
			result = new StartInstallationResult();
		}

		if (!licence.isPackageTypeAvailable(pkg.type)) {
			throw new PackageTypeNotLicenced(pkg.type);
		}

		result.conflictingTasks.addAll(vehicle.getRunningTasks(taskRepository).stream()
				.filter(t -> !t.getPackage().id.equals(pkg.id)
						&& t.getPackage().type == pkg.type
						&& t.getPackage().slot.equals(pkg.slot))
				.collect(toList()));

		Task previouslyStarted = vehicle.getRunningTasks(taskRepository).stream().filter(x -> x.getPackage().id.equals(pkg.id)).findFirst().orElse(null);
		Task started = previouslyStarted != null ? previouslyStarted : startTask(pkg, vehicle);
		result.startedTasks.add(started);

		return result;
	}

	private Task startTask(Package pkg, Vehicle v) {
		Task task = new Task(pkg, v, eventPublisher);
		task.addLog(new LogEntry(Severity.INFO, "Task created with id " + task.getId() + " to apply " + pkg + " to " + v));
		taskRepository.insert(task);
		vehicleRepository.tryFindById(v.id).addTask(task);

		return task;
	}
	
	public static class StartInstallationResult {
		public final List<Task> startedTasks = new ArrayList<>();
		public final List<Task> conflictingTasks = new ArrayList<>();
	}
}
