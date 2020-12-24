package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.diagnosis.ErrorCategory;
import fleetmanagement.backend.diagnosis.LocalizedString;
import fleetmanagement.backend.diagnosis.StateEntry;
import fleetmanagement.test.TestScenario;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

public class DiagnosisHistorySQLiteRepositoryTest {
    private DiagnosisHistorySQLiteRepository tested;
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
        tested = new DiagnosisHistorySQLiteRepository(temporaryFolder.getRoot());
        new File(temporaryFolder.getRoot(), vehicleId.toString()).mkdir();
        new File(temporaryFolder.getRoot(), secondVehicleId.toString()).mkdir();
    }

    @Test
    public void returnsOldestHistory_WhereEndNotNull() {
        String deviceId = "device1";
        ZonedDateTime now = ZonedDateTime.now();
        StateEntry oldestNotEndedEntry = new StateEntry(now.minusDays(10), null, null, null, null);
        StateEntry oldestEndedEntry = new StateEntry(now.minusDays(5), now, null, null, null);
        StateEntry stateEntry = new StateEntry(now, now, null, null, null);
        tested.addHistory(vehicleId, deviceId, Arrays.asList(stateEntry, oldestNotEndedEntry, oldestEndedEntry));

        StateEntry history = tested.getOldestHistory(vehicleId);

        assertTrue(isEquals(oldestEndedEntry, history));
    }

    @Test
    public void removesAllEntries_EqualsOrOlderThanParameter() {
        String deviceId = "device1";
        ZonedDateTime now = ZonedDateTime.now();
//
//        ArrayList<StateEntry> stateEntries = new ArrayList<>();
//        stateEntries.add( new StateEntry(now, now, null, null, null));
//        for (int i = 0; i < 10000; i++) {
//            stateEntries.add( new StateEntry(now.minusMinutes(i), now.minusMinutes(i), null, null, null));
//         }
//        tested.addHistory(vehicleId, deviceId, stateEntries);
//
        StateEntry nowEntry = new StateEntry(now, now, null, null, null);
        StateEntry oldEntry = new StateEntry(now.minusDays(5), now, null, null, null);
        StateEntry olderEntry = new StateEntry(now.minusDays(10), now, null, null, null);
        tested.addHistory(vehicleId, deviceId, Arrays.asList(nowEntry, oldEntry, olderEntry));

        tested.reduceHistory(vehicleId, now.minusDays(5));
        List<StateEntry> historyRange = tested.getHistoryRange(vehicleId, deviceId, now, now.minusDays(100));

        assertEquals(1, historyRange.size());
        assertTrue(isEquals(nowEntry, historyRange.get(0)));
    }

    @Test
    public void removesOnlyNotEndedEntries() {
        String deviceId = "device1";
        ZonedDateTime now = ZonedDateTime.now();
        StateEntry oldestNotEndedEntry = new StateEntry(now.minusDays(10), null, null, null, null);
        StateEntry oldEndedEntry = new StateEntry(now.minusDays(1), now, null, null, null);
        StateEntry stateEntry = new StateEntry(now, now, null, null, null);
        tested.addHistory(vehicleId, deviceId, Arrays.asList(stateEntry, oldestNotEndedEntry, oldEndedEntry));

        tested.reduceHistory(vehicleId, now.minusDays(1));
        List<StateEntry> historyRange = tested.getHistoryRange(vehicleId, deviceId, now, now.minusDays(100));

        assertEquals(2, historyRange.size());
    }

    @Test
    public void addAndGetHistory() {
        String deviceId = "device1";
        LocalizedString missing = new LocalizedString("Missing");
        missing.put("fr", "perdu");
        StateEntry stateEntry1 = new StateEntry(ZonedDateTime.now().plusDays(1), null, "6", ErrorCategory.ERROR, new LocalizedString("Error"));
        StateEntry stateEntry2 = new StateEntry(ZonedDateTime.now(), ZonedDateTime.now().plusHours(1), "-1", ErrorCategory.FATAL, missing);

        tested.addHistory(vehicleId, deviceId, stateEntry1);
        tested.addHistory(vehicleId, deviceId, stateEntry2);

        List<StateEntry> history = tested.getHistory(vehicleId, deviceId);

        assertEquals(2, history.size());
        StateEntry firstRecord = history.get(0);
        StateEntry secondRecord = history.get(1);

        assertTrue(isEquals(stateEntry1, firstRecord));
        assertTrue(isEquals(stateEntry2, secondRecord));

    }

    @Test
    public void generateHistory() {
        String deviceId = "device1";
        LocalizedString missing = new LocalizedString("Missing");
        missing.put("fr", "perdu");
        StateEntry stateEntry1 = new StateEntry(ZonedDateTime.now().plusDays(1), null, "6", ErrorCategory.ERROR, new LocalizedString("Error"));
        StateEntry stateEntry2 = new StateEntry(ZonedDateTime.now(), ZonedDateTime.now().plusHours(1), "-1", ErrorCategory.FATAL, missing);

        ArrayList<StateEntry> stateEntries = new ArrayList<>();

        for (int i = 0; i < 1144; i++) {
            stateEntries.add(stateEntry1);
        }
        tested.addHistory(vehicleId, deviceId, stateEntries );


        assertTrue(isEquals(stateEntry1, tested.getLatestHistory(vehicleId,deviceId)));


    }

    @Test
    public void checkHistoryStringsDeleteCascade() {
        String deviceId = "device1";
        StateEntry stateEntry1 = new StateEntry(ZonedDateTime.now(), ZonedDateTime.now().plusHours(1),
                "-1", ErrorCategory.FATAL, new LocalizedString("Error"));

        tested.addHistory(vehicleId, deviceId, stateEntry1);
        tested.delete(vehicleId, deviceId);

        assertEquals(0, getHistoryStringTableRowCount());
    }

    @Test
    public void returnsOnlyUnfinishedHistoryRecords() {
        String deviceId = "device1";
        ZonedDateTime now = ZonedDateTime.now();
        StateEntry notEndedEntry1 = new StateEntry(now.minusDays(10), null, null, null, null);
        StateEntry notEndedEntry2 = new StateEntry(now, null, null, null, null);
        StateEntry endedEntry = new StateEntry(now.minusDays(10), now, null, null, null);
        tested.addHistory(vehicleId, deviceId, Arrays.asList(notEndedEntry1, notEndedEntry2, endedEntry));

        List<StateEntry> unfinishedHistory = tested.getUnfinishedHistory(vehicleId, deviceId);

        assertEquals(2, unfinishedHistory.size());
    }

    @Test
    public void returnsOnlyUnfinishedHistoryRecordsForSelectedDevice() {
        String deviceId = "device1";
        ZonedDateTime now = ZonedDateTime.now();
        StateEntry notEndedEntry1 = new StateEntry(now.minusDays(10), null, null, null, null);
        StateEntry notEndedEntry2 = new StateEntry(now, null, null, null, null);
        StateEntry notEndedEntry3 = new StateEntry(now.minusDays(1), null, null, null, null);
        tested.addHistory(vehicleId, deviceId, Arrays.asList(notEndedEntry1, notEndedEntry2));
        tested.addHistory(vehicleId, "anotherDevice", Arrays.asList(notEndedEntry3));

        List<StateEntry> unfinishedHistory = tested.getUnfinishedHistory(vehicleId, deviceId);

        assertEquals(2, unfinishedHistory.size());
    }



    int getHistoryStringTableRowCount() {
        Integer count = (Integer) tested.connect(vehicleId, connection -> {
            try {
                PreparedStatement stmt = connection.prepareStatement("SELECT count(*) rows FROM historyStrings;");
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    return Integer.valueOf(rs.getInt("rows"));
            } catch (Exception e) {
            }
            return null;
        });
        return count.intValue();
    }

    boolean isEquals(StateEntry stateEntry1, StateEntry stateEntry2) {
        return new EqualsBuilder()
                .append(stateEntry1.start, stateEntry2.start)
                .append(stateEntry1.end, stateEntry2.end)
                .append(stateEntry1.code, stateEntry2.code)
                .append(stateEntry1.category, stateEntry2.category)
                .append(stateEntry1.message, stateEntry2.message)
                .isEquals();
    }
}