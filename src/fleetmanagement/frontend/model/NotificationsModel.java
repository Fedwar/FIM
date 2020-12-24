package fleetmanagement.frontend.model;

import fleetmanagement.backend.accounts.Account;
import fleetmanagement.backend.accounts.AccountRepository;
import fleetmanagement.backend.diagnosis.DiagnosisRepository;
import fleetmanagement.backend.groups.GroupRepository;
import fleetmanagement.backend.notifications.settings.NotificationSetting;
import fleetmanagement.backend.notifications.settings.NotificationSettingRepository;
import fleetmanagement.backend.notifications.settings.Parameter;
import fleetmanagement.backend.notifications.settings.Type;
import fleetmanagement.backend.operationData.OperationDataRepository;
import fleetmanagement.backend.vehicles.VehicleRepository;
import fleetmanagement.config.Licence;
import fleetmanagement.frontend.I18n;
import fleetmanagement.frontend.UserSession;

import java.util.*;
import java.util.stream.Collectors;

import static fleetmanagement.backend.notifications.settings.Parameter.*;

public class NotificationsModel extends Admin {

    private UserSession session;
    public List<Notification> notifications;
    public Parameter[] parameters = {PACKAGE_TYPE, INDICATOR_ID, DEVICE_NAME, ERROR_LIMIT, UPPER_LIMIT, LOWER_LIMIT
            , INVALID_VALUE, VEHICLE_OFFLINE_TIMEOUT, ALL_VEHICLES, VEHICLE_NAME, GROUP_NAME, REPEAT_DELAY};
    public Set<String> indicatorNames;
    public Set<String> packageTypeNames;
    public List<String> groupNames;
    public List<String> vehicleNames;
    public Set<String> deviceNames;
    public String defaultEmail = "";

    public NotificationsModel(UserSession session, Licence licence
            , NotificationSettingRepository notificationSettingRepository, DiagnosisRepository diagnosisRepository
            , OperationDataRepository operationDataRepository, VehicleRepository vehicleRepository
            , GroupRepository groupRepository, AccountRepository accountRepository) {
        super(licence);
        this.session = session;
        notifications = notificationSettingRepository.stream()
                .map(n -> new Notification(n, session.getLocale()))
                .collect(Collectors.toList());
        deviceNames = diagnosisRepository.listAll().stream()
                .flatMap(d -> d.getDevices().stream())
                .map(d -> DiagnosisDetails.getDeviceName(d, session))
                .filter(s -> !s.equals("-"))
                .collect(Collectors.toSet());
        indicatorNames = operationDataRepository.stream()
                .flatMap(o -> o.indicators.stream())
                .map(i -> i.id)
                .collect(Collectors.toSet());
        packageTypeNames = licence.getPackageTypes().stream()
                .map(i -> i.getResourceKey())
                .collect(Collectors.toSet());
        vehicleNames = new ArrayList<>();
        vehicleNames.add("");
        vehicleNames.addAll(vehicleRepository.listAll().stream()
                .map(i -> i.uic)
                .collect(Collectors.toSet()));
        groupNames = new ArrayList<>();
        groupNames.add("");
        groupNames.addAll(groupRepository.stream()
                .map(i -> i.name)
                .collect(Collectors.toSet()));

        Account account = accountRepository.findByLogin(session.getUsername());
        if (account != null)
            defaultEmail = account.email;

    }



    public static class Notification {
        public String id;
        public Type type;
        public String mailList;
        public Map<Parameter, String> parameters;

        public Notification(NotificationSetting notificationSetting, Locale locale) {
            id = notificationSetting.id.toString();
            type = notificationSetting.type;
            mailList = notificationSetting.getMailList();
            parameters = notificationSetting.getParameters().entrySet().stream()
                    .filter(e -> e.getValue() != null && !e.getValue().isEmpty())
                    .collect(Collectors.toMap(e -> e.getKey(),
                            e -> (e.getKey() == PACKAGE_TYPE ? I18n.get(locale, e.getValue()) : e.getValue()) )
                    );
        }
    }
}
