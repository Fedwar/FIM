package fleetmanagement.backend.packages;

import fleetmanagement.backend.tasks.*;
import fleetmanagement.backend.vehicles.Vehicle;




public class PackageInstallationStatus {
	
	public enum State { NotInstalled, InstallationUpcoming, Installed }
	public final State state;
	public final int progressPercent;
	
	public PackageInstallationStatus(State state, int progressPercent) {
		this.state = state;
		this.progressPercent = progressPercent;
	}

	public static PackageInstallationStatus create(Package pkg, Vehicle v, TaskRepository tasks) {
		for (Task t : v.getRunningTasks(tasks)) {
			if (t.getPackage().id.equals(pkg.id))
				return new PackageInstallationStatus(State.InstallationUpcoming, t.getStatus().percent);
		}
		
		if (pkg.isInstalledOn(v, tasks))
			return new PackageInstallationStatus(State.Installed, 100);
		
		return new PackageInstallationStatus(State.NotInstalled, 0);
	}
	
}
