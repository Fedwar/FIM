package fleetmanagement.test;

import java.security.Principal;
import java.util.*;

import javax.ws.rs.core.SecurityContext;

public class SecurityContextStub implements SecurityContext {
	
	public boolean isSecure = false;
	public List<String> roles = new ArrayList<String>();

	@Override
	public String getAuthenticationScheme() {
		return null;
	}

	@Override
	public Principal getUserPrincipal() {
		return () -> "Test User";
	}

	@Override
	public boolean isSecure() {
		return isSecure;
	}

	@Override
	public boolean isUserInRole(String role) {
		return roles.contains(role);
	}

}
