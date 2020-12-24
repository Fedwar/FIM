package fleetmanagement.backend.diagnosis;

import java.time.ZonedDateTime;
import java.util.*;

public class Snapshot {
	public final UUID vehicleId;
	public final int version;
	public final ZonedDateTime timestamp;
	public final List<DeviceSnapshot> devices;

	public Snapshot(UUID vehicleId, int version, ZonedDateTime timestamp, List<DeviceSnapshot> devices) {
		this.vehicleId = vehicleId;
		this.version = version;
		this.timestamp = timestamp;
		this.devices = devices;
	}

	public boolean hasDevice(String id) {
		return devices.stream().anyMatch(x -> x.id.equals(id));
	}

}
