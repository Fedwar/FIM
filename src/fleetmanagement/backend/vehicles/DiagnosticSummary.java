package fleetmanagement.backend.vehicles;

import java.time.ZonedDateTime;

public class DiagnosticSummary {
	
	public enum DiagnosticSummaryType {
		Ok, NotSeen, DeviceErrors
	}

	public final int brokenDevices;
	public final ZonedDateTime lastSeen;
	public final DiagnosticSummaryType type;
	
	public DiagnosticSummary(DiagnosticSummaryType type, ZonedDateTime lastSeen, int brokenDevices) {
		this.lastSeen = lastSeen;
		this.type = type;
		this.brokenDevices = brokenDevices;
	}

	public static DiagnosticSummary ok() {
		return new DiagnosticSummary(DiagnosticSummaryType.Ok, null, 0);
	}

	public static DiagnosticSummary notSeenSince(ZonedDateTime lastSeen) {
		return new DiagnosticSummary(DiagnosticSummaryType.NotSeen, lastSeen, 0);
	}

	public static DiagnosticSummary deviceErrors(int brokenDevices) {
		return new DiagnosticSummary(DiagnosticSummaryType.DeviceErrors, null, brokenDevices);
	}
}
