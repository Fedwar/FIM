package fleetmanagement.backend.vehicles;

import org.apache.commons.lang3.ObjectUtils;

import java.time.ZonedDateTime;
import java.util.*;

public class LiveInformation {
	public final Position position;
	public final String startStation;
	public final String destinationStation;
	public final String trainType;
	public final String tripNumber;
	public final List<NextStation> nextStations;
	public final ZonedDateTime received;

	public LiveInformation(Position position, String startStation, String destinationStation, String trainType, String tripNumber, List<NextStation> nextStations, ZonedDateTime received) {
		this.position = position;
		this.startStation = startStation;
		this.destinationStation = destinationStation;
		this.trainType = trainType;
		this.tripNumber = tripNumber;
		this.nextStations = Collections.unmodifiableList(nextStations);
		this.received = ObjectUtils.defaultIfNull(received, ZonedDateTime.now());
	}

	public boolean hasAnyTripInfo() {
		return startStation != null || destinationStation != null || trainType != null || tripNumber != null;
	}
	
	public static class Position {
		public final double latitude;
		public final double longitude;
		
		public Position(double lat, double lon) {
			this.latitude = lat;
			this.longitude = lon;
		}
		
		@Override
		public int hashCode() {
			return (int) (latitude + longitude) * 1000;
		}
		
		@Override
		public boolean equals(Object obj) {
			Position other = (Position)obj;
			return other.latitude == this.latitude && other.longitude == this.longitude;
		}
	}
	
	public static class NextStation {
		
		public final String name;
		public final String plannedArrival;
		public final String estimatedArrival;

		public NextStation(String name, String plannedArrival, String estimatedArrival) {
			this.name = name;
			this.plannedArrival = plannedArrival;
			this.estimatedArrival = estimatedArrival;
		}
		
	}
}
