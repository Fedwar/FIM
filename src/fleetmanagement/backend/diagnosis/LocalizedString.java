package fleetmanagement.backend.diagnosis;

import org.apache.log4j.Logger;

import java.util.*;

public class LocalizedString {
    private Map<String, String> localeMap = new HashMap<>();

    public LocalizedString() {}

    public LocalizedString(Map<String, String> values) {
        if (values != null)
            this.localeMap = new HashMap<>(values);
    }

    public LocalizedString(String value) {
        if (value != null)
            localeMap.put(Locale.getDefault().getLanguage(), value);
    }

    public LocalizedString(Locale locale, String value) {
        if (value != null && locale != null)
            localeMap.put(locale.getLanguage(), value);
    }

    public boolean contains(Locale locale) {
        return localeMap.containsKey(locale.getLanguage());
    }

    public String get(List<Locale> localeList) {
        String value = null;
        for (Locale locale : localeList) {
            value = localeMap.get(locale.getLanguage());
            if (value != null) break;
        }
        return (value == null ? "" : value);
    }

    public String get(Locale locale) {
        String value = localeMap.get(locale.getLanguage());
        return (value == null ? "" : value);
    }

    public Map<String, String> getLocaleMap() {
        return localeMap;
    }

    public String put(String locale, String value) {
        return localeMap.put(locale, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocalizedString that = (LocalizedString) o;
        return Objects.equals(localeMap, that.localeMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localeMap);
    }
}


