package fleetmanagement.backend.vehiclecommunication.upload.filter;

import gsp.util.DoNotObfuscate;

import java.util.Arrays;
import java.util.Objects;

import static fleetmanagement.backend.vehiclecommunication.upload.filter.ConditionType.*;

@DoNotObfuscate
public class UploadFilterCondition {
    public final ConditionType type;
    public final String matchString;
    public String regex;
    public final static ConditionType[] regexTypes = {VEHICLE_NAME, GROUP_NAME, FILE_NAME};

    public UploadFilterCondition(ConditionType type, String matchString) {
        this.type = type;
        this.matchString = matchString;
        init();
    }

    public boolean matches(String value) {
        if (regex == null)
            return value.equals(matchString);
        else
            return value.matches(regex);
    }

    public void init() {
        if (Arrays.stream(regexTypes).anyMatch(type::equals))
            regex = matchString.replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*");
        else
            regex = null;
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UploadFilterCondition that = (UploadFilterCondition) o;
        return type == that.type &&
                Objects.equals(matchString, that.matchString) &&
                Objects.equals(regex, that.regex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, matchString, regex);
    }
}
