package fleetmanagement.frontend.model;

import fleetmanagement.config.Licence;

import java.util.ArrayList;
import java.util.List;

public class Dashboard {
	public Licence licence;
	public String softwareVersion;
	public final Statistics statistics = new Statistics();
	public final List<RunningInstallation> runningInstallations = new ArrayList<>();
	public final List<DiagnosticError> diagnosticErrors = new ArrayList<>();
	public final DataPackets dataPackets = new DataPackets();
	
	public static class RunningInstallation {
		public int progress;
		public int finishedInstallations;
		public int totalInstallations;
		public String name;
		public String packageId;
		public String packageGroupName;
		public String packageGroupId;
	}
	
	public static class Statistics {
		public int vehicles;
		public int packages;
		public int totalPackageFiles;
		public String totalPackageSize;
	}
	
	public static class DiagnosticError {
		public String vehicleName;
		public String vehicleId;
		public String description;
	}

	public static class DataPackets {
		public List<FilterDirectory.FileModel> newDataPackets = new ArrayList<>();
		public int newDataPacketsCount;
		public int dataPacketsCount;
	}
}
