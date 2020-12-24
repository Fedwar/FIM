package fleetmanagement.frontend.security.config;

import java.util.List;

import javax.xml.bind.annotation.*;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.mgt.DefaultSecurityManager;

import fleetmanagement.frontend.security.SecurityRole;
import fleetmanagement.frontend.security.shiro.*;
import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter.SessionProvider;
import gsp.util.DoNotObfuscate;

@XmlRootElement(name="active-directory")
public class ActiveDirectoryConfigXml implements SecurityConfiguration {
	public ActiveDirectoryConnectionXml connection;
	@XmlElement(name="group-roles-mapping") public ActiveDirectoryMappingsXml groupRoleMappings;
	
	@DoNotObfuscate
	public static class ActiveDirectoryConnectionXml {
		public String url;
		public String user;
		public String password;
		public String domain;
		
		public UsernamePasswordToken getCredentials() {
			return new UsernamePasswordToken(user, password);
		}
	}
	
	@DoNotObfuscate
	public static class ActiveDirectoryMappingsXml {
		public List<ActiveDirectoryGroupRoleMappingXml> map;
		
		public GroupRoleMapping toRolesMap() {
			GroupRoleMapping result = new GroupRoleMapping();
			for (ActiveDirectoryGroupRoleMappingXml mapping : map) {
				ActiveDirectoryGroup group = new ActiveDirectoryGroup(mapping.group);
				for (SecurityRole role : SecurityRole.parseRoleList(mapping.roles)) {
					result.add(group, role);
				}
			}
			return result;
		}
	}
	
	@DoNotObfuscate
	public static class ActiveDirectoryGroupRoleMappingXml {
		@XmlAttribute public String group;
		@XmlAttribute(name="to-roles") public String roles;
	}

	@Override
	public SessionProvider buildSessionProvider() {
		DefaultSecurityManager securityManager = new DefaultSecurityManager();
		securityManager.setCacheManager(new MemoryConstrainedCacheManager());
		securityManager.setRealm(new CustomActiveDirectoryRealm(connection.getCredentials(), connection.url, new ActiveDirectoryDomain(connection.domain), groupRoleMappings.toRolesMap()));
		return new ShiroSessionProvider(securityManager);
	}
}