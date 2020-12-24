package fleetmanagement.frontend.model;

import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskStatus;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.transformers.DurationFormatter;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

public class VehicleDetails {

	public String uic;
	public String additional_uic;
	public String id;
	public String name;
	public String groupId;
	public String groupName;
	public boolean autoSync;
	public String clientVersion;
	public ZonedDateTime lastSeen;
	public String routeInformation;
	public boolean showGeo;
	public boolean showDiagnosis;
	public boolean showIpAddress;
	public boolean showOperationInfo;
	public boolean showAutoSync;
	public String latitude;
	public String longitude;
	public String lastLiveInfoUpdate;
	public List<ComponentVersion> versions = new ArrayList<>();
	public List<RunningTask> runningTasks = new ArrayList<>();
	public List<CompletedTask> completedTasks = new ArrayList<>();
	public String diagnosticError = null;
	public InstallPackageModal installPackageModal;
	public StatusMessage message;
	public Map<String, String> groupsForAssigning = new LinkedHashMap<>();
	public String ipAddress;

	public static class ComponentVersion {
		public String component;
		public String slot;
		public String version;
		public String packageId;
		public String validity;
		public Boolean active;
	}
	
	public static class DiagnosticError {
		public ZonedDateTime since;
		public String description;
	}

	public static class RunningTask {
		public String taskId;
		public String packageId;
		public String packageName;
		public int progress;
		public ZonedDateTime startDate;
		public String estimatedCompletion;
		public boolean taskWasStartedOnVehicle;
		public String status;

		public static RunningTask create(Task backendTask, UserSession request) {
			RunningTask task = new RunningTask();
			task.taskId = backendTask.getId().toString();
			task.packageId = backendTask.getPackage().id.toString();
			task.packageName = Name.of(backendTask.getPackage(), request);
			task.progress = backendTask.getStatus().percent;
			task.startDate = backendTask.getStartedAt();
			task.taskWasStartedOnVehicle = backendTask.getStatus().clientStage != TaskStatus.ClientStage.PENDING;
			task.status = Name.of(backendTask.getStatus().clientStage, request);

			Instant estimatedCompletion = backendTask.getEstimatedCompletionDate();
			if (estimatedCompletion == null)
				task.estimatedCompletion = I18n.get(request, "unknown");
			else
				task.estimatedCompletion = DurationFormatter.asHumanReadable(Duration.between(Instant.now(), estimatedCompletion), request.getLocale());
			return task;
		}
	}
	
	public static class CompletedTask {
		public String taskId;
		public String packageId;
		public String packageName;
		public String vehicleId;
		public String vehicleName;
		public String status;
		public String statusCssClass;
		public ZonedDateTime startDate;
		public ZonedDateTime completionDate;

		public static CompletedTask create(Task backendTask, UserSession request) {
			return create(backendTask, request, null);
		}

		public static CompletedTask create(Task backendTask, UserSession request, VehicleRepository vehicleRepository) {
			CompletedTask task = new CompletedTask();
			task.taskId = backendTask.getId().toString();
			task.packageId = backendTask.getPackage().id.toString();
			task.packageName = Name.of(backendTask.getPackage(), request);
			task.vehicleId = backendTask.getVehicleId().toString();
			if (vehicleRepository != null) {
				Vehicle v = vehicleRepository.tryFindById(backendTask.getVehicleId());
				task.vehicleName = v == null ? null : v.getName();
			}
			task.startDate = backendTask.getStartedAt();
			task.completionDate = backendTask.getCompletedAt();
			task.statusCssClass = getStatusCss(backendTask.getStatus());
			task.status = getLocalizedStatus(backendTask.getStatus(), request);
			return task;
		}

		public static String getLocalizedStatus(TaskStatus taskStatus, UserSession request) {
			switch (taskStatus.serverStatus) {
				case Finished:
					return I18n.get(request, "finished");
				case Cancelled:
					return I18n.get(request, "cancelled_at_x_percent", taskStatus.percent);
				case Failed:
					return I18n.get(request, "failed_at_x_percent", taskStatus.percent);
				default:
					throw new RuntimeException("Unexpected status: " + taskStatus.serverStatus);
			}
		}

		public static String getStatusCss(TaskStatus taskStatus) {
			switch (taskStatus.serverStatus) {
				case Finished:
					return "Finished";
				case Cancelled:
					return "Cancelled";
				case Failed:
					return "Failed";
				default:
					throw new RuntimeException("Unexpected status: " + taskStatus.serverStatus);
			}
		}
	}
}
