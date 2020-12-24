package fleetmanagement.frontend.languages;

import fleetmanagement.config.FimConfig;
import fleetmanagement.config.Licence;
import gsp.util.DoNotObfuscate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

@DoNotObfuscate
@Component
public class Languages {
    private static final Logger logger = Logger.getLogger(Languages.class);
    private static final String FILE_NAME = "languages.properties";
    private final Map<String, Locale> supportedLanguages = new LinkedHashMap<>();

    private final File languagesDirectory;
    @Autowired
    private Licence licence;
    private LinkedList<String> configLanguages = new LinkedList<>();

    @Autowired
    public Languages(FimConfig config) {
        this.languagesDirectory = config.getConfigDirectory();
    }

    public Languages(File languagesDirectory, Licence licence) {
        this.languagesDirectory = languagesDirectory;
        this.licence = licence;
        load();
    }

    @PostConstruct
    public void load() {
        addSupportedLanguage(Locale.GERMAN);
        addSupportedLanguage(Locale.ENGLISH);
        addSupportedLanguage(Locale.forLanguageTag("es"));
        addSupportedLanguage(Locale.FRENCH);
        addSupportedLanguage(Locale.forLanguageTag("cs"));
        addSupportedLanguage(Locale.forLanguageTag("ru"));
        addSupportedLanguage(Locale.forLanguageTag("pl"));

        loadLanguagesFromConfig(languagesDirectory);
    }

    private void addSupportedLanguage(Locale l) {
        supportedLanguages.put(l.getLanguage(), l);
    }

    public Map<String, Locale> getSupportedLanguages() {
        return supportedLanguages;
    }

    private void loadLanguagesFromConfig(File languagesDirectory) {
        File propertiesFile = new File(languagesDirectory, FILE_NAME);
        if (propertiesFile.exists()) {
            Properties properties = new Properties();
            try {
                FileInputStream fileInputStream = new FileInputStream(propertiesFile);
                properties.load(fileInputStream);
                fileInputStream.close();
            } catch (IOException e) {
                logger.warn("Properties for Languages not found");
            }
            properties.entrySet().stream().filter(e -> e.getValue().equals("1"))
                    .forEach(e -> configLanguages.add(e.getKey().toString()));
            configLanguages.retainAll(supportedLanguages.keySet());
        } else {
            configLanguages.addAll(supportedLanguages.keySet());
        }
    }

    public List<String> getLanguages() {
        List<String> languages = (List<String>) configLanguages.clone();
        languages.retainAll(licence.getLanguages());
        return languages;
    }
}
