package fleetmanagement.backend.reports.datasource.vehicles;

import fleetmanagement.TempFileRule;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationDataHistoryRepository;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.repositories.disk.OperationDataHistorySQLiteRepository;
import fleetmanagement.backend.repositories.memory.InMemoryOperationDataRepository;
import fleetmanagement.test.TestScenarioPrefilled;
import org.junit.*;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class OperationDataReportDataSourceTest {
    private static TestScenarioPrefilled scenario;

    @ClassRule
    public static TempFileRule tempDir = new TempFileRule();

    @BeforeClass
    public static void before() {
        scenario = new TestScenarioPrefilled();
        OperationDataHistoryRepository operationDataHistoryRepository =
                new OperationDataHistorySQLiteRepository(tempDir);
        //noinspection ResultOfMethodCallIgnored
        tempDir.newFolder(scenario.vehicle1.id.toString());
        //noinspection ResultOfMethodCallIgnored
        tempDir.newFolder(scenario.vehicle2.id.toString());
        scenario.operationDataRepository = new InMemoryOperationDataRepository(operationDataHistoryRepository);
        addFewIndicatorsWithDifferentTypesOfData(scenario.operationDataRepository);
    }

    @Test
    public void getsCorrectAllIndicatorsData() {
        ArrayList<OperationDataReportDataSource.VehicleData> actualData = getActualMonthsData();

        List<OperationDataReportDataSource.VehicleData> expectedData = new ArrayList<>();

        OperationDataReportDataSource.VehicleData vehicleData1 =
                new OperationDataReportDataSource.VehicleData(scenario.vehicle1.getName());
        OperationDataReportDataSource.VehicleData vehicleData2 =
                new OperationDataReportDataSource.VehicleData(scenario.vehicle2.getName());
        OperationDataReportDataSource.IndicatorData indicatorData11 =
                new OperationDataReportDataSource.IndicatorData("id11");
        OperationDataReportDataSource.IndicatorData indicatorData12 =
                new OperationDataReportDataSource.IndicatorData("id12");
        OperationDataReportDataSource.IndicatorData indicatorData21 =
                new OperationDataReportDataSource.IndicatorData("id21");
        OperationDataReportDataSource.IndicatorData indicatorData22 =
                new OperationDataReportDataSource.IndicatorData("id22");
        OperationDataReportDataSource.IndicatorData indicatorWithZero =
                new OperationDataReportDataSource.IndicatorData("indicatorWithZero");

        List<String> dates = Arrays.asList("2018-01", "2018-02", "2018-03");
        dates.forEach(e -> indicatorData11.history.put(e, 11d));
        dates.forEach(e -> indicatorData12.history.put(e, 12.4d));
        dates.forEach(e -> indicatorData21.history.put(e, 21d));
        dates.forEach(e -> indicatorData22.history.put(e, 22.1d));
        dates.forEach(e -> indicatorWithZero.history.put(e, 0d));

        vehicleData1.indicators.add(indicatorData11);
        vehicleData1.indicators.add(indicatorData12);
        vehicleData1.indicators.add(new OperationDataReportDataSource.IndicatorData("id21"));
        vehicleData1.indicators.add(new OperationDataReportDataSource.IndicatorData("id22"));
        vehicleData1.indicators.add(new OperationDataReportDataSource.IndicatorData("indicatorWithFalse"));
        vehicleData1.indicators.add(new OperationDataReportDataSource.IndicatorData("indicatorWithTrue"));
        vehicleData1.indicators.add(new OperationDataReportDataSource.IndicatorData("indicatorWithZero"));
        vehicleData1.indicators.add(new OperationDataReportDataSource.IndicatorData("indicatorWithEmptyString"));
        vehicleData1.indicators.add(new OperationDataReportDataSource.IndicatorData("indicatorWithNoNumberString"));

        vehicleData2.indicators.add(new OperationDataReportDataSource.IndicatorData("id11"));
        vehicleData2.indicators.add(new OperationDataReportDataSource.IndicatorData("id12"));
        vehicleData2.indicators.add(indicatorData21);
        vehicleData2.indicators.add(indicatorData22);
        vehicleData2.indicators.add(new OperationDataReportDataSource.IndicatorData("indicatorWithFalse"));
        vehicleData2.indicators.add(new OperationDataReportDataSource.IndicatorData("indicatorWithTrue"));
        vehicleData2.indicators.add(indicatorWithZero);
        vehicleData2.indicators.add(new OperationDataReportDataSource.IndicatorData("indicatorWithEmptyString"));
        vehicleData2.indicators.add(new OperationDataReportDataSource.IndicatorData("indicatorWithNoNumberString"));

        expectedData.add(vehicleData1);
        expectedData.add(vehicleData2);

        assertEquals(expectedData, actualData);
    }

    @Test
    public void generatesCorrectDataFromDoubleValue() {
        Double expected = 11d;
        Double actual = getActualMonthsData().get(0).indicators.get(0).history.get("2018-01");
        assertEquals(expected, actual);
    }

    @Test
    public void generatesCorrectDataFromStringValue() {
        Double expected = 12.4d;
        Double actual = getActualMonthsData().get(0).indicators.get(1).history.get("2018-01");
        assertEquals(expected, actual);
    }

    @Test
    public void generatesCorrectDataFromIntegerValue() {
        Double expected = 21d;
        Double actual = getActualMonthsData().get(1).indicators.get(2).history.get("2018-01");
        assertEquals(expected, actual);
    }

    @Test
    public void generatesCorrectDataFromFractionalValue() {
        Double expected = 22.1d;
        Double actual = getActualMonthsData().get(1).indicators.get(3).history.get("2018-01");
        assertEquals(expected, actual);
    }

    @Test
    public void doesNotGenerateDataFromBooleanValues() {
        assertNull(getActualMonthsData().get(1).indicators.get(4).history.get("2018-01"));
        assertNull(getActualMonthsData().get(1).indicators.get(5).history.get("2018-01"));
    }

    @Test
    public void generatesCorrectDataFromZeroValue() {
        Double expected = 0d;
        Double actual = getActualMonthsData().get(1).indicators.get(6).history.get("2018-01");
        assertEquals(expected, actual);
    }

    @Test
    public void doesNotGenerateDataFromEmptyStringValue() {
        assertNull(getActualMonthsData().get(1).indicators.get(7).history.get("2018-01"));
    }

    @Test
    public void doesNotGenerateDataFromNoNumberStringValue() {
        assertNull(getActualMonthsData().get(1).indicators.get(8).history.get("2018-01"));
    }

    @Test
    public void doesNotGenerateDataFromNoSelectedIndicator() {
        assertEquals(9, getActualMonthsData().get(1).indicators.size());
    }

    @Test
    public void generatesWeeksReport() {
        Map<String, Double> expected = new LinkedHashMap<>();
        expected.put("2018-01 week 5", 11d);
        expected.put("2018-02 week 3", 11d);
        expected.put("2018-03 week 1", 11d);

        Map<String, Double> actual = getActualWeeksData().get(0).indicators.get(0).history;

        assertEquals(expected, actual);
        assertEquals(3, actual.size());
    }

    @Test
    public void generatesDaysReport() {
        Double expected = 11d;

        Double actual = getActualDaysData().get(0).indicators.get(0).history.get("2018-02-15");

        assertEquals(expected, actual);
        assertEquals(1, getActualDaysData().get(0).indicators.get(0).history.size());
    }

    @Test
    public void generatesHoursReport() {
        Double expected = 11d;

        Double actual = getActualHoursData().get(0).indicators.get(0).history.get("2018-02-15 00");

        assertEquals(expected, actual);
        assertEquals(1, getActualHoursData().get(0).indicators.get(0).history.size());
    }

    private static void addFewIndicatorsWithDifferentTypesOfData(OperationDataRepository operationDataRepository) {
        List<ZonedDateTime> times = new LinkedList<>();
        times.add(ZonedDateTime.parse("2018-01-31T00:00:00-00:00"));
        times.add(ZonedDateTime.parse("2018-02-15T00:00:00-00:00"));
        times.add(ZonedDateTime.parse("2018-03-01T00:00:00-00:00"));

        Indicator indicator11 = new Indicator("id11", "unit1", 11d);
        Indicator indicator12 = new Indicator("id12", "unit2", "12.4");
        Indicator indicator21 = new Indicator("id21", "unit1", 21);
        Indicator indicator22 = new Indicator("id22", "unit2", 22.1);
        Indicator indicatorWithFalse = new Indicator("indicatorWithFalse", "unit3", false);
        Indicator indicatorWithTrue = new Indicator("indicatorWithTrue", "unit4", true);
        Indicator indicatorWithZero = new Indicator("indicatorWithZero", "unit5", 0);
        Indicator indicatorWithEmptyString = new Indicator("indicatorWithEmptyString", "unit6", "");
        Indicator indicatorWithNoNumberString = new Indicator("indicatorWithNoNumberString", "unit7", "word1");
        Indicator notSelectedIndicator = new Indicator("notSelectedIndicator", "unit8", 23d);

        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle1.id, e, indicator11));
        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle1.id, e, indicator12));
        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle2.id, e, indicator21));
        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle2.id, e, indicator22));
        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle2.id, e, indicatorWithFalse));
        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle2.id, e, indicatorWithTrue));
        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle2.id, e, indicatorWithZero));
        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle2.id, e, indicatorWithEmptyString));
        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle2.id, e, indicatorWithNoNumberString));
        times.forEach(e-> scenario.addOperationData(operationDataRepository, scenario.vehicle2.id, e, notSelectedIndicator));
    }

    private ArrayList<OperationDataReportDataSource.VehicleData> getActualMonthsData() {
        return getActualData(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString() + "," + scenario.vehicle2.id.toString(),
                "id11,id12,id21,id22,indicatorWithFalse,indicatorWithTrue," +
                        "indicatorWithZero,indicatorWithEmptyString,indicatorWithNoNumberString",
                "months"
        );
    }

    private ArrayList<OperationDataReportDataSource.VehicleData> getActualWeeksData() {
        return getActualData(
                "2018-01-31",
                "2018-03-01",
                scenario.vehicle1.id.toString(),
                "id11",
                "weeks"
        );
    }

    private ArrayList<OperationDataReportDataSource.VehicleData> getActualDaysData() {
        return getActualData(
                "2018-02-14",
                "2018-02-16",
                scenario.vehicle1.id.toString(),
                "id11",
                "days"
        );
    }

    private ArrayList<OperationDataReportDataSource.VehicleData> getActualHoursData() {
        return getActualData(
                "2018-02-14",
                "2018-02-16",
                scenario.vehicle1.id.toString(),
                "id11",
                "hours"
        );
    }

    private ArrayList<OperationDataReportDataSource.VehicleData> getActualData
            (String start, String end, String vehiclesIds, String selectedIndicators, String rangeBy) {
        Map<String, String> reportFilters = new HashMap<>();
        reportFilters.put("earliestReportDate", start);
        reportFilters.put("latestReportDate", end);
        reportFilters.put("selectedVehicles", vehiclesIds);
        reportFilters.put("selectedIndicators", selectedIndicators);
        reportFilters.put("rangeBy", rangeBy);

        OperationDataReportDataSource operationDataReportDataSource = new OperationDataReportDataSource(
                scenario.operationDataRepository,
                scenario.vehicleRepository,
                reportFilters
        );
        return operationDataReportDataSource.getData();
    }
}