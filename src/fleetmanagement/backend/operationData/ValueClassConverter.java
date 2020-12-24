package fleetmanagement.backend.operationData;

public class ValueClassConverter {
    public Object value;

    public ValueClassConverter(String value, String valueClass) {
        if (value == null || valueClass == null) {
            this.value = value;
        } else {
            Class<?> cls = null;
            try {
                cls = Class.forName(valueClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            if (cls == String.class) {
                this.value = value;
            } else if (cls == Double.class) {
                this.value = Double.parseDouble(value);
            } else if (cls == Long.class) {
                this.value = Long.parseLong(value);
            } else if (cls == Integer.class) {
                this.value = Integer.parseInt(value);
            } else if (cls == Boolean.class) {
                this.value = Boolean.valueOf(value);
            }
        }
    }

    public Object getValue() {
        return value;
    }
}
