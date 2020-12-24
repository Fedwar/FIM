package fleetmanagement.frontend;

import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.controllers.FrontendResource;
import fleetmanagement.frontend.controllers.Templates;
import fleetmanagement.frontend.languages.Languages;
import fleetmanagement.frontend.security.webserver.SecurityAnnotationProcessor;
import fleetmanagement.frontend.security.webserver.ThreadLocalSessionFilter;
import fleetmanagement.frontend.webserver.FrontendExceptionMapper;
import fleetmanagement.frontend.webserver.ModelAndViewWriter;
import fleetmanagement.webserver.LicenceFilter;
import fleetmanagement.webserver.RequestLoggingFilter;
import fleetmanagement.webserver.Webserver;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

@Component
public class Frontend {

    private static final Logger logger = Logger.getLogger(Frontend.class);
    private Webserver webserver;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private FimConfig config;
    @Autowired
    private Licence licence;
    @Autowired
    NotificationService notificationService;

    @Autowired
    ThreadLocalSessionFilter session;
    @Autowired
    Languages languages;

    public void start() throws Exception {
        this.webserver = new Webserver(config.frontendPort, false);
        TempDirectory tempDir = new TempDirectory(config.getDataDirectory());
        tempDir.clean();

        WebFiles.init();
        Templates.init(languages, notificationService);

        webserver.addRequestFilter(session);
        webserver.addResponseFilter(session);
        webserver.addRequestFilter(new RequestLoggingFilter());
        webserver.addRequestFilter(new LicenceFilter(licence));
        webserver.addResponseFilter(new RequestLoggingFilter());
        webserver.addMessageBodyWriter(new ModelAndViewWriter(session));
        webserver.addExceptionMapper(new FrontendExceptionMapper(notificationService));
        webserver.addResourceFilterFactory(new SecurityAnnotationProcessor());

        applicationContext.getBeansOfType(FrontendResource.class).values()
                .forEach(webserver::addResource);

        webserver.start();
    }

    public void stop() throws Exception {
        webserver.stop();
        WebFiles.shutdown();
    }

    public void openFrontendInBrowser() {
        if (Desktop.isDesktopSupported()) {
            try {
                logger.info("Opening frontend in browser");
                Desktop.getDesktop().browse(new URI("http://localhost:" + webserver.getPort()));
            } catch (Exception e) {
                logger.error("Unable to open Frontend in browser", e);
            }
        }
    }

}
