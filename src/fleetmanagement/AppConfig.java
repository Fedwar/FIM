package fleetmanagement;

import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Licence;
import fleetmanagement.config.LicenceImpl;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;

import java.io.File;

@Configuration
public class AppConfig {

    private static final Logger logger = Logger.getLogger(AppConfig.class);

    public static String CONFIG_FILE = "server.properties";
    public static final int BACKEND_DEFAULT_PORT = 29667;
    public static final int FRONTEND_DEFAULT_PORT = 29668;
    public static final String GROUP_IMPORT_DEFAULT_DIR = "./Import";
    public static final String FILTER_INCOMING_DEFAULT_DIR = "./Incoming";

    @Value("${BackendPort:29667}")
    private int backendPort;
    @Value("${BackendHttps1Port:29669}")
    private int backendHttps1Port;
    @Value("${BackendHttps2Port:29670}")
    private int backendHttps2Port;
    @Value("${UseHttpsByDefault:false}")
    private boolean httpsByDefault;
    @Value("${FrontendPort:29668}")
    private int frontendPort;
    @Value("${GroupImport:./Import}")
    private String groupImport;
    @Value("${FilterIncoming:./Incoming}")
    private String filterIncoming;
    @Value("${DataDirectory:data}")
    private String dataDirectory;

    @Bean
    public static PropertyResourceConfigurer propertyConfigurer() {
        PropertySourcesPlaceholderConfigurer c = new PropertySourcesPlaceholderConfigurer();
        c.setLocation(new FileSystemResource(CONFIG_FILE));
        c.setIgnoreResourceNotFound(true);
        return c;
    }

    @Bean
    public Licence licence() {
        Licence l = new LicenceImpl(new File(dataDirectory));
        logger.debug("Licence created");
        return l;
    }

    @Bean
    public FimConfig fimConfig() {
        FimConfig c = new FimConfig(groupImport, filterIncoming, dataDirectory, frontendPort,
                new int[] {backendPort, backendHttps1Port, backendHttps2Port}, httpsByDefault);
        logger.debug("Application config created");
        logger.info("Using data directory: " + c.getDataDirectory().getAbsolutePath());
        return c;
    }
}
