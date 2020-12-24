package fleetmanagement.frontend.model;

import fleetmanagement.backend.vehicles.Vehicle;

import java.time.ZonedDateTime;
import java.util.*;

public class PackageDetails {
	public Boolean installationInProgress;
	public Integer installationProgressPercent;
	public ZonedDateTime installationStartedAt;
	public String installationEstimatedCompletion;
	public List<VehicleReference> installationVehicles = new ArrayList<>();
	//	public String overallUpdatePercent;
	public Integer installedCount;
	public Integer vehicleCount;
//	public String overallUpdateVehicles;
	public String name;
	public String type;
	public String version;
	public String size;
	public String key;
	public String slot;
	public String startOfPeriod;
	public String endOfPeriod;
	public String groupId;
	public String groupName;
	public List<VehicleReference> installedVehicles = new ArrayList<>();
	public List<VehicleDetails.CompletedTask> completedTasks = new ArrayList<>();
// 	public List<VehicleReference> finishedVehicles = new ArrayList<>();
//	public List<VehicleReference> updatingVehicles = new ArrayList<>();
//	public List<VehicleReference> availableForInstallation = new ArrayList<>();
	public VehicleGroupMap vehicleMapForInstallation;
	public Map<String, String> groupsForAssigning = new LinkedHashMap<>();
	public Map<String, String> groupsForRemovingPackagesFromThem = new LinkedHashMap<>();
	public ConflictingTasksModal conflictingTasksModal;
	public StatusMessage message;
	public boolean downloadAvailable;
	
	public static class VehicleReference {
		public String key;
		public String name;
		public int progress;
		
		public VehicleReference(Vehicle v, int progress) {
			this.key = v.id.toString();
			this.name = v.getName();
			this.progress = progress;
		}		
		
		public VehicleReference(Vehicle v) {
			this(v, -1);
		}
	}
}
