package fleetmanagement.backend.vehiclecommunication.upload;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.regex.*;

import javax.xml.bind.annotation.*;

import com.google.gson.Gson;

import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.packages.ActivityLog;
import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.packages.PackageRepository;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.tasks.TaskStatus.ClientStage;
import fleetmanagement.backend.vehiclecommunication.FileUploadListener;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TaskStatusFileUploadListener implements FileUploadListener {

    private static final Logger logger = Logger.getLogger(TaskStatusFileUploadListener.class);

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Pattern FILENAME_REGEX = Pattern.compile("task-status_([A-Za-z0-9\\-]+)\\.json");

	@Autowired
	private TaskRepository tasks;
	@Autowired
    private NotificationService notificationService;
	@Autowired
	private VehicleRepository vehicles;
	private final Gson gson = new Gson();

	public TaskStatusFileUploadListener() {
	}

	TaskStatusFileUploadListener(TaskRepository tasks, VehicleRepository vehicles,
								 NotificationService notificationService) {
		this.tasks = tasks;
		this.vehicles = vehicles;
		this.notificationService = notificationService;
	}
	
	@Override
	public boolean canHandleUploadedFile(String filename) {
		return FILENAME_REGEX.matcher(filename).matches();
	}
	
	@Override
	public void onFileUploaded(UUID vehicleId, String filename, byte[] fileContent) {
		Matcher m = FILENAME_REGEX.matcher(filename);
		m.matches();
		
		String id = m.group(1);
		UUID taskId = UUID.fromString(id);
		Status status = gson.fromJson(new String(fileContent, UTF8), Status.class);
		ClientStage stage = toClientStage(status.progress.stage);

		tasks.update(taskId, task -> {
			if (task == null)
				throw new RuntimeException("Could not process status for unknown task: " + id);
			
			task.setClientStatus(stage, status.progress.percent);
		});

		Task task = tasks.tryFindById(taskId);
		if (task != null) {
			Vehicle vehicle = vehicles.tryFindById(task.getVehicleId());
			Package pkg = task.getPackage();
			if (stage == ClientStage.FINISHED) {
				ActivityLog.vehicleMessage(ActivityLog.Operations.INSTALLATION_FINISHED, pkg, vehicle, null);
			};
			if (stage == ClientStage.FAILED) {
				ActivityLog.vehicleMessage(ActivityLog.Operations.INSTALLATION_FAILED, pkg, vehicle, null);
			};
			if (stage == ClientStage.CANCELLED) {
			    logger.debug("Sending email notification about failed package installation");
                if (notificationService != null)
			        notificationService.processEvent(Events.taskLogUpdated(task));
				ActivityLog.vehicleMessage(ActivityLog.Operations.INSTALLATION_CANCELLED, pkg, vehicle, null);
			};
		}
	}

	private ClientStage toClientStage(String stage) {
		if (stage.equals("INITIAL"))
			return ClientStage.INITIALIZING;
		
		return ClientStage.valueOf(stage);
	}
	
	@XmlRootElement(name="status")
	public class Status {
		@XmlElement(name="progress") Progress progress;
	}
	
	@DoNotObfuscate
	public class Progress {
		@XmlElement(name="stage") String stage;
		@XmlElement(name="percent") int percent;
	}

}