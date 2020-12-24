package fleetmanagement.frontend.model;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import fleetmanagement.backend.vehicles.*;
import fleetmanagement.frontend.UserSession;

public class MapViewModel {

	public final List<Vehicle> vehicles = new ArrayList<>();
	public final Vehicle mapCenter;

	public MapViewModel(VehicleRepository vehicleRepository, UserSession request) {
		loadVehicles(vehicleRepository.listAll(), request);
		mapCenter = null;
	}

	public MapViewModel(VehicleRepository vehicleRepository, String groupId, UserSession request) {
		loadVehicles(vehicleRepository.listByGroup(groupId), request);
		mapCenter = null;
	}

	public MapViewModel(VehicleRepository vehicleRepository, fleetmanagement.backend.vehicles.Vehicle vehicle, UserSession request) {
		loadVehicles(vehicleRepository.listAll(), request);
		mapCenter = vehicles.stream().filter(v -> v.id.equals(vehicle.id.toString())).findFirst().orElse(null);
	}

	private void loadVehicles(List<fleetmanagement.backend.vehicles.Vehicle> vehicleList, UserSession request) {
		for (fleetmanagement.backend.vehicles.Vehicle v : vehicleList) {
			if (v.liveInformation != null) {
				LiveInformation liveInfo = v.liveInformation;
				fleetmanagement.backend.vehicles.LiveInformation.Position pos = liveInfo.position;
				if (pos != null) {
					LiveInformationViewModel live = new LiveInformationViewModel(liveInfo, request);
					Vehicle onMap = new Vehicle();
					onMap.latitude = String.format(Locale.ROOT, "%.6f", pos.latitude);
					onMap.longitude = String.format(Locale.ROOT, "%.6f", pos.longitude);
					onMap.name = v.getName();
					onMap.id = v.id.toString();
					onMap.lastUpdated = live.lastUpdatedAgo;
					onMap.secondsSinceLastUpdate = (int) liveInfo.received.until(ZonedDateTime.now(), ChronoUnit.SECONDS);
					onMap.typeAndTrip = live.tripDescription;
					onMap.startDestination = live.startDestination;
					onMap.nextStations = new ArrayList<>();
					for (fleetmanagement.backend.vehicles.LiveInformation.NextStation s : liveInfo.nextStations) {
						NextStation station = new NextStation();
						station.name = s.name;
						station.plannedArrival = s.plannedArrival;
						if (s.estimatedArrival != null && s.plannedArrival != null)
							station.estimatedDelay = diffMinutes(s.estimatedArrival, s.plannedArrival);
						onMap.nextStations.add(station);
					}
					vehicles.add(onMap);
				}
			}
		}
	}

	private int diffMinutes(String time, String referenceTime) {
		int rawDiff = toMinutesOfDay(time) - toMinutesOfDay(referenceTime);
		int oneDay = 24 * 60;

		if (Math.abs(rawDiff) > Math.abs(oneDay / 2))
			return rawDiff - (int) Math.signum(rawDiff) * oneDay;
		return rawDiff;
	}

	private int toMinutesOfDay(String time) {
		String[] hm = time.split(":");
		return Integer.parseInt(hm[0]) * 60 + Integer.parseInt(hm[1]);
	}

	public static class Vehicle {
		public String name;
		public String id;
		public String latitude;
		public String longitude;
		public String typeAndTrip;
		public String startDestination;
		public String lastUpdated;
		public int secondsSinceLastUpdate;
		public List<NextStation> nextStations;
	}

	public static class NextStation {
		public String name;
		public String plannedArrival;
		public Integer estimatedDelay;
	}
}
