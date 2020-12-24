package fleetmanagement;

import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;

public class Dates {

    public static void assertEquals(ZonedDateTime expected, ZonedDateTime actual, TemporalUnit unit) {
        org.junit.Assert.assertEquals(expected.truncatedTo(unit), actual.truncatedTo(unit));
    }

}
