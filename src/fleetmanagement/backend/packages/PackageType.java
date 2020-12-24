package fleetmanagement.backend.packages;

import java.util.Set;
import java.util.function.Predicate;

import fleetmanagement.backend.packages.importers.*;
import fleetmanagement.backend.tasks.*;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleVersions;
import gsp.util.DoNotObfuscate;

@DoNotObfuscate
public enum PackageType {

	DataSupply {
		@Override
		public boolean isInstalledOn(Package p, Vehicle v, TaskRepository tasks) {
			String versionOnVehicle = v.versions.getDataSupplyVersion(p.slot);
			return versionOnVehicle != null && versionOnVehicle.equals(p.version);
		}

		@Override
		public PackageImporter getPackageImporter() {
			return new DataSupplyPackageImporter();
		}

		@Override
		public String getTaskType() {
			return "data-supply";
		}

		@Override
		public String getResourceKey() {
			return "data_supply";
		}
	},
	
	CopyStick {
		@Override
		public boolean isInstalledOn(Package pkg, Vehicle vehicle, TaskRepository tasks) {
			Predicate<Task> wasPreviouslyInstalled = t -> t.getPackage().id.equals(pkg.id) && t.isFinished();
			return vehicle.getTasks(tasks).stream().anyMatch(wasPreviouslyInstalled);
		}

		@Override
		public PackageImporter getPackageImporter() {
			return new CopyStickPackageImporter();
		}

		@Override
		public String getTaskType() {
			return "remote-copystick";
		}

		@Override
		public String getResourceKey() {
			return "remote_copystick";
		}

	},

	ClientConfig {
		@Override
		public boolean isInstalledOn(Package pkg, Vehicle vehicle, TaskRepository tasks) {
			Predicate<Task> wasPreviouslyInstalled = t -> t.getPackage().id.equals(pkg.id) && t.isFinished();
			return vehicle.getTasks(tasks).stream().anyMatch(wasPreviouslyInstalled);
		}

		@Override
		public PackageImporter getPackageImporter() {
			return new ClientConfigPackageImporter();
		}

		@Override
		public String getTaskType() {
			return "client-config";
		}

		@Override
		public String getResourceKey() {
			return "client_config";
		}

	},

	Indis5MultimediaContent {
		@Override
		public PackageImporter getPackageImporter() {
			return new Indis5MultimediaContentPackageImporter();
		}

		@Override
		public String getTaskType() {
			return "multimedia-content";
		}

		@Override
		public String getResourceKey() {
			return "indis5_multimedia_content";
		}

	},
	
	Indis3MultimediaContent {
		@Override
		public PackageImporter getPackageImporter() {
			return new Indis3MultimediaContentPackageImporter();
		}

		@Override
		public String getTaskType() {
			return "indis3-multimedia-content";
		}

		@Override
		public String getResourceKey() {
			return "indis3_multimedia_content";
		}

	},
	
	XccEnnoSeatReservation {
		@Override
		public boolean isInstalledOn(Package p, Vehicle v, TaskRepository tasks) {
			Predicate<Task> wasPreviouslyInstalled = t -> t.getPackage().id.equals(p.id) && t.isFinished();
			return v.getTasks(tasks).stream().anyMatch(wasPreviouslyInstalled);
		}

		@Override
		public PackageImporter getPackageImporter() {
			return new XccEnnoSeatReservationPackageImporter();
		}

		@Override
		public String getTaskType() {
			return "xcc-enno-seat-reservation";
		}

		@Override
		public String getResourceKey() {
			return "xcc_enno_seat_reservation";
		}
	},
	
	OebbDigitalContent {
		@Override
		public PackageImporter getPackageImporter() {
			return new OebbDigitalContentPackageImporter();
		}

		@Override
		public String getTaskType() {
			return "oebb-digital-content";
		}

		@Override
		public String getResourceKey() {
			return "oebb_digital_content";
		}
	},
	
	PassengerTvContent {
		@Override
		public PackageImporter getPackageImporter() {
			return new PassengerTvContentPackageImporter();
		}

		@Override
		public String getTaskType() {
			return "passenger-tv-content";
		}

		@Override
		public String getResourceKey() {
			return "passenger_tv_content";
		}
	},
	
	SbhOutageTicker {
		@Override
		public PackageImporter getPackageImporter() {
			return new SbhOutageTickerPackageImporter();
		}

		@Override
		public String getTaskType() {
			return "sbh-outage-ticker";
		}

		@Override
		public String getResourceKey() {
			return "sbh_outage_ticker";
		}
	};
	
	public boolean isInstalledOn(Package p, Vehicle v, TaskRepository tasks) {
		Set<String> allByType = v.versions.getAllVersionsByType(this);
		return allByType.contains(p.version);
	}
	
	public abstract PackageImporter getPackageImporter();
	public abstract String getTaskType();
	public abstract String getResourceKey();

	public static PackageType getByResourceKey(String resourceKey) {
		for (PackageType packageType: values()){
			if (packageType.getResourceKey().equals(resourceKey)) {
				return packageType;
			}
		}
		return null;
	}
	
}