package fleetmanagement.backend.notifications.settings;

import gsp.util.DoNotObfuscate;

@DoNotObfuscate
public enum Type {
    DIAGNOSIS_MAX_ERRORS {
        @Override
        public String getResourceKey() {
            return "notif_diagnosis_max_errors";
        }
    }, DIAGNOSED_DEVICE_ERROR {
        @Override
        public String getResourceKey() {
            return "notif_diagnosed_device_error";
        }
    }, INDICATOR_VALUE_RANGE {
        @Override
        public String getResourceKey() {
            return "notif_indicator_out_of_range";
        }
    }, INDICATOR_INVALID_VALUE {
        @Override
        public String getResourceKey() {
            return "notif_indicator_invalid_value";
        }
    }, PACKAGE_INSTALL_ERROR {
        @Override
        public String getResourceKey() {
            return "notif_package_install_error";
        }
    }, SERVER_EXCEPTION {
        @Override
        public String getResourceKey() {
            return "notif_server_error";
        }
    }, PACKAGE_IMPORT_ERROR {
        @Override
        public String getResourceKey() {
            return "notif_import_error";
        }
    }, VEHICLE_OFFLINE {
        @Override
        public String getResourceKey() {
            return "notif_vehicle_offline";
        }
    };

    public abstract String getResourceKey();

    public static Type getByResourceKey(String resourceKey) {
        for (Type type : values()) {
            if (type.getResourceKey().equals(resourceKey)) {
                return type;
            }
        }
        return null;
    }

}
