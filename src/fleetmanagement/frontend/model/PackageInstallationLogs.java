package fleetmanagement.frontend.model;

import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.UserSession;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PackageInstallationLogs {

	public String packageId;
	public String packageDescription;
	public List<LogMessage> logs;

	public PackageInstallationLogs(fleetmanagement.backend.packages.Package pkg, List<Task> tasks,
								   VehicleRepository vehicleRepository, UserSession request) {
		this.packageId = pkg.id.toString();
		this.packageDescription = Name.of(pkg, request);
		DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder()
				.appendLocalized(FormatStyle.MEDIUM, FormatStyle.MEDIUM)
				.toFormatter(request.getLocale())
				.withZone(ZoneId.systemDefault());
		this.logs = getLogs(tasks, vehicleRepository, timeFormatter);
	}
	
	private static List<LogMessage> getLogs(List<Task> tasks, VehicleRepository vehicleRepository, DateTimeFormatter timeFormatter) {
		List<LogMessage> result = new ArrayList<>();
		for (Task t : tasks) {
			result.addAll(t.getLogMessages().stream()
					.map(le -> LogMessage.create(le, vehicleRepository.tryFindById(t.getVehicleId()), timeFormatter))
					.collect(Collectors.toList()));
		}
		result.sort(Comparator.comparing(LogMessage::getInstant).reversed());
		return result;
	}
}
