package fleetmanagement.backend.diagnosis;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DiagnosedDeviceTest {


    @Test
    public void currentStateIsEmptyList_WhenNoStatesAndNoHistory() {
        DiagnosedDevice tested = new DiagnosedDevice("id", "location", new LocalizedString("name"), "type", null
                , null, true, null, null);
        List<StateEntry> currentState = tested.getCurrentState();

        assertNotNull(currentState);
        assertTrue(currentState.isEmpty());
    }

}