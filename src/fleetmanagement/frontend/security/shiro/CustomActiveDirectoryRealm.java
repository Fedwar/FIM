package fleetmanagement.frontend.security.shiro;

import java.util.*;
import java.util.stream.Collectors;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.LdapContext;

import org.apache.log4j.Logger;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.*;
import org.apache.shiro.realm.ldap.*;
import org.apache.shiro.subject.PrincipalCollection;

public class CustomActiveDirectoryRealm extends AbstractLdapRealm {

    private static final Logger logger = Logger.getLogger(CustomActiveDirectoryRealm.class);
    public static final String SEARCH_FILTER_USER_PRINCIPAL = "(&(objectCategory=Person)(objectClass=user)(userPrincipalName={0}))";
    public static final String SEARCH_FILTER_WINDOWS_LOGON = "(&(objectCategory=Person)(objectClass=user)(sAMAccountName={0}))";
    public final GroupRoleMapping groupRolesMap;
    public final ActiveDirectoryDomain domain;

	public CustomActiveDirectoryRealm(UsernamePasswordToken systemAccount, String activeDirectoryUrl, ActiveDirectoryDomain domain, GroupRoleMapping groupRolesMap) {
		setSystemUsername(systemAccount.getUsername());
		setSystemPassword(String.valueOf(systemAccount.getPassword()));
		setUrl(activeDirectoryUrl);
		this.domain = domain;
		this.groupRolesMap = groupRolesMap;
		this.setSearchBase(domain.toLdapSearchBase());
		setAuthenticationCachingEnabled(true);
		setAuthorizationCachingEnabled(true);
		setCachingEnabled(true);
	}

	@Override
    protected AuthenticationInfo queryForAuthenticationInfo(AuthenticationToken token, LdapContextFactory ldapContextFactory) throws NamingException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        LdapContext ctx = null;
        try {
            ctx = ldapContextFactory.getLdapContext((Object)domain.qualify(upToken.getUsername()), (Object)String.valueOf(upToken.getPassword()));
        } finally {
            LdapUtils.closeContext(ctx);
        }
        Set<String> roles = getRoles(upToken.getUsername(), ldapContextFactory);
        if (roles.isEmpty()) {
            throw new UnauthorizedException("Could not find any group memberships for user: " + upToken.getUsername());
        }

        return new SimpleAuthenticationInfo(upToken.getUsername(), upToken.getPassword(), getName());
    }

    @Override
    protected AuthorizationInfo queryForAuthorizationInfo(PrincipalCollection principals, LdapContextFactory ldapContextFactory) throws NamingException {
        String username = (String) getAvailablePrincipal(principals);
        Set<String> roles = getRoles(username, ldapContextFactory);
        return new SimpleAuthorizationInfo(roles);
    }

    protected Set<String> getRoles(String username, LdapContextFactory ldapContextFactory) throws NamingException {
        LdapContext ldapContext = ldapContextFactory.getLdapContext((Object)domain.qualify(systemUsername), (Object)systemPassword);

        try {
            List<ActiveDirectoryGroup> groups = tryGetActiveDirectoryGroups(ldapContext, SEARCH_FILTER_WINDOWS_LOGON, username);

            if (groups == null)
                groups = tryGetActiveDirectoryGroups(ldapContext, SEARCH_FILTER_USER_PRINCIPAL, domain.qualify(username));

            if (groups == null)
                throw new UnauthorizedException("Could not find any group memberships for user: " + username);

            Set<String> roles = getRoleNamesForGroups(groups);
            logger.info("Roles found for user: " + roles);

            return roles;
        } finally {
            LdapUtils.closeContext(ldapContext);
        }
    }

    protected List<ActiveDirectoryGroup> tryGetActiveDirectoryGroups(LdapContext ldapContext, String searchFilter, String searchParameter) throws NamingException {
        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
         NamingEnumeration<SearchResult> answer = ldapContext.search(searchBase, searchFilter, new Object[] { searchParameter }, searchCtls);

        while (answer.hasMoreElements()) {
            SearchResult sr = answer.next();

            if (logger.isDebugEnabled()) {
                logger.debug("Retrieving group names for user [" + sr.getName() + "]");
            }

            Attributes attrs = sr.getAttributes();

            NamingEnumeration<? extends Attribute> ae = attrs.getAll();
            while (ae.hasMore()) {
                Attribute attr = ae.next();

                if (attr.getID().equals("memberOf")) {
                    Collection<String> groupNames = LdapUtils.getAllAttributeValues(attr);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Groups found for user [" + searchParameter + "]: " + groupNames);
                    }
                    
                    LdapUtils.closeEnumeration(answer);
                    return groupNames.stream().map(x -> new ActiveDirectoryGroup(x)).collect(Collectors.toList());
                }
            }
        }
        
        LdapUtils.closeEnumeration(answer);
        return null;
    }

    protected Set<String> getRoleNamesForGroups(List<ActiveDirectoryGroup> userGroups) {
    	return userGroups.stream()
    			.flatMap(group -> groupRolesMap.getRolesForGroup(group).stream())
    			.map(x -> x.name())
    			.collect(Collectors.toSet());
    }
}
