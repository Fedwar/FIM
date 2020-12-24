package fleetmanagement.backend.vehiclecommunication;

import java.util.UUID;

import javax.ws.rs.Path;

import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.*;

public class TaskResource {

	private final TaskRepository tasks;
	private final UUID taskId;
	private final Package pkg;
	private NotificationService notificationService;

	public TaskResource(TaskRepository tasks, Task task, NotificationService notificationService) {
		this.tasks = tasks;
		this.taskId = task.getId();
		this.pkg = task.getPackage();
		this.notificationService = notificationService;
	}
	
	@Path("files")
	public PackageResource getPackage() {
		return new PackageResource(pkg, tasks, taskId, notificationService);
	}
	
}
