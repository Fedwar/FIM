package fleetmanagement.frontend.model;

import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.ErrorCategory;
import fleetmanagement.backend.diagnosis.LocalizedString;
import fleetmanagement.backend.diagnosis.StateEntry;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DiagnosedDeviceModelTest {

    private DiagnosedDeviceModel tested;
    private UserSession session;
    private TestScenarioPrefilled scenario;
    private Vehicle vehicle;
    private StateEntry stateEntry1;
    private StateEntry stateEntry2;
    private StateEntry stateEntry3;

    @Before
    public void setup() {
        stateEntry1 = new StateEntry(ZonedDateTime.now().plusDays(1), null, "-1", ErrorCategory.FATAL, new LocalizedString("Missing"));
        stateEntry2 = new StateEntry(ZonedDateTime.now(), null, "6", ErrorCategory.ERROR, new LocalizedString("Error"));
        stateEntry3 = new StateEntry(ZonedDateTime.now().plusDays(2), null, "0", ErrorCategory.OK, new LocalizedString("Ok"));
        session = new SessionStub();
        scenario = new TestScenarioPrefilled();
        vehicle = scenario.vehicle2;
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void modelHasAllDeviceHistory() {
        DiagnosedDevice diagnosedDevice = new DiagnosedDevice("device1");
        scenario.addDiagnosis(vehicle, Collections.singletonList(diagnosedDevice));
        List<StateEntry> history = Arrays.asList(stateEntry1, stateEntry2, stateEntry3);
        when(scenario.diagnosisHistoryRepository.getHistory(vehicle.id, diagnosedDevice.getId())).thenReturn(history);

        tested = new DiagnosedDeviceModel(scenario.diagnosisRepository, vehicle.id, diagnosedDevice.getId(), session, scenario.licence);

        assertEquals(history.stream().map(s -> s.category.toString()).collect(Collectors.toList()),
                tested.errors.stream().map(s -> s.category).collect(Collectors.toList()));
        assertEquals(history.stream().map(s -> s.start).collect(Collectors.toList()),
                tested.errors.stream().map(s -> s.start).collect(Collectors.toList()));
        assertEquals(history.stream().map(s -> s.code).collect(Collectors.toList()),
                tested.errors.stream().map(s -> s.code).collect(Collectors.toList()));
    }

}