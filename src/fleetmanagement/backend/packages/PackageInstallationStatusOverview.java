package fleetmanagement.backend.packages;

import java.util.*;
import java.util.stream.Collectors;

import fleetmanagement.backend.installations.PackageInstallation;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import org.apache.log4j.Logger;

import static fleetmanagement.backend.packages.PackageInstallationStatus.State.Installed;

public class PackageInstallationStatusOverview {

	private static final Logger logger = Logger.getLogger(PackageInstallationStatusOverview.class);

	public boolean installationInProgress;
	public final int progressPercent;
	public final int installed;
	public final int total;
	public final Map<Vehicle, PackageInstallationStatus> statusByVehicle;
	public final List<Task> currentTasks;

	public PackageInstallationStatusOverview(boolean installationInProgress, int progressPercent, int installed, int total,
			Map<Vehicle, PackageInstallationStatus> statusByVehicle, List<Task> currentTasks) {
		this.progressPercent = progressPercent;
		this.installationInProgress = installationInProgress;
		this.installed = installed;
		this.total = total;
		this.statusByVehicle = statusByVehicle;
		this.currentTasks = currentTasks;
	}
	
	public static PackageInstallationStatusOverview create(Package pkg, VehicleRepository vehicles, TaskRepository tasks) {

		List<Vehicle> potentials = pkg.groupId == null ? vehicles.listAll()
				: vehicles.listByGroup(pkg.groupId.toString());

		Map<Vehicle, PackageInstallationStatus> statusByVehicle = potentials.stream()
				.collect(Collectors.toMap(v -> v,
						v -> PackageInstallationStatus.create(pkg, v, tasks)));

		Set<UUID> installed = statusByVehicle.entrySet().stream()
				.filter(e -> e.getValue().state == Installed)
				.map(e -> e.getKey().id)
				.collect(Collectors.toSet());

		int installedCount = installed.size();
		int progress = 0;
		int totalCount = pkg.groupId == null ? installedCount : potentials.size();

		PackageInstallation packageInstallation = pkg.installation;
		List<Task> installationTasks = Collections.emptyList();
		if (packageInstallation != null) {
			installationTasks = pkg.installation.getTasks().stream()
					.map(tasks::tryFindById)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());

			long totalProgress = installationTasks.stream()
					.mapToInt(t -> t.getStatus().percent).sum();
			progress = installationTasks.isEmpty() ? 0
					: (int) (totalProgress / installationTasks.size());

			if (pkg.groupId == null) {
				Set<UUID> candidates = installed;
				installationTasks.forEach(t -> candidates.add(t.getVehicleId()));
				totalCount = candidates.size();
			}
		}

		return new PackageInstallationStatusOverview(packageInstallation != null, progress,
				installedCount, totalCount, statusByVehicle, installationTasks);
	}
}
