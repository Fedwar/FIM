package fleetmanagement.frontend.model;

import java.time.ZonedDateTime;
import java.util.*;

import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.frontend.*;

public class VehicleTaskList implements Iterable<VehicleTaskList.Task> {
	
	public final String vehicleId;
	public final String vehicleDescription;
	private final List<Task> tasks = new ArrayList<>();
	
	public VehicleTaskList(fleetmanagement.backend.vehicles.Vehicle backendVehicle, TaskRepository taskRepository, UserSession request) {
		
		vehicleId = backendVehicle.id.toString();
		vehicleDescription = backendVehicle.getName();
		
		for (fleetmanagement.backend.tasks.Task backendTask : backendVehicle.getTasks(taskRepository)) {
			Task task = new Task();
			task.taskId = backendTask.getId().toString();
			task.packageId = backendTask.getPackage().id.toString();
			task.packageName = Name.of(backendTask.getPackage(), request);
			task.startDate = backendTask.getStartedAt();
			task.completionDate = backendTask.getCompletedAt();
			task.status = getLocalizedStatus(backendTask, request) + ", " + backendTask.getStatus().percent + "%";
			task.statusCssClass = getStatusCss(backendTask);
			
			tasks.add(task);
		}
		tasks.sort((task1, task2) -> -task1.startDate.compareTo(task2.startDate));
	}

	public int size() {
		return tasks.size();
	}
	
	@Override
	public Iterator<Task> iterator() {
		return tasks.iterator();
	}
	
	private String getLocalizedStatus(fleetmanagement.backend.tasks.Task backendTask, UserSession request) {
		if (backendTask.isCancelled())
			return I18n.get(request, "cancelled");
		if (backendTask.isFailed())
			return I18n.get(request, "failed");

		return Name.of(backendTask.getStatus().clientStage, request);
	}
	
	private String getStatusCss(fleetmanagement.backend.tasks.Task backendTask) {
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
		public String packageId;
		public String packageName;
		public ZonedDateTime startDate;
		public ZonedDateTime completionDate;
		public String status;
		public String statusCssClass;
	}
}
