package fleetmanagement.frontend.security.config;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;

import org.junit.*;

import fleetmanagement.TestFiles;
import fleetmanagement.frontend.security.SecurityRole;
import fleetmanagement.frontend.security.none.NoSecuritySessionProvider;
import fleetmanagement.frontend.security.shiro.*;
import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter.SessionProvider;

public class SecurityConfiguratorTest {
	private SecurityConfigurator tested;
	
	@Before
	public void setup() {
		tested = new SecurityConfigurator();
	}
	
	@Test
	public void loadsActiveDirectorySecurityConfiguration() throws Exception {		
		ShiroSessionProvider loaded = (ShiroSessionProvider)loadSecurityConfiguration("security/active-directory.config");
		
		assertNotNull(loaded.securityManager.getCacheManager());
		CustomActiveDirectoryRealm realm = (CustomActiveDirectoryRealm)loaded.securityManager.getRealms().stream().findFirst().orElseThrow(() -> new RuntimeException());
		assertEquals(new ActiveDirectoryDomain("example.com"), realm.domain);
		assertEquals(Arrays.asList(SecurityRole.Admin, SecurityRole.User), realm.groupRolesMap.getRolesForGroup(new ActiveDirectoryGroup("Dev-India")));
	}
	
	@Test
	public void loadsSimpleUserList() throws Exception {		
		ShiroSessionProvider loaded = (ShiroSessionProvider)loadSecurityConfiguration("security/user-list.config");
		
		UserListRealm realm = (UserListRealm)loaded.securityManager.getRealms().stream().findFirst().orElseThrow(() -> new RuntimeException());
		assertTrue(realm.accountExists("JohnF", "12345", SecurityRole.User));
	}
	
	@Test
	public void loadsDisabledSecurityConfiguration() throws Exception {		
		SessionProvider loaded = loadSecurityConfiguration("security/no-security.config");
		assertTrue(loaded instanceof NoSecuritySessionProvider);
	}
	
	@Test
	public void defaultsToNoSecurityIfNoConfigurationPresent() throws Exception {		
		SessionProvider loaded = tested.buildSessionProvider(new File("non-existant-security.config"));
		assertTrue(loaded instanceof NoSecuritySessionProvider);
	}
	
	private SessionProvider loadSecurityConfiguration(String file) throws Exception {
		return tested.buildSessionProvider(TestFiles.find(file));
	}
}
