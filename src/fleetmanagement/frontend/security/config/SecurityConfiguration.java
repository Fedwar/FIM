package fleetmanagement.frontend.security.config;

import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter.SessionProvider;

public interface SecurityConfiguration {
	public SessionProvider buildSessionProvider();
}
