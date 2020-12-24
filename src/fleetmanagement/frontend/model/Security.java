package fleetmanagement.frontend.model;

import fleetmanagement.frontend.security.SecurityPermission;
import fleetmanagement.frontend.security.SecurityRole;

import javax.ws.rs.core.SecurityContext;
import java.util.*;
import java.util.stream.Collectors;

import static fleetmanagement.frontend.security.SecurityPermission.*;

public class Security {
    public final boolean hasAddOnsPermission;
    public final boolean hasGarbageCollectionPermission;
    public final boolean hasVehicleGroupChangePermission;
    public final boolean hasGroupsEditPermission;
    public final boolean hasVehicleNameEditPermission;
    public final boolean hasVehicleDeletePermission;
    public final boolean hasAutoPackageSyncPermission;
    public final boolean hasEmailNotificationsPermission;
    public final boolean hasFiltersEditPermission;
    public final boolean hasInterfacesActionsPermission;
    public final boolean hasPackagesActionsPermission;
    public final boolean hasWritePermission;
    public final boolean hasDataMigrationPermission;
    public final boolean hasSettingsPermission;
    public final boolean hasNotificationTestPermission;
    public final boolean hasPackagePreprocessingPermission;

    public final String username;
    private final List<SecurityRole> roles;
    public final List<String> userRoles;

    private static Map<SecurityPermission, List<SecurityRole>> permissions = new HashMap<>();

    static {
        permissions.put(ADD_ONS, Collections.singletonList(SecurityRole.Admin));
        permissions.put(GARBAGE_COLLECTION, Arrays.asList(SecurityRole.Admin, SecurityRole.Config));
        permissions.put(GROUPS_EDIT, Arrays.asList(SecurityRole.Admin, SecurityRole.Config));
        permissions.put(VEHICLE_GROUP_CHANGE, Arrays.asList(SecurityRole.Admin, SecurityRole.Config, SecurityRole.User));
        permissions.put(VEHICLE_NAME_EDIT, Arrays.asList(SecurityRole.Admin, SecurityRole.Config));
        permissions.put(VEHICLE_DELETE, Arrays.asList(SecurityRole.Admin, SecurityRole.Config));
        permissions.put(AUTO_PACKAGE_SYNC, Arrays.asList(SecurityRole.Admin, SecurityRole.Config, SecurityRole.User));
        permissions.put(EMAIL_NOTIFICATIONS, Arrays.asList(SecurityRole.Admin, SecurityRole.Config, SecurityRole.User));
        permissions.put(FILTERS_EDIT, Arrays.asList(SecurityRole.Admin, SecurityRole.Config));
        permissions.put(INTERFACES_ACTIONS, Arrays.asList(SecurityRole.Admin, SecurityRole.Config, SecurityRole.User));
        permissions.put(PACKAGES_ACTIONS, Arrays.asList(SecurityRole.Admin, SecurityRole.Config, SecurityRole.User));
        permissions.put(WRITE, Arrays.asList(SecurityRole.Admin, SecurityRole.Config, SecurityRole.User));
        permissions.put(DATA_MIGRATION, Collections.singletonList(SecurityRole.Admin));
        permissions.put(SETTINGS, Arrays.asList(SecurityRole.Admin, SecurityRole.Config));
        permissions.put(NOTIFICATION_TEST, Arrays.asList(SecurityRole.Admin, SecurityRole.Config));
        permissions.put(PACKAGE_PREPROCESSING, Arrays.asList(SecurityRole.Admin, SecurityRole.Config, SecurityRole.User));
    }

    public Security(SecurityContext context, String username) {
        this.username = username;
        roles = Arrays.stream(SecurityRole.values())
                .filter(role -> context.isUserInRole(role.toString()))
                .collect(Collectors.toList());

        userRoles = roles.stream().map(Objects::toString)
                .collect(Collectors.toList());

        hasAddOnsPermission = doesTheRoleHavePermission(ADD_ONS);
        hasGarbageCollectionPermission = doesTheRoleHavePermission(GARBAGE_COLLECTION);
        hasGroupsEditPermission = doesTheRoleHavePermission(GROUPS_EDIT);
        hasVehicleGroupChangePermission = doesTheRoleHavePermission(VEHICLE_GROUP_CHANGE);
        hasVehicleNameEditPermission = doesTheRoleHavePermission(VEHICLE_NAME_EDIT);
        hasVehicleDeletePermission = doesTheRoleHavePermission(VEHICLE_DELETE);
        hasAutoPackageSyncPermission = doesTheRoleHavePermission(AUTO_PACKAGE_SYNC);
        hasEmailNotificationsPermission = doesTheRoleHavePermission(EMAIL_NOTIFICATIONS);
        hasFiltersEditPermission = doesTheRoleHavePermission(FILTERS_EDIT);
        hasInterfacesActionsPermission = doesTheRoleHavePermission(INTERFACES_ACTIONS);
        hasPackagesActionsPermission = doesTheRoleHavePermission(PACKAGES_ACTIONS);
        hasWritePermission = doesTheRoleHavePermission(WRITE);
        hasDataMigrationPermission = doesTheRoleHavePermission(DATA_MIGRATION);
        hasSettingsPermission = doesTheRoleHavePermission(SETTINGS);
        hasNotificationTestPermission = doesTheRoleHavePermission(NOTIFICATION_TEST);
        hasPackagePreprocessingPermission = doesTheRoleHavePermission(PACKAGE_PREPROCESSING);
    }

    private boolean doesTheRoleHavePermission(SecurityPermission permission) {
        return permissions.get(permission).stream().anyMatch(role -> roles.contains(role));
    }
}
