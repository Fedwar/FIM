package fleetmanagement.frontend.transformers;

import java.time.format.FormatStyle;
import java.time.temporal.TemporalAccessor;
import java.util.*;

import org.rythmengine.extension.IFormatter;

public class DateTimeFormatter implements IFormatter {
	
	private final Map<String, FormatStyle> namedStyles = new HashMap<>();
	
	public DateTimeFormatter() {
		namedStyles.put("short-datetime", FormatStyle.SHORT);
		namedStyles.put("medium-datetime", FormatStyle.MEDIUM);
	}
	
	@Override
	public String format(Object obj, String pattern, Locale locale, String timezone) {
		if (!(obj instanceof TemporalAccessor))
			return null;
		
		java.time.format.DateTimeFormatter fmt = parseFormatPattern(pattern).withLocale(locale);
		return fmt.format((TemporalAccessor)obj);
	}

	private java.time.format.DateTimeFormatter parseFormatPattern(String pattern) {
		
		if (namedStyles.containsKey(pattern))
			return formatterFromStyle(pattern);
		else
			return formatterFromPattern(pattern);
	}

	private java.time.format.DateTimeFormatter formatterFromStyle(String pattern) {
		
		FormatStyle style = namedStyles.get(pattern);
		return java.time.format.DateTimeFormatter.ofLocalizedDateTime(style);
	}

	private java.time.format.DateTimeFormatter formatterFromPattern(String pattern) {
		return java.time.format.DateTimeFormatter.ofPattern(pattern);
	}
}
