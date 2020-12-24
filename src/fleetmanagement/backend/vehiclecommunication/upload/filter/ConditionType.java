package fleetmanagement.backend.vehiclecommunication.upload.filter;

import gsp.util.DoNotObfuscate;

@DoNotObfuscate
public enum ConditionType {

    VEHICLE_NAME {
        @Override
        public String getResourceKey() {
            return "ad_filter_condition_vehicle_name";
        }
    }, FILE_NAME {
        @Override
        public String getResourceKey() {
            return "ad_filter_condition_file_name";
        }
    }, GROUP_NAME {
        @Override
        public String getResourceKey() {
            return "ad_filter_condition_group_name";
        }
    };

    public abstract String getResourceKey();

    public static ConditionType getByResourceKey(String resourceKey) {
        for (ConditionType conditionType : values()) {
            if (conditionType.getResourceKey().equals(resourceKey)) {
                return conditionType;
            }
        }
        return null;
    }

}