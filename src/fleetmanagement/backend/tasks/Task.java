package fleetmanagement.backend.tasks;

import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import fleetmanagement.backend.packages.Package;
import fleetmanagement.backend.tasks.LogEntry.Severity;
import fleetmanagement.backend.tasks.TaskStatus.*;
import fleetmanagement.backend.vehicles.Vehicle;
import gsp.util.WrappedException;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;

public class Task implements Cloneable {

	private static final Logger logger = Logger.getLogger(Task.class);

	private final UUID id;
	private final Package pkg;
	private final UUID vehicleId;
	private final TaskJson description;
	private List<LogEntry> logs = new CopyOnWriteArrayList<>();
	private final ZonedDateTime startedAt;
	private ZonedDateTime completedAt;
	private final Clock clock;
	private TaskStatus status;
	
	private Instant firstProgressReceivedAt;
	private int firstProgress;

	@Getter
	private ApplicationEventPublisher eventPublisher;

	@Deprecated
	public Task(Package pkg, Vehicle vehicle) {
		this(UUID.randomUUID(), pkg, vehicle.id, ZonedDateTime.now(), null, new TaskStatus(), new ArrayList<>(), Clock.systemDefaultZone());
	}

	public Task(Package pkg, Vehicle vehicle, ApplicationEventPublisher eventPublisher) {
		this(UUID.randomUUID(), pkg, vehicle.id, ZonedDateTime.now(), null, new TaskStatus(), new ArrayList<>(), Clock.systemDefaultZone(), eventPublisher);
	}

	@Deprecated
	public Task(UUID id, Package pkg, UUID vehicleId, ZonedDateTime startedAt, ZonedDateTime completedAt,
				TaskStatus status, List<LogEntry> logs, Clock clock) {
		this(id, pkg, vehicleId, startedAt, completedAt, status, logs, clock, null);
	}

	public Task(UUID id, Package pkg, UUID vehicleId, ZonedDateTime startedAt, ZonedDateTime completedAt,
				TaskStatus status, List<LogEntry> logs, Clock clock, ApplicationEventPublisher eventPublisher) {
		this.id = id;
		this.pkg = pkg;
		this.vehicleId = vehicleId;
		this.clock = clock;
		this.description = new TaskJson(pkg.type.getTaskType(), id.toString(), "/tasks/" + id + "/files");
		this.logs.addAll(logs);
		this.startedAt = startedAt;
		this.completedAt = completedAt;
		this.status = status;
		this.eventPublisher = eventPublisher;
	}
	
	public Task clone() {
		try {
			Task cloned = (Task)super.clone();
			cloned.logs = new CopyOnWriteArrayList<>(this.logs);
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new WrappedException(e);
		}
	}
	
	public ZonedDateTime getStartedAt() {
		return startedAt;
	}
	
	public ZonedDateTime getCompletedAt() {
		return completedAt;
	}

	public UUID getId() {
		return id;
	}

	public TaskJson getTaskJson() {
		return description;
	}

	public Package getPackage() {
		return pkg;
	}

	public UUID getVehicleId() {
		return vehicleId;
	}
	
	public boolean isCancelled() {
		return status.serverStatus == ServerStatus.Cancelled; 
	}
	
	public boolean isFinished() {
		return status.serverStatus == ServerStatus.Finished; 
	}
	
	public boolean isFailed() {
		return status.serverStatus == ServerStatus.Failed;
	}
	
	public boolean isCompleted() {
		return status.hasCompleted();
	}

	public synchronized void cancel() {
		addLog(new LogEntry(Severity.WARNING, "Task cancelled"));
		logger.info("Cancelling package " + pkg.toString() + " for vehicle " + getVehicleId());
		setServerStatus(ServerStatus.Cancelled);
	}
	
	public TaskStatus getStatus() {
		return status;
	}

	public void addLog(LogEntry logEntry) {
		logs.add(logEntry);
	}
	
	public List<LogEntry> getLogMessages() {
		return Collections.unmodifiableList(logs);
	}

	public void setClientStatus(ClientStage stage, int percent) {
		
		if (status.clientStage == stage && status.percent == percent)
			return;
		
		ServerStatus serverStatus = status.serverStatus;

		if (serverStatus == ServerStatus.Pending)
			serverStatus = ServerStatus.Running;
		
		if (stage == ClientStage.FINISHED)
			serverStatus = ServerStatus.Finished;
		
		if (stage == ClientStage.CANCELLED) {
			boolean clientReportsLegacyFail = status.serverStatus != ServerStatus.Cancelled;
			serverStatus = clientReportsLegacyFail ? ServerStatus.Failed : ServerStatus.Cancelled;
		}
		
		if (stage == ClientStage.FAILED)
			serverStatus = ServerStatus.Failed;
		
		if (firstProgressReceivedAt == null) {
			firstProgressReceivedAt = clock.instant();
			firstProgress = percent;
		}
		
		TaskStatus newStatus = new TaskStatus(serverStatus, stage, percent);
		Severity severity = serverStatus == ServerStatus.Failed ? Severity.ERROR : Severity.INFO;
		addLog(new LogEntry(severity, "Client reported status: " + newStatus.clientStage + ", " + newStatus.percent + "% completed."));
		setStatus(newStatus);
	}

	public void setServerStatus(ServerStatus serverStatus) {
		
		if (status.serverStatus == serverStatus)
			return;
		
		TaskStatus newStatus = new TaskStatus(serverStatus, status.clientStage, status.percent);
		Severity severity = serverStatus == ServerStatus.Cancelled ? Severity.WARNING : Severity.INFO;
		addLog(new LogEntry(severity, "Server changed status to " + newStatus.serverStatus));
		setStatus(newStatus);		
	}

	private void setStatus(TaskStatus newStatus) {
		if (completedAt == null && newStatus.hasCompleted())
			completedAt = ZonedDateTime.now();
		
		status = newStatus;

		if (newStatus.hasCompleted()) {
			logger.debug("Task " + id + " is complete");
			if (eventPublisher != null) {
				eventPublisher.publishEvent(new TaskCompleteEvent(this, this));
			}
		}
	}
	
	public synchronized Instant getEstimatedCompletionDate() {
		int progress = status.percent - firstProgress;
		
		if (status.percent < 5 || progress == 0 || firstProgressReceivedAt == null || isCompleted() )
			return null;
		
		Duration timeTaken = Duration.between(clock.instant(), firstProgressReceivedAt).negated();	
		Duration durationPerPercent = timeTaken.dividedBy(progress);
		
		if (durationPerPercent.isZero() || durationPerPercent.isNegative())
			return null;
		
		return clock.instant().plus(durationPerPercent.multipliedBy(100 - status.percent));
	}

	public void setClientStarted() {
		if (getStatus().clientStage == ClientStage.PENDING)
			setClientStatus(ClientStage.PENDING, 0);
	}

	public void setClientDownloading() {
		if (!isCompleted())
			setClientStatus(ClientStage.DOWNLOADING, getStatus().percent);
	}
}
