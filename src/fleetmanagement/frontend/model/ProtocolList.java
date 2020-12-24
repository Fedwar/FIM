package fleetmanagement.frontend.model;

import fleetmanagement.backend.Backend;
import fleetmanagement.backend.repositories.disk.OnDiskVehicleRepository;
import fleetmanagement.config.Licence;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ProtocolList extends Admin implements Iterable<ProtocolList.ProtocolItem> {
	
	private final List<ProtocolItem> protocols = new ArrayList<>();

	public ProtocolList(Backend backend, Licence licence, OnDiskVehicleRepository vehicleRepository) {
		super(licence);

		for (int i=0; i<3; i++) {
			ProtocolItem protocol = new ProtocolItem(i, backend, vehicleRepository);
			protocols.add(protocol);
		}
	}
	
	public int size() {
		return protocols.size();
	}
	
	@Override
	public Iterator<ProtocolItem> iterator() {
		return protocols.iterator();
	}
	
	public static class ProtocolItem {
		public int index;
		public String name;
		public boolean enabled;
		public int port;
		public int vehicleCount;
		
		public ProtocolItem(int index, Backend backend, OnDiskVehicleRepository vehicleRepository) {
			this.index = index;
			this.name = backend.getProtocolName(index);
			this.enabled = backend.getProtocolState(index);
			this.port = backend.getProtocolPort(index);
			
			List<fleetmanagement.backend.vehicles.Vehicle> backendVehicles = vehicleRepository.listAll();
			int c = 0;
			for (fleetmanagement.backend.vehicles.Vehicle v : backendVehicles) {
				if (v.lastSeenProtocol == index) 
					c++;
			}
			this.vehicleCount = c;
		}
	}

	public static class ProtocolItemDetail extends Admin {
		public int index;
		public String name;
		public boolean enabled;
		public int port;
		public List<Vehicle> vehicles = new ArrayList<>();
		public int errno;
		
		public ProtocolItemDetail(int index, Backend backend, Licence licence, OnDiskVehicleRepository vehicleRepository) {
			super(licence);
			this.index = index;
			this.name = backend.getProtocolName(index);
			this.enabled = backend.getProtocolState(index);
			this.port = backend.getProtocolPort(index);
			
			List<fleetmanagement.backend.vehicles.Vehicle> backendVehicles = vehicleRepository.listAll();
			for (fleetmanagement.backend.vehicles.Vehicle v : backendVehicles) {
				if (v.lastSeenProtocol == index) {
					Vehicle vehicle = new Vehicle();
					vehicle.name = v.getName();
					vehicle.key = v.id.toString();
					vehicles.add(vehicle);
				}
			}
		}
	}

	public static class Vehicle {
		public String name;
		public String key;
	}
}
