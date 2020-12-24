package fleetmanagement.backend.reports.datasource.vehicles;

import org.apache.log4j.Logger;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VehiclesReportDataSourceUtils {
    private static final Logger logger = Logger.getLogger(VehiclesReportDataSourceUtils.class);

    static String toReportFormatDate(ZonedDateTime zonedDateTime, ChronoUnit rangeBy) {
        if (rangeBy.equals(ChronoUnit.SECONDS)) {
            return zonedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ");
        }
        if (rangeBy.equals(ChronoUnit.HOURS))
            return zonedDateTime.toString().substring(0, zonedDateTime.toString().indexOf("T") + 3)
                    .replace("T", " ");
        if (rangeBy.equals(ChronoUnit.DAYS))
            return zonedDateTime.toString().substring(0, zonedDateTime.toString().indexOf("T"));
        if (rangeBy.equals(ChronoUnit.WEEKS)) {
            LocalDate localDate = zonedDateTime.toLocalDate();
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int weekNumber = localDate.get(weekFields.weekOfMonth());
            return zonedDateTime.toString().substring(0, zonedDateTime.toString().indexOf("T") - 3) +
                    " week " + weekNumber;
        }
        if (rangeBy.equals(ChronoUnit.MONTHS))
            return zonedDateTime.toString().substring(0, zonedDateTime.toString().indexOf("T") - 3);
        logger.error("Incorrect type of ranging");
        return null;
    }

    static List<ZonedDateTime> getDatesBetween(ZonedDateTime startDate, ZonedDateTime endDate, ChronoUnit rangeBy) {
        if (rangeBy.equals(ChronoUnit.HOURS)) {
            long numOfHoursBetween = rangeBy.between(startDate, endDate) + 1;
            return IntStream.iterate(0, i -> i + 1)
                    .limit(numOfHoursBetween)
                    .mapToObj(startDate::plusHours)
                    .collect(Collectors.toList());
        }
        if (rangeBy.equals(ChronoUnit.DAYS)) {
            long numOfDaysBetween = rangeBy.between(startDate, endDate) + 1;
            return IntStream.iterate(0, i -> i + 1)
                    .limit(numOfDaysBetween)
                    .mapToObj(startDate::plusDays)
                    .collect(Collectors.toList());
        }
        if (rangeBy.equals(ChronoUnit.WEEKS) | rangeBy.equals(ChronoUnit.MONTHS)) {
            List<ZonedDateTime> daysBetween = getDatesBetween(startDate, endDate, ChronoUnit.DAYS);
            List<ZonedDateTime> rangeByBetween = new LinkedList<>();
            if (daysBetween != null) {
                List<String> reportDateFormatRangeByBetween = new LinkedList<>();
                for (ZonedDateTime zonedDateTime : daysBetween) {
                    String reportDateFormat = toReportFormatDate(zonedDateTime, rangeBy);
                    if (!reportDateFormatRangeByBetween.contains(reportDateFormat)) {
                        reportDateFormatRangeByBetween.add(reportDateFormat);
                        rangeByBetween.add(zonedDateTime);
                    }
                }
                rangeByBetween.set(rangeByBetween.size() - 1, endDate);
            }
            return rangeByBetween;
        }
        logger.error("Incorrect type of ranging");
        return new LinkedList<>();
    }

    static <T> Map<String, T> getSortedMapByKey(Map<String, T> unsortedMap) {
        Map<String, T> sortedMap = new LinkedHashMap<>();
        unsortedMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

        return sortedMap;
    }

    static boolean isDateInRange(ZonedDateTime date, ZonedDateTime earliestReportDate, ZonedDateTime latestReportDate) {
        ZonedDateTime greenwichTime = date.withZoneSameLocal(ZoneId.of("Greenwich"));
        return (!earliestReportDate.isAfter(greenwichTime)) && (!greenwichTime.isAfter(latestReportDate));
    }

    static ZonedDateTime getChronoUnitStart(ZonedDateTime time, ChronoUnit chronoUnit) {
        if (chronoUnit.equals(ChronoUnit.MONTHS))
            return time.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        if (chronoUnit.equals(ChronoUnit.WEEKS)) {
            if (time.getDayOfWeek().equals(DayOfWeek.SUNDAY))
                return time.truncatedTo(ChronoUnit.DAYS);
            else
                return time.minusWeeks(1).with(DayOfWeek.SUNDAY).truncatedTo(ChronoUnit.DAYS);
        }
        else
            return time.truncatedTo(chronoUnit);
    }

    static ZonedDateTime getChronoUnitEnd(ZonedDateTime time, ChronoUnit chronoUnit) {
        return getChronoUnitStart(time, chronoUnit)
                .plus(1, chronoUnit).minus(1, ChronoUnit.SECONDS);
    }

    static ChronoUnit getChronoUnit(String rangeBy) {
        if (rangeBy.equals("hours"))
            return ChronoUnit.HOURS;
        if (rangeBy.equals("days"))
            return ChronoUnit.DAYS;
        if (rangeBy.equals("weeks"))
            return ChronoUnit.WEEKS;
        if (rangeBy.equals("months"))
            return ChronoUnit.MONTHS;
        logger.error("Incorrect type of ranging");
        return null;
    }
}
