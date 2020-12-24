package fleetmanagement.frontend.security.shiro;

import java.util.List;

import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.realm.SimpleAccountRealm;

import fleetmanagement.frontend.security.SecurityRole;

public class UserListRealm extends SimpleAccountRealm {
	
	public void addAccount(String username, String password, List<SecurityRole> roles) {
		addAccount(username, password, roles.stream().map(x -> x.name()).toArray(x -> new String[x]));
	}

	public boolean accountExists(String user, String password, SecurityRole role) {
		SimpleAccount account = this.users.get(user);
		
		if (account != null) {
			return password.equals(account.getCredentials()) && account.getRoles().contains(role.toString()); 
		}
		
		return false;
	}
}
