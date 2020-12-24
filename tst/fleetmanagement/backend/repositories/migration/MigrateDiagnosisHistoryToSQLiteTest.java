package fleetmanagement.backend.repositories.migration;

import fleetmanagement.TempFileRule;
import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

public class MigrateDiagnosisHistoryToSQLiteTest {

    private MigrateDiagnosisHistoryToSQLite tested;
    private TestScenario scenario;
    ErrorHistory errorHistory;


    @Rule
    public TempFileRule tempFolder = new TempFileRule();
    private StateEntry stateEntry1;
    private StateEntry stateEntry2;

    @Before
    public void setup() {
        scenario = new TestScenario();

        stateEntry1 = new StateEntry(ZonedDateTime.now().plusDays(1), null, "-1", ErrorCategory.FATAL, new LocalizedString("Missing"));
        stateEntry2 = new StateEntry(ZonedDateTime.now(), null, "6", ErrorCategory.ERROR, new LocalizedString("Error"));
        errorHistory = new ErrorHistory(asList(
                stateEntry1.endingAt(ZonedDateTime.now().plusDays(10))
                , stateEntry2.endingAt(ZonedDateTime.now().plusDays(10)
                )
        ));

    }

    @Test
    public void migrationClearsHistory() {

        DiagnosedDevice diagnosedDevice1 = newDevice("diagnosedDevice1", stateEntry1, stateEntry2);
        DiagnosedDevice diagnosedDevice2 = newDevice("diagnosedDevice2", stateEntry1, stateEntry2);
        Diagnosis diagnosis = newDiagnosis(diagnosedDevice1, diagnosedDevice2);

        tested = new MigrateDiagnosisHistoryToSQLite(scenario.diagnosisHistoryRepository, Arrays.asList(diagnosis));
        tested.migrate();

        assertEquals(0, diagnosedDevice1.getErrorHistory().getEntries().size());
        assertEquals(0, diagnosedDevice2.getErrorHistory().getEntries().size());
    }

    @Test
    public void migrationAddsHistoryToSQLite() {
        DiagnosedDevice diagnosedDevice1 = newDevice("diagnosedDevice1", stateEntry1, stateEntry2);
        DiagnosedDevice diagnosedDevice2 = newDevice("diagnosedDevice2", stateEntry1, stateEntry2);
        Diagnosis diagnosis1 = newDiagnosis(diagnosedDevice1);
        Diagnosis diagnosis2 = newDiagnosis(diagnosedDevice2);

        tested = new MigrateDiagnosisHistoryToSQLite(scenario.diagnosisHistoryRepository, Arrays.asList(diagnosis1, diagnosis2));
        tested.migrate();

        verify(scenario.diagnosisHistoryRepository).addHistory(diagnosis1.getVehicleId(), "diagnosedDevice1", asList(stateEntry1, stateEntry2));
        verify(scenario.diagnosisHistoryRepository).addHistory(diagnosis2.getVehicleId(), "diagnosedDevice2", asList(stateEntry1, stateEntry2));
    }

    Diagnosis newDiagnosis(DiagnosedDevice... devices) {
        return new Diagnosis(UUID.randomUUID(), ZonedDateTime.now(), asList(devices));
    }

    DiagnosedDevice newDevice(String id, StateEntry... states) {
        VersionInfo versions = new VersionInfo("1.0", null);
        errorHistory = new ErrorHistory(asList(stateEntry1, stateEntry2));
        return new DiagnosedDevice(id
                , "location"
                , new LocalizedString("name")
                , "type"
                , DeviceStatus.DEFECT.toString()
                , asList(stateEntry1)
                , true
                , versions
                , errorHistory);

    }


}