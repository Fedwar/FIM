package fleetmanagement.backend.vehiclecommunication.upload;

import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.*;

import com.google.gson.Gson;

import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.packages.PackageTypeRepository;
import fleetmanagement.backend.packages.sync.PackageSyncService;
import fleetmanagement.backend.tasks.TaskRepository;
import fleetmanagement.backend.vehicles.*;
import fleetmanagement.backend.vehicles.LiveInformation.*;
import fleetmanagement.config.Licence;
import gsp.util.DoNotObfuscate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class VehiclePositionUploadListener extends TypicalVehicleUploadListener {

	private static final Charset UTF8 = Charset.forName("UTF-8");
	private final Gson gson = new Gson();

	public VehiclePositionUploadListener(@Autowired VehicleRepository vehicles) {
		super(vehicles);
	}
	
	@Override
	public boolean canHandleUploadedFile(String filename) {
		return filename.equals("position.json");
	}
	
	@Override
	public void onFileUploaded(Vehicle sender, String filename, byte[] fileContent) {		
		VehicleInfo info = gson.fromJson(new String(fileContent, UTF8), VehicleInfo.class);
		
		Position position = null; 
		if (info.latitude != null && info.longitude != null)
			position = new Position(info.latitude, info.longitude);
		
		List<NextStation> nextStations = new ArrayList<>();
		if (info.nextStations != null) {
			for (NextStationInfo station : info.nextStations) {
				nextStations.add(new LiveInformation.NextStation(station.name, station.plannedArrival, station.estimatedArrival));
			}
		}
		
		sender.lastSeen = ZonedDateTime.now();
		sender.liveInformation = new LiveInformation(position, info.start, info.destination, info.trainType, info.tripNumber, nextStations, ZonedDateTime.now());
	}
	
	@DoNotObfuscate
	public class VehicleInfo {
		Double latitude;
		Double longitude;
		String start;
		String destination;
		String trainType;
		String tripNumber;
		List<NextStationInfo> nextStations;
	}
	
	@DoNotObfuscate
	public class NextStationInfo {
		String name;
		String plannedArrival;
		String estimatedArrival;
	}
}