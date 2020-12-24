package fleetmanagement.backend.diagnosis;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LocalizedStringTest {

    public LocalizedString tested;
    public List<Locale> acceptableLanguages;

    @Before
    public void before() {
        tested = new LocalizedString();
        tested.put("en", "en");
        tested.put("de", "de");

        acceptableLanguages = Arrays.asList(Locale.JAPANESE, Locale.GERMAN, Locale.ENGLISH);
    }

    @Test
    public void returnsEmptyString_WhenNoLocaleFound() {
        acceptableLanguages = Arrays.asList(Locale.JAPANESE, Locale.ITALY);
        assertEquals("", tested.get(acceptableLanguages));
    }

    @Test
    public void get() {
        acceptableLanguages = Arrays.asList(Locale.GERMAN, Locale.ENGLISH, Locale.ITALY);
        assertEquals("de", tested.get(acceptableLanguages));

        acceptableLanguages = Arrays.asList(Locale.ITALY, Locale.ENGLISH, Locale.GERMAN);
        assertEquals("en", tested.get(acceptableLanguages));
    }

    @Test
    public void contains() {
        assertTrue(tested.contains(Locale.GERMAN));
    }

    @Test
    public void testTwoAgrConstructor() {
        tested = new LocalizedString(null, null);
        assertEquals(0, tested.getLocaleMap().size());

        tested = new LocalizedString(Locale.GERMAN, "de");
        assertEquals("de", tested.get(Locale.GERMAN));
    }
    @Test
    public void equals() {
        tested = new LocalizedString(Locale.GERMAN, "de");
        tested.put("en", "en");

        LocalizedString equalString = new LocalizedString(Locale.ENGLISH, "en");
        equalString.put("de", "de");

        assertTrue(tested.equals(equalString));
    }








}