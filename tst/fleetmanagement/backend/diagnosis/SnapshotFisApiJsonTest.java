package fleetmanagement.backend.diagnosis;

import com.google.gson.Gson;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.test.TestScenario;
import gsp.configuration.LocalFiles;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static org.junit.Assert.*;

public class SnapshotFisApiJsonTest {

    private final Gson gson = new Gson();
    private TestScenario scenario;
    private Vehicle vehicle;

    @Before
    public void setup() throws Exception {
        scenario = new TestScenario();
        vehicle = scenario.addVehicle();
    }
    @Test
    public void parsesFromJson() throws IOException {
        File jsonFile = LocalFiles.find("test-resources/diagnonsis/diagnosis-fis-api.json");
        SnapshotFisApiJson json = gson.fromJson(FileUtils.readFileToString(jsonFile, "UTF-8"), SnapshotFisApiJson.class);

        assertEquals(3, json.components.size());
        assertEquals(2, json.components.get(0).status_history.size());

        SnapshotFisApiJson.ComponentSnapshotJson component1 = json.components.get(0);
        SnapshotFisApiJson.StateJson componentStatusHistory1 = component1.status_history.get(0);
        SnapshotFisApiJson.StateJson componentStatusHistory2 = component1.status_history.get(1);

        SnapshotFisApiJson.ComponentSnapshotJson component2 = json.components.get(1);
        SnapshotFisApiJson.ComponentSnapshotJson component3 = json.components.get(2);

        assertEquals("66833", component1.id);
        assertEquals("A128", component1.location);
        assertEquals("UKR2+", component1.name.get("en"));
        assertEquals("UKR2+", component1.name.get("de"));
        assertEquals("Defect", component1.status);
        assertEquals(-23, component1.states.get(0).code.longValue());
        assertEquals("CommunicationTimeout", component1.states.get(0).description.get("en"));
        assertEquals("Verbindungsfehler", component1.states.get(0).description.get("de"));
        assertEquals("UKR", component1.type);
        assertEquals(7, component1.versions.size());
        assertEquals("2.0.0.0", component1.versions.get("subKernel"));
        assertEquals(new Integer(-1), componentStatusHistory1.code);
        assertEquals("Timeout", componentStatusHistory1.description.get("en"));
        assertEquals("Zeituberschreitung", componentStatusHistory1.description.get("de"));
        assertEquals("2018-05-01T18:25:43Z", componentStatusHistory1.started);
        assertEquals("2018-06-01T18:25:43Z", componentStatusHistory1.ended);
        assertEquals("Timeout", componentStatusHistory2.description.get("en"));
        assertEquals("Zeituberschreitung", componentStatusHistory2.description.get("de"));
        assertEquals("2018-04-28T03:25:00Z", componentStatusHistory2.started);
        assertEquals("2018-04-29T17:12:00Z", componentStatusHistory2.ended);

        assertEquals("781ex", component2.id);
        assertEquals("Degraded", component2.status);
        assertEquals(null, component2.location);
        assertEquals("LED-Side (7)", component2.name.get("en"));
        assertEquals("LED Seite (7)", component2.name.get("de"));
        assertEquals(new Integer(-1), component2.states.get(0).code);
        assertEquals("BrokenLEDs", component2.states.get(0).description.get("en"));
        assertEquals("Defekte LEDs", component2.states.get(0).description.get("de"));
        assertEquals("Side LED Display", component2.type);
        assertEquals("LDIZ2AC8", component2.versions.get("software"));
        assertEquals("1.34A", component2.versions.get("font"));
        assertEquals(1, component2.status_history.size());

        assertEquals("53tx", component3.id);
        assertNull(component3.location);
        assertNull(component3.name);
        assertEquals("Works correctly", component3.status);
        assertEquals("Side LED Display", component3.type);
        assertNull(component3.versions);
        assertNull(component3.status_history);
    }

    @Test
    public void toSnapshot() throws IOException {
        File jsonFile = LocalFiles.find("test-resources/diagnonsis/diagnosis-fis-api.json");

        SnapshotFisApiJson json = gson.fromJson(FileUtils.readFileToString(jsonFile, "UTF-8"), SnapshotFisApiJson.class);

        Snapshot snapshot = json.toSnapshot(vehicle.id);

        assertEquals(3, snapshot.devices.size());

        DeviceSnapshot component1 = snapshot.devices.get(0);
        DeviceSnapshot component2 = snapshot.devices.get(1);
        DeviceSnapshot component3 = snapshot.devices.get(2);

        assertEquals("UKR2+", component1.name.get(Locale.ENGLISH));
        assertEquals("UKR2+", component1.name.get(Locale.GERMAN));
        assertEquals("Defect", component1.status);
        assertEquals("Degraded", component2.status);
        assertEquals("Works correctly", component3.status);

        assertEquals("ERROR", component1.states.get(0).type.name());
        assertEquals("WARNING", component2.states.get(0).type.name());


    }

}