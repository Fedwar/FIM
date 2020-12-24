package fleetmanagement.backend.tasks;

import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class TaskLogFile {

	private boolean includeVehicle = false;
	private final DateTimeFormatter datetime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());
	private final List<Task> tasks;
	private VehicleRepository vehicleRepository;
	private final String name;

	public TaskLogFile(Task task) {
		this.name = "task-" + task.getId() + "-log.txt";
		this.tasks = Collections.singletonList(task);
	}

	public TaskLogFile(List<Task> tasks, String name, VehicleRepository vehicleRepository) {
		this.name = name;
		this.tasks = tasks;
		includeVehicle = true;
		this.vehicleRepository = vehicleRepository;
	}

	public String getContent() {
		StringBuilder result = new StringBuilder();
		List<LogEntry> entries = new ArrayList<>();
		Map<LogEntry, Vehicle> vehicleMap = new HashMap<>();
		for (Task t : tasks) {
			entries.addAll(t.getLogMessages());
			if (includeVehicle) {
				Vehicle v = vehicleRepository.tryFindById(t.getVehicleId());
				t.getLogMessages().forEach(e -> vehicleMap.put(e, v));
			}
		}
		entries.sort(Comparator.comparing(e -> e.time, Comparator.reverseOrder()));
		for (LogEntry entry : entries) {
			result.append(datetime.format(entry.time)).append(" ")
					.append(entry.severity).append(" ");
			if (includeVehicle) {
				result.append(vehicleMap.get(entry).getName()).append(" ");
			}
			result.append(entry.message).append("\r\n");
		}
		return result.toString();
	}
	
	public String getFilename() {
		return name;
//		return "task-" + task.getId() + "-log.txt";
	}
	
}
