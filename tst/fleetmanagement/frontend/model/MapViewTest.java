package fleetmanagement.frontend.model;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;
import java.util.*;

import fleetmanagement.frontend.UserSession;
import org.junit.*;

import fleetmanagement.backend.vehicles.*;
import fleetmanagement.backend.vehicles.LiveInformation.*;
import fleetmanagement.test.*;


public class MapViewTest {
	
	private TestScenarioPrefilled scenario;
	private UserSession request;
	
	@Before
	public void setup() {
		scenario = new TestScenarioPrefilled();
		request = new SessionStub();
	}
	
	@Test
	public void showsVehiclePositionsWithKnownPositions() {
		scenario.vehicle1.liveInformation = new LiveInformation(new Position(123, 456), null, null, null, null, Collections.emptyList(), ZonedDateTime.now());
		MapViewModel tested = createMapView();
		assertEquals(1, tested.vehicles.size());
		MapViewModel.Vehicle v = tested.vehicles.get(0);
		assertEquals("123.000000", v.latitude);
		assertEquals("456.000000", v.longitude);
		assertEquals(scenario.vehicle1.getName(), v.name);
		assertEquals(scenario.vehicle1.id.toString(), v.id);
	}
	
	@Test
	public void showsRouteInformation() {
		scenario.vehicle1.liveInformation = new LiveInformation(new Position(123, 456), "Berlin", "Mьnchen", "RE", "12345", Collections.emptyList(), ZonedDateTime.now());
		MapViewModel tested = createMapView();
		assertEquals(1, tested.vehicles.size());
		MapViewModel.Vehicle v = tested.vehicles.get(0);
		assertEquals("RE 12345", v.typeAndTrip);
		assertEquals("Berlin - Mьnchen", v.startDestination);
	}

	@Test
	public void showsNextStations() {
		List<NextStation> nextStations = Arrays.asList(new NextStation("Leipzig", "10:00", "10:03"), new NextStation("Nьrnberg", "11:00", null));
		scenario.vehicle1.liveInformation = new LiveInformation(new Position(123, 456), null, null, null, null, nextStations, ZonedDateTime.now());
		MapViewModel tested = createMapView();
		MapViewModel.Vehicle v = tested.vehicles.get(0);
		assertEquals(2, v.nextStations.size());
		assertEquals("Leipzig", v.nextStations.get(0).name);
		assertEquals("10:00", v.nextStations.get(0).plannedArrival);
		assertEquals(Integer.valueOf(3), v.nextStations.get(0).estimatedDelay);
		assertNull(v.nextStations.get(1).estimatedDelay);
	}

	@Test
	public void showsNoDelay_WhenPlannedArrival_OrEstimatedArrival_Unknown() {
		List<NextStation> nextStations = Arrays.asList(new NextStation("Nьrnberg", null, "10:00"),
				new NextStation("Nьrnberg", "10:00", null));
		scenario.vehicle1.liveInformation = new LiveInformation(new Position(123, 456), null, null, null, null, nextStations, ZonedDateTime.now());
		MapViewModel tested = createMapView();
		MapViewModel.Vehicle v = tested.vehicles.get(0);
		assertNull("", v.nextStations.get(0).estimatedDelay);
		assertNull("", v.nextStations.get(1).estimatedDelay);
	}

	@Test
	public void correctlyEstimatesDelaysAroundMidnight() {
		List<NextStation> nextStations = Arrays.asList(new NextStation("", "23:59", "00:00"), new NextStation("", "00:00", "23:59"));
		scenario.vehicle1.liveInformation = new LiveInformation(new Position(123, 456), null, null, null, null, nextStations, ZonedDateTime.now());
		MapViewModel tested = createMapView();
		MapViewModel.Vehicle v = tested.vehicles.get(0);
		assertEquals(2, v.nextStations.size());
		assertEquals(Integer.valueOf(1), v.nextStations.get(0).estimatedDelay);
		assertEquals(Integer.valueOf(-1), v.nextStations.get(1).estimatedDelay);
	}
	
	@Test
	public void returnsNoStringsOnMissingLiveInfo() {
		scenario.vehicle1.liveInformation = new LiveInformation(new Position(123, 456), null, null, null, null, Collections.emptyList(), ZonedDateTime.now());
		MapViewModel tested = createMapView();
		MapViewModel.Vehicle v = tested.vehicles.get(0);
		assertNull(v.typeAndTrip);
		assertNull(v.startDestination);
	}
	
	@Test
	public void fillsLiveInfoTimestampDependentFields() {
		scenario.vehicle1.liveInformation = new LiveInformation(new Position(0, 0), null, null, null, null, Collections.emptyList(), ZonedDateTime.now().minusDays(1));
		MapViewModel tested = createMapView();
		assertEquals("Updated: 1 day ago", tested.vehicles.get(0).lastUpdated);
		assertEquals(24 * 60 * 60, tested.vehicles.get(0).secondsSinceLastUpdate);
	}
	
	@Test
	public void centersMapOnVehicle() {
		MapViewModel tested = createMapView(scenario.vehicle1);
		assertNull(tested.mapCenter);
		
		scenario.vehicle1.liveInformation = new LiveInformation(new Position(1, 2), null, null, null, null, Collections.emptyList(), ZonedDateTime.now().minusMinutes(1));
		tested = createMapView(scenario.vehicle1);
		assertEquals("1.000000", tested.mapCenter.latitude);
		assertEquals("2.000000", tested.mapCenter.longitude);
	}

	private MapViewModel createMapView() {
		return new MapViewModel(scenario.vehicleRepository, request);
	}

	private MapViewModel createMapView(Vehicle vehicle) {
		return new MapViewModel(scenario.vehicleRepository, vehicle, request);
	}
}
