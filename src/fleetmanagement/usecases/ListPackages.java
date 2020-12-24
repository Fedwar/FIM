package fleetmanagement.usecases;

import java.util.*;
import java.util.stream.Collectors;

import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.model.*;
import fleetmanagement.frontend.model.PackageList.Category;

public class ListPackages {
	private final List<Package> packages;
	private final VehicleRepository vehicles;
	private final TaskRepository tasks;

	public ListPackages(List<Package> packages, VehicleRepository vehicles, TaskRepository tasks) {
		this.packages = packages;
		this.vehicles = vehicles;
		this.tasks = tasks;
	}

	public PackageList listPackages(UserSession request, GroupRepository groupRepository) {
		PackageList result = new PackageList();

		Map<PackageType, List<Package>> groupedPackages = packages.stream().collect(Collectors.groupingBy(x -> x.type));
		
		for (PackageType t : PackageType.values()) {
			if (!groupedPackages.containsKey(t))
				continue;
			
			Category category = new Category();
			category.name = Name.of(t, request);
			category.icon = Symbol.of(t);
			for (Package p : groupedPackages.get(t)) {
				PackageList.Entry uiPackage = new PackageList.Entry(p, groupRepository, request);
				PackageInstallationStatusOverview status = PackageInstallationStatusOverview.create(p, vehicles, tasks);
				uiPackage.installationInProgress = status.installationInProgress;
				uiPackage.installedCount = status.installed;
				uiPackage.vehicleCount = status.total;
				category.packages.add(uiPackage);
			} 
			
			category.packages.sort(Comparator.comparing(x -> x.name));
			result.categories.add(category);
		}
		
		return result;
	}
}
