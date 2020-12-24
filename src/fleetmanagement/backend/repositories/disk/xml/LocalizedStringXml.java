package fleetmanagement.backend.repositories.disk.xml;


import fleetmanagement.backend.diagnosis.LocalizedString;
import gsp.util.DoNotObfuscate;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@DoNotObfuscate
public class LocalizedStringXml {
    @XmlElement
    public List<LocalizedItemXml> localeList = new ArrayList<>();

    public LocalizedStringXml() {
    }

    public LocalizedStringXml(LocalizedString o) {
        if (o != null)
            localeList = o.getLocaleMap().entrySet().stream()
                    .map(LocalizedItemXml::new)
                    .collect(Collectors.toList());
    }

    public LocalizedString toLocalizedString() {
        return new LocalizedString(localeList.stream()
                .collect(Collectors.toMap(LocalizedItemXml::getLocale, LocalizedItemXml::getValue)));
    }

    @DoNotObfuscate
    public static class LocalizedItemXml {
        @XmlAttribute
        public String locale;
        @XmlAttribute
        public String value;

        public LocalizedItemXml() {
        }

        public LocalizedItemXml(Map.Entry<String, String> entry) {
            this.locale = entry.getKey();
            this.value = entry.getValue();
        }

        public String getLocale() {
            return locale;
        }

        public String getValue() {
            return value;
        }
    }
}

