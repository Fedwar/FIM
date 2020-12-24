package fleetmanagement.frontend.model;

import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.UserSession;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class PackageTaskList implements Iterable<PackageTaskList.Task> {

	public final String packageId;
	public final String packageName;
	public final String packageType;
	private final List<Task> tasks;

	public PackageTaskList(fleetmanagement.backend.packages.Package pkg, TaskRepository taskRepository,
						   VehicleRepository vehicleRepository, UserSession request) {
		
		packageId = pkg.id.toString();
		packageName = Name.of(pkg, request);
		packageType = Name.of(pkg.type, request);

		tasks = taskRepository.getTasksByPackage(pkg.id).stream()
				.sorted(Comparator.comparing(fleetmanagement.backend.tasks.Task::getCompletedAt, Comparator.nullsFirst(Comparator.reverseOrder()))
						.thenComparing(fleetmanagement.backend.tasks.Task::getStartedAt, Comparator.reverseOrder()))
				.map(t -> Task.create(t, vehicleRepository, request))
				.collect(Collectors.toList());
	}

	public int size() {
		return tasks.size();
	}
	
	@Override
	public Iterator<Task> iterator() {
		return tasks.iterator();
	}
	
	private static String getLocalizedStatus(fleetmanagement.backend.tasks.Task backendTask, UserSession request) {
		if (backendTask.isCancelled())
			return I18n.get(request, "cancelled");
		if (backendTask.isFailed())
			return I18n.get(request, "failed");

		return Name.of(backendTask.getStatus().clientStage, request);
	}
	
	private static String getStatusCss(fleetmanagement.backend.tasks.Task backendTask) {
		switch (backendTask.getStatus().serverStatus)
		{
			case Finished: return "Finished";
			case Cancelled: return "Cancelled";
			case Failed: return "Failed";
			default: return "Running";
		}
	}

	public static class Task {
		public String taskId;
		public String vehicleId;
		public String vehicleName;
		public ZonedDateTime startDate;
		public ZonedDateTime completionDate;
		public String status;
		public String statusCssClass;

		public static Task create(fleetmanagement.backend.tasks.Task backendTask,
								  VehicleRepository vehicleRepository, UserSession request) {
			Task task = new Task();
			task.taskId = backendTask.getId().toString();
			task.vehicleId = backendTask.getVehicleId().toString();
			Vehicle v = vehicleRepository.tryFindById(backendTask.getVehicleId());
			task.vehicleName = v == null ? null : v.getName();
			task.startDate = backendTask.getStartedAt();
			task.completionDate = backendTask.getCompletedAt();
			task.status = getLocalizedStatus(backendTask, request) + ", " + backendTask.getStatus().percent + "%";
			task.statusCssClass = getStatusCss(backendTask);
			return task;
		}
	}
}
