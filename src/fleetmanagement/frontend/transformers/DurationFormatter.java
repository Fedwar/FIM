package fleetmanagement.frontend.transformers;

import java.time.*;
import java.util.*;

import org.ocpsoft.prettytime.PrettyTime;
import org.rythmengine.extension.IFormatter;

public class DurationFormatter implements IFormatter {

	@Override
	public String format(Object obj, String pattern, Locale locale, String timezone) {
		if (!(obj instanceof Duration))
			return null;
		
		return asHumanReadable((Duration)obj, locale);
	}

	public static String asHumanReadable(Duration duration, Locale locale) {
		Date d = Date.from(Instant.now().plus(duration));
		PrettyTime formatter = new PrettyTime(locale);
		return formatter.format(d);
	}

}
