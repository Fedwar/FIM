package fleetmanagement.frontend.security.none;

import static org.junit.Assert.*;

import java.util.Locale;

import javax.ws.rs.core.SecurityContext;

import org.junit.*;

import fleetmanagement.frontend.UserSession;
import gsp.testutil.TestMonotonicClock;

public class NoSecuritySessionProviderTest {
	
	private NoSecuritySessionProvider tested;
	private UserSession session;
	private TestMonotonicClock clock;
	
	@Before
	public void setup() {
		clock = new TestMonotonicClock();
		tested = new NoSecuritySessionProvider(clock);
		session = tested.get(null);
	}
	
	@Test
	public void createsSessionIfSessionCookieIsNotPresent() {
		assertNotNull(session);
	}
	
	@Test
	public void newUsersAreAutomaticallyLoggedIn() {
		SecurityContext sc = session.getSecurityContext();
		assertTrue(sc.isSecure());
		assertNotNull(sc.getAuthenticationScheme());
		assertEquals("developer", sc.getUserPrincipal().getName());
		assertTrue(sc.isUserInRole("administrator"));
	}
		
	@Test
	public void allowsLogout() {
		session.logout();
		
		assertFalse(session.getSecurityContext().isSecure());
		assertNull(session.getSecurityContext().getUserPrincipal());
	}
	
	@Test
	public void allowsLoginWithAnyCredentials() {
		session.logout();
		session.login("foo", "bar");
		
		assertTrue(session.getSecurityContext().isSecure());
	}
	
	@Test
	public void allowsSettingTheLocale() {
		session.setLocale(Locale.SIMPLIFIED_CHINESE);
		
		assertEquals(Locale.SIMPLIFIED_CHINESE, session.getLocale());
	}
	
	@Test
	public void retrievesSessionById() {
		clock.advanceMsecs(2 * 60 * 1000);
		UserSession retrieved = tested.get(session.getId());
		
		assertSame(session, retrieved);
	}
	
	@Test
	public void returnsNewSessionForUnknownSessionIds() {
		UserSession retrieved = tested.get("foo");
		
		assertNotNull(retrieved);
		assertNotEquals(session.getId(), retrieved.getId());
	}
	
	@Test
	public void removesExpiredSessions() {
		UserSession expired = tested.get(null);
		clock.advanceMsecs(2 * 60 * 60 * 1000);
		
		UserSession newSession = tested.get(expired.getId());
		
		assertNotEquals(newSession.getId(), expired.getId());
	}
}
