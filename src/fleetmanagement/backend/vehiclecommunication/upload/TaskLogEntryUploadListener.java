package fleetmanagement.backend.vehiclecommunication.upload;

import com.google.gson.Gson;
import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.backend.tasks.LogEntry;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehiclecommunication.FileUploadListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.bind.annotation.XmlRootElement;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TaskLogEntryUploadListener implements FileUploadListener {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Pattern FILENAME_REGEX = Pattern.compile("task-log_([A-Za-z0-9\\-]+)(_[0-9]+)?\\.json");

	@Autowired
	private TaskRepository tasks;
	@Autowired
	private NotificationService notificationService;
	private final Gson gson = new Gson();

	public TaskLogEntryUploadListener() {
	}

	TaskLogEntryUploadListener(TaskRepository tasks, NotificationService notificationService) {
		this.tasks = tasks;
		this.notificationService = notificationService;
	}

	@Override
	public boolean canHandleUploadedFile(String filename) {
		return FILENAME_REGEX.matcher(filename).matches();
	}

	@Override
	public void onFileUploaded(UUID vehicleId, String filename, byte[] data) {		
		LogEntry entry = parseLogEntry(data);		
		Task task = findAssociatedTask(filename);
		task.addLog(entry);
		notificationService.processEvent(Events.taskLogUpdated(task));
	}

	private Task findAssociatedTask(String filename) {
		Matcher m = FILENAME_REGEX.matcher(filename);
		m.matches();
		
		String id = m.group(1);
		Task task = tasks.tryFindById(UUID.fromString(id));
		if (task == null)
			throw new UnknownTask(id);
		
		return task;
	}

	private LogEntry parseLogEntry(byte[] data) {
		LogJson log = gson.fromJson(new String(data, UTF8), LogJson.class);
		return new LogEntry(parseSeverity(log.severity), log.message);
	}

	private Severity parseSeverity(String severity) {
		switch (severity) {
			case "info": return Severity.INFO;
			case "warning": return Severity.WARNING;
			case "error": return Severity.ERROR;
			default: return Severity.INFO;
		}
	}

	@XmlRootElement
	private static class LogJson {
		String severity = "";
		String message;
	}
	
	static class UnknownTask extends RuntimeException {
		public UnknownTask(String id) {
			super("Could not process log for unknown task: " + id);
		}

		private static final long serialVersionUID = 1L;
	}
}
