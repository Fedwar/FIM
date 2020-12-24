package fleetmanagement.backend.reports.datasource.vehicles;

import fleetmanagement.backend.reports.datasource.vehicles.ConnectionStatusReportDataSource.DataItem;
import fleetmanagement.backend.vehicles.ConnectionStatusRepository;
import fleetmanagement.backend.vehicles.Vehicle;
import fleetmanagement.backend.vehicles.VehicleRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static fleetmanagement.TestObjectFactory.createVehicle;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionStatusReportDataSourceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private ConnectionStatusRepository connectionStatusRepository;

    private Vehicle vehicle1 = createVehicle("vehicle1");
    private Vehicle vehicle2 = createVehicle("vehicle2");
    private Vehicle vehicle3 = createVehicle("vehicle3");

    @Before
    public void before() {
        when(vehicleRepository.tryFindById(vehicle1.id)).thenReturn(vehicle1);
        when(vehicleRepository.tryFindById(vehicle2.id)).thenReturn(vehicle2);
        when(vehicleRepository.tryFindById(vehicle3.id)).thenReturn(vehicle3);
        addConnectionStatusesToRepository();
    }

    @Test
    public void getCorrectAllMonthsReportData() {
        Map<String, DataItem> expectedData = getExpectedMonthsReportData();
        Map<String, DataItem> actualData = getActualData(
                vehicle1.id.toString() + "," + vehicle2.id.toString() +
                        "," + vehicle3.id.toString(),
                "months"
        );
        assertEquals(expectedData, actualData);
    }

    @Test
    public void setsIrregularMonthStatusIfMostDaysOfRangeAreOnlineButNoInformationAboutMostDaysOfMonth() {
        Map<String, DataItem> actualData = getActualData(vehicle1.id.toString(), "months");
        assertEquals(1, actualData.get("2018-01").irregularCount);
    }

    @Test
    public void setsIrregularMonthStatusIfAtLeastOneDayIsIrregular() {
        Map<String, DataItem> actualData = getActualData(vehicle2.id.toString(), "months");
        assertEquals(1, actualData.get("2018-01").irregularCount);
    }

    @Test
    public void setsOnlineMonthStatusIfMostDaysAreOnline() {
        Map<String, DataItem> actualData = getActualData(vehicle1.id.toString(), "months");
        assertEquals(1, actualData.get("2018-02").onlineCount);
    }

    @Test
    public void setsIrregularMonthStatusIfWrongThatMostDaysAreOnline() {
        Map<String, DataItem> actualData = getActualData(vehicle2.id.toString(), "months");
        assertEquals(1, actualData.get("2018-02").irregularCount);
    }

    @Test
    public void setsOfflineMonthStatusIfThereIsNoInformationAboutIt () {
        Map<String, DataItem> actualData = getActualData(vehicle3.id.toString(), "months");
        assertEquals(1, actualData.get("2018-03").offlineCount);
    }

    @Test
    public void setsIrregularWeekStatusIfMostDaysOfRangeAreOnlineButNoInformationAboutMostDaysOfMonth() {
        Map<String, DataItem> actualData = getActualData(vehicle1.id.toString(), "weeks");
        assertEquals(1, actualData.get("2018-01 week 5").irregularCount);
    }

    @Test
    public void setsIrregularWeekStatusIfAtLeastOneDayIsIrregular() {
        Map<String, DataItem> actualData = getActualData(vehicle2.id.toString(), "weeks");
        assertEquals(1, actualData.get("2018-01 week 5").irregularCount);
    }

    @Test
    public void setsOnlineWeekStatusIfMostDaysAreOnline() {
        Map<String, DataItem> actualData = getActualData(vehicle1.id.toString(), "weeks");
        assertEquals(1, actualData.get("2018-02 week 3").onlineCount);
    }

    @Test
    public void setsIrregularWeekStatusIfWrongThatMostDaysAreOnline() {
        Map<String, DataItem> actualData = getActualData(vehicle1.id.toString(), "weeks");
        assertEquals(1, actualData.get("2018-02 week 5").irregularCount);
    }

    @Test
    public void setsOfflineWeekStatusIfThereIsNoInformationAboutIt () {
        Map<String, DataItem> actualData = getActualData(vehicle3.id.toString(), "weeks");
        assertEquals(1, actualData.get("2018-03 week 1").offlineCount);
    }

    @Test
    public void setsIrregularDayStatusIfAtLeastOneHourIsIrregular () {
        Map<String, DataItem> actualData = getActualData(vehicle2.id.toString(), "days");
        assertEquals(1, actualData.get("2018-01-31").irregularCount);
    }

    @Test
    public void setsOnlineDayStatusIfMostHoursAreOnline () {
        Map<String, DataItem> actualData = getActualData(
                vehicle1.id.toString() + "," + vehicle2.id.toString(),
                "days"
        );
        assertEquals(2, actualData.get("2018-02-15").onlineCount);
    }

    @Test
    public void setsIrregularDayStatusIfWrongThatMostHoursAreOnline() {
        Map<String, DataItem> actualData = getActualData(vehicle2.id.toString(), "days");
        assertEquals(1, actualData.get("2018-03-01").irregularCount);
    }

    @Test
    public void setsOfflineDayStatusIfThereIsNoInformationAboutIt () {
        Map<String, DataItem> actualData = getActualData(vehicle3.id.toString(), "days");
        assertEquals(1, actualData.get("2018-03-01").offlineCount);
    }

    @Test
    public void setsTheHourStatusWhichTakenFromRepository() {
        Map<String, DataItem> actualData = getActualData(
                vehicle1.id.toString() + "," + vehicle2.id.toString() +
                        "," + vehicle3.id.toString(),
                "hours"
        );
        assertEquals(1, actualData.get("2018-01-31 12").onlineCount);
        assertEquals(1, actualData.get("2018-01-31 12").offlineCount);
        assertEquals(1, actualData.get("2018-01-31 12").irregularCount);
    }

    private void addConnectionStatusesToRepository() {
        Map<String, ConnectionStatus> connectionStatusMap1 = new HashMap<>();
        Map<String, ConnectionStatus> connectionStatusMap2 = new HashMap<>();

        //filling all days, included in report range as online and
        //no information about days, which not included in report range
        for (int hour = 11; hour < 24; hour++)
            connectionStatusMap1.put("2018-01-31 " + hour + ":00:00", ConnectionStatus.ONLINE);

        //filling one hour as irregular
        connectionStatusMap2.put("2018-01-31 12:00:00", ConnectionStatus.IRREGULAR);

        //filling in more than half of days more than half of hours are online
        for (int day = 11; day < 26; day++)
            for (int hour = 11; hour < 24; hour++)
                connectionStatusMap1.put("2018-02-" + day + " " + hour + ":00:00", ConnectionStatus.ONLINE);

        //filling in NOT more than half of days more than half of hours are online
        for (int day = 11; day < 25; day++)
            for (int hour = 11; hour < 24; hour++)
                connectionStatusMap2.put("2018-02-" + day + " " + hour + ":00:00", ConnectionStatus.ONLINE);

        //filling in more than half of days NOT more than half of hours are online
        for (int day = 11; day < 27; day++)
            for (int hour = 11; hour < 23; hour++)
                connectionStatusMap1.put("2018-03-" + day + " " + hour + ":00:00", ConnectionStatus.ONLINE);

        //filling NOT more than half of hours are online in one day
        for (int hour = 11; hour < 23; hour++)
            connectionStatusMap2.put("2018-03-01 " + hour + ":00:00", ConnectionStatus.ONLINE);

        when(connectionStatusRepository.getVehicleHours("2018-01-31 00:00:00", "2018-03-01 23:59:59", vehicle1))
                .thenReturn(connectionStatusMap1);
        when(connectionStatusRepository.getVehicleHours("2018-01-31 00:00:00", "2018-03-01 23:59:59", vehicle2))
                .thenReturn(connectionStatusMap2);
    }

    private Map<String, DataItem> getActualData(String vehiclesIds, String rangeBy) {
        Map<String, String> monthsReportFilters = getReportFilters(vehiclesIds, rangeBy);
        ConnectionStatusReportDataSource connectionStatusReportDataSource = new ConnectionStatusReportDataSource(
                vehicleRepository,
                connectionStatusRepository,
                monthsReportFilters
        );
        return connectionStatusReportDataSource.getData();
    }

    private Map<String, String> getReportFilters(String vehiclesIds, String rangeBy) {
        Map<String, String> reportFilters = new HashMap<>();
        reportFilters.put("earliestReportDate", "2018-01-31");
        reportFilters.put("latestReportDate", "2018-03-01");
        reportFilters.put(
                "selectedVehicles",
                vehiclesIds
        );
        reportFilters.put("rangeBy", rangeBy);
        return reportFilters;
    }

    private Map<String, DataItem> getExpectedMonthsReportData() {
        Map<String, DataItem> expectedData = new LinkedHashMap<>();

        DataItem januaryDataItem = new DataItem("2018-01");
        DataItem februaryDataItem = new DataItem("2018-02");
        DataItem marchDataItem = new DataItem("2018-03");

        januaryDataItem.offlineCount = 1;
        januaryDataItem.irregularCount = 2;
        februaryDataItem.onlineCount = 1;
        februaryDataItem.offlineCount = 1;
        februaryDataItem.irregularCount = 1;
        marchDataItem.offlineCount = 1;
        marchDataItem.irregularCount = 2;

        expectedData.put("2018-01", januaryDataItem);
        expectedData.put("2018-02", februaryDataItem);
        expectedData.put("2018-03", marchDataItem);

        return expectedData;
    }
}