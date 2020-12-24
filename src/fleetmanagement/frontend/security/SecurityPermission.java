package fleetmanagement.frontend.security;

import gsp.util.DoNotObfuscate;

@DoNotObfuscate
public enum SecurityPermission {
    ADD_ONS,
    GARBAGE_COLLECTION,
    GROUPS_EDIT,
    VEHICLE_GROUP_CHANGE,
    VEHICLE_NAME_EDIT,
    VEHICLE_DELETE,
    AUTO_PACKAGE_SYNC,
    EMAIL_NOTIFICATIONS,
    FILTERS_EDIT,
    INTERFACES_ACTIONS,
    PACKAGES_ACTIONS,
    WRITE,
    SETTINGS,
    NOTIFICATION_TEST,
    DATA_MIGRATION,
    PACKAGE_PREPROCESSING
}
