package fleetmanagement.backend.reports.datasource.vehicles;

public enum ConnectionStatus {

    ONLINE("o"),
    OFFLINE("f"),
    IRREGULAR("i");

    ConnectionStatus(String dbCode) {
        this.dbCode = dbCode;
    }

    private final String dbCode;

    public String getDbCode() {
        return dbCode;
    }

    public static ConnectionStatus getByDbCode (String code) {
        if (code.equals("o")) return ONLINE;
        if (code.equals("f")) return OFFLINE;
        if (code.equals("i")) return IRREGULAR;
        return null;
    }
}
