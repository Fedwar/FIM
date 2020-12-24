package fleetmanagement.frontend.model;

import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.Diagnosis;
import fleetmanagement.backend.diagnosis.LocalizedString;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.test.SessionStub;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NotificationsModelTest {

    private TestScenarioPrefilled scenario;
    private UserSession session;

    @Before
    public void setup() {
        scenario = new TestScenarioPrefilled();
        session = new SessionStub();
    }

    @Test
    public void modelHasAllDiagnosedDeviceNames() {
        addDiagnosisWithDevices("device1","device2");
        addDiagnosisWithDevices("device3", "device4");
        List<String> names = Arrays.asList("device1", "device2", "device3", "device4");

        NotificationsModel tested = new NotificationsModel(session, scenario.licence, scenario.notificationRepository
                , scenario.diagnosisRepository, scenario.operationDataRepository, scenario.vehicleRepository
                , scenario.groupRepository, scenario.accountRepository);

        assertEquals(4, tested.deviceNames.size());
        assertTrue( tested.deviceNames.containsAll(names));
    }

    @Test
    public void modelHasAllIndicatorNames() {
        addOperationDataWithIndicators("ind1","ind2");
        addOperationDataWithIndicators("ind3", "ind4");
        List<String> names = Arrays.asList("ind1", "ind2", "ind3", "ind4");

        NotificationsModel tested = new NotificationsModel(session, scenario.licence, scenario.notificationRepository
                , scenario.diagnosisRepository, scenario.operationDataRepository, scenario.vehicleRepository
                , scenario.groupRepository, scenario.accountRepository);

        assertEquals(4, tested.indicatorNames.size());
        assertTrue( tested.indicatorNames.containsAll(names));
    }


    Diagnosis addDiagnosisWithDevices(String... names) {
        List<DiagnosedDevice> devices = new ArrayList<>();
        for (String name : names) {
            devices.add(new DiagnosedDevice("id", "location", new LocalizedString(name), "type", null
                    , null, true, null, null));
        }
        Diagnosis diagnosis = new Diagnosis(UUID.randomUUID(), ZonedDateTime.now(), devices);
        scenario.diagnosisRepository.insert(diagnosis);
        return diagnosis;
    }

    OperationData addOperationDataWithIndicators(String... names) {
        List<Indicator> indicators = new ArrayList<>();
        for (String name : names) {
            indicators.add(new Indicator(name, null, 1, null));
        }
        return scenario.addOperationData(UUID.randomUUID(), indicators.toArray(new Indicator[]{}) );
    }

}