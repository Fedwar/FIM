package fleetmanagement.frontend.security.config;

import static org.apache.commons.lang3.StringUtils.*;

import java.util.List;

import javax.xml.bind.annotation.*;

import org.apache.shiro.mgt.DefaultSecurityManager;

import fleetmanagement.frontend.security.SecurityRole;
import fleetmanagement.frontend.security.shiro.*;
import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter.SessionProvider;
import gsp.util.DoNotObfuscate;

@XmlRootElement(name="user-list")
public class UserListConfigXml implements SecurityConfiguration {
	
	public List<UserXml> user;

	@Override
	public SessionProvider buildSessionProvider() {
		UserListRealm realm = new UserListRealm();
		for (UserXml u : user) {
			u.validate();
			realm.addAccount(u.login, u.password, SecurityRole.parseRoleList(u.roles));
		}
		return new ShiroSessionProvider(new DefaultSecurityManager(realm));
	}

	@DoNotObfuscate
	public static class UserXml {
		@XmlAttribute String login;
		@XmlAttribute String password;
		@XmlAttribute String roles;
		
		public void validate() {
			if (isBlank(login) || isBlank(password) || isBlank(roles)) 
				throw new RuntimeException("User definition is missing attribute 'login', 'password' or 'roles'.");
		}
	}
}
