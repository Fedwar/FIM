package fleetmanagement.backend.repositories.disk.xml;

import java.io.File;
import java.time.*;
import java.util.*;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.repositories.disk.xml.XmlSerializer;
import fleetmanagement.backend.repositories.disk.xml.XmlToInstant;
import fleetmanagement.backend.repositories.disk.xml.XmlToZonedDateTime;
import fleetmanagement.backend.tasks.*;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.tasks.TaskStatus.*;
import gsp.util.DoNotObfuscate;

public class TaskXmlFile implements XmlFile<Task>  {
	
	private static final int CURRENT_FORMAT_VERSION = 2;
	
	private static final XmlSerializer serializer = new XmlSerializer(TaskXml.class);
	private static final String fileName = "task.xml";
	private final File file;

	public TaskXmlFile(File taskDirectory) {
		this.file = new File(taskDirectory, fileName);
	}

	@Override
	public File file() {
		return file;
	}

	@Override
	public void delete() {
		file.delete();
	}

	@Override
	public boolean exists() {
		return file.exists();
	}

    @Override
    public Task load() {
        return null;
    }

    public TaskXml meta() {
		TaskXml loaded = (TaskXml)serializer.load(file);
		performMigrationIfNecessary(loaded);
		return loaded;
	}

	public void save(Task t) {
		
		TaskXml meta = new TaskXml();
		meta.formatVersion = CURRENT_FORMAT_VERSION;
		meta.id = t.getId();
		meta.packageId = t.getPackage().id;
		meta.vehicleId = t.getVehicleId();
		meta.startedAt = t.getStartedAt();
		meta.completedAt = t.getCompletedAt();
		meta.status = new TaskStatusXml();
		meta.status.clientStage = t.getStatus().clientStage;
		meta.status.progress = t.getStatus().percent;
		meta.status.serverStatus = t.getStatus().serverStatus;
		meta.logs = new ArrayList<>();
		
		for (LogEntry e : t.getLogMessages()) {
			LogEntryXml entry = new LogEntryXml();
			entry.message = e.message;
			entry.severity = e.severity;
			entry.time = e.time;
			meta.logs.add(entry);
		}
		
		serializer.save(meta, file);
	}

	private void performMigrationIfNecessary(TaskXml t) {
		if (t.formatVersion < CURRENT_FORMAT_VERSION) {
			performMigrationFromV1IfNecessary(t);
			serializer.save(t, file);
		}
	}

	private void performMigrationFromV1IfNecessary(TaskXml t) {
		if (t.formatVersion == 1)
		{
			if (t.status.serverStatus == ServerStatus.Cancelled)
				t.status.serverStatus = ServerStatus.Failed;
			t.formatVersion = 2;
		}
	}

	@XmlRootElement(name="task")
	public static class TaskXml {
		@XmlAttribute(name="format-version") public int formatVersion;
		@XmlAttribute public UUID id;
		@XmlAttribute(name="package-id") public UUID packageId;
		@XmlAttribute(name="vehicle-id") public UUID vehicleId;
		@XmlAttribute(name="started-at") @XmlJavaTypeAdapter(XmlToZonedDateTime.class) public ZonedDateTime startedAt;
		@XmlAttribute(name="completed-at") @XmlJavaTypeAdapter(XmlToZonedDateTime.class) public ZonedDateTime completedAt;
		@XmlElement public TaskStatusXml status;
		@XmlElementWrapper(name="logs") @XmlElement(name="entry") public List<LogEntryXml> logs;
	}
	
	@DoNotObfuscate
	public static class LogEntryXml {
		@XmlAttribute @XmlJavaTypeAdapter(XmlToInstant.class) public Instant time;
		@XmlAttribute public Severity severity;
		@XmlAttribute public String message;
		
		public LogEntry toLogEntry() {
			return new LogEntry(time, severity, message);
		}
	}
	
	@DoNotObfuscate
	public static class TaskStatusXml {
		@XmlAttribute(name="client-stage") public ClientStage clientStage;
		@XmlAttribute(name="server-status") public ServerStatus serverStatus;
		@XmlAttribute public int progress;

		public TaskStatus toTaskStatus() {
			return new TaskStatus(serverStatus, clientStage, progress);
		}
	}
}
