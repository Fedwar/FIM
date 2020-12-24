package fleetmanagement.frontend.security.shiro;

import static org.junit.Assert.*;

import org.junit.*;

public class ActiveDirectoryDomainTest {
	
	ActiveDirectoryDomain tested;
	
	@Before
	public void setup() {
		tested = new ActiveDirectoryDomain("gsp.local");
	}
	
	@Test
	public void transformsDomainNameToLdapSearchBase() {
		assertEquals("DC=gsp,DC=local", tested.toLdapSearchBase());
	}
	
	@Test
	public void addsDomainNameToUnqualifiedUserName() {
		assertEquals("baerm@gsp.local", tested.qualify("baerm"));
	}
	
	@Test
	public void leavesQualifiedDomainNameIntact() {
		assertEquals("baerm@gsp.local", tested.qualify("baerm@gsp.local"));
	}
	
	@Test
	public void outputsDomainName() {
		assertEquals("gsp.local", tested.toString());
	}
	
	@Test
	public void ignoresOptionalLeadingAtInDomainName() {
		tested = new ActiveDirectoryDomain("@google.de");
		assertEquals("baerm@google.de", tested.qualify("baerm"));
	}
	
	@Test
	public void hasDummyImplementationForUnknownDomain() {
		ActiveDirectoryDomain unknown = ActiveDirectoryDomain.NO_DOMAIN;
		assertEquals("baerm", unknown.qualify("baerm"));
		assertEquals("", unknown.toLdapSearchBase());
		assertEquals("<no domain>", unknown.toString());
	}
}
