package fleetmanagement.backend.repositories.disk;

import fleetmanagement.TempFile;
import fleetmanagement.TempFileRule;
import fleetmanagement.TestFiles;
import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.test.TestScenarioPrefilled;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OnDiskOperationDataRepositoryTest {

    private OnDiskOperationDataRepository tested;
    private File repositoryDir;
    private TestScenarioPrefilled scenario;
    private UUID vehicleId;


    @Rule
    public TempFileRule tempDir = new TempFileRule();

    @Before
    public void setup() {
        scenario = new TestScenarioPrefilled();
        vehicleId = scenario.vehicle1.id;
        repositoryDir = tempDir;
        tested = new OnDiskOperationDataRepository(repositoryDir, scenario.operationDataHistoryRepository);
    }

    @Test
    public void insert()  {
        Indicator indicator = new Indicator("mileage", "km", 123);
        OperationData operationData = new OperationData(vehicleId, ZonedDateTime.now(), Arrays.asList(indicator));
        tested.insert(operationData);

        tested = new OnDiskOperationDataRepository(repositoryDir, scenario.operationDataHistoryRepository);
        tested.loadFromDisk();

        OperationData loaded = tested.tryFindById(vehicleId);
        assertEquals(operationData.vehicleId, loaded.vehicleId);
        assertEquals(operationData.updated, loaded.updated);
        assertEquals(operationData.indicators.size(), loaded.indicators.size());
    }

    @Test
    public void insertSavesOperationHistory() {
        Indicator indicator = new Indicator("mileage", "km", 123);
        OperationData operationData = new OperationData(vehicleId, ZonedDateTime.now(), Arrays.asList(indicator));
        tested.insert(operationData);

        verify(scenario.operationDataHistoryRepository, times(1))
                .addHistory(eq(vehicleId), any(List.class));

    }

    @Test(expected = UnsupportedOperationException.class)
    public void update() {
        tested.update(vehicleId, od -> {
            od.setIndicatorValue("mileage", "1000", "mile");
        });
    }

    @Test
    public void snaphotIntegrationCreatesOperationData() {
        assertNull(tested.tryFindById(vehicleId));
        integrateSnapshot(vehicleId);
        assertNotNull(tested.tryFindById(vehicleId));
    }


    @Test
    public void loadsLegacyXmlFile() throws IOException {
        UUID uuid = UUID.fromString("9fb1e180-2cc6-4245-9da6-10021b07708a");
        File legacyFile = TestFiles.find("legacy-database-files/operationDataWithHistory.xml");
        FileUtils.copyFile(legacyFile, new File(tempDir.newFolder(uuid.toString()), "operationData.xml"));
        tested.loadFromDisk();

        OperationData operationData = tested.tryFindById(uuid);
        assertNotNull(operationData.vehicleId);
        assertNotNull(operationData.updated);
        assertEquals(6, operationData.indicators.size());
    }

    @Test
    public void migratesIndicatorsHistoryOnLoad() throws IOException {
        tested = new OnDiskOperationDataRepository(repositoryDir, new OperationDataHistorySQLiteRepository(repositoryDir));

        UUID uuid = UUID.randomUUID();
        File legacyFile = TestFiles.find("legacy-database-files/operationDataWithHistory.xml");
        FileUtils.copyFile(legacyFile, new File(tempDir.newFolder(uuid.toString()), "operationData.xml"));
        tested.loadFromDisk();
        OperationData operationData = tested.tryFindById(uuid);

        List<History> dieseltankHistory = tested.getIndicatorHistory(operationData.vehicleId, "dieseltank1");
        List<History> clima_temp_a = tested.getIndicatorHistory(operationData.vehicleId, "clima_temp_A");
        List<History> display_powersafe = tested.getIndicatorHistory(operationData.vehicleId, "display_powersafe");

        assertEquals("2330", dieseltankHistory.get(0).value);
        assertEquals("2019-02-28T16:59:13.710+03:00[Europe/Moscow]", dieseltankHistory.get(0).timeStamp.toString());
        assertEquals(23.5, clima_temp_a.get(0).value);
        assertEquals("2019-02-28T16:59:13.710+03:00[Europe/Moscow]", clima_temp_a.get(0).timeStamp.toString());
        assertEquals(false, display_powersafe.get(0).value);
        assertEquals("2019-02-28T16:59:13.710+03:00[Europe/Moscow]", display_powersafe.get(0).timeStamp.toString());
    }


    @Test
    public void snaphotIntegrationPersistsOperationData() {
        OperationData operationData = integrateSnapshot(vehicleId);

        tested = new OnDiskOperationDataRepository(repositoryDir, scenario.operationDataHistoryRepository);
        tested.loadFromDisk();

        OperationData loaded = tested.listAll().get(0);
        assertEquals(1, tested.listAll().size());
        assertEquals(operationData.vehicleId, loaded.vehicleId);
        assertEquals(operationData.updated, loaded.updated);
        assertEquals(operationData.indicators.size(), loaded.indicators.size());

        Indicator loadedIndicator = loaded.indicators.get(0);
        Indicator indicator = operationData.indicators.get(0);

        assertEquals(indicator.id, loadedIndicator.id);
        assertEquals(indicator.value, loadedIndicator.value);
        assertEquals(indicator.unit, loadedIndicator.unit);
        assertEquals(indicator.updated, loadedIndicator.updated);
    }

    @Test
    public void snaphotIntegrationSavesOperationHistory() {
        integrateSnapshot(vehicleId);
        integrateSnapshot(vehicleId);

        verify(scenario.operationDataHistoryRepository, times(2))
                .addHistory(eq(vehicleId), any(List.class));

    }

    @Test
    public void delete() {
        OperationData operationData = integrateSnapshot(vehicleId);

        tested.delete(vehicleId);

        tested = new OnDiskOperationDataRepository(repositoryDir, scenario.operationDataHistoryRepository);
        tested.loadFromDisk();

        assertEquals(0, tested.listAll().size());

    }

    @Test
    public void listAll()   {
        OperationData operationData1 = integrateSnapshot(vehicleId);
        OperationData operationData2 = integrateSnapshot(scenario.vehicle2.id);

        tested = new OnDiskOperationDataRepository(repositoryDir, scenario.operationDataHistoryRepository);
        tested.loadFromDisk();

        List<OperationData> operationData = tested.listAll();

        assertEquals(2, operationData.size());
        assertNotNull(tested.tryFindById(operationData1.vehicleId));
        assertNotNull(tested.tryFindById(operationData2.vehicleId));
    }

    @Test
    public void tryFindById() {
        OperationData operationData1 = integrateSnapshot(vehicleId);
        OperationData operationData2 = integrateSnapshot(scenario.vehicle2.id);

        OperationData operationData = tested.tryFindById(operationData2.vehicleId);

        assertNotNull(operationData);
        assertEquals(operationData.vehicleId, operationData2.vehicleId);
    }

    private OperationData integrateSnapshot(UUID vehicleId) {
        return scenario.addOperationData(tested, vehicleId,
                new Indicator("tank1", "liter", "12" ),
                new Indicator("tank1", "liter", "34" )
        );

    }


    @Test
    public void noException_WhenXmlFileIsEmpty() throws Exception {
        TempFile folder = tempDir.newFolder(UUID.randomUUID().toString());
        String xmlFile = tested.getXmlFile(folder).file().getName();
        folder.newFile(xmlFile);

        tested.loadFromDisk();
    }
}