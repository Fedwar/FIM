package fleetmanagement.backend.repositories.exception;

import fleetmanagement.frontend.I18n;

import java.util.Locale;

public class LocalizableException extends RuntimeException {

    private String key;
    private Object[] params;

    public LocalizableException(String key, Object... params) {
        this.key = key;
        this.params = params;
    }

    public LocalizableException(Throwable cause, String key, Object... params) {
        super(cause);
        this.key = key;
        this.params = params;
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage(Locale.ENGLISH);
    }

    public String getLocalizedMessage(Locale locale) {
        try {
            return I18n.get(locale, key, params);
        } catch (Exception e) {
            return key;
        }
    }
}
