package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.diagnosis.DiagnosisHistoryRepository;
import fleetmanagement.backend.diagnosis.ErrorCategory;
import fleetmanagement.backend.diagnosis.LocalizedString;
import fleetmanagement.backend.diagnosis.StateEntry;
import fleetmanagement.backend.repositories.QueryBuilder;
import fleetmanagement.config.FimConfig;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class DiagnosisHistorySQLiteRepository implements DiagnosisHistoryRepository {

    private final File directory;
    private static final Logger logger = Logger.getLogger(DiagnosisHistorySQLiteRepository.class);
    public static final String DB_NAME = "diagnosisHistory.db";

    @Autowired
    public DiagnosisHistorySQLiteRepository(FimConfig config) {
        this(config.getVehicleDirectory());
    }

    public DiagnosisHistorySQLiteRepository(File directory) {
        this.directory = directory;
    }

    @Override
    public void addHistory(UUID vehicleId, String deviceId, StateEntry stateEntry) {
        transaction(vehicleId, connection -> {
            insert(connection, deviceId, stateEntry);
        });
    }

    @Override
    public void addHistory(UUID vehicleId, String deviceId, List<StateEntry> stateEntries) {
        transaction(vehicleId, connection -> {
            for (StateEntry stateEntry : stateEntries) {
                insert(connection, deviceId, stateEntry);
            }
        });
    }

    @Override
    public List<StateEntry> getHistoryRange(UUID vehicleId, String deviceId, ZonedDateTime periodStart, ZonedDateTime periodEnd) {
        return (List<StateEntry>) connect(vehicleId, connection -> {
            return getHistoryRange(connection, deviceId, periodStart, periodEnd);
        });
    }


    private List<StateEntry> getHistoryRange(Connection conn, String deviceId, ZonedDateTime periodStart
            , ZonedDateTime periodEnd) {
        try {
            String query = "SELECT * FROM history WHERE deviceId = ? AND start >= ? AND start <= ? ORDER BY start;";
            PreparedStatement stmt = new QueryBuilder(conn, query)
                    .addParameter(deviceId)
                    .addParameter(periodStart.isAfter(periodEnd) ? periodEnd : periodStart)
                    .addParameter(periodStart.isAfter(periodEnd) ? periodStart : periodEnd)
                    .prepareStatement();
            return getHistory(conn, stmt);
        } catch (SQLException e) {
            logger.error("Can't get history from database", e);
            return null;
        }
    }

    @Override
    public List<StateEntry> getHistory(UUID vehicleId, String deviceId) {
        return (List<StateEntry>) connect(vehicleId, connection -> {
            String query = "SELECT * FROM history WHERE deviceId = ? ORDER BY start desc;";
            return getHistory(connection, query, deviceId);
        });
    }

    @Override
    public StateEntry getLatestHistory(UUID vehicleId, String deviceId) {
        return (StateEntry) connect(vehicleId, connection -> {
            String query = "SELECT * FROM history WHERE deviceId = ? ORDER BY start desc LIMIT 1;";
            return getHistory(connection, query, deviceId).stream().findFirst().orElse(null);
        });
    }

    @Override
    public void delete(UUID vehicleId, String deviceId, StateEntry stateEntry) {
        transaction(vehicleId, connection -> {
            delete(connection, deviceId, stateEntry);
        });
    }

    @Override
    public void delete(UUID vehicleId, String deviceId, List<StateEntry> stateEntries) {
        transaction(vehicleId, connection -> {
            for (StateEntry stateEntry : stateEntries) {
                delete(connection, deviceId, stateEntry);
            }
        });
    }

    @Override
    public void delete(UUID vehicleId, String deviceId) {
        connect(vehicleId, connection -> {
            delete(connection, deviceId, null);
        });
    }

    @Override
    public StateEntry getOldestHistory(UUID vehicleId) {
        return (StateEntry) connect(vehicleId, connection -> {
            try {
                String query = "SELECT * FROM history WHERE end is not null ORDER BY start LIMIT 1;";
                PreparedStatement stmt = connection.prepareStatement(query);
                return getHistory(connection, stmt).stream().findFirst().orElse(null);
            } catch (SQLException e) {
                return null;
            }
        });
    }

    @Override
    public List<StateEntry> getUnfinishedHistory(UUID vehicleId, String deviceId) {
        return (List<StateEntry>) connect(vehicleId, connection -> {
            String query = "SELECT * FROM history WHERE deviceId = ? and end is null ORDER BY start;";
            return getHistory(connection, query, deviceId);
        });
    }

    @Override
    public void reduceHistory(UUID vehicleId, ZonedDateTime toDate) {
        transaction(vehicleId, connection -> {
            try {
                connection.createStatement().executeUpdate("PRAGMA foreign_keys = ON;");
                new QueryBuilder(connection).delete("history").where()
                        .conditionCustom("start", " <= ", toDate)
                        .conditionNotNull("end")
                        .executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        connect(vehicleId, connection -> {
            try {
                connection.createStatement().executeUpdate("VACUUM");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }


    @Override
    public long getDataSize(UUID id) {
        File vehicleDir = getVehicleDirectory(id);
        File databaseFile = new File(vehicleDir, DB_NAME);
        if (databaseFile.exists())
            return databaseFile.length();
        return 0;
    }


    private List<StateEntry> getHistory(Connection conn, String query, String... params) {
        try {
            QueryBuilder queryBuilder = new QueryBuilder(conn, query);
            for (int i = 0; i < params.length; i++) {
                queryBuilder.addParameter(params[i]);
            }
            PreparedStatement stmt = queryBuilder.prepareStatement();
            return getHistory(conn, stmt);
        } catch (SQLException e) {
            logger.error("Can't get history from database", e);
            return Collections.EMPTY_LIST;
        }
    }

    private List<StateEntry> getHistory(Connection conn, PreparedStatement stmt) {
        try {
            ResultSet rs = stmt.executeQuery();
            List<StateEntry> result = new ArrayList<>();
            PreparedStatement hsStmt = conn.prepareStatement("SELECT locale, value FROM historyStrings WHERE historyId = ? and fieldName = ?;");
            while (rs.next()) {
                ZonedDateTime start = readTimeStamp(rs, "start");
                ZonedDateTime end = readTimeStamp(rs, "end");
                String code = rs.getString("code");
                String category = rs.getString("category");
                Long id = rs.getLong("id");
                LocalizedString message = readLocalizedString(hsStmt, id, "message");
                result.add(new StateEntry(start, end, code,
                        category == null ? null : ErrorCategory.valueOf(category),
                        message));
            }
            stmt.close();
            hsStmt.close();
            return result;
        } catch (SQLException e) {
            logger.error("Can't get history from database", e);
            return Collections.EMPTY_LIST;
        }
    }

    LocalizedString readLocalizedString(PreparedStatement stmt, Long id, String fieldName) throws SQLException {
        LocalizedString localizedString = new LocalizedString();
        int i = 1;
        stmt.setLong(i++, id);
        stmt.setString(i++, fieldName);
        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            String locale = rs.getString("locale");
            String value = rs.getString("value");
            localizedString.put(locale, value);
        }
        return localizedString;
    }

    void setTimeStamp(PreparedStatement stmt, int parameterIndex, ZonedDateTime zonedDateTime) throws SQLException {
        if (zonedDateTime != null)
            stmt.setTimestamp(parameterIndex, Timestamp.valueOf(zonedDateTime.toLocalDateTime()));
        else
            stmt.setNull(parameterIndex, 0);
    }

    ZonedDateTime readTimeStamp(ResultSet rs, String fieldName) throws SQLException {
        Timestamp value = rs.getTimestamp(fieldName);
        if (value == null)
            return null;
        else
            return ZonedDateTime.of(value.toLocalDateTime(), ZonedDateTime.now().getZone());
    }


    private void insert(Connection conn, String deviceId, StateEntry stateEntry) {
        try {
            String query = "INSERT INTO history (deviceId, start, end, code, category) VALUES (?, ?, ?, ?, ?);";
            new QueryBuilder(conn, query)
                    .addParameter(deviceId)
                    .addParameter(stateEntry.start)
                    .addParameter(stateEntry.end)
                    .addParameter(stateEntry.code)
                    .addParameter(stateEntry.category)
                    .executeUpdate();

            PreparedStatement stmt = conn.prepareStatement("SELECT last_insert_rowid() id;");
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                query = "INSERT INTO historyStrings (historyId, fieldName, locale, value) VALUES (?, ?, ?, ?);";
                long historyId = rs.getLong("id");
                for (Map.Entry<String, String> entry : stateEntry.message.getLocaleMap().entrySet()) {
                    new QueryBuilder(conn, query)
                            .addParameter(historyId)
                            .addParameter("message")
                            .addParameter(entry.getKey())
                            .addParameter(entry.getValue())
                            .executeUpdate();
                }
            }
        } catch (SQLException e) {
            logger.error("Can't insert indicator history", e);
        }
    }

    private Connection getConnection(UUID vehicleId) {
        File vehicleDir = getVehicleDirectory(vehicleId);
        File databaseFile = new File(vehicleDir, DB_NAME);
        boolean dbExists = databaseFile.exists();

        String url;
        try {
            url = "jdbc:sqlite:" + databaseFile.getCanonicalPath();
        } catch (IOException e) {
            logger.error("Can't make canonical path to vehicle data directory! " + vehicleDir.toString(), e);
            return null;
        }

        Connection conn;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            logger.error("Can't connect to " + DB_NAME + " database! ", e);
            return null;
        }


        if (!dbExists) {
            Statement stmt;
            try {
                stmt = conn.createStatement();
            } catch (SQLException e) {
                logger.error("Can't create statement to run against " + DB_NAME + " database! ", e);
                return null;
            }
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS history (id INTEGER PRIMARY KEY, deviceId TEXT, start DATETIME, end DATETIME, code TEXT, category TEXT);");
                stmt.execute("CREATE INDEX idx_history ON history (deviceId);");
                stmt.execute("CREATE TABLE IF NOT EXISTS historyStrings (historyId INTEGER REFERENCES history(id) ON DELETE CASCADE, " +
                        "fieldName TEXT, locale TEXT, value TEXT);");
                stmt.execute("CREATE INDEX idx_historyStrings ON historyStrings (historyId, fieldName);");
                stmt.close();
            } catch (SQLException e) {
                logger.error("Can't create history table! ", e);
                return null;
            }
        }

        return conn;
    }

    private File getVehicleDirectory(UUID vehicleId) {
        return new File(directory, vehicleId.toString());
    }


//    private List<History> getHistoryRange(Connection conn, String indicatorId, ZonedDateTime
//            earliestReportDate, ZonedDateTime latestReportDate) {
//        String query = "SELECT * FROM history WHERE indicatorId = ? AND timestamp >= ? AND timestamp <= ? ORDER BY timestamp;";
//        PreparedStatement stmt;
//        try {
//            stmt = conn.prepareStatement(query);
//            int i = 1;
//            stmt.setString(i++, indicatorId);
//            stmt.setTimestamp(i++, Timestamp.valueOf(earliestReportDate.toLocalDateTime()));
//            stmt.setTimestamp(i++, Timestamp.valueOf(latestReportDate.toLocalDateTime()));
//            return getHistory(stmt);
//        } catch (SQLException e) {
//            logger.error("Can't get history from database", e);
//            return null;
//        }
//    }


    private void delete(Connection connection, String deviceId, StateEntry stateEntry) {
        try {
            connection.createStatement().executeUpdate("PRAGMA foreign_keys = ON;");
            if (stateEntry == null) {
                new QueryBuilder(connection).delete("history").where().conditionEquals("deviceId", deviceId).executeUpdate();
            } else {
                new QueryBuilder(connection).delete("history").where()
                        .conditionEquals("deviceId", deviceId)
                        .conditionEquals("code", stateEntry.code)
                        .conditionEquals("start", stateEntry.start)
                        .conditionEquals("end", stateEntry.end)
                        .conditionEquals("category", stateEntry.category.toString())
                        .executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Can't delete diagnosis history", e);
        }
    }

    public void transaction(UUID vehicleId, Consumer<Connection> action) {
        connect(vehicleId, connection -> {
            try {
                connection.setAutoCommit(false);
                action.accept(connection);
                connection.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void connect(UUID vehicleId, Consumer<Connection> action) {
        try (Connection conn = getConnection(vehicleId)) {
            if (conn == null)
                return;
            action.accept(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Object connect(UUID vehicleId, Function<Connection, Object> action) {
        try (Connection conn = getConnection(vehicleId)) {
            if (conn == null)
                return null;
            return action.apply(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
