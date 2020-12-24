package fleetmanagement.frontend.model;

import java.time.*;

import fleetmanagement.backend.vehicles.LiveInformation;
import fleetmanagement.frontend.*;
import fleetmanagement.frontend.transformers.DurationFormatter;

public class LiveInformationViewModel {
	
	public final String startDestination;
	public final String tripDescription;
	public final String routeSummary;
	public final String lastUpdatedAgo;
	
	public LiveInformationViewModel(LiveInformation live, UserSession request) {
		this.startDestination = composeStartDestination(live);
		this.tripDescription = composeTripDescription(live);
		this.routeSummary = composeRouteDescription(tripDescription, startDestination);
		this.lastUpdatedAgo = composeLastUpdatedString(live, request);
	}

	private static String composeRouteDescription(String tripDescription, String startDestination) {
		if (tripDescription == null)
			return null;
		
		if (startDestination == null)
			return tripDescription;

		return tripDescription + ": " + startDestination;
	}

	private static String composeStartDestination(LiveInformation live) {
		if (live.destinationStation == null)
			return null;
		
		if (live.startStation == null)
			return live.destinationStation;
		
		return live.startStation + " - " + live.destinationStation;
	}

	private static String composeTripDescription(LiveInformation live) {
		if (live.trainType == null || live.tripNumber == null)
			return null;
		
		return live.trainType + " " + live.tripNumber;
	}
	
	private static String composeLastUpdatedString(LiveInformation live, UserSession request) {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime received = live.received;		
		String lastUpdate = received == null ? I18n.get(request, "unknown") : DurationFormatter.asHumanReadable(Duration.between(now, received), request.getLocale());
		return I18n.get(request, "updated_ago", lastUpdate);
	}
}
