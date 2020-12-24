package fleetmanagement.frontend;

import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.*;

public class I18n {

	private static final Logger logger = Logger.getLogger(I18n.class);

	public static String get(UserSession request, String key, Object... args) {
		Locale l = request.getLocale();
		return get(l, key, args);
	}

	public static String get(Locale locale, String key, Object... args) {
		ResourceBundle messages = ResourceBundle.getBundle("messages", locale);
		try {
			MessageFormat formatter = new MessageFormat(messages.getString(key), locale);
			return formatter.format(args);
		} catch (MissingResourceException e) {
			logger.error("i18n can't find a key in language resource bundle! key=" + key + ", language=" +locale.getLanguage());
			return key;
		}
	}
}
