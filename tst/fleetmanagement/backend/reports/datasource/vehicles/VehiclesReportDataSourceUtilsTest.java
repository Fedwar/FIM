package fleetmanagement.backend.reports.datasource.vehicles;

import org.junit.Test;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.Assert.*;

public class VehiclesReportDataSourceUtilsTest {

    @Test
    public void toReportFormatDate() {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse("2019-03-15T12:23:45-00:00");

        assertEquals(
                "2019-03-15 12",
                VehiclesReportDataSourceUtils.toReportFormatDate(zonedDateTime, ChronoUnit.HOURS)
        );
        assertEquals(
                "2019-03-15",
                VehiclesReportDataSourceUtils.toReportFormatDate(zonedDateTime, ChronoUnit.DAYS)
        );
        assertEquals(
                "2019-03 week 3",
                VehiclesReportDataSourceUtils.toReportFormatDate(zonedDateTime, ChronoUnit.WEEKS)
        );
        assertEquals(
                "2019-03",
                VehiclesReportDataSourceUtils.toReportFormatDate(zonedDateTime, ChronoUnit.MONTHS)
        );
    }

    @Test
    public void getDatesBetweenWorksCorrectlyWithHoursReports() {
        ZonedDateTime hoursStartDate = ZonedDateTime.parse("2018-12-31T00:00:00-00:00");
        ZonedDateTime hoursEndDate = ZonedDateTime.parse("2019-01-01T23:00:00-00:00");

        List<ZonedDateTime> hoursExpected = new LinkedList<>();
        for (int hour = 0; hour <= 9; hour++)
            hoursExpected.add(ZonedDateTime.parse("2018-12-31T0" + hour +":00:00-00:00"));
        for (int hour = 10; hour <= 23; hour++)
            hoursExpected.add(ZonedDateTime.parse("2018-12-31T" + hour +":00:00-00:00"));
        for (int hour = 0; hour <= 9; hour++)
            hoursExpected.add(ZonedDateTime.parse("2019-01-01T0" + hour +":00:00-00:00"));
        for (int hour = 10; hour <= 23; hour++)
            hoursExpected.add(ZonedDateTime.parse("2019-01-01T" + hour +":00:00-00:00"));

        List<ZonedDateTime> hoursActual =
                VehiclesReportDataSourceUtils.getDatesBetween(hoursStartDate, hoursEndDate, ChronoUnit.HOURS);

        assertEquals(hoursExpected, hoursActual);
    }

    @Test
    public void getDatesBetweenWorksCorrectlyWithDaysReports() {
        ZonedDateTime daysStartDate = ZonedDateTime.parse("2018-12-30T00:00:00-00:00");
        ZonedDateTime daysEndDate = ZonedDateTime.parse("2019-01-02T00:00:00-00:00");

        List<ZonedDateTime> daysExpected = Arrays.asList(
                ZonedDateTime.parse("2018-12-30T00:00:00-00:00"),
                ZonedDateTime.parse("2018-12-31T00:00:00-00:00"),
                ZonedDateTime.parse("2019-01-01T00:00:00-00:00"),
                ZonedDateTime.parse("2019-01-02T00:00:00-00:00")
        );

        List<ZonedDateTime> daysActual =
                VehiclesReportDataSourceUtils.getDatesBetween(daysStartDate, daysEndDate, ChronoUnit.DAYS);

        assertEquals(daysExpected, daysActual);
    }

    @Test
    public void getDatesBetweenWorksCorrectlyWithWeeksReports() {
        ZonedDateTime weeksStartDate = ZonedDateTime.parse("2018-12-26T00:00:00-00:00");
        ZonedDateTime weeksEndDate = ZonedDateTime.parse("2019-01-07T00:00:00-00:00");

        List<ZonedDateTime> weeksExpected = Arrays.asList(
                ZonedDateTime.parse("2018-12-26T00:00:00-00:00"),
                ZonedDateTime.parse("2018-12-30T00:00:00-00:00"),
                ZonedDateTime.parse("2019-01-01T00:00:00-00:00"),
                ZonedDateTime.parse("2019-01-07T00:00:00-00:00")
        );

        List<ZonedDateTime> weeksActual =
                VehiclesReportDataSourceUtils.getDatesBetween(weeksStartDate, weeksEndDate, ChronoUnit.WEEKS);

        assertEquals(weeksExpected, weeksActual);
    }

    @Test
    public void getDatesBetweenWorksCorrectlyWithMonthsReports() {
        ZonedDateTime monthsStartDate = ZonedDateTime.parse("2018-11-29T00:00:00-00:00");
        ZonedDateTime monthsEndDate = ZonedDateTime.parse("2019-02-02T00:00:00-00:00");

        List<ZonedDateTime> monthsExpected = Arrays.asList(
                ZonedDateTime.parse("2018-11-29T00:00:00-00:00"),
                ZonedDateTime.parse("2018-12-01T00:00:00-00:00"),
                ZonedDateTime.parse("2019-01-01T00:00:00-00:00"),
                ZonedDateTime.parse("2019-02-02T00:00:00-00:00")
        );

        List<ZonedDateTime> monthsActual =
                VehiclesReportDataSourceUtils.getDatesBetween(monthsStartDate, monthsEndDate, ChronoUnit.MONTHS);

        assertEquals(monthsExpected, monthsActual);
    }

    @Test
    public void getSortedMapByKey() {
        Map<String, String> unsortedMap = new LinkedHashMap<>();
        unsortedMap.put("2016-12-04", "second date");
        unsortedMap.put("2016-12-05", "third date");
        unsortedMap.put("2016-12-03", "first date");

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("2016-12-03", "first date");
        expected.put("2016-12-04", "second date");
        expected.put("2016-12-05", "third date");

        Map<String, String> actual = VehiclesReportDataSourceUtils.getSortedMapByKey(unsortedMap);

        assertEquals(new LinkedList<>(expected.entrySet()), new LinkedList<>(actual.entrySet()));
    }

    @Test
    public void isDateInRange() {
        ZonedDateTime earliestReportDate = ZonedDateTime.parse("2014-12-31T00:00:00-00:00");
        ZonedDateTime latestReportDate = ZonedDateTime.parse("2018-12-30T23:59:59-00:00");

        assertTrue(VehiclesReportDataSourceUtils.isDateInRange(
                ZonedDateTime.parse("2016-12-02T21:32:00-00:00"),
                earliestReportDate,
                latestReportDate
        ));
        assertFalse(VehiclesReportDataSourceUtils.isDateInRange(
                ZonedDateTime.parse("2014-12-30T23:59:00-00:00"),
                earliestReportDate,
                latestReportDate
        ));
        assertFalse(VehiclesReportDataSourceUtils.isDateInRange(
                ZonedDateTime.parse("2018-12-31T00:00:00-00:00"),
                earliestReportDate,
                latestReportDate
        ));
        assertTrue(VehiclesReportDataSourceUtils.isDateInRange(
                ZonedDateTime.parse("2014-12-31T00:00:00-00:00"),
                earliestReportDate,
                latestReportDate
        ));
        assertTrue(VehiclesReportDataSourceUtils.isDateInRange(
                ZonedDateTime.parse("2018-12-30T23:59:00-00:00"),
                earliestReportDate,
                latestReportDate
        ));
    }

    @Test
    public void getChronoUnitStartWorksCorrectlyWithHours() {
        ZonedDateTime time = ZonedDateTime.parse("2018-12-30T23:59:59-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-12-30T23:00:00-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitStart(time, ChronoUnit.HOURS)
        );
    }

    @Test
    public void getChronoUnitStartWorksCorrectlyWithDays() {
        ZonedDateTime time = ZonedDateTime.parse("2018-12-30T23:59:59-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-12-30T00:00:00-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitStart(time, ChronoUnit.DAYS)
        );
    }

    @Test
    public void getChronoUnitStartWorksCorrectlyWithWeeks() {
        ZonedDateTime time = ZonedDateTime.parse("2018-12-31T23:59:59-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-12-30T00:00:00-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitStart(time, ChronoUnit.WEEKS)
        );
    }

    @Test
    public void getChronoUnitStartWorksCorrectlyWithSunday() {
        ZonedDateTime time = ZonedDateTime.parse("2018-12-30T23:59:59-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-12-30T00:00:00-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitStart(time, ChronoUnit.WEEKS)
        );
    }

    @Test
    public void getChronoUnitStartWorksCorrectlyWithMonths() {
        ZonedDateTime time = ZonedDateTime.parse("2018-12-30T23:59:59-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-12-01T00:00:00-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitStart(time, ChronoUnit.MONTHS)
        );
    }

    @Test
    public void getChronoUnitEndWorksCorrectlyWithHours() {
        ZonedDateTime time = ZonedDateTime.parse("2018-10-01T00:00:00-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-10-01T00:59:59-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitEnd(time, ChronoUnit.HOURS)
        );
    }

    @Test
    public void getChronoUnitEndWorksCorrectlyWithDays() {
        ZonedDateTime time = ZonedDateTime.parse("2018-10-01T00:00:00-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-10-01T23:59:59-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitEnd(time, ChronoUnit.DAYS)
        );
    }

    @Test
    public void getChronoUnitEndWorksCorrectlyWithWeeks() {
        ZonedDateTime time = ZonedDateTime.parse("2018-10-01T00:00:00-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-10-06T23:59:59-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitEnd(time, ChronoUnit.WEEKS)
        );
    }

    @Test
    public void getChronoUnitEndWorksCorrectlyWithSunday() {
        ZonedDateTime time = ZonedDateTime.parse("2018-10-07T00:00:00-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-10-13T23:59:59-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitEnd(time, ChronoUnit.WEEKS)
        );
    }

    @Test
    public void getChronoUnitEndWorksCorrectlyWithMonths() {
        ZonedDateTime time = ZonedDateTime.parse("2018-10-01T00:00:00-00:00");
        assertEquals(
                ZonedDateTime.parse("2018-10-31T23:59:59-00:00"),
                VehiclesReportDataSourceUtils.getChronoUnitEnd(time, ChronoUnit.MONTHS)
        );
    }
}