package fleetmanagement.backend.tasks;

import gsp.util.DoNotObfuscate;

import java.time.Instant;

public class LogEntry {
	@DoNotObfuscate
	public enum Severity {
		INFO,
		WARNING,
		ERROR
	}
	
	public final Instant time;
	public final Severity severity;
	public final String message;
	
	public LogEntry(Severity severity, String message) {
		this(Instant.now(), severity, message);
	}
	
	public LogEntry(Instant time, Severity severity, String message) {
		this.time = time;
		this.severity = severity;
		this.message = message;
	}
	
	@Override
	public boolean equals(Object arg0) {
		LogEntry other = (LogEntry)arg0;
		return time.equals(other.time) && severity == other.severity && message.equals(other.message);
	}
	
	@Override
	public int hashCode() {
		return time.hashCode();
	}
	
	@Override
	public String toString() {
		return String.format("[%s] %s %s", time, severity, message);
	}
	
}
