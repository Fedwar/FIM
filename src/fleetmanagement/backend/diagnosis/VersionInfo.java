package fleetmanagement.backend.diagnosis;

import java.util.*;

public class VersionInfo {
	public enum VersionType {
		Software,
		Fontware
	}
	
	private final Map<String, String> versions = new HashMap<>();
	
	public VersionInfo() {
	}
	
	public VersionInfo clone() {
		VersionInfo cloned = new VersionInfo();
		cloned.versions.putAll(versions);
		return cloned;
	}

	public VersionInfo(String softwareVersion, String fontVersion) {
		versions.put(VersionType.Software.toString(), softwareVersion);
		versions.put(VersionType.Fontware.toString(), fontVersion);
	}

	public VersionInfo(Map<String, String> versionData) {
		if (versionData != null)
			versions.putAll(versionData);
	}

	public String get(VersionType type) {
		return versions.get(type.toString());
	}
	public Map<String, String> getAll() {
		return versions;
	}

}
