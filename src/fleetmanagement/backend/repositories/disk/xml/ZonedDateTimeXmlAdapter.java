package fleetmanagement.backend.repositories.disk.xml;

import gsp.util.DoNotObfuscate;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@DoNotObfuscate
public class ZonedDateTimeXmlAdapter extends TemporalAccessorXmlAdapter<ZonedDateTime> {
    public ZonedDateTimeXmlAdapter() {
        super(DateTimeFormatter.ISO_ZONED_DATE_TIME, ZonedDateTime::from);
    }
}
