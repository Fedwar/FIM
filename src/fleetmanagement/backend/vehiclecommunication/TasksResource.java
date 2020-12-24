package fleetmanagement.backend.vehiclecommunication;

import com.google.gson.annotations.SerializedName;
import com.sun.jersey.api.NotFoundException;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskJson;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.backend.webserver.UnknownVehicleRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@Path("tasks")
public class TasksResource {
	
	//modifiable for test
	private int longPollingTimeoutMillis = 600000;
	private int taskPollingMillis = 1000;

	@Autowired
	private TaskRepository tasks;
	@Autowired
	private VehicleRepository vehicles;
	@Autowired
	private NotificationService notificationService;

	public TasksResource() {
	}

	TasksResource(TaskRepository tasks, VehicleRepository vehicles, NotificationService notificationService) {
		this.tasks = tasks;
		this.vehicles = vehicles;
		this.notificationService = notificationService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public TaskList getTasks(
			@QueryParam("uic") String uic,
			@QueryParam("wait-for-change") @DefaultValue("false") boolean waitForChange, 
			@QueryParam("last-tasklist-id") String lastTasklistId) throws InterruptedException {
		UUID vehicleId = findVehicleByUiс(uic);
		updateLastSeen(vehicleId);

		if (waitForChange) {
			if (lastTasklistId == null)
				lastTasklistId = createCurrentTaskList(vehicleId).id;
			
			long start = System.nanoTime();
			while (Objects.equals(lastTasklistId, createCurrentTaskList(vehicleId).id) && timeoutNotReached(start)) {
				Thread.sleep(taskPollingMillis);
			}
		}

		return createCurrentTaskList(vehicleId);
	}
	
	@Path("/{id}")
	public TaskResource getTaskById(@PathParam("id") String id) {
		UUID taskId = UUID.fromString(id);
		Task t = tasks.tryFindById(taskId);
		
		if (t == null)
			throw new NotFoundException("Task not found: " + id);
		
		return new TaskResource(tasks, t, notificationService);
	}
	
	@XmlRootElement(name="tasks")
	public static class TaskList {
		
		@SerializedName("tasklist-id")
		public String id;
		
		@XmlElement(name="task")
		public List<TaskJson> tasks = new ArrayList<>();

		public void add(TaskJson task) {
			tasks.add(task);
		}
	}
	
	private void updateLastSeen(UUID vehicleId) {
		vehicles.update(vehicleId, vehicle -> {
			vehicle.lastSeen = ZonedDateTime.now();
		});
	}

	private UUID findVehicleByUiс(String uic) {
		Vehicle v = vehicles.tryFindByUIC(uic);
		if (v == null)
			throw new UnknownVehicleRequest(Status.BAD_REQUEST);
		return v.id;
	}

	private TaskList createCurrentTaskList(UUID vehicleId) {
		Vehicle v = vehicles.tryFindById(vehicleId);
		if (v == null)
			throw new UnknownVehicleRequest(Status.BAD_REQUEST);
		
		TaskList result = new TaskList();
		result.id = "0";
		
		Task next = v.getNextTask(tasks);
		if (next != null) {
			result.id = next.getId().toString();
			result.add(next.getTaskJson());
		}
		
		return result;
	}
	
	private boolean timeoutNotReached(long start) {
		return System.nanoTime() <= start + (longPollingTimeoutMillis * 1E6);
	}

	public void setLongPollingTimeoutMillis(int longPollingTimeoutMillis) {
		this.longPollingTimeoutMillis = longPollingTimeoutMillis;
	}

	public void setTaskPollingMillis(int taskPollingMillis) {
		this.taskPollingMillis = taskPollingMillis;
	}

	public int getLongPollingTimeoutMillis() {
		return longPollingTimeoutMillis;
	}

	public int getTaskPollingMillis() {
		return taskPollingMillis;
	}
}
