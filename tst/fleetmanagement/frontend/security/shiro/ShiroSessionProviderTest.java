package fleetmanagement.frontend.security.shiro;

import static org.junit.Assert.*;

import java.util.Locale;

import javax.ws.rs.core.SecurityContext;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.junit.*;

import fleetmanagement.frontend.UserSession;

public class ShiroSessionProviderTest {
	
	private ShiroSessionProvider tested;
	private UserSession session;
	
	@Before
	public void setup() {
		SimpleAccountRealm realm = new SimpleAccountRealm();
		realm.addAccount("micha", "correct-password", "admin");
		tested = new ShiroSessionProvider(new DefaultSecurityManager(realm));
		session = tested.get(null);
	}
	
	@Test
	public void createsDefaultSessionForEmptyCookie() {
		SecurityContext sc = session.getSecurityContext();
		assertEquals(Locale.getDefault(), session.getLocale());
		assertNotNull(session.getId());
		assertFalse(sc.isSecure());
		assertFalse(sc.isUserInRole("admin"));
		assertNotNull(sc.getAuthenticationScheme());
	}
	
	@Test
	public void allowsSettingTheLocale() {
		session.setLocale(Locale.TRADITIONAL_CHINESE);
		assertEquals(Locale.TRADITIONAL_CHINESE, session.getLocale());
	}
	
	@Test
	public void allowsLogin() {
		boolean success = session.login("micha", "correct-password");

		assertTrue(success);
		SecurityContext sc = session.getSecurityContext();
		assertTrue(sc.isSecure());
		assertTrue(sc.isUserInRole("admin"));
		assertEquals("micha", sc.getUserPrincipal().getName());
	}
	
	@Test
	public void allowsLogout() {
		session.login("micha", "correct-password");
		session.logout();

		SecurityContext sc = session.getSecurityContext(); 
		assertFalse(sc.isSecure());
		assertFalse(sc.isUserInRole("admin"));
		assertNull(sc.getUserPrincipal());
	}
	
	@Test
	public void loginFailesWithIncorrectCredentials() {
		boolean success = session.login("micha", "wrong-password");

		SecurityContext sc = session.getSecurityContext();
		assertFalse(success);
		assertFalse(sc.isSecure());
		assertFalse(sc.isUserInRole("admin"));
		assertNull(sc.getUserPrincipal());
	}
	
	
	@Test
	public void remembersLoginBySessionId() {
		session.login("micha", "correct-password");
		
		UserSession other = tested.get(session.getId());
		
		SecurityContext sc = other.getSecurityContext();
		assertTrue(sc.isSecure());
		assertEquals("micha", sc.getUserPrincipal().getName());
	}
	
}
