package fleetmanagement.frontend.model;

import static java.util.Arrays.*;

import java.util.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

import fleetmanagement.backend.packages.*;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageInstallationStatus.State;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.frontend.UserSession;

public class InstallPackageModal {

	public String vehicleKey;
	public String vehicleName;
	public boolean isAtLeastOnePackageInstallable = false;
	public final List<InstallablePackageType> packageTypes = new ArrayList<>();
	
	public InstallPackageModal(Vehicle v, List<Package> packages, TaskRepository tasks, UserSession request) {
		vehicleKey = v.id.toString();
		vehicleName = v.getName();
		
		Map<PackageType, List<Package>> groupedPackages = packages.stream().collect(Collectors.groupingBy(x -> x.type));
		
		for (PackageType t : groupedPackages.keySet()) {
			InstallablePackageType type = new InstallablePackageType();
			type.name = Name.of(t, request);
			type.icon = Symbol.of(t);
			
			for (Package pkg : groupedPackages.get(t)) {
				InstallablePackage p = toInstallablePackage(v, type, pkg, tasks, request);
				if (p.isInstallable)
					isAtLeastOnePackageInstallable = true;
				type.installablePackages.add(p);
			}
			
			type.installablePackages.sort(Comparator.comparing(x -> x.name));
			packageTypes.add(type);
		}
		packageTypes.sort(Comparator.comparing(x -> x.name));
	}

	private InstallablePackage toInstallablePackage(Vehicle v, InstallablePackageType type, Package pkg, TaskRepository tasks, UserSession request) {
		InstallablePackage result = new InstallablePackage();
		result.name = Name.of(pkg, request);
		result.packageId = pkg.id.toString();
		PackageInstallationStatus status = PackageInstallationStatus.create(pkg, v, tasks);
		result.status = Name.of(status, request); 
		result.isInstallable = status.state != State.InstallationUpcoming;
		
		return result;
	}

	public static class InstallablePackageType {
		public String icon;
		public String name;
		public final List<InstallablePackage> installablePackages = new ArrayList<>();
	}
	
	public static class InstallablePackage {
		public String name;
		public String packageId;
		public String status;
		public boolean isInstallable;
	}
}
