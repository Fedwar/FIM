package fleetmanagement.backend.reports.datasource.vehicles;

import fleetmanagement.backend.vehicles.ConnectionStatusRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import org.apache.log4j.Logger;

import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ConnectionStatusReportDataSource extends VehiclesReportDataSource {
    private static final Logger logger = Logger.getLogger(ConnectionStatusReportDataSource.class);

    private ConnectionStatusRepository connectionStatusRepository;
    private LinkedHashMap<String, DataItem> data = new LinkedHashMap<>();

    public ConnectionStatusReportDataSource(VehicleRepository vehicles, ConnectionStatusRepository connectionStatusRepository,
                                            Map<String, String> filters) {
        super(vehicles, filters);
        this.connectionStatusRepository = connectionStatusRepository;
        generateData();
    }

    public Map<String, DataItem> getData() {
        return data;
    }

    @Override
    void generateData() {
        for (String vehicleId : selectedVehicles) {
            Map<String, String> vehicleData = prepareData(vehicleId);
            for (String date : vehicleData.keySet()) {
                DataItem dataItem = data.get(date);
                if (dataItem == null) {
                    dataItem = new DataItem(date);
                    data.put(date, dataItem);
                }
                dataItem.inc(vehicleData.get(date));
            }
        }
    }

    private Map<String, String> prepareData(String vehicleId) {
        Map<String, String> data = new HashMap<>();

        List<ZonedDateTime> allDates =
                VehiclesReportDataSourceUtils.getDatesBetween(earliestReportDate, latestReportDate, rangeBy);

        Map<String, ConnectionStatus> hoursByVehicle = getHoursByVehicle(vehicleId);

        if (allDates == null) {
            logger.error("No days between first and last days! Report the error to developers. First day = " + earliestReportDate + ", last day = " + latestReportDate + ", range = " + rangeBy);
            return VehiclesReportDataSourceUtils.getSortedMapByKey(data);
        }

        for (ZonedDateTime zonedDateTime : allDates) {
            String reportFormatDate = VehiclesReportDataSourceUtils.toReportFormatDate(zonedDateTime, rangeBy);

            if (rangeBy.equals(ChronoUnit.HOURS)) {
                if (hoursByVehicle.keySet().contains(reportFormatDate))
                    data.put(reportFormatDate, hoursByVehicle.get(reportFormatDate).toString());
                else
                    data.put(reportFormatDate, ConnectionStatus.OFFLINE.toString());
            }

            if (rangeBy.equals(ChronoUnit.DAYS))
                data.put(reportFormatDate, getConnectionStatusOfTheDay(hoursByVehicle, reportFormatDate).toString());

            if (rangeBy.equals(ChronoUnit.WEEKS))
                data.put(reportFormatDate, getConnectionStatusOfTheWeek(hoursByVehicle, zonedDateTime).toString());

            if (rangeBy.equals(ChronoUnit.MONTHS))
                data.put(reportFormatDate, getConnectionStatusOfTheMonth(hoursByVehicle, zonedDateTime).toString());
        }

        return VehiclesReportDataSourceUtils.getSortedMapByKey(data);
    }

    private Map<String, ConnectionStatus> getHoursByVehicle(String vehicleId) {
        Map<String, ConnectionStatus> notCut = connectionStatusRepository.getVehicleHours(
                VehiclesReportDataSourceUtils.toReportFormatDate(earliestReportDate, ChronoUnit.SECONDS),
                VehiclesReportDataSourceUtils.toReportFormatDate(latestReportDate, ChronoUnit.SECONDS),
                vehicles.tryFindById(UUID.fromString(vehicleId))
        );
        Map<String, ConnectionStatus> cut = new HashMap<>();
        if (notCut != null) {
            for (String hour : notCut.keySet())
                cut.put(hour.substring(0, hour.indexOf(":")), notCut.get(hour));
        }
        return cut;
    }

    private ConnectionStatus getConnectionStatusOfTheDay(
            Map<String, ConnectionStatus> hoursByVehicle,
            String reportFormatDate) {

        int onlineHoursCount = 0;
        int irregularHoursCount = 0;
        for (String hour : hoursByVehicle.keySet()) {
            if (hour.contains(reportFormatDate)) {
                if (hoursByVehicle.get(hour) == ConnectionStatus.ONLINE)
                    onlineHoursCount++;
                if (hoursByVehicle.get(hour) == ConnectionStatus.IRREGULAR)
                    irregularHoursCount++;
            }
        }
        if (onlineHoursCount > 12)
            return ConnectionStatus.ONLINE;
        else if ((onlineHoursCount + irregularHoursCount) >= 1)
            return ConnectionStatus.IRREGULAR;
        else
            return ConnectionStatus.OFFLINE;
    }

    private ConnectionStatus getConnectionStatusOfTheWeek(
            Map<String, ConnectionStatus> hoursByVehicle,
            ZonedDateTime zonedDateTime) {

        ZonedDateTime firstDay = VehiclesReportDataSourceUtils.getChronoUnitStart(zonedDateTime, rangeBy);
        ZonedDateTime lastDay = VehiclesReportDataSourceUtils.getChronoUnitEnd(zonedDateTime, rangeBy);
        List<ZonedDateTime> days = VehiclesReportDataSourceUtils.getDatesBetween(firstDay, lastDay, ChronoUnit.DAYS);

        int onlineDaysCount = 0;
        int irregularDaysCount = 0;
        if (days == null) {
            logger.error("No days between first and last days! Report the error to developers. First day = " + firstDay.toString() + ", last day = " + lastDay.toString());
            return ConnectionStatus.OFFLINE;
        }
        for (ZonedDateTime zonedDateTimeDay : days) {
            String reportFormatDay = zonedDateTimeDay.toString().substring(0, zonedDateTime.toString().indexOf("T"));
            ConnectionStatus connectionStatus = getConnectionStatusOfTheDay(hoursByVehicle, reportFormatDay);
            if (connectionStatus == ConnectionStatus.ONLINE)
                onlineDaysCount++;
            if (connectionStatus == ConnectionStatus.IRREGULAR)
                irregularDaysCount++;
        }

        if (onlineDaysCount > (7 / 2))
            return ConnectionStatus.ONLINE;
        else if ((onlineDaysCount + irregularDaysCount) >= 1)
            return ConnectionStatus.IRREGULAR;
        else
            return ConnectionStatus.OFFLINE;
    }

    private ConnectionStatus getConnectionStatusOfTheMonth(
            Map<String, ConnectionStatus> hoursByVehicle,
            ZonedDateTime zonedDateTime) {

        int daysCount = YearMonth.of(zonedDateTime.getYear(), zonedDateTime.getMonthValue()).lengthOfMonth();
        ZonedDateTime firstDay = zonedDateTime.withDayOfMonth(1);
        ZonedDateTime lastDay = zonedDateTime.withDayOfMonth(daysCount);
        List<ZonedDateTime> days = VehiclesReportDataSourceUtils.getDatesBetween(firstDay, lastDay, ChronoUnit.DAYS);

        int onlineDaysCount = 0;
        int irregularDaysCount = 0;
        if (days == null) {
            logger.error("No days between first and last days! Report the error to developers. First day = " + firstDay.toString() + ", last day = " + lastDay.toString());
            return ConnectionStatus.OFFLINE;
        }
        for (ZonedDateTime zonedDateTimeDay : days) {
            String reportFormatDay = zonedDateTimeDay.toString().substring(0, zonedDateTime.toString().indexOf("T"));
            ConnectionStatus connectionStatus = getConnectionStatusOfTheDay(hoursByVehicle, reportFormatDay);
            if (connectionStatus == ConnectionStatus.ONLINE)
                onlineDaysCount++;
            if (connectionStatus == ConnectionStatus.IRREGULAR)
                irregularDaysCount++;
        }

        if (onlineDaysCount > (daysCount / 2))
            return ConnectionStatus.ONLINE;
        else if ((onlineDaysCount + irregularDaysCount) >= 1)
            return ConnectionStatus.IRREGULAR;
        else
            return ConnectionStatus.OFFLINE;
    }

    public static class DataItem {
        public int onlineCount = 0;
        public int offlineCount = 0;
        public int irregularCount = 0;
        public String date;

        public DataItem(String date) {
            this.date = date;
        }

        void inc(String connectionStatus) {
            if (connectionStatus.equals("ONLINE"))
                onlineCount++;
            if (connectionStatus.equals("OFFLINE"))
                offlineCount++;
            if (connectionStatus.equals("IRREGULAR"))
                irregularCount++;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataItem dataItem = (DataItem) o;
            return onlineCount == dataItem.onlineCount &&
                    offlineCount == dataItem.offlineCount &&
                    irregularCount == dataItem.irregularCount &&
                    date.equals(dataItem.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(onlineCount, offlineCount, irregularCount, date);
        }
    }
}
