package fleetmanagement.frontend.security.config;

import javax.xml.bind.annotation.XmlRootElement;

import fleetmanagement.frontend.security.none.NoSecuritySessionProvider;
import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter.SessionProvider;
import gsp.util.MonotonicClock;

@XmlRootElement(name="no-security")
public class NoSecurityConfigXml implements SecurityConfiguration {

	@Override
	public SessionProvider buildSessionProvider() {
		return new NoSecuritySessionProvider(new MonotonicClock());
	}

}
