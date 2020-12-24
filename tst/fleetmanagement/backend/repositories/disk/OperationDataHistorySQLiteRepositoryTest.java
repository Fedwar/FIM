package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.test.TestScenario;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static junit.framework.TestCase.assertEquals;

public class OperationDataHistorySQLiteRepositoryTest {
    private OperationDataHistorySQLiteRepository tested;
    private TestScenario scenario;
    private UUID vehicleId;
    private UUID secondVehicleId;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() {
        scenario = new TestScenario();
        vehicleId = UUID.randomUUID();
        secondVehicleId = UUID.randomUUID();
        tested = new OperationDataHistorySQLiteRepository(temporaryFolder.getRoot());
        new File(temporaryFolder.getRoot(), vehicleId.toString()).mkdir();
        new File(temporaryFolder.getRoot(), secondVehicleId.toString()).mkdir();
    }

    @Test
    public void returnsOldestHistory() {
        Indicator indicatorNow = new Indicator("tank1", "liter", "12", ZonedDateTime.now());
        Indicator indicatorOld = new Indicator("tank1", "liter", "10", ZonedDateTime.now().minusDays(5));
        Indicator indicatorOldest = new Indicator("tank1", "liter", "10", ZonedDateTime.now().minusDays(10));
        tested.addHistory(vehicleId, Arrays.asList(indicatorNow, indicatorOld, indicatorOldest));

        History history = tested.getOldestHistory(vehicleId);

        assertEquals(indicatorOldest.value, history.value);
        assertEquals(indicatorOldest.updated, history.timeStamp);
    }

    @Test
    public void returnsLimitedHistory() {
        Indicator indicatorNow = new Indicator("tank1", "liter", "12", ZonedDateTime.now());
        Indicator indicatorOld = new Indicator("tank1", "liter", "10", ZonedDateTime.now().minusDays(5));
        Indicator indicatorOldest = new Indicator("tank1", "liter", "5", ZonedDateTime.now().minusDays(10));
        tested.addHistory(vehicleId, Arrays.asList(indicatorNow, indicatorOld, indicatorOldest));

        List<History> history = tested.getHistory(vehicleId, "tank1", 2);

        assertEquals(2, history.size());
        assertEquals(indicatorNow.value, history.get(0).value);
        assertEquals(indicatorNow.updated, history.get(0).timeStamp);
        assertEquals(indicatorOld.value, history.get(1).value);
        assertEquals(indicatorOld.updated, history.get(1).timeStamp);
    }

    @Test
    public void removesAllEntries_EqualsOrOlderThanParameter() {
        String indicatorId = "tank1";
        ZonedDateTime now = ZonedDateTime.now();
//
//
//        ArrayList<Indicator> stateEntries = new ArrayList<>();
//        stateEntries.add(new Indicator(indicatorId, "liter", "12", now));
//        for (int i = 0; i < 10000; i++) {
//            stateEntries.add( new Indicator(indicatorId, "liter", "12", now.minusMinutes(i)));
//        }
//        tested.addHistory(vehicleId, stateEntries);
//

        Indicator indicatorNow = new Indicator(indicatorId, "liter", "12", now);
        Indicator indicatorOld = new Indicator(indicatorId, "liter", "10", now.minusDays(5));
        Indicator indicatorOldest = new Indicator(indicatorId, "liter", "10", now.minusDays(10));
        tested.addHistory(vehicleId, Arrays.asList(indicatorNow, indicatorOld, indicatorOldest));

        tested.reduceHistory(vehicleId, now.minusDays(5));
        List<History> historyRange = tested.getHistoryRange(vehicleId, indicatorId, now, now.minusDays(100));

        assertEquals(1, historyRange.size());
        History history = historyRange.get(0);
        assertEquals(indicatorNow.value, history.value);
        assertEquals(indicatorNow.updated.truncatedTo(ChronoUnit.MILLIS), history.timeStamp);
    }

    @Test
    public void getsHistory_GroupedByChronoUnit() {
        String indicatorId = "tank1";
        ZonedDateTime today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);


        ArrayList<Indicator> stateEntries = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            stateEntries.add( new Indicator(indicatorId, "liter", String.valueOf(i), today.minusMinutes(i)));
        }
        tested.addHistory(vehicleId, stateEntries);


        List<History> historyRange = tested.getHistoryRange(vehicleId, indicatorId, today, today.minusDays(10), ChronoUnit.DAYS);
        assertEquals(8, historyRange.size());

        historyRange = tested.getHistoryRange(vehicleId, indicatorId, today, today.minusDays(10), ChronoUnit.HOURS);
        assertEquals(168, historyRange.size());


    }

    @Test
    public void addAndGetHistory() {
        Indicator indicator = new Indicator("tank1", "liter", "12");
        Indicator indicator1 = new Indicator("tank1", "liter", "10", ZonedDateTime.now().plusHours(1));

        tested.addHistory(vehicleId,indicator );
        tested.addHistory(vehicleId,indicator1 );

        List<History> history = tested.getHistory(vehicleId, "tank1");

        assertEquals(2, history.size());
        History firstRecord = history.get(0);
        History secondRecord = history.get(1);

        assertEquals(indicator.value, firstRecord.value);
        assertEquals(indicator.updated, firstRecord.timeStamp);
        assertEquals(indicator1.value, secondRecord.value);
        assertEquals(indicator1.updated, secondRecord.timeStamp);
    }

    @Test
    public void getHistoryForVehicle() {
        Indicator indicator = new Indicator("tank1", "liter", "12");

        tested.addHistory(vehicleId,indicator );
        tested.addHistory(secondVehicleId,indicator );

        List<History> history = tested.getHistory(vehicleId, "tank1");

        assertEquals(1, history.size());
        History firstRecord = history.get(0);
        assertEquals(indicator.value, firstRecord.value);
        assertEquals(indicator.updated, firstRecord.timeStamp);
    }

    @Test
    public void getHistoryRange() {
        ZonedDateTime startDate = ZonedDateTime.now().withDayOfMonth(1);
        for (int i = 1; i < 10; i++) {
            tested.addHistory(vehicleId,new Indicator("tank1", "liter", String.valueOf(i+1), startDate.plusDays(i)));
        }

        List<History> history = tested.getHistoryRange(vehicleId,"tank1", startDate.plusDays(1), startDate.plusDays(5)) ;

        assertEquals(5, history.size());
        assertEquals(2, history.get(0).timeStamp.getDayOfMonth());
        assertEquals("2", history.get(0).value);
        assertEquals(6, history.get(4).timeStamp.getDayOfMonth());
        assertEquals("6", history.get(4).value);
    }

}