package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.operationData.History;
import fleetmanagement.backend.operationData.Indicator;
import fleetmanagement.backend.operationData.OperationDataHistoryRepository;
import fleetmanagement.backend.operationData.ValueClassConverter;
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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
public class OperationDataHistorySQLiteRepository implements OperationDataHistoryRepository {

    public static final String DB_NAME = "operationDataHistory.db";
    private static final Logger logger = Logger.getLogger(OperationDataHistorySQLiteRepository.class);
    private final File directory;

    @Autowired
    public OperationDataHistorySQLiteRepository(FimConfig config) {
        directory = config.getVehicleDirectory();
    }

    public OperationDataHistorySQLiteRepository(File directory) {
        this.directory = directory;
    }

    @Override
    public void addHistory(UUID vehicleId, Indicator indicator) {
        try (Connection conn = getConnection(vehicleId)) {
            insert(conn, indicator);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addHistory(UUID vehicleId, List<Indicator> indicators) {
        try (Connection conn = getConnection(vehicleId)) {
            conn.setAutoCommit(false);
            for (Indicator indicator : indicators) {
                insert(conn, indicator);
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<History> getHistory(UUID vehicleId, String indicatorId) {
        try (Connection conn = getConnection(vehicleId)) {
            String query = "SELECT * FROM history WHERE indicatorId = ? ORDER BY timestamp;";
            PreparedStatement stmt = new QueryBuilder(conn, query)
                    .addParameter(indicatorId)
                    .prepareStatement();
            return getHistory(stmt);
        } catch (SQLException e) {
            logger.error("Can't get history from database", e);
            return Collections.EMPTY_LIST;
        }
    }


    @Override
    public void delete(UUID vehicleId, Indicator indicator) {
        try (Connection conn = getConnection(vehicleId)) {
            delete(conn, indicator.id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(UUID vehicleId, List<Indicator> indicators) {
        try (Connection conn = getConnection(vehicleId)) {
            for (Indicator indicator : indicators) {
                delete(conn, indicator.id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addHistory(UUID vehicleId, Map<Indicator, List<History>> historyMap) {
        try (Connection conn = getConnection(vehicleId)) {
            conn.setAutoCommit(false);
            historyMap.entrySet();
            for (Map.Entry<Indicator, List<History>> entry : historyMap.entrySet()) {
                for (History history : entry.getValue()) {
                    insert(conn, entry.getKey().id, history.timeStamp, history.value);
                }
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<History> getHistoryRange(UUID vehicleId, String indicatorId, ZonedDateTime beginDate, ZonedDateTime endDate, ChronoUnit timeUnit) {
        return (List<History>) connect(vehicleId, connection -> {
            return getHistoryRange(connection, indicatorId, beginDate, endDate, timeUnit);
        });
    }

    @Override
    public List<History> getHistoryRange(UUID vehicleId, String indicatorId, ZonedDateTime beginDate, ZonedDateTime endDate) {
        try (Connection conn = getConnection(vehicleId)) {
            return getHistoryRange(conn, indicatorId, beginDate, endDate, null);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Map<String, List<History>> getHistoryRange(UUID vehicleId, List<String> indicatorIdList, ZonedDateTime beginDate, ZonedDateTime endDate) {
        return (Map<String, List<History>>) connect(vehicleId, connection -> {
            HashMap<String, List<History>> historyMap = new HashMap<>();
            for (String indicatorId : indicatorIdList) {
                historyMap.put(indicatorId, getHistoryRange(connection, indicatorId, beginDate, endDate, null));
            }
            return historyMap;
        });
    }

    @Override
    public History getOldestHistory(UUID vehicleId) {
        return (History) connect(vehicleId, connection -> {
            try {
                String query = "SELECT * FROM history ORDER BY timestamp LIMIT 1;";
                PreparedStatement stmt = connection.prepareStatement(query);
                return getHistory(stmt).stream().findFirst().orElse(null);
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    @Override
    public List<History> getHistory(UUID vehicleId, String indicatorId, int limit) {
        return (List<History>) connect(vehicleId, connection -> {
            try {
                String query = "SELECT * FROM history WHERE indicatorId = ? ORDER BY timestamp desc LIMIT ?;";
                PreparedStatement stmt = new QueryBuilder(connection, query)
                        .addParameter(indicatorId)
                        .addParameter(limit)
                        .prepareStatement();
                return getHistory(stmt);
            } catch (SQLException e) {
                e.printStackTrace();
                return Collections.EMPTY_LIST;
            }
        });
    }

    @Override
    public void reduceHistory(UUID vehicleId, ZonedDateTime timestamp) {
        transaction(vehicleId, connection -> {
            try {
                new QueryBuilder(connection).delete("history").where()
                        .conditionCustom("timestamp", " <= ", timestamp)
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

    private List<History> getHistory(PreparedStatement stmt) throws SQLException {
        ResultSet rs = stmt.executeQuery();
        List<History> result = toHistory(rs);
        stmt.close();
        return result;
    }

    private List<History> toHistory(ResultSet rs) throws SQLException {
        List<History> result = new ArrayList<>();
        while (rs.next()) {
            String value = rs.getString("value");
            String valueClass = rs.getString("valueClass");
            ZonedDateTime timeStamp = ZonedDateTime.of(rs.getTimestamp("timeStamp").toLocalDateTime(), ZonedDateTime.now().getZone());
            result.add(new History(new ValueClassConverter(value, valueClass).getValue(), timeStamp));
        }
        return result;
    }


    private void insert(Connection conn, Indicator indicator) {
        insert(conn, indicator.id, indicator.updated, indicator.value);
    }

    private void insert(Connection conn, String indicatorId, ZonedDateTime updated, Object value) {
        try {
            String query = "INSERT INTO history (indicatorId, timestamp, value, valueClass) VALUES (?, ?, ?, ?);";
            new QueryBuilder(conn, query)
                    .addParameter(indicatorId)
                    .addParameter(updated)
                    .addParameter(value.toString())
                    .addParameter(value.getClass().getName())
                    .executeUpdate();
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

        Statement stmt;
        try {
            stmt = conn.createStatement();
        } catch (SQLException e) {
            logger.error("Can't create statement to run against " + DB_NAME + " database! ", e);
            return null;
        }

        if (!dbExists) {
            try {
                stmt.execute("CREATE TABLE IF NOT EXISTS history (indicatorId TEXT, timestamp DATETIME, value TEXT, valueClass TEXT);");
                stmt.execute("CREATE INDEX idx_history ON history (indicatorId, timestamp);");
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


    private List<History> getHistoryRange(Connection conn, String indicatorId, ZonedDateTime beginDate, ZonedDateTime endDate, ChronoUnit groupBy) {
        try {
            PreparedStatement stmt = null;
            if (groupBy != ChronoUnit.HOURS && groupBy != ChronoUnit.DAYS) {
                String query = "SELECT * FROM history WHERE indicatorId = ? AND timestamp >= ? AND timestamp <= ? ORDER BY timestamp;";
                stmt = new QueryBuilder(conn, query)
                        .addParameter(indicatorId)
                        .addParameter(beginDate.isAfter(endDate) ? endDate : beginDate)
                        .addParameter(beginDate.isAfter(endDate) ? beginDate : endDate)
                        .prepareStatement();
            } else {
                String query = "select indicatorId, timestamp, AVG(value) value, valueClass " +
                        " FROM ( SELECT indicatorId, (timestamp / ? * ?) as timestamp, value, valueClass " +
                        " FROM history WHERE indicatorId = ? AND timestamp >= ? AND timestamp <= ? ) data " +
                        " GROUP BY indicatorId, timestamp, valueClass " +
                        " ORDER BY timestamp; ";
                Integer converter = (groupBy == ChronoUnit.HOURS ? 3600*1000 : (groupBy == ChronoUnit.DAYS ? 86400*1000 : 1));
                stmt = new QueryBuilder(conn, query)
                        .addParameter(converter)
                        .addParameter(converter)
                        .addParameter(indicatorId)
                        .addParameter(beginDate.isAfter(endDate) ? endDate : beginDate)
                        .addParameter(beginDate.isAfter(endDate) ? beginDate : endDate)
                        .prepareStatement();
            }

            return getHistory(stmt);
        } catch (SQLException e) {
            logger.error("Can't get history from database", e);
            return null;
        }
    }


    private void delete(Connection conn, String indicatorId) {
        try {
            String query = "DELETE history WHERE indicatorId = ? ;";
            new QueryBuilder(conn, query)
                    .addParameter(indicatorId)
                    .executeUpdate();
        } catch (SQLException e) {
            logger.error("Can't delete indicator history", e);
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
