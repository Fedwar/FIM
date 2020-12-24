package fleetmanagement.frontend.controllers;

import fleetmanagement.frontend.*;

public abstract class FrontendController implements FrontendResource {
	
	protected final UserSession session;
	
	public FrontendController(UserSession session) {
		this.session = session;
	}
	
	protected String i18n(String key, Object... args) {
		return I18n.get(session, key, args);
	}
}