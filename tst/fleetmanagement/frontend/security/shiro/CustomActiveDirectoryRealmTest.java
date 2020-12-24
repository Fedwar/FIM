package fleetmanagement.frontend.security.shiro;

import fleetmanagement.frontend.security.SecurityRole;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.ldap.LdapContextFactory;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CustomActiveDirectoryRealmTest {

    private CustomActiveDirectoryRealm tested;
    private DefaultSecurityManager sm;
    private Subject subject;
    private GroupRoleMapping rolesMap;
    @Mock
    private LdapContextFactory ldapFactory;
    @Mock
    private LdapContext ldapContext;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        UsernamePasswordToken ldapSystemUser = new UsernamePasswordToken("admin", "admin-pass");
        rolesMap = new GroupRoleMapping();
        tested = new CustomActiveDirectoryRealm(ldapSystemUser, "ldap://example.com:389", new ActiveDirectoryDomain("example.com"), rolesMap);
        tested.setLdapContextFactory(ldapFactory);

        sm = new DefaultSecurityManager(tested);
        sm.setCacheManager(new MemoryConstrainedCacheManager());
        subject = new Subject.Builder(sm).buildSubject();
    }

    @Test(expected = AuthenticationException.class)
    public void doesNotLogin_WhenNoRolesFoundForUserGroups() throws Exception {
        when(ldapFactory.getLdapContext((Object) any(), (Object) any())).thenReturn(ldapContext);

        assignRolesToGroup("admin", SecurityRole.Admin, SecurityRole.User);
        configureActiveDirectoryToReturnGroupForWindowsUser("user", "CN=users,DC=example,DC=com");

        subject.login(new UsernamePasswordToken("user", "user-pass"));
    }

    @Test
    public void authorizesUserWithActiveDirectoryGroups() throws Exception {
        when(ldapFactory.getLdapContext((Object) "user@example.com", (Object) "user-pass")).thenReturn(ldapContext);
        when(ldapFactory.getLdapContext((Object) "admin@example.com", (Object) "admin-pass")).thenReturn(ldapContext);

        assignRolesToGroup("admin", SecurityRole.Admin, SecurityRole.User);
        configureActiveDirectoryToReturnGroupForWindowsUser("user", "CN=admin,DC=example,DC=com");

        subject.login(new UsernamePasswordToken("user", "user-pass"));

        assertTrue(subject.hasRole(SecurityRole.Admin.name()));
        assertTrue(subject.hasRole(SecurityRole.User.name()));
        assertFalse(subject.hasRole("guest"));
    }

    @Test(expected = org.apache.shiro.authc.AuthenticationException.class)
    public void throwsExceptionWhenLoggingInWithInvalidCredentials() throws Exception {
        when(ldapFactory.getLdapContext((Object) "user@example.com", (Object) "wrong-pass")).thenThrow(new RuntimeException());

        subject.login(new UsernamePasswordToken("user", "wrong-pass"));
    }

    void assignRolesToGroup(String groupName, SecurityRole... roles) {
        for (SecurityRole role : roles) {
            rolesMap.add(new ActiveDirectoryGroup(groupName), role);
        }
    }

    private void configureActiveDirectoryToReturnGroupForWindowsUser(String user, String group) throws NamingException {
        BasicAttributes attrs = new BasicAttributes();
        attrs.put("memberOf", group);
        SearchResult sr = new SearchResult("", null, attrs, false);
        when(ldapContext.search(eq("DC=example,DC=com"), eq(CustomActiveDirectoryRealm.SEARCH_FILTER_WINDOWS_LOGON), AdditionalMatchers.aryEq(new Object[]{user}), any(SearchControls.class))).thenAnswer(x -> new NamingEnumerationStub<SearchResult>(sr));
    }

    private static class NamingEnumerationStub<T> implements NamingEnumeration<T> {

        private Iterator<T> iterator;

        @SafeVarargs
        public NamingEnumerationStub(T... list) {
            iterator = Arrays.asList(list).iterator();
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public T nextElement() {
            return iterator.next();
        }

        @Override
        public void close() throws NamingException {
        }

        @Override
        public boolean hasMore() throws NamingException {
            return hasMoreElements();
        }

        @Override
        public T next() throws NamingException {
            return nextElement();
        }
    }

}
