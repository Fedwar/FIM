package fleetmanagement.frontend.security.webserver;

import com.sun.jersey.core.header.OutBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.languages.Languages;
import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter.SessionProvider;
import fleetmanagement.test.LicenceStub;
import fleetmanagement.test.SessionStub;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.SecurityContext;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ThreadLocalSessionFilterTest {
    private ThreadLocalSessionFilter tested;
    @Mock
    private SessionProvider sessions;
    @Mock
    private ContainerRequest request;
    @Mock
    private UserSession session;
    private Licence licence;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        Map<String, Cookie> cookies = new HashMap<>();
        cookies.put(ThreadLocalSessionFilter.SESSION_COOKIE_NAME, new Cookie(ThreadLocalSessionFilter.SESSION_COOKIE_NAME, "1234"));
        when(request.getCookies()).thenReturn(cookies);

        when(session.getId()).thenReturn("1234");
        when(session.getLocale()).thenReturn(Locale.JAPANESE);
        when(sessions.get("1234")).thenReturn(session);

        licence = new LicenceStub();
        Languages languages = new Languages(null, licence);

        tested = new ThreadLocalSessionFilter(sessions, languages);
    }

    @Test
    public void identifiesSessionUsingSessionCookie() {
        tested.filter(request);

        assertEquals("1234", tested.getId());
    }

    @Test
    public void redirectsSessionFunctionsToThreadLocalSession() {
        tested.filter(request);

        assertEquals(Locale.JAPANESE, tested.getLocale());

        tested.setLocale(Locale.TRADITIONAL_CHINESE);
        verify(session).setLocale(Locale.TRADITIONAL_CHINESE);

        tested.logout();
        verify(session).logout();

        tested.login("user", "pass");
        verify(session).login("user", "pass");

        SecurityContext sc = mock(SecurityContext.class);
        when(session.getSecurityContext()).thenReturn(sc);
        assertEquals(sc, tested.getSecurityContext());
    }

    @Test
    public void usesFirstAcceptableLanguageProposedByBrowser() {
        when(request.getAcceptableLanguages()).thenReturn(Arrays.asList(Locale.KOREAN, Locale.GERMAN, Locale.ENGLISH));

        tested.filter(request);

        verify(session).setLocale(Locale.GERMAN);
    }

    @Test
    public void supportedLanguageOnTopOfList() {
        session = new SessionStub();

        when(request.getAcceptableLanguages()).thenReturn(Arrays.asList(Locale.KOREAN, Locale.GERMAN, Locale.JAPANESE, Locale.ENGLISH));
        when(sessions.get("1234")).thenReturn(session);

        tested.filter(request);

        assertEquals(Locale.GERMAN, session.getAcceptableLanguages().get(0));
        assertEquals(4, session.getAcceptableLanguages().size());
    }

    @Test
    public void returnBrowserLanguages_WhenNoSupportedLanguages() {
        session = new SessionStub();

        List<Locale> localeList = Arrays.asList(Locale.KOREAN, Locale.JAPANESE, Locale.CHINA);

        when(request.getAcceptableLanguages()).thenReturn(localeList);
        when(sessions.get("1234")).thenReturn(session);

        tested.filter(request);

        assertEquals(localeList, session.getAcceptableLanguages());

    }

    @Test
    public void setsSessionCookieInResponse() {
        when(session.getId()).thenReturn("123456789");
        ContainerResponse response = mock(ContainerResponse.class);
        OutBoundHeaders headers = new OutBoundHeaders();
        when(response.getHttpHeaders()).thenReturn(headers);

        tested.filter(request);
        tested.filter(request, response);

        assertEquals("fim_session=123456789;Version=1", headers.getFirst("Set-Cookie").toString());
    }
}
