package fleetmanagement.backend.tasks;

import gsp.util.DoNotObfuscate;

public class TaskStatus {
	
	@DoNotObfuscate
	public enum ClientStage {
		PENDING,
		INITIALIZING,
		DOWNLOADING,
		WAITING,
		ACTIVATING,
		FINISHED,
		CANCELLED,
		FAILED
	}
	
	@DoNotObfuscate
	public enum ServerStatus {
		Pending,
		Running,
		Finished,
		Cancelled,
		Failed
	}
	
	public final ClientStage clientStage;
	public final ServerStatus serverStatus;
	public final int percent;
	
	public TaskStatus() {
		this(ServerStatus.Pending, ClientStage.PENDING, 0);
	}
	
	public TaskStatus(ServerStatus serverStatus, ClientStage clientStage, int percent) {
		this.clientStage = clientStage;
		this.serverStatus = serverStatus;
		this.percent = percent;
	}
	
	@Override
	public boolean equals(Object obj) {
		TaskStatus other = (TaskStatus)obj;
		return clientStage == other.clientStage && serverStatus == other.serverStatus && percent == other.percent;
	}
	
	@Override
	public int hashCode() {
		return percent;
	}

	public boolean hasCompleted() {
		return serverStatus == ServerStatus.Cancelled || serverStatus == ServerStatus.Finished || serverStatus == ServerStatus.Failed;
	}
}