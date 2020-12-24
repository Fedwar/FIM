package fleetmanagement.backend.notifications.settings;

import gsp.util.DoNotObfuscate;

@DoNotObfuscate
public enum Parameter {
    ERROR_LIMIT {
        @Override
        public String getResourceKey() {
            return "notif_error_limit";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, INDICATOR_ID {
        @Override
        public String getResourceKey() {
            return "notif_indicator_id";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, DEVICE_NAME{
        @Override
        public String getResourceKey() {
            return "notif_device_name";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, UPPER_LIMIT {
        @Override
        public String getResourceKey() {
            return "notif_upper_limit";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, LOWER_LIMIT {
        @Override
        public String getResourceKey() {
            return "notif_lower_limit";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, INVALID_VALUE {
        @Override
        public String getResourceKey() {
            return "notif_invalid_value";
        }

        @Override
        public String getDescriptionKey() {
            return "notif_invalid_value_desc";
        }
    }, PACKAGE_TYPE {
        @Override
        public String getResourceKey() {
            return "notif_package_type";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, VEHICLE_NAME {
        @Override
        public String getResourceKey() {
            return "notif_vehicle_name";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, GROUP_NAME {
        @Override
        public String getResourceKey() {
            return "notif_group_name";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, VEHICLE_OFFLINE_TIMEOUT {
        @Override
        public String getResourceKey() {
            return "notif_vehicle_offline_timeout";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, ALL_VEHICLES {
        @Override
        public String getResourceKey() {
            return "notif_all_vehicles";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    }, REPEAT_DELAY {
        @Override
        public String getResourceKey() {
            return "notif_repeat_delay";
        }

        @Override
        public String getDescriptionKey() {
            return null;
        }
    };

    public abstract String getResourceKey();

    public abstract String getDescriptionKey();

    public static Parameter getByResourceKey(String resourceKey) {
        for (Parameter type : values()) {
            if (type.getResourceKey().equals(resourceKey)) {
                return type;
            }
        }
        return null;
    }

}
