package fleetmanagement.backend.repositories.migration;

import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationData;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;


import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MigrateIndicatorsHistoryToSQLiteTest {

    private MigrateIndicatorsHistoryToSQLite tested;
    private TestScenario scenario;
    private List<History> historyList;
    private Indicator indicator ;
    private Indicator indicatorWithoutHistory;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() {
        scenario = new TestScenario();
        historyList = Arrays.asList(new History("12", ZonedDateTime.now().minusHours(10)),
                new History("10", ZonedDateTime.now())
        );
        indicator = new Indicator("ind1", "liter", "10", ZonedDateTime.now(), historyList);
        indicatorWithoutHistory = new Indicator("ind2", "liter", "10", ZonedDateTime.now());
    }

    @Test
    public void migrationClearsIndicatorHistory() {
        Indicator indicator1 = indicator;
        Indicator indicator2 = indicator.clone();
        OperationData operationData = newOperationData(indicator1);
        OperationData operationData1 = newOperationData(indicator2);

        tested = new MigrateIndicatorsHistoryToSQLite(scenario.operationDataHistoryRepository, Arrays.asList(operationData, operationData1));
        tested.migrate();

        assertTrue(indicator1.getHistory().isEmpty());
        assertTrue(indicator2.getHistory().isEmpty());
    }

    @Test
    public void migrationAddsHistoryToSQLite() {
        OperationData operationData = newOperationData(indicator);
        OperationData operationData1 = newOperationData(indicatorWithoutHistory);

        tested = new MigrateIndicatorsHistoryToSQLite(scenario.operationDataHistoryRepository, Arrays.asList(operationData, operationData1));
        tested.migrate();

        verify(scenario.operationDataHistoryRepository, times(1)).addHistory(any(UUID.class), any(Map.class));
    }

    OperationData newOperationData(Indicator... indicators) {
        List<History> historyList = Arrays.asList(new History("12", ZonedDateTime.now().minusHours(10)),
                new History("10", ZonedDateTime.now())
        );
        return new OperationData(UUID.randomUUID(), ZonedDateTime.now(), Arrays.asList(indicators));
    }

}