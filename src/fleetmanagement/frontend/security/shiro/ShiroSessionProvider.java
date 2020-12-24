package fleetmanagement.frontend.security.shiro;

import java.util.*;

import javax.ws.rs.core.SecurityContext;

import org.apache.log4j.Logger;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;

import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter.SessionProvider;

public class ShiroSessionProvider implements SessionProvider {
	private static final Logger logger = Logger.getLogger(ShiroSessionProvider.class);
	public final DefaultSecurityManager securityManager;

	public ShiroSessionProvider(DefaultSecurityManager securityManager) {
		this.securityManager = securityManager;
	}

	@Override
	public UserSession get(String sessionId) {
		Subject user = buildUserFromSessionId(sessionId);
		user.getSession().touch();
		return new ShiroSession(user);
	}
	
	private Subject buildUserFromSessionId(String sessionId) {
		Subject.Builder bld = new Subject.Builder(securityManager);
		
		if (sessionId != null) {
			bld.sessionId(sessionId);
		}
		
		return bld.buildSubject();
	}
	
	private static class ShiroSession implements UserSession {
		
		private final Subject subject;
		private final ShiroSecurityContext securityContext;
		private Locale locale;
		private List<Locale> acceptableLanguages = new ArrayList<>();
		private String selectedLanguage;

		public ShiroSession(Subject subject) {
			this.subject = subject;
			this.locale = Locale.getDefault();
			this.securityContext = new ShiroSecurityContext(subject);
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
		public boolean login(String user, String password) {
			try {
				subject.login(new UsernamePasswordToken(user, password));
				logger.info("User logged in, " + user);
				ShiroSessionProvider.logger.info("Logged in sucessfully: " + user);
				return true;
			}
			catch (Exception e) {
				ShiroSessionProvider.logger.error("Login attempt failed for user " + user, e);
				return false;
			}
		}

		@Override
		public void logout() {
			subject.logout();
		}

		@Override
		public String getId() {
			return subject.getSession().getId().toString();
		}

		@Override
		public SecurityContext getSecurityContext() {
			return securityContext;
		}

		@Override
		public String getUsername() {
			return subject.getPrincipal() == null ? "" : subject.getPrincipal().toString();
		}

		@Override
		public List<Locale> getAcceptableLanguages() {
			return acceptableLanguages;
		}

		@Override
		public void setAcceptableLanguages(List<Locale> acceptableLanguages) {
			this.acceptableLanguages = acceptableLanguages;
		}
	}
	
}