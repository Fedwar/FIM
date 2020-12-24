package fleetmanagement.frontend.model;

import java.time.ZoneId;
import java.time.format.*;
import java.util.*;
import java.util.stream.Collectors;

import fleetmanagement.backend.tasks.*;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.frontend.UserSession;

public class TaskDetails {
	public String id;
	public String vehicleId;
	public String vehicleDescription;
	public String packageDescription;
	public List<LogMessage> logs;

	public TaskDetails(Task task, Vehicle vehicle, UserSession request) {
		this.id = task.getId().toString();
		this.vehicleId = vehicle.id.toString();
		this.vehicleDescription = vehicle.getName();
		this.packageDescription = Name.of(task.getPackage(), request);
		DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().appendLocalized(FormatStyle.MEDIUM, FormatStyle.MEDIUM).toFormatter(request.getLocale()).withZone(ZoneId.systemDefault());
		this.logs = getLogs(task, vehicle, timeFormatter);
	}
	
	private static List<LogMessage> getLogs(Task task, Vehicle v, DateTimeFormatter timeFormatter) {
		return task.getLogMessages().stream().map(le -> LogMessage.create(le, v, timeFormatter)).collect(Collectors.toList());
	}
	
}
