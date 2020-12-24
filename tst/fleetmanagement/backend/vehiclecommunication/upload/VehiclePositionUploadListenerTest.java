package fleetmanagement.backend.vehiclecommunication.upload;

import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import fleetmanagement.backend.packages.sync.PackageSyncService;
import org.junit.*;

import fleetmanagement.backend.vehicles.*;
import fleetmanagement.test.TestScenario;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class VehiclePositionUploadListenerTest {

    private TestScenario scenario;
    private VehiclePositionUploadListener tested;
    private Vehicle vehicle;

    @Before
    public void setup() throws Exception {
        scenario = new TestScenario();
        vehicle = scenario.addVehicle();
        tested = new VehiclePositionUploadListener(scenario.vehicleRepository);
    }

    @Test
    public void setsVehicleInfoFromUploadedJson() {
        uploadJson("{ \"latitude\": \"52.442701\", \"longitude\": \"13.328645\", \"start\": \"Berlin\", \"destination\": \"M�nchen\", \"trainType\": \"ICE\", \"tripNumber\": \"12345\" }");

        LiveInformation liveInfo = vehicle.liveInformation;
        assertEquals(liveInfo.position, new LiveInformation.Position(52.442701, 13.328645));
        assertEquals(liveInfo.startStation, "Berlin");
        assertEquals(liveInfo.destinationStation, "M�nchen");
        assertEquals(liveInfo.trainType, "ICE");
        assertEquals(liveInfo.tripNumber, "12345");
        assertEquals(0, liveInfo.nextStations.size());

        uploadJson("{ }");

        liveInfo = vehicle.liveInformation;
        assertNull(liveInfo.position);
        assertNull(liveInfo.startStation);
        assertNull(liveInfo.destinationStation);
        assertNull(liveInfo.trainType);
        assertNull(liveInfo.tripNumber);
        assertEquals(0, liveInfo.nextStations.size());
    }

    @Test
    public void setsNextStationsFromUploadedJson() {
        uploadJson("{ \"nextStations\" : [ {\"name\": \"M�nchen\", \"plannedArrival\": \"20:00\", \"estimatedArrival\" : \"21:00\" }, {\"name\": \"Berlin\", \"plannedArrival\": \"23:00\" } ] }");
        LiveInformation liveInfo = vehicle.liveInformation;
        assertEquals(2, liveInfo.nextStations.size());
        assertEquals("M�nchen", liveInfo.nextStations.get(0).name);
        assertEquals("20:00", liveInfo.nextStations.get(0).plannedArrival);
        assertEquals("21:00", liveInfo.nextStations.get(0).estimatedArrival);
        assertEquals("Berlin", liveInfo.nextStations.get(1).name);
        assertEquals("23:00", liveInfo.nextStations.get(1).plannedArrival);
        assertNull(liveInfo.nextStations.get(1).estimatedArrival);

        uploadJson("{ }");
        assertEquals(0, vehicle.liveInformation.nextStations.size());
    }

    @Test
    public void determinesIfFileCanBeHandlesd() {
        assertFalse(tested.canHandleUploadedFile("unknown-stuff.data"));
        assertTrue(tested.canHandleUploadedFile("position.json"));
    }

    @Test
    public void setsLiveInfoTimestampOnIncomingLiveInfo() {
        ZonedDateTime testStart = ZonedDateTime.now();
        uploadJson("{ \"nextStations\" : [ {\"name\": \"M�nchen\", \"plannedArrival\": \"20:00\", \"estimatedArrival\" : \"21:00\" }, {\"name\": \"Berlin\", \"plannedArrival\": \"23:00\" } ] }");
        LiveInformation liveInfo = vehicle.liveInformation;
        assertFalse(liveInfo.received.isBefore(testStart));
        assertTrue((int) testStart.until(liveInfo.received, ChronoUnit.MILLIS) < 500);
    }

    private void uploadJson(String json) {
        String filename = "position.json";
        tested.onFileUploaded(vehicle, filename, json.getBytes(Charset.forName("UTF-8")));
    }
}
