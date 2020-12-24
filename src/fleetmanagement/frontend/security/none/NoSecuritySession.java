package fleetmanagement.frontend.security.none;

import java.security.Principal;
import java.util.*;

import javax.ws.rs.core.SecurityContext;

import fleetmanagement.frontend.UserSession;
import org.apache.log4j.Logger;

public class NoSecuritySession implements UserSession {

	private static final Logger logger = Logger.getLogger(NoSecuritySession.class);

	public static final String NO_PERMISSIONS_USER = "no-permissions";
	
	public long lastActivity;
	private final String id;
	private Locale locale;
	private boolean isLoggedIn;
	private boolean hasPermissions;
	private final SecurityContext securityContext;
	private List<Locale> acceptableLanguages = new ArrayList<>();
	private String selectedLanguage;
	private String username;

	public NoSecuritySession(long now) {
		this.securityContext = new DevelopmentSecurityContext();
		this.id = UUID.randomUUID().toString();
		this.locale = Locale.getDefault();
		this.isLoggedIn = true;
		this.lastActivity = now;
		this.hasPermissions = true;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean login(String user, String password) {
		isLoggedIn = true;
		hasPermissions = !user.equals(NO_PERMISSIONS_USER);
		logger.info("User logged in. " + user);
		this.username = user;
		return true;
	}

	@Override
	public void logout() {
		isLoggedIn = false;
		hasPermissions = false;
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public void setLocale(Locale locale) {
		this.locale = locale;
	}


	@Override
	public String getSelectedLanguage() {
		return selectedLanguage;
	}

	@Override
	public void setSelectedLanguage(String language) {
		this.selectedLanguage = language;
	}

	@Override
	public List<Locale> getAcceptableLanguages() {
		return acceptableLanguages;
	}

	@Override
	public void setAcceptableLanguages(List<Locale> acceptableLanguages) {
		this.acceptableLanguages = acceptableLanguages;
	}

	@Override
	public SecurityContext getSecurityContext() {
		return securityContext;
	}

	@Override
	public String getUsername() {
		return username;
	}

	private class DevelopmentSecurityContext implements SecurityContext {
		@Override
		public boolean isUserInRole(String role) {
			if (!isLoggedIn)
				return false;
			
			if (!hasPermissions)
				return false;
			
			return true;
		}
		
		@Override
		public boolean isSecure() {
			return isLoggedIn;
		}
		
		@Override
		public Principal getUserPrincipal() {
			return isLoggedIn ? () -> "developer" : null;
		}
		
		@Override
		public String getAuthenticationScheme() {
			return "developer";
		}
	}

}
