package fleetmanagement.frontend;

import java.util.List;
import java.util.Locale;

import javax.ws.rs.core.SecurityContext;

public interface UserSession {
	String getId();
	
	boolean login(String user, String password);
	void logout();

	Locale getLocale();
	void setLocale(Locale locale);

	String getSelectedLanguage();
	void setSelectedLanguage(String language);

	List<Locale> getAcceptableLanguages();
	void setAcceptableLanguages(List<Locale> acceptableLanguages);

	SecurityContext getSecurityContext();

	String getUsername();
}
