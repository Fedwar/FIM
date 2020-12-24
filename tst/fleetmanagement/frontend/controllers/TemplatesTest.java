package fleetmanagement.frontend.controllers;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import fleetmanagement.frontend.security.SecurityRole;
import fleetmanagement.test.*;

public class TemplatesTest {
	
	private SessionStub session;
	private TestScenarioPrefilled scenario = new TestScenarioPrefilled();

	@Before
	public void setup() {
		session = new SessionStub();
		Templates.init(null, null);
	}
	
	@Test
	public void deniesWritingPermissionWithoutUserRole() {
		String rendered = Templates.renderPage("security-test.html", session.getLocale(), session.getSecurityContext(), null, null);
		assertTrue(rendered.contains("hasWritePermission: false"));
	}
	
	@Test
	public void grantsWritingPermissionForUserRole() {
		SecurityContextStub securityContext = session.getSecurityContext();
		securityContext.roles.add(SecurityRole.User.name());
		
		String rendered = Templates.renderPage("security-test.html", session.getLocale(), securityContext, null, null);
		
		assertTrue(rendered.contains("hasWritePermission: true"));
	}
	
}
