package fleetmanagement.frontend.security.shiro;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.apache.shiro.subject.Subject;

public class ShiroSecurityContext implements SecurityContext {
	
	private final Subject subject;

	public ShiroSecurityContext(Subject subject) {
		this.subject = subject;
	}

	@Override
	public String getAuthenticationScheme() {
		return "shiro";
	}

	@Override
	public Principal getUserPrincipal() {
		if (subject.getPrincipal() == null)
			return null;
		
		return () -> subject.getPrincipal().toString();
	}

	@Override
	public boolean isSecure() {
		return subject.isAuthenticated();
	}

	@Override
	public boolean isUserInRole(String role) {
		return subject.hasRole(role);
	}
}