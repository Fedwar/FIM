package fleetmanagement.usecases;

import fleetmanagement.backend.groups.Group;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageInstallationStatusOverview;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.Name;
import fleetmanagement.frontend.model.PackageDetails;
import fleetmanagement.frontend.model.VehicleDetails;
import fleetmanagement.frontend.model.VehicleGroupMap;
import fleetmanagement.frontend.transformers.DurationFormatter;
import org.apache.commons.io.FileUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static fleetmanagement.backend.packages.PackageInstallationStatus.State.Installed;
import static fleetmanagement.backend.packages.PackageInstallationStatus.State.NotInstalled;

public class ListPackageDetails {

	private static final int TASK_COUNT = 4;

	private final VehicleRepository vehicles;
	private final TaskRepository tasks;
	private final GroupRepository groups;
	private final PackageRepository packages;
	
	public ListPackageDetails(VehicleRepository vehicles, TaskRepository tasks, GroupRepository groups, PackageRepository packages) {
		this.vehicles = vehicles;
		this.tasks = tasks;
		this.groups = groups;
		this.packages = packages;
	}
	
	public PackageDetails listPackageDetails(Package p, UserSession request) {		
		PackageInstallationStatusOverview overview = PackageInstallationStatusOverview.create(p, vehicles, tasks);
		PackageDetails vm = new PackageDetails();
		fillInstallationStatus(vm, overview, request);
		vm.name = Name.of(p, request);
		vm.type = Name.of(p.type, request);
		vm.version = p.version;
		vm.size = I18n.get(request, "size_and_filecount", FileUtils.byteCountToDisplaySize(p.size.bytes), p.size.files);
		vm.key = p.id.toString();
		vm.slot = p.slot == 0 ? null : p.slot.toString();
		vm.startOfPeriod = p.startOfPeriod;
		vm.endOfPeriod = p.endOfPeriod;
		Group g = p.groupId == null ? null : groups.tryFindById(p.groupId);
		vm.groupId = g == null ? null : g.id.toString();
		vm.groupName = g == null ? null : g.name;
		vm.downloadAvailable = p.archive != null;
		fillGroupsForAssigningAndRemoving(vm, p);
		vm.completedTasks = getRecentTasks(p, request);
		return vm;
	}

	private void fillInstallationStatus(PackageDetails vm, PackageInstallationStatusOverview overview, UserSession request) {
		vm.installationInProgress = overview.installationInProgress;
		vm.installedCount = overview.installed;
		vm.vehicleCount = overview.total;
		vm.installationProgressPercent = overview.progressPercent;

		List<Vehicle> installed = new ArrayList<>();
		List<Vehicle> notInstalled = new ArrayList<>();
		Map<UUID, Vehicle> vehicleMap = new HashMap<>();
		overview.statusByVehicle.forEach((key, value) -> {
			if (value.state == Installed) {
				installed.add(key);
			} else if (value.state == NotInstalled) {
				notInstalled.add(key);
			}
			vehicleMap.put(key.id, key);
		});
		vm.installedVehicles = installed.stream()
				.map(PackageDetails.VehicleReference::new)
				.collect(Collectors.toList());

		List<Task> runningTasks = overview.currentTasks.stream()
				.filter(t -> !t.isCompleted())
				.collect(Collectors.toList());
		vm.installationStartedAt = runningTasks.stream()
				.map(Task::getStartedAt)
				.min(Comparator.naturalOrder())
				.orElse(null);
		Instant estimatedCompletion = runningTasks.stream()
				.map(Task::getEstimatedCompletionDate)
				.filter(Objects::nonNull)
				.max(Comparator.naturalOrder())
				.orElse(null);
		if (estimatedCompletion == null)
			vm.installationEstimatedCompletion = I18n.get(request, "unknown");
		else
			vm.installationEstimatedCompletion = DurationFormatter.asHumanReadable(
					Duration.between(Instant.now(), estimatedCompletion), request.getLocale());

		vm.installationVehicles = overview.currentTasks.stream()
				.map(t -> new PackageDetails.VehicleReference(vehicleMap.get(t.getVehicleId())))
				.collect(Collectors.toList());

		vm.vehicleMapForInstallation = new VehicleGroupMap(notInstalled, groups.listAll());
	}

	private void fillGroupsForAssigningAndRemoving(PackageDetails vm, Package p) {
		List<Group> sortedGroupsList = new ArrayList<>(groups.mapAll().values());
		sortedGroupsList.sort(Comparator.comparing(g -> g.name));
		for (Group group : sortedGroupsList) {
			if (!packages.isGroupContainsPackageDuplicate(p, group))
				vm.groupsForAssigning.put(group.id.toString(), group.name);
			else
				vm.groupsForRemovingPackagesFromThem.put(group.id.toString(), group.name);
		}
	}

	private List<VehicleDetails.CompletedTask> getRecentTasks(Package p, UserSession request) {
		List<Task> packageTasks = tasks.getTasksByPackage(p.id).stream()
				.filter(Task::isCompleted)
				.sorted(Comparator.comparing(Task::getCompletedAt).reversed())
				.collect(Collectors.toList());
		if (packageTasks.size() > TASK_COUNT) {
			packageTasks = packageTasks.subList(0, TASK_COUNT);
		}
		return packageTasks.stream()
				.map(t -> VehicleDetails.CompletedTask.create(t, request, vehicles))
				.collect(Collectors.toList());
	}

}
