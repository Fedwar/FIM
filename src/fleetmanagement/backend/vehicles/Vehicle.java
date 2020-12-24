package fleetmanagement.backend.vehicles;

import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import gsp.util.WrappedException;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

public class Vehicle implements Cloneable {
	public final UUID id;
	public final String uic;
	public String additional_uic;
	public String ipAddress;
	public final VehicleVersions versions;
	public boolean autoSync;
	public ZonedDateTime lastSeen;
	public int lastSeenProtocol;
	public String clientVersion;
	public LiveInformation liveInformation;
	
	private DiagnosticSummary fromDiagnosis;
	private List<UUID> taskQueue = new ArrayList<>();
	private String groupId;
	private String name;
	
	public Vehicle(String uic, String additional_uic, String name, String clientVersion, ZonedDateTime lastSeen, String groupId, boolean autoSync, int lastSeenProtocol) {
		this(UUID.randomUUID(), uic, additional_uic, name, clientVersion, lastSeen, groupId, autoSync, lastSeenProtocol,null);
	}
	
	public Vehicle(UUID id, String uic, String additional_uic, String name, String clientVersion, ZonedDateTime lastSeen, String groupId, boolean autoSync, int lastSeenProtocol, String ipAddress) {
		this.id = id;
		this.uic = uic;
		this.additional_uic = additional_uic;
		this.name = name;
		this.clientVersion = clientVersion;
		this.lastSeen = lastSeen;
		this.lastSeenProtocol = lastSeenProtocol;
		this.ipAddress = ipAddress;
		this.versions = new VehicleVersions();
		this.fromDiagnosis = DiagnosticSummary.ok();
		this.groupId = groupId;
		this.autoSync = autoSync;
	}
	
	public Vehicle clone() {
		try {
			Vehicle cloned = (Vehicle) super.clone();
			cloned.taskQueue = new ArrayList<>(this.taskQueue);
			return cloned;
		}
		catch (CloneNotSupportedException e) {
			throw new WrappedException(e);
		}
	}

	public void addTask(Task task) {
		taskQueue.add(task.getId());
	}

	public void removeTask(Task task) {
		taskQueue.remove(task.getId());
	}
	
	public List<Task> getTasks(TaskRepository tasks) {
		return taskQueue.stream()
				.map(tasks::tryFindById)
				.filter(Objects::nonNull)
				.collect(toList());
	}
	
	public List<UUID> getTaskIds() {
		return taskQueue;
	}

	public List<Task> getRunningTasks(TaskRepository tasks) {
		return getTasks(tasks).stream()
				.filter(x -> !x.isCompleted())
				.collect(toList());
	}

	public Task getNextTask(TaskRepository tasks) {
		List<Task> running = getRunningTasks(tasks);
		return running.isEmpty() ? null : running.get(0);
	}
	
	public DiagnosticSummary getDiagnosticSummary(ZonedDateTime now) {
		ZonedDateTime noConnectionThreshold = now.minus(2, ChronoUnit.DAYS);
		if (lastSeen != null && lastSeen.isBefore(noConnectionThreshold))
			return DiagnosticSummary.notSeenSince(lastSeen);
		
		return fromDiagnosis;
	}
	
	public void updateDiagnosticSummary(Diagnosis diagnosis) {
		int brokenDevices = diagnosis.countBrokenDevices();
		fromDiagnosis = brokenDevices != 0 ? DiagnosticSummary.deviceErrors(brokenDevices) : DiagnosticSummary.ok(); 
	}
	
	@Override
	public String toString() {
		return name + uic + " (" + id + ")";
	}

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
