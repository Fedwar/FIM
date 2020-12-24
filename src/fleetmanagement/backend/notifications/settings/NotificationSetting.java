package fleetmanagement.backend.notifications.settings;

import fleetmanagement.backend.packages.PackageType;
import fleetmanagement.backend.repositories.Persistable;
import gsp.util.WrappedException;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class NotificationSetting implements Persistable<UUID> {

    private static final String EMAIL_REGEX = "^[\\w-\\+]+(\\.[\\w]+)*@[\\w-]+(\\.[\\w]+)*(\\.[a-z]{2,})$";
    Pattern pattern = Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE);

    public final UUID id;
    public final Type type;
    private String mailList;
    private InternetAddress[] mailAddress;
    private Map<Parameter, String> parameters = new LinkedHashMap<>();


    public NotificationSetting(UUID id, Type type, String mailList, Map<Parameter, String> parameters) throws AddressException {
        if (type == null)
            throw new NullPointerException("NotificationSetting type is null");
        this.id = id;
        this.type = type;
        if (parameters != null) {
            for (Map.Entry<Parameter, String> entry : parameters.entrySet()) {
                this.parameters.put(entry.getKey(), defaultIfNull(entry.getValue(), ""));
            }
        }
        setMailList(mailList);
    }

    public Map<Parameter, String> getParameters() {
        return new LinkedHashMap<>(parameters);
    }

    public String getParameter(Parameter param) {
        return defaultIfNull(parameters.get(param), "");
    }

    public NotificationSetting(Type type, String mailList, Map<Parameter, String> parameters) throws AddressException {
        this(UUID.randomUUID(), type, mailList, parameters);
    }

    public static NotificationSetting diagnosedDeviceError(String deviceName, String mailList) throws AddressException {
        HashMap<Parameter, String> parameters = new HashMap<>();
        parameters.put(Parameter.DEVICE_NAME, deviceName);
        return new NotificationSetting(UUID.randomUUID(), Type.DIAGNOSED_DEVICE_ERROR, mailList, parameters);
    }

    public static NotificationSetting diagnosisMaxErrors(long errorLimit, String mailList) throws AddressException {
        HashMap<Parameter, String> parameters = new HashMap<>();
        parameters.put(Parameter.ERROR_LIMIT, String.valueOf(errorLimit));
        return new NotificationSetting(UUID.randomUUID(), Type.DIAGNOSIS_MAX_ERRORS, mailList, parameters);
    }

    public static NotificationSetting packageInstallError(String mailList) throws AddressException {
        HashMap<Parameter, String> parameters = new HashMap<>();
        return new NotificationSetting(UUID.randomUUID(), Type.PACKAGE_INSTALL_ERROR, mailList, parameters);
    }

    public static NotificationSetting serverException(String mailList) throws AddressException {
        HashMap<Parameter, String> parameters = new HashMap<>();
        return new NotificationSetting(UUID.randomUUID(), Type.SERVER_EXCEPTION, mailList, parameters);
    }

    public static NotificationSetting vehicleOffline(String mailList, Integer offlineDelay, Integer repeatDelay,
                                                     String groupName, String vehicleName, String allVehicles) throws AddressException {
        HashMap<Parameter, String> parameters = new HashMap<>();
        parameters.put(Parameter.VEHICLE_OFFLINE_TIMEOUT, String.valueOf(offlineDelay));
        parameters.put(Parameter.REPEAT_DELAY, String.valueOf(repeatDelay));
        if (allVehicles != null)
            parameters.put(Parameter.ALL_VEHICLES, "true");
        parameters.put(Parameter.GROUP_NAME, groupName);
        parameters.put(Parameter.VEHICLE_NAME, vehicleName);
        return new NotificationSetting(UUID.randomUUID(), Type.VEHICLE_OFFLINE, mailList, parameters);
    }

    public static NotificationSetting indicatorValueRange(String indicatorId, long lowerLimit, long upperLimit, String mailList) throws AddressException {
        HashMap<Parameter, String> parameters = new HashMap<>();
        parameters.put(Parameter.INDICATOR_ID, indicatorId);
        parameters.put(Parameter.UPPER_LIMIT, String.valueOf(upperLimit));
        parameters.put(Parameter.LOWER_LIMIT, String.valueOf(lowerLimit));
        return new NotificationSetting(UUID.randomUUID(), Type.INDICATOR_VALUE_RANGE, mailList, parameters);
    }

    public static NotificationSetting importError(PackageType packageType, String mailList) throws AddressException {
        HashMap<Parameter, String> parameters = new HashMap<>();
        parameters.put(Parameter.PACKAGE_TYPE, packageType.name());
        return new NotificationSetting(UUID.randomUUID(), Type.PACKAGE_IMPORT_ERROR, mailList, parameters);
    }

    public static NotificationSetting indicatorInvalidValue(String indicatorId, String invalidValue, String mailList) throws AddressException {
        HashMap<Parameter, String> parameters = new HashMap<>();
        parameters.put(Parameter.INDICATOR_ID, indicatorId);
        parameters.put(Parameter.INVALID_VALUE, String.valueOf(invalidValue));
        return new NotificationSetting(UUID.randomUUID(), Type.INDICATOR_INVALID_VALUE, mailList, parameters);
    }

    public Type type() {
        return type;
    }

    @Override
    public UUID id() {
        return id;
    }

    @Override
    public NotificationSetting clone() {
        try {
            return (NotificationSetting) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new WrappedException(e);
        }
    }

    public String getMailList() {
        return mailList;
    }

    public void setMailList(String mailList) throws AddressException {
        if (mailList == null || mailList.isEmpty())
            throw new AddressException("");
        this.mailAddress = InternetAddress.parse(mailList);
        for (InternetAddress mailAddress : mailAddress) {
            if (!pattern.matcher(mailAddress.getAddress()).matches())
                throw new AddressException("");
        }
        this.mailList = mailList;
    }

    public InternetAddress[] getMailAddress() {
        return mailAddress;
    }


}
