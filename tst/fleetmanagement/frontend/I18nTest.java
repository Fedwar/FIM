package fleetmanagement.frontend;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import fleetmanagement.test.SessionStub;

public class I18nTest {

	private SessionStub germanRequest = new SessionStub(Locale.GERMAN);
	private SessionStub englishRequest = new SessionStub(Locale.ENGLISH);
	private SessionStub unsupportedLanguageRequest = new SessionStub(new Locale("foo", "bar"));

	@Before
	public void before() {
		Locale.setDefault(Locale.ENGLISH);
	}

	@Test
	public void localizesToRequestLocale() {
		assertEquals("de", I18n.get(germanRequest, "unit_test_string"));
		assertEquals("en", I18n.get(englishRequest, "unit_test_string"));
		assertEquals("en", I18n.get(unsupportedLanguageRequest, "unit_test_string"));
	}
	
	@Test
	public void supportsObjectArguments() {
		assertEquals("foo 5", I18n.get(germanRequest, "unit_test_args", "foo", 5));
	}
	
	@Test
	public void supportsArgumentMovementInLocalization() {
		assertEquals("22.10.", I18n.get(germanRequest, "unit_test_day_and_month", "22", "10"));
		assertEquals("10/22", I18n.get(englishRequest, "unit_test_day_and_month", "22", "10"));
		assertEquals("10/22", I18n.get(unsupportedLanguageRequest, "unit_test_day_and_month", "22", "10"));
	}
}
