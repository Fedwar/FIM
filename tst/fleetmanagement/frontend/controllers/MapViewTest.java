package fleetmanagement.frontend.controllers;

import fleetmanagement.backend.vehicles.LiveInformation;
import fleetmanagement.backend.vehicles.LiveInformation.Position;
import fleetmanagement.frontend.model.MapAndGroups;
import fleetmanagement.frontend.webserver.ModelAndView;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class MapViewTest {
	private MapView tested;
	private TestScenarioPrefilled scenario;
	
	@Before
	public void setup() {
		scenario = new TestScenarioPrefilled();
		scenario.vehicle1.liveInformation = new LiveInformation(new Position(1, 1), null, null, null, null, Collections.emptyList(), ZonedDateTime.now());
		tested = new MapView(new SessionStub(), scenario.vehicleRepository, scenario.groupRepository, scenario.licence);
	}

	@Test
	public void rendersMapForAllVehicles() {
		ModelAndView<MapAndGroups> vm = tested.getMapUI();

		assertEquals("map.html", vm.page);
		assertEquals(1, vm.viewmodel.mapView.vehicles.size());
	}

	@Test
	public void rendersMapForSingleVehicle() {
		ModelAndView<MapAndGroups> vm = tested.getMapCenteredOnVehicle(scenario.vehicle1.id.toString());

		assertEquals("map.html", vm.page);
		assertEquals(1, vm.viewmodel.mapView.vehicles.size());
	}

	@Test
	public void mapNotLicenced() {
		scenario.licence.mapAvailable = false;

		ModelAndView<MapAndGroups> vm = tested.getMapCenteredOnVehicle(scenario.vehicle1.id.toString());
		assertEquals("map-not-licenced.html", vm.page);

		vm = tested.getMapUI();
		assertEquals("map-not-licenced.html", vm.page);

		vm = tested.byGroup("group");
		assertEquals("map-not-licenced.html", vm.page);

	}
}
