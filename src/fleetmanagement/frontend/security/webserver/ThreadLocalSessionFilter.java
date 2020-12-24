package fleetmanagement.frontend.security.webserver;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import fleetmanagement.config.FimConfig;
import fleetmanagement.frontend.UserSession;
import fleetmanagement.frontend.languages.Languages;
import fleetmanagement.frontend.security.config.SecurityConfigurator;
import gsp.configuration.LocalFiles;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.SecurityContext;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

@Component
public class ThreadLocalSessionFilter implements UserSession, ContainerRequestFilter, ContainerResponseFilter {

    public interface SessionProvider {
        UserSession get(String sessionId);
    }

    private static final Logger logger = Logger.getLogger(ThreadLocalSessionFilter.class);

    public static final String SESSION_COOKIE_NAME = "fim_session";
    public static final String LANGUAGE_COOKIE_NAME = "fim_language";
    private final ThreadLocal<UserSession> session = new ThreadLocal<>();
    private SessionProvider sessionProvider;
    @Autowired
    private FimConfig config;
    @Autowired
    private Languages languages;

    public ThreadLocalSessionFilter() {
    }

    public ThreadLocalSessionFilter(SessionProvider sessionProvider, Languages languages) {
        this.sessionProvider = sessionProvider;
        this.languages = languages;
    }

    @PostConstruct
    public void loadSecurityConfiguration() throws Exception {
        SecurityConfigurator configurator = new SecurityConfigurator();

        File securityConfig = new File(this.config.getConfigDirectory(), "security.config");
        File legacySecurityConfig = LocalFiles.findIfExisting("security.config");
        if (legacySecurityConfig != null && !securityConfig.exists()) {
            logger.warn("Migrating legacy security.config from " + legacySecurityConfig + " to " + securityConfig);
            FileUtils.moveFile(legacySecurityConfig, securityConfig);
        }

        sessionProvider = configurator.buildSessionProvider(securityConfig);
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        UserSession s = findSession(request);
        setLanguages(s, request);
        request.setSecurityContext(s.getSecurityContext());
        session.set(s);
        return request;
    }

    private void setLanguages(UserSession session, ContainerRequest request) {
        Locale sessionLanguage;
        LinkedHashMap acceptableLanguages = new LinkedHashMap();

        if (session.getSelectedLanguage() != null) {
            sessionLanguage = Locale.forLanguageTag(session.getSelectedLanguage());
        } else {
            Cookie languageCookie = request.getCookies().get(LANGUAGE_COOKIE_NAME);
            if (languageCookie != null) {
                sessionLanguage = Locale.forLanguageTag(languageCookie.getValue());
            } else {
                sessionLanguage = request.getAcceptableLanguages().stream()
                        .filter(x -> languages.getLanguages().contains(x.getLanguage())).findFirst().orElse(null);
            }
        }

        if (sessionLanguage != null) {
            acceptableLanguages.put(sessionLanguage.getLanguage(), sessionLanguage);
        }

        request.getAcceptableLanguages().stream().forEach(l -> acceptableLanguages.put(l.getLanguage(), l));
        session.setAcceptableLanguages(new ArrayList<>(acceptableLanguages.values()));

        if (sessionLanguage == null) {
            session.setLocale(Locale.ENGLISH);
        } else {
            session.setLocale(sessionLanguage);
        }

    }


    private UserSession findSession(ContainerRequest request) {
        Cookie sessionCookie = request.getCookies().get(SESSION_COOKIE_NAME);
        return sessionProvider.get(sessionCookie == null ? null : sessionCookie.getValue());
    }

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        setSessionCookie(response);
        return response;
    }

    private void setSessionCookie(ContainerResponse response) {
        String sessionId = session.get().getId();
        response.getHttpHeaders().add("Set-Cookie", new NewCookie(SESSION_COOKIE_NAME, sessionId));
        String selectedLanguage = getSelectedLanguage();
        if (selectedLanguage != null)
            response.getHttpHeaders().add("Set-Cookie", new NewCookie(LANGUAGE_COOKIE_NAME, selectedLanguage, "/", null, null, 999999999, false));
    }

    @Override
    public boolean login(String user, String password) {
        return session.get().login(user, password);
    }

    @Override
    public void logout() {
        session.get().logout();
    }

    @Override
    public Locale getLocale() {
        return session.get().getLocale();
    }

    @Override
    public String getId() {
        return session.get().getId();
    }

    @Override
    public void setLocale(Locale locale) {
        session.get().setLocale(locale);
    }

    @Override
    public String getSelectedLanguage() {
        return session.get().getSelectedLanguage();
    }

    @Override
    public void setSelectedLanguage(String language) {
        session.get().setSelectedLanguage(language);
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return session.get().getAcceptableLanguages();
    }

    @Override
    public void setAcceptableLanguages(List<Locale> acceptableLanguages) {
        session.get().setAcceptableLanguages(acceptableLanguages);
    }

    @Override
    public SecurityContext getSecurityContext() {
        return session.get().getSecurityContext();
    }

    @Override
    public String getUsername() {
        return session.get().getUsername();
    }
}
