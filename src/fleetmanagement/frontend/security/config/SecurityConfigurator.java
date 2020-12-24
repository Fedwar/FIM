package fleetmanagement.frontend.security.config;

import java.io.File;

import javax.xml.bind.*;

import org.apache.log4j.Logger;

import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter.SessionProvider;

public class SecurityConfigurator {
	private static final Class<?>[] ACCEPTED_CONFIG_FORMATS = { ActiveDirectoryConfigXml.class, UserListConfigXml.class, NoSecurityConfigXml.class };
	private static final Logger logger = Logger.getLogger(SecurityConfigurator.class);

	public SessionProvider buildSessionProvider(File securityConfig) throws Exception {
		try {
			return buildSessionProviderInternal(securityConfig);
		}
		catch (Exception e) {
			logger.error("Unable to load security configuration from " + securityConfig, e);
			throw new RuntimeException("Unable to load security configuration from " + securityConfig, e);
		}
	}

	private SessionProvider buildSessionProviderInternal(File securityConfig) throws JAXBException {
		if (!securityConfig.exists())
			return new NoSecurityConfigXml().buildSessionProvider();
		
		SecurityConfiguration config = loadSecurityConfiguration(securityConfig);
		return config.buildSessionProvider();
	}
	
	private SecurityConfiguration loadSecurityConfiguration(File securityConfig) throws JAXBException {
		JAXBContext jaxb = JAXBContext.newInstance(ACCEPTED_CONFIG_FORMATS);
		return (SecurityConfiguration) jaxb.createUnmarshaller().unmarshal(securityConfig);
	}
}
