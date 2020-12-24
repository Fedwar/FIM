package fleetmanagement.backend.settings;

import fleetmanagement.backend.repositories.Persistable;
import gsp.util.DoNotObfuscate;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Objects;

@Entity
@DoNotObfuscate
public class Setting implements Persistable<SettingName> {

    @Id
    private SettingName id;
    @Column
    private String stringValue;
    @Column
    private Long longValue;
    @Column
    private Double doubleValue;

    public Setting() {
    }

    public Setting(SettingName id, Object value) {
        this.id = id;
        setValue(value);
    }

    public String getStringValue() {
        return stringValue;
    }

    public Object getValue() {
        return Arrays.asList(stringValue, longValue, doubleValue).stream()
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    public void setValue(Object value) {
        if (value instanceof String)
            stringValue = (String)value;
        else if (value instanceof Long)
            longValue = (Long)value;
        else if (value instanceof Double)
            doubleValue = (Double)value;
        else
            throw new UnsupportedOperationException("Setting can not have value of type " + Object.class);
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }



    @Override
    public SettingName id() {
        return id;
    }

    @Override
    public Setting clone() {
        try {
            return (Setting)super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
