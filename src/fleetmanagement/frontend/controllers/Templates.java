package fleetmanagement.frontend.controllers;

import com.sun.jersey.api.NotFoundException;
import fleetmanagement.backend.events.Events;
import fleetmanagement.backend.notifications.NotificationService;
import fleetmanagement.frontend.languages.Languages;
import fleetmanagement.frontend.model.Security;
import fleetmanagement.frontend.transformers.DateTimeFormatter;
import fleetmanagement.frontend.transformers.DurationFormatter;
import gsp.configuration.LocalFiles;
import gsp.platform.Development;
import org.apache.log4j.Logger;
import org.rythmengine.Rythm;

import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.util.*;

public class Templates {
    private static NotificationService notificationService;
    private static File templateRoot;
    private static final Logger logger = Logger.getLogger(Templates.class);
    private static Languages languages;

    public static void init(Languages languages, NotificationService notifications) {
        Templates.languages = languages;
        notificationService = notifications;

        if (templateRoot == null) {
            templateRoot = LocalFiles.find("frontend/pages/");
            Map<String, Object> config = new HashMap<>();
            config.put("engine.mode", Development.isDeveloperPlatform() ? "dev" : "prod");

            List<File> templates = new ArrayList<>();
            templates.add(templateRoot);
            templates.add(LocalFiles.find("frontend/js/"));
            config.put("home.template.dir", templates);

            Rythm.init(config);
            Rythm.engine().registerFormatter(new DurationFormatter());
            Rythm.engine().registerFormatter(new DateTimeFormatter());
        }
    }

    public static String renderPage(String page, Locale l, SecurityContext sc, Object viewmodel, String username) {
        String result;
        try {
            File f = new File(templateRoot, page);
            result = renderFile(f, l, sc, viewmodel, username);
        } catch (Exception e) {
            logger.error("Unhandled exception while rendering a page! Sending an email notification.");
            if (notificationService != null)
                notificationService.processEvent(Events.serverException(e));
            throw e;
        }
        return result;
    }

    public static String renderJavascript(String script, Locale l, SecurityContext sc, String username) {
        File f = new File(templateRoot, "../js/" + script);
        return renderFile(f, l, sc, null, username);
    }

    private static String renderFile(File f, Locale l, SecurityContext sc, Object viewmodel, String username) {
        if (!f.exists())
            throw new NotFoundException();

        Rythm.engine().prepare(l);
        return Rythm.render(f, new TemplateArgs(viewmodel, sc, username));
    }

    private static class TemplateArgs extends HashMap<String, Object> {
        private static final long serialVersionUID = 9016076412828622497L;

        public TemplateArgs(Object viewmodel, SecurityContext sc, String username) {
            put("vm", viewmodel);
            put("security", new Security(sc, username));
            put("languages", languages);
        }
    }

}
