package fleetmanagement.backend.reports.datasource.vehicles;

import fleetmanagement.TempFileRule;
import fleetmanagement.backend.diagnosis.DiagnosedDevice;
import fleetmanagement.backend.diagnosis.DiagnosisHistoryRepository;
import fleetmanagement.backend.diagnosis.StateEntry;
import fleetmanagement.backend.repositories.disk.DiagnosisHistorySQLiteRepository;
import fleetmanagement.backend.repositories.memory.InMemoryDiagnosisRepository;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class DiagnosisReportDataSourceTest {
    private static StateEntry monthEntryRange1;
    private static StateEntry monthEntryRange2;
    private static StateEntry monthEntry01_30;
    private static StateEntry monthEntry01_31;
    private static StateEntry monthEntry02_15;
    private static StateEntry monthEntry03_01;
    private static StateEntry monthEntry03_02;
    private static StateEntry weekEntryRange1;
    private static StateEntry weekEntryRange2;
    private static StateEntry weekEntry05_30;
    private static StateEntry weekEntry05_31;
    private static StateEntry weekEntry06_01;
    private static StateEntry weekEntry06_02;
    private static StateEntry weekEntry06_03;
    private static StateEntry hourEntryRange1;
    private static StateEntry hourEntryRange2;
    private static StateEntry hourEntry03_15;
    private static StateEntry hourEntry04_22;
    private static StateEntry hourEntry04_23;
    private static StateEntry hourEntry05_00;
    private static DiagnosedDevice diagnosedDevice1 = new DiagnosedDevice("1");
    private static DiagnosedDevice diagnosedDevice2 = new DiagnosedDevice("2");
    private static DiagnosedDevice diagnosedDevice3 = new DiagnosedDevice("3");
    private static DiagnosedDevice diagnosedDevice4 = new DiagnosedDevice("4");

    @Rule
    public TempFileRule tempDir = new TempFileRule();

    private TestScenarioPrefilled scenario;

    @BeforeClass
    public static void beforeClass() {
        monthEntryRange1 = new StateEntry(
                ZonedDateTime.parse("2017-12-31T00:00:00-00:00"),
                ZonedDateTime.parse("2019-12-31T00:00:00-00:00"),
                null,
                null,
                null
        );
        monthEntryRange2 = new StateEntry(
                ZonedDateTime.parse("2017-12-31T00:00:00-00:00"),
                ZonedDateTime.parse("2018-02-28T00:00:00-00:00"),
                null,
                null,
                null
        );
        monthEntry01_30 = new StateEntry(
                ZonedDateTime.parse("2018-01-30T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        monthEntry01_31 = new StateEntry(
                ZonedDateTime.parse("2018-01-31T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        monthEntry02_15 = new StateEntry(
                ZonedDateTime.parse("2018-02-15T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        monthEntry03_01 = new StateEntry(
                ZonedDateTime.parse("2018-03-01T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        monthEntry03_02 = new StateEntry(
                ZonedDateTime.parse("2018-03-02T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        weekEntryRange1 = new StateEntry(
                ZonedDateTime.parse("2017-01-01T00:00:00-00:00"),
                ZonedDateTime.parse("2019-06-01T00:00:00-00:00"),
                null,
                null,
                null
        );
        weekEntryRange2= new StateEntry(
                ZonedDateTime.parse("2019-06-01T00:00:00-00:00"),
                ZonedDateTime.parse("2019-12-31T00:00:00-00:00"),
                null,
                null,
                null
        );
        weekEntry05_30 = new StateEntry(
                ZonedDateTime.parse("2019-05-30T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        weekEntry05_31 = new StateEntry(
                ZonedDateTime.parse("2019-05-31T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        weekEntry06_01 = new StateEntry(
                ZonedDateTime.parse("2019-06-01T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        weekEntry06_02 = new StateEntry(
                ZonedDateTime.parse("2019-06-02T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        weekEntry06_03 = new StateEntry(
                ZonedDateTime.parse("2019-06-03T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        hourEntryRange1 = new StateEntry(
                ZonedDateTime.parse("2019-06-03T15:00:00-00:00"),
                ZonedDateTime.parse("2019-06-04T01:00:00-00:00"),
                null,
                null,
                null
        );
        hourEntryRange2 = new StateEntry(
                ZonedDateTime.parse("2019-06-04T22:00:00-00:00"),
                ZonedDateTime.parse("2019-06-05T15:00:00-00:00"),
                null,
                null,
                null
        );
        hourEntry03_15 = new StateEntry(
                ZonedDateTime.parse("2019-06-03T15:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        hourEntry04_22 = new StateEntry(
                ZonedDateTime.parse("2019-06-04T22:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        hourEntry04_23 = new StateEntry(
                ZonedDateTime.parse("2019-06-04T23:00:00-00:00"),
                null,
                null,
                null,
                null
        );
        hourEntry05_00 = new StateEntry(
                ZonedDateTime.parse("2019-06-05T00:00:00-00:00"),
                null,
                null,
                null,
                null
        );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void before() {
        scenario = new TestScenarioPrefilled();
        DiagnosisHistoryRepository diagnosisHistoryRepository = new DiagnosisHistorySQLiteRepository(tempDir);
        tempDir.newFolder(scenario.vehicle1.id.toString());
        tempDir.newFolder(scenario.vehicle2.id.toString());
        tempDir.newFolder(scenario.vehicle3.id.toString());
        scenario.diagnosisRepository = new InMemoryDiagnosisRepository(diagnosisHistoryRepository);
    }

    @Test
    public void addsErrorToMonthsReportIfStateEntryHasOnlyStartValue() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository.insertDeviceHistory(
                scenario.vehicle1.id,
                "1",
                Arrays.asList(monthEntry01_31, monthEntry02_15, monthEntry03_01)
        );

        Map actualData = getActualData(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString(),
                "months"
        );

        Map<String, Integer> expectedVehicleData = new LinkedHashMap<>();
        expectedVehicleData.put("2018-01", 1);
        expectedVehicleData.put("2018-02", 1);
        expectedVehicleData.put("2018-03", 1);

        assertEquals(expectedVehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void addsErrorToWeeksReportIfStateEntryHasOnlyStartValue() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository.insertDeviceHistory(
                scenario.vehicle1.id,
                "1",
                Arrays.asList(weekEntry05_31, weekEntry06_01, weekEntry06_02)
        );

        Map actualData = getActualData(
                "2019-05-31",
                "2019-06-02",
                scenario.vehicle1.id.toString(),
                "weeks"
        );

        Map<String, Integer> expectedVehicleData = new LinkedHashMap<>();
        expectedVehicleData.put("2019-05 week 5", 1);
        expectedVehicleData.put("2019-06 week 1", 1);
        expectedVehicleData.put("2019-06 week 2", 1);

        assertEquals(expectedVehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void addsErrorToDaysReportIfStateEntryHasOnlyStartValue() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository.insertDeviceHistory(
                scenario.vehicle1.id,
                "1",
                Arrays.asList(weekEntry05_31, weekEntry06_01, weekEntry06_02)
        );

        Map actualData = getActualData(
                "2019-05-31",
                "2019-06-02",
                scenario.vehicle1.id.toString(),
                "days"
        );

        Map<String, Integer> expectedVehicleData = new LinkedHashMap<>();
        expectedVehicleData.put("2019-05-31", 1);
        expectedVehicleData.put("2019-06-01", 1);
        expectedVehicleData.put("2019-06-02", 1);

        assertEquals(expectedVehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void addsErrorToHoursReportIfStateEntryHasOnlyStartValue() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository.insertDeviceHistory(
                scenario.vehicle1.id,
                "1",
                Arrays.asList(hourEntry04_22, hourEntry04_23)
        );

        Map actualData = getActualData(
                "2019-06-04",
                "2019-06-04",
                scenario.vehicle1.id.toString(),
                "hours"
        );

        Map<String, Integer> expectedVehicleData = new LinkedHashMap<>();
        for (int hour = 0; hour <= 9; hour++)
            expectedVehicleData.put("2019-06-04 0" + hour, 0);
        for (int hour = 10; hour <= 23; hour++)
            expectedVehicleData.put("2019-06-04 " + hour, 0);
        expectedVehicleData.put("2019-06-04 22", 1);
        expectedVehicleData.put("2019-06-04 23", 1);

        assertEquals(expectedVehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void doesNotAddErrorToMonthsReportIfStateEntryHasOnlyStartValueButNotInRange() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Arrays.asList(monthEntry01_30, monthEntry03_02));

        Map actualData = getActualData(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString(),
                "months"
        );

        Map<String, Integer> expectedVehicleData = new LinkedHashMap<>();
        expectedVehicleData.put("2018-01", 0);
        expectedVehicleData.put("2018-02", 0);
        expectedVehicleData.put("2018-03", 0);

        assertEquals(expectedVehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void doesNotAddErrorToWeeksReportIfStateEntryHasOnlyStartValueButNotInRange() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Arrays.asList(weekEntry05_30, weekEntry06_03));

        Map actualData = getActualData(
                "2019-05-31",
                "2019-06-02",
                scenario.vehicle1.id.toString(),
                "weeks"
        );

        Map<String, Integer> expectedVehicleData = new LinkedHashMap<>();
        expectedVehicleData.put("2019-05 week 5", 0);
        expectedVehicleData.put("2019-06 week 1", 0);
        expectedVehicleData.put("2019-06 week 2", 0);

        assertEquals(expectedVehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void doesNotAddErrorToDaysReportIfStateEntryHasOnlyStartValueButNotInRange() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Arrays.asList(weekEntry05_30, weekEntry06_03));

        Map actualData = getActualData(
                "2019-05-31",
                "2019-06-02",
                scenario.vehicle1.id.toString(),
                "days"
        );

        Map<String, Integer> expectedVehicleData = new LinkedHashMap<>();
        expectedVehicleData.put("2019-05-31", 0);
        expectedVehicleData.put("2019-06-01", 0);
        expectedVehicleData.put("2019-06-02", 0);

        assertEquals(expectedVehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void doesNotAddErrorToHoursReportIfStateEntryHasOnlyStartValueButNotInRange() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Arrays.asList(hourEntry03_15, hourEntry05_00));

        Map actualData = getActualData(
                "2019-06-04",
                "2019-06-04",
                scenario.vehicle1.id.toString(),
                "hours"
        );
        Map<String, Integer> vehicle1ActualData = (HashMap<String, Integer>)actualData.get(scenario.vehicle1.getName());

        assertEquals(0, vehicle1ActualData.values().stream().filter(e -> !e.equals(0)).count());
    }

    @Test
    public void addsCorrectErrorsCountFromRangeToMonthsReport() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Collections.singletonList(monthEntryRange2));

        Map actualData = getActualData(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString(),
                "months"
        );

        Map<String, Integer> expectedVehicleData = new LinkedHashMap<>();
        expectedVehicleData.put("2018-01", 1);
        expectedVehicleData.put("2018-02", 1);
        expectedVehicleData.put("2018-03", 0);

        assertEquals(expectedVehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void addsCorrectErrorsCountFromRangeToWeeksReport() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.addDiagnosis(scenario.vehicle2, Collections.singletonList(diagnosedDevice2));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Collections.singletonList(weekEntryRange1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle2.id, "2", Collections.singletonList(weekEntryRange2));

        Map actualData1 = getActualData(
                "2019-05-31",
                "2019-06-02",
                scenario.vehicle1.id.toString(),
                "days"
        );
        Map actualData2 = getActualData(
                "2019-05-31",
                "2019-06-02",
                scenario.vehicle2.id.toString(),
                "days"
        );

        Map<String, Integer> expectedVehicleData1 = new LinkedHashMap<>();
        expectedVehicleData1.put("2019-05-31", 1);
        expectedVehicleData1.put("2019-06-01", 1);
        expectedVehicleData1.put("2019-06-02", 0);
        Map<String, Integer> expectedVehicleData2 = new LinkedHashMap<>();
        expectedVehicleData2.put("2019-05-31", 0);
        expectedVehicleData2.put("2019-06-01", 1);
        expectedVehicleData2.put("2019-06-02", 1);

        assertEquals(expectedVehicleData1, actualData1.get(scenario.vehicle1.getName()));
        assertEquals(expectedVehicleData2, actualData2.get(scenario.vehicle2.getName()));
    }

    @Test
    public void addsCorrectErrorsCountFromRangeToDaysReport() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.addDiagnosis(scenario.vehicle2, Collections.singletonList(diagnosedDevice2));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Collections.singletonList(weekEntryRange1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle2.id, "2", Collections.singletonList(weekEntryRange2));

        Map actualData1 = getActualData(
                "2019-05-31",
                "2019-06-02",
                scenario.vehicle1.id.toString(),
                "weeks"
        );
        Map actualData2 = getActualData(
                "2019-05-31",
                "2019-06-02",
                scenario.vehicle2.id.toString(),
                "weeks"
        );

        Map<String, Integer> expectedVehicleData1 = new LinkedHashMap<>();
        expectedVehicleData1.put("2019-05 week 5", 1);
        expectedVehicleData1.put("2019-06 week 1", 1);
        expectedVehicleData1.put("2019-06 week 2", 0);
        Map<String, Integer> expectedVehicleData2 = new LinkedHashMap<>();
        expectedVehicleData2.put("2019-05 week 5", 0);
        expectedVehicleData2.put("2019-06 week 1", 1);
        expectedVehicleData2.put("2019-06 week 2", 1);

        assertEquals(expectedVehicleData1, actualData1.get(scenario.vehicle1.getName()));
        assertEquals(expectedVehicleData2, actualData2.get(scenario.vehicle2.getName()));
    }

    @Test
    public void addsCorrectErrorsCountFromRangeToHoursReport() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.addDiagnosis(scenario.vehicle2, Collections.singletonList(diagnosedDevice2));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Collections.singletonList(hourEntryRange1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle2.id, "2", Collections.singletonList(hourEntryRange2));

        Map actualData1 = getActualData(
                "2019-06-04",
                "2019-06-04",
                scenario.vehicle1.id.toString(),
                "hours"
        );
        Map actualData2 = getActualData(
                "2019-06-04",
                "2019-06-04",
                scenario.vehicle2.id.toString(),
                "hours"
        );

        Map<String, Integer> expectedVehicleData1 = new LinkedHashMap<>();
        for (int hour = 0; hour <= 9; hour++)
            expectedVehicleData1.put("2019-06-04 0" + hour, 0);
        for (int hour = 10; hour <= 23; hour++)
            expectedVehicleData1.put("2019-06-04 " + hour, 0);
        expectedVehicleData1.put("2019-06-04 00", 1);
        expectedVehicleData1.put("2019-06-04 01", 1);
        Map<String, Integer> expectedVehicleData2 = new LinkedHashMap<>();
        for (int hour = 0; hour <= 9; hour++)
            expectedVehicleData2.put("2019-06-04 0" + hour, 0);
        for (int hour = 10; hour <= 23; hour++)
            expectedVehicleData2.put("2019-06-04 " + hour, 0);
        expectedVehicleData2.put("2019-06-04 22", 1);
        expectedVehicleData2.put("2019-06-04 23", 1);

        assertEquals(expectedVehicleData1, actualData1.get(scenario.vehicle1.getName()));
        assertEquals(expectedVehicleData2, actualData2.get(scenario.vehicle2.getName()));
    }

    @Test
    public void summarizesErrorsFromManyEntries() {
        scenario.addDiagnosis(scenario.vehicle1, Collections.singletonList(diagnosedDevice1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Arrays.asList(monthEntryRange1, monthEntryRange2));

        Map actualData = getActualData(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString(),
                "months"
        );

        Map<String, Integer> expectedVehicleData = new LinkedHashMap<>();
        expectedVehicleData.put("2018-01", 2);
        expectedVehicleData.put("2018-02", 2);
        expectedVehicleData.put("2018-03", 1);

        assertEquals(expectedVehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void summarizesErrorsFromManyDevices() {
        scenario.addDiagnosis(scenario.vehicle1, Arrays.asList(diagnosedDevice1, diagnosedDevice2));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "1", Arrays.asList(monthEntryRange1, monthEntryRange1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle1.id, "2", Arrays.asList(monthEntryRange1, monthEntryRange2));

        Map actualData = getActualData(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString(),
                "months"
        );

        Map<String, Integer> vehicleData = new LinkedHashMap<>();
        vehicleData.put("2018-01", 4);
        vehicleData.put("2018-02", 4);
        vehicleData.put("2018-03", 3);

        assertEquals(vehicleData, actualData.get(scenario.vehicle1.getName()));
    }

    @Test
    public void summarizesErrorsFromManyVehicles() {
        scenario.addDiagnosis(scenario.vehicle2, Arrays.asList(diagnosedDevice1, diagnosedDevice2));
        scenario.addDiagnosis(scenario.vehicle3, Arrays.asList(diagnosedDevice3, diagnosedDevice4));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle2.id, "1", Arrays.asList(monthEntryRange1, monthEntryRange1));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle2.id, "2", Arrays.asList(monthEntryRange1, monthEntryRange2));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle3.id, "3", Arrays.asList(monthEntryRange2, monthEntry03_02));
        scenario.diagnosisRepository
                .insertDeviceHistory(scenario.vehicle3.id, "4", Arrays.asList(monthEntry03_02, monthEntry03_01));

        Map actualData = getActualData(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString() + "," + scenario.vehicle2.id.toString() +
                        "," + scenario.vehicle3.id.toString(),
                "months"
        );

        Map<String, Map<String, Integer>> expectedData = new LinkedHashMap<>();
        Map<String, Integer> vehicle1Data = new LinkedHashMap<>();
        vehicle1Data.put("2018-01", 0);
        vehicle1Data.put("2018-02", 0);
        vehicle1Data.put("2018-03", 0);
        expectedData.put(scenario.vehicle1.getName(), vehicle1Data);
        Map<String, Integer> vehicle2Data = new LinkedHashMap<>();
        vehicle2Data.put("2018-01", 4);
        vehicle2Data.put("2018-02", 4);
        vehicle2Data.put("2018-03", 3);
        expectedData.put(scenario.vehicle2.getName(), vehicle2Data);
        Map<String, Integer> vehicle3Data = new LinkedHashMap<>();
        vehicle3Data.put("2018-01", 1);
        vehicle3Data.put("2018-02", 1);
        vehicle3Data.put("2018-03", 1);
        expectedData.put(scenario.vehicle3.getName(), vehicle3Data);
        Map<String, Integer> allData = new LinkedHashMap<>();
        allData.put("2018-01", 5);
        allData.put("2018-02", 5);
        allData.put("2018-03", 4);
        expectedData.put("All vehicles errors count", allData);

        assertEquals(expectedData, actualData);
    }

    private Map getActualData(String start, String end, String vehiclesIds, String rangeBy) {
        Map<String, String> reportFilters = new HashMap<>();
        reportFilters.put("earliestReportDate", start);
        reportFilters.put("latestReportDate", end);
        reportFilters.put(
                "selectedVehicles",
                vehiclesIds
        );
        reportFilters.put("rangeBy", rangeBy);

        DiagnosisReportDataSource tested = new DiagnosisReportDataSource(
                scenario.diagnosisRepository,
                scenario.vehicleRepository,
                reportFilters
        );
        return tested.getData();
    }
}