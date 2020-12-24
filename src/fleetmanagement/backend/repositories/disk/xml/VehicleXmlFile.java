package fleetmanagement.backend.repositories.disk.xml;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.*;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.tasks.Task;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.*;
import fleetmanagement.backend.vehicles.LiveInformation.NextStation;
import fleetmanagement.backend.vehicles.VehicleVersions.Versioned;
import gsp.util.DoNotObfuscate;

public class VehicleXmlFile implements XmlFile<Vehicle> {
	
	private static final XmlSerializer serializer = new XmlSerializer(VehicleXml.class);
	private final File file;
	private final TaskRepository taskRepository;
	
	public VehicleXmlFile(File directory, TaskRepository taskRepository) {
		this.file = new File(directory, "vehicle.xml");
		this.taskRepository = taskRepository;
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
	public Vehicle load() {
		VehicleXml meta = (VehicleXml)serializer.load(file);
		Vehicle v = new Vehicle(meta.id, meta.uic, meta.additional_uic, meta.name, meta.clientVersion,
				meta.lastSeen, meta.groupId, meta.autoSync, meta.lastSeenProtocol, meta.ipAddress);

		LiveInformation.Position position = null;
		if (meta.position != null)
			 position = new LiveInformation.Position(meta.position.latitude, meta.position.longitude);

		List<NextStation> nextStations = new ArrayList<>();
		for (NextStationXml ns : meta.nextStations) {
			nextStations.add(new NextStation(ns.name, ns.plannedArrival, ns.estimatedArrival));
		}
		
		ZonedDateTime timestamp = meta.liveInfoTimestamp != null ? meta.liveInfoTimestamp : ZonedDateTime.now();
		if (position != null || !nextStations.isEmpty() ||
				meta.startStation != null || meta.destinationStation != null ||
				meta.trainType != null || meta.tripNumber != null) {
			v.liveInformation = new LiveInformation(position, meta.startStation, meta.destinationStation, meta.trainType, meta.tripNumber, nextStations, timestamp);
		}

		meta.versions.stream()
				.map(VersionXml::toVersioned)
				.forEach(versioned -> v.versions.add(versioned));

		meta.components.stream()
				.map(ComponentXml::toVersioned)
				.forEach(versioned -> v.versions.add(versioned));

		for (Task task: taskRepository.getTasksByVehicle(v.id)) {
			v.addTask(task);
		}

		return v;
	}

	public void save(Vehicle v) {
		VehicleXml meta = new VehicleXml();
		meta.formatVersion = 1;
		meta.id = v.id;
		meta.lastSeen = v.lastSeen;
		meta.lastSeenProtocol = v.lastSeenProtocol;
		meta.name = v.getName();
		meta.uic = v.uic;
		meta.additional_uic = v.additional_uic;
		meta.autoSync = v.autoSync;
		meta.groupId = v.getGroupId();
		meta.clientVersion = v.clientVersion;
		meta.ipAddress = v.ipAddress;

		if (v.liveInformation != null) {
			LiveInformation liveInfo = v.liveInformation;
			if (liveInfo.position != null) {
				meta.position = new PositionXml();
				meta.position.latitude = liveInfo.position.latitude;
				meta.position.longitude = liveInfo.position.longitude;
			}
			
			meta.liveInfoTimestamp = liveInfo.received;
			meta.trainType = liveInfo.trainType;
			meta.tripNumber = liveInfo.tripNumber;
			meta.startStation = liveInfo.startStation;
			meta.destinationStation = liveInfo.destinationStation;
	
			for (NextStation ns : liveInfo.nextStations) {
				NextStationXml xml = new NextStationXml();
				xml.name = ns.name;
				xml.plannedArrival = ns.plannedArrival;
				xml.estimatedArrival = ns.estimatedArrival;
				meta.nextStations.add(xml);
			}
		}

		for (Versioned versioned : v.versions.getAll()) {
			meta.components.add(new ComponentXml(versioned));
		}
		
		serializer.save(meta, file);
	}

	@XmlRootElement(name="vehicle")
	private static class VehicleXml {
		@XmlAttribute(name="format-version") public int formatVersion;
		@XmlAttribute public UUID id;
		@XmlAttribute public String uic;
		@XmlAttribute public String additional_uic;
		@XmlAttribute public String name;
		@XmlAttribute public String ipAddress;
		@XmlAttribute public String groupId;
		@XmlAttribute public boolean autoSync;
		@XmlAttribute public String clientVersion;
		@XmlAttribute public String trainType;
		@XmlAttribute public String tripNumber;
		@XmlAttribute public String startStation;
		@XmlAttribute public String destinationStation;
		@XmlAttribute(name="last-seen-protocol") public int lastSeenProtocol = 0;
		@XmlElement public PositionXml position;
		@XmlElementWrapper(name="next-stations") @XmlElement(name="station") public List<NextStationXml> nextStations = new ArrayList<>();
		@XmlAttribute(name="last-seen") @XmlJavaTypeAdapter(XmlToZonedDateTime.class) public ZonedDateTime lastSeen;
		@Deprecated
		@XmlElementWrapper(name="versions") @XmlElement(name="version") public List<VersionXml> versions = new ArrayList<>();
		@XmlElementWrapper(name="components") @XmlElement(name="component") public List<ComponentXml> components = new ArrayList<>();
		@XmlAttribute(name="live-info-timestamp") @XmlJavaTypeAdapter(XmlToZonedDateTime.class) public ZonedDateTime liveInfoTimestamp;
	}

	@DoNotObfuscate
	private static class ComponentXml {
		public PackageType type;
		public String version;
		public Integer slot;
		public String validityBegin;
		public String validityEnd;
		public Boolean active;

		public ComponentXml() {
		}

		public ComponentXml(Versioned versioned) {
			this.type = versioned.type;
			this.version = versioned.version;
			this.slot = versioned.slot;
			this.validityBegin = versioned.validityBegin;
			this.validityEnd = versioned.validityEnd;
			this.active = versioned.active;
		}

		Versioned toVersioned() {
			return new Versioned(type, slot, version, validityBegin, validityEnd, active);
		}
	}

	@Deprecated
	@DoNotObfuscate
	private static class VersionXml {
		public String component;
		public String version;

		Versioned toVersioned() {
			if (component.equals("DV_SLOT_1"))
				return new Versioned(PackageType.DataSupply,1, version, null, null, false);
			if (component.equals("DV_SLOT_2"))
				return new Versioned(PackageType.DataSupply,2, version, null, null, false);
			if (component.equals("DV_SLOT_3"))
				return new Versioned(PackageType.DataSupply,3, version, null, null, false);
			return new Versioned(PackageType.valueOf(component), version);
		}
	}

	@DoNotObfuscate
	private static class PositionXml {
		public double latitude;
		public double longitude;
	}
	
	@DoNotObfuscate
	private static class NextStationXml {
		public String name;
		public String plannedArrival;
		public String estimatedArrival;
	}
}
