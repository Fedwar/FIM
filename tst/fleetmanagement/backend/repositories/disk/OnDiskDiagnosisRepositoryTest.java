package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.diagnosis.*;
import fleetmanagement.backend.diagnosis.ErrorCategory;
import fleetmanagement.backend.diagnosis.VersionInfo.VersionType;
import fleetmanagement.backend.repositories.exception.DiagnosisDuplicationException;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OnDiskDiagnosisRepositoryTest {
    private OnDiskDiagnosisRepository tested;
    private TestScenarioPrefilled scenario;
    private File repositoryDir;
    private UUID vehicleId;

    @Rule
    public TempFileRule tempFolder = new TempFileRule();

    @Before
    public void setup() {
        repositoryDir = tempFolder;
        scenario = new TestScenarioPrefilled();
        tested = new OnDiskDiagnosisRepository(repositoryDir, scenario.diagnosisHistoryRepository);
    }

    @After
    public void teardown() {
        repositoryDir.delete();
    }

    @Test
    public void returnsNullForNonExistantDiagnosis()  {
        UUID vehicleId = UUID.randomUUID();

        assertNull(tested.tryFindByVehicleId(vehicleId));
    }

    @Test
    public void storesDiagnosisInVehicleFolder() throws Exception {
        UUID vehicleId = UUID.randomUUID();
        tested.insert(new Diagnosis(vehicleId));

        assertNotNull(tested.tryFindByVehicleId(vehicleId));
        assertTrue(new File(repositoryDir, vehicleId.toString()).exists());
        assertTrue(new File(repositoryDir, vehicleId.toString() + "/diagnosis.xml").exists());
    }

    @Test
    public void updatesDiagnosisInMemory() {
        UUID vehicleId = UUID.fromString("c0f55f62-b990-47bc-8caa-f42313669948");
        Diagnosis original = new Diagnosis(vehicleId);
        tested.insert(original);

        tested.update(vehicleId, diagnosis -> {
            diagnosis.setLastUpdated(ZonedDateTime.now());
        });

        assertNull(original.getLastUpdated());
        assertNotNull(tested.tryFindByVehicleId(original.getVehicleId()).getLastUpdated());
    }

    @Test
    public void updateSavesDiagnosisOnDisk() {
        UUID vehicleId = UUID.fromString("c0f55f62-b990-47bc-8caa-f42313669948");
        tested.insert(new Diagnosis(vehicleId));
        assertNull(tested.tryFindByVehicleId(vehicleId).getLastUpdated());

        tested.update(vehicleId, diagnosis -> {
            diagnosis.setLastUpdated(ZonedDateTime.now());
        });

        tested = new OnDiskDiagnosisRepository(repositoryDir, scenario.diagnosisHistoryRepository);
        tested.loadFromDisk();

        assertNotNull(tested.tryFindByVehicleId(vehicleId).getLastUpdated());
    }

    @Test
    public void deletesPreviouslyStoredDiagnosis() {
        UUID vehicleId = UUID.randomUUID();
        Diagnosis toDelete = new Diagnosis(vehicleId);
        tested.insert(toDelete);

        tested.delete(toDelete.getVehicleId());

        assertNull(tested.tryFindByVehicleId(vehicleId));
        assertFalse(new File(repositoryDir, vehicleId.toString() + "/diagnosis.xml").exists());
    }

    @Test
    public void noException_WhenXmlFileIsEmpty() throws Exception {
        TempFile folder = tempFolder.newFolder(UUID.randomUUID().toString());
        String xmlFile = tested.getXmlFile(folder).file().getName();
        folder.newFile(xmlFile);

        tested.loadFromDisk();
    }

    @Test(expected = DiagnosisDuplicationException.class)
    public void throwsExceptionOnReceivingDuplicateDiagnosis() {
        UUID vehicleId = UUID.fromString("c0f55f62-b990-47bc-8caa-f42313669948");
        Diagnosis diagnosis1 = new Diagnosis(vehicleId);
        Diagnosis diagnosis2 = new Diagnosis(vehicleId);

        tested.insert(diagnosis1);
        tested.insert(diagnosis2);
    }

    @Test
    public void migratesDeviceHistoryOnLoad() throws IOException {
        tested = new OnDiskDiagnosisRepository(repositoryDir, new DiagnosisHistorySQLiteRepository(repositoryDir));

        UUID uuid = UUID.randomUUID();
        File legacyFile = TestFiles.find("legacy-database-files/diagnosisWithHistory.xml");
        FileUtils.copyFile(legacyFile, new File(tempFolder.newFolder(uuid.toString()), "diagnosis.xml"));
        tested.loadFromDisk();
        Diagnosis diagnosis = tested.tryFindByVehicleId(uuid);

        List<StateEntry> history = tested.getDiagnosedDeviceHistory(uuid, "66833");

        for (DiagnosedDevice device : diagnosis.getDevices()) {
            assertEquals(0, device.getErrorHistory().getEntries().size());
        }

        assertEquals(3, history.size());
        assertEquals("2018-05-01T18:25:43+03:00[Europe/Moscow]", history.get(0).start.toString());
        assertEquals("2018-06-01T18:25:43+03:00[Europe/Moscow]", history.get(0).end.toString());
        assertEquals("-1", history.get(0).code);
        assertEquals(ErrorCategory.ERROR, history.get(0).category);
        assertEquals("Timeout", history.get(0).message.get(Locale.ENGLISH));
        assertEquals("Zeituberschreitung", history.get(0).message.get(Locale.GERMAN));
    }


    @Test
    public void properlyPersistsDiagnosis() {
        UUID vehicleId = UUID.randomUUID();
        Diagnosis saved = newDiagnosis(vehicleId);
        tested.insert(saved);

        tested = new OnDiskDiagnosisRepository(repositoryDir, scenario.diagnosisHistoryRepository);
        tested.loadFromDisk();

        Diagnosis loaded = tested.tryFindByVehicleId(vehicleId);
        assertNotNull(loaded);
        assertEquals(saved.getVehicleId(), loaded.getVehicleId());
        assertEquals(saved.getLastUpdated(), loaded.getLastUpdated());
        assertEquals(saved.getDevices().size(), loaded.getDevices().size());

        DiagnosedDevice savedComponent = saved.getDevice("id");
        DiagnosedDevice loadedComponent = loaded.getDevice("id");
        assertEquals(savedComponent.getId(), loadedComponent.getId());
        assertEquals(savedComponent.getLocation(), loadedComponent.getLocation());
        assertEquals(savedComponent.getName(), loadedComponent.getName());
        assertEquals(savedComponent.getType(), loadedComponent.getType());
        assertEquals(savedComponent.getStatus(), loadedComponent.getStatus());
        assertEquals(savedComponent.getCurrentState().size(), loadedComponent.getCurrentState().size());
        assertEquals(savedComponent.isDisabled(), loadedComponent.isDisabled());
        assertEquals("1.0", loadedComponent.getVersion(VersionType.Software));
        assertNull(loadedComponent.getVersion(VersionType.Fontware));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void doesNotCrashWhenModifyingNonExistantDiagnosis() {
        Consumer<Diagnosis> updateCall = mock(Consumer.class);

        tested.update(UUID.randomUUID(), updateCall);

        verify(updateCall).accept(null);
    }

    Diagnosis newDiagnosis(UUID vehicleId) {
        StateEntry stateEntry1 = new StateEntry(null, null, "-1", ErrorCategory.FATAL, new LocalizedString("Missing"));
        StateEntry stateEntry2 = new StateEntry(null, null, "6", ErrorCategory.ERROR, new LocalizedString("Error"));
        ErrorHistory errorHistory = new ErrorHistory(asList(
                stateEntry1.endingAt(ZonedDateTime.now().minusDays(10))
                , stateEntry2.endingAt(ZonedDateTime.now().minusDays(10)
                )
        ));
        VersionInfo versions = new VersionInfo("1.0", null);

        DiagnosedDevice diagnosedDevice = new DiagnosedDevice("id"
                , "location"
                , new LocalizedString("name")
                , "type"
                , DeviceStatus.DEFECT.toString()
                , asList(stateEntry1, stateEntry2)
                , true
                , versions
                , errorHistory);

        return new Diagnosis(vehicleId, ZonedDateTime.now(), asList(diagnosedDevice));
    }

}
