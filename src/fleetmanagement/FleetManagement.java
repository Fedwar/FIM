package fleetmanagement;

import fleetmanagement.backend.Backend;
import fleetmanagement.frontend.Frontend;
import gsp.logging.Log4j;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

public class FleetManagement {

    AnnotationConfigApplicationContext applicationContext;
    private final Frontend frontend;
    private final Backend backend;

    private static final Logger logger = Logger.getLogger(FleetManagement.class);

    static {
        Log4j.ensureIsInitialized();
    }

    public static void main(String[] args) throws Exception {
        logger.info("Starting FIM-Server " + FleetManagement.getVersion());
        FleetManagement app = new FleetManagement();
        app.start();
        Runtime.getRuntime().addShutdownHook(new Thread(app.new ShutdownHook()));

        if (Arrays.asList(args).contains("--open-browser"))
            app.frontend.openFrontendInBrowser();

        while (true)
            Thread.sleep(1000);
    }

    public FleetManagement() {
        this(null);
    }

    public FleetManagement(File dataDirectory) {
        if (dataDirectory != null) {
            System.setProperty("DataDirectory", dataDirectory.getAbsolutePath());
        }
        applicationContext = new AnnotationConfigApplicationContext();
        applicationContext.setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
        applicationContext.scan("fleetmanagement");
        applicationContext.refresh();
        frontend = applicationContext.getBean(Frontend.class);
        backend = applicationContext.getBean(Backend.class);
    }

    public static String getVersion() {
        String version = FleetManagement.class.getPackage().getImplementationVersion();
        return version == null ? "Developer Build" : version;
    }

    public void start() throws Exception {
        try {
            Locale.setDefault(Locale.ENGLISH);
            backend.start();
            frontend.start();
        } catch (Exception e) {
            logger.error("Could not start application.", e);
            throw e;
        }
    }

    public void stop() throws Exception {
        backend.stop();
        frontend.stop();
    }

    private class ShutdownHook implements Runnable {
        @Override
        public void run() {
            try {
                stop();
                applicationContext.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
