package fleetmanagement.backend.vehiclecommunication;

import static org.junit.Assert.*;

import java.time.ZonedDateTime;

import org.junit.*;

import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class LoginResourceTest {

	private LoginResource tested;
	private TestScenario scenario;

	@Before
	public void setup() throws Exception {
		scenario = new TestScenario();
		tested = new LoginResource(scenario.vehicleRepository, 1);
	}

	@Test
	public void firstLoginCreatesVehicle() throws Exception {
		tested.login("new-uic", "1.2.34567.0", "additional_uic");
		assertEquals(1, scenario.vehicleRepository.listAll().size());
		Vehicle newVehicle = scenario.vehicleRepository.listAll().get(0);
		assertEquals("new-uic", newVehicle.uic);
		assertEquals("1.2.34567.0", newVehicle.clientVersion);
		assertEquals("additional_uic", newVehicle.additional_uic);
	}

	@Test
	public void loginWithoutAdditionalUid() throws Exception {
		tested.login("new-uic", "1.2.34567.0", null);
		assertEquals(1, scenario.vehicleRepository.listAll().size());
		Vehicle newVehicle = scenario.vehicleRepository.listAll().get(0);
		assertEquals("new-uic", newVehicle.uic);
		assertEquals("1.2.34567.0", newVehicle.clientVersion);
		assertNull(newVehicle.additional_uic);
	}

	@Test
	public void responseStatus403_WhenLicencedVehicleLimitExceeded() throws Exception {
		scenario.licence.maxVehicleCount = 0;
		Response response = tested.login("new-uic", "1.2.34567.0", null);

		assertEquals(  Response.Status.FORBIDDEN.getStatusCode(), response.getStatus() );
	}

	@Test
	public void subsequentLoginsUpdateLastSeenDateAndClientVersion() throws Exception {
		ZonedDateTime previouslySeen = ZonedDateTime.now().minusDays(2); 
		Vehicle v = scenario.addVehicle();
		v.lastSeen = previouslySeen;

		tested.login(v.uic, "1.3.45678.0", "additional_uic");

		Vehicle vehicle = scenario.vehicleRepository.tryFindById(v.id);
		assertEquals(1, scenario.vehicleRepository.listAll().size());
		assertNotEquals(previouslySeen, vehicle.lastSeen);
		assertEquals("1.3.45678.0", vehicle.clientVersion);
	}

}