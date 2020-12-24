package fleetmanagement.test;

import java.util.*;

import fleetmanagement.frontend.UserSession;

public class SessionStub implements UserSession {

	private Locale locale;
	private SecurityContextStub securityContext;
	private List<Locale> acceptableLanguages = new ArrayList<>();
	private String selectedLanguage;

	public SessionStub() {
		this(Locale.ENGLISH);
		Locale.setDefault(new Locale("en"));

		acceptableLanguages.add(Locale.ENGLISH);
		acceptableLanguages.add(Locale.GERMAN);
	}
	
	public SessionStub(Locale locale) {
		this.locale = locale;
		this.securityContext = new SecurityContextStub();
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	@Override
	public boolean login(String user, String password) {
		return false;
	}

	@Override
	public void logout() {
	}

	@Override
	public String getId() {
		return "0";
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
	public SecurityContextStub getSecurityContext() {
		return this.securityContext;
	}

	@Override
	public String getUsername() {
		return null;
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
