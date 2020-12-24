package fleetmanagement.backend.repositories.disk;

import fleetmanagement.backend.reports.datasource.vehicles.ConnectionStatus;
import fleetmanagement.backend.vehicles.ConnectionStatusRepository;
import fleetmanagement.backend.vehicles.Vehicle;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class ConnectionStatusSQLiteRepository implements ConnectionStatusRepository {

    private final File directory;
    private static final Logger logger = Logger.getLogger(ConnectionStatusSQLiteRepository.class);
    private static final String CONNECT_DB = "connect.db";
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public ConnectionStatusSQLiteRepository(FimConfig config) {
        this(config.getVehicleDirectory());
    }

    public ConnectionStatusSQLiteRepository(File directory) {
        this.directory = directory;
    }

    private void updateConnectionInfo(Connection conn, String lastSeen, String at, String status) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("UPDATE vehicle_connections SET at = ?, status = ? WHERE at = ?;");
        pstmt.setString(1, lastSeen);
        pstmt.setString(2, status);
        pstmt.setString(3, at);
        pstmt.executeUpdate();
    }

    private void updateConnectionStatus(Connection conn, String at, String status) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("UPDATE vehicle_connections SET status = ? WHERE at = ?;");
        pstmt.setString(1, status);
        pstmt.setString(2, at);
    }

    private void insertConnectionStatus(Connection conn, String at, String status) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO vehicle_connections (at, status) VALUES (?, ?);");
        pstmt.setString(1, at);
        pstmt.setString(2, status);
        pstmt.executeUpdate();
    }


    @Override
    public void saveConnectionInfo(Vehicle vehicle) {
        Connection conn = getConnection(vehicle);
        if (conn == null)
            return;

        try {
            String at = null;
            ConnectionStatus status = ConnectionStatus.ONLINE;
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT at, status FROM vehicle_connections ORDER BY at DESC LIMIT 1;");
                if (rs.next()) {
                    at = rs.getString("at");
                    status = ConnectionStatus.getByDbCode(rs.getString("status"));
                }
                stmt.close();
            } catch (SQLException e) {
                logger.error("Can't get latest row from vehicle_connections table! ", e);
                return;
            }

            String lastSeen = simpleDateFormat.format(Date.from(vehicle.lastSeen.toInstant()));

            try {
                saveConnectionInfo(conn, at, lastSeen, status);
            } catch (SQLException e) {
                File databaseFile = new File(getVehicleDirectory(vehicle), CONNECT_DB);
                logger.error("Can't save connection info! "  + databaseFile, e);
            }
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("Can't close sqlite connection! ", e);
            }
        }
    }

    Connection getConnection(Vehicle vehicle) {
        File vehicleDir = getVehicleDirectory(vehicle);
        File databaseFile = new File(vehicleDir, CONNECT_DB);
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
            logger.error("Can't connect to vehicle connections database! ", e);
            return null;
        }

        if (!dbExists) {
            createTable(conn);
        }

        return conn;
    }

    void createTable(Connection conn) {
        try (Statement stmt  = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS vehicle_connections (at CHARACTER(19) PRIMARY KEY, status CHARACTER(1));");
        } catch (SQLException e) {
            logger.error("Can't create vehicle_connections table! ", e);
        }
    }

    private void saveConnectionInfo(Connection conn, String at, String lastSeen, ConnectionStatus status) throws SQLException {
        if (at != null) {
            String oldHours = at.substring(0, 13);
            String newHours = lastSeen.substring(0,13);

            if (oldHours.equals(newHours)) {
                String oldMins = at.substring(14,15);
                String newMins = lastSeen.substring(14,15);
                if (Integer.parseInt(newMins) - Integer.parseInt(oldMins) > 10) {
                    status = ConnectionStatus.IRREGULAR;
                }
                updateConnectionInfo(conn, lastSeen, at, status.getDbCode());
            } else {
                String oldMins = at.substring(14,15);
                if (Integer.parseInt(oldMins) < 50) {
                    updateConnectionStatus(conn, ConnectionStatus.IRREGULAR.getDbCode(), at);
                }

                String newMins = lastSeen.substring(14,15);
                if (Integer.parseInt(newMins) > 10) {
                    status = ConnectionStatus.IRREGULAR;
                }
                insertConnectionStatus(conn, lastSeen, status.getDbCode());
            }
        } else {
            insertConnectionStatus(conn, lastSeen, ConnectionStatus.ONLINE.getDbCode());
        }
    }


    private Map<String, ConnectionStatus> readVehicleHoursFromDB(Connection conn, String dateFrom, String dateTo) {
        Map<String, ConnectionStatus> result = new HashMap<>();
        String at;
        ConnectionStatus status;

        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT at, status FROM vehicle_connections WHERE at >= ? AND at <= ? ORDER BY at;");
            stmt.setString(1, dateFrom);
            stmt.setString(2, dateTo);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                at = rs.getString("at");
                status = ConnectionStatus.getByDbCode(rs.getString("status"));
                result.put(at, status);
            }
            stmt.close();
        } catch (SQLException e) {
            logger.error("Can't get hours from vehicle_connections table! ", e);
            return null;
        }

        return result;
    }

    File getVehicleDirectory(Vehicle vehicle) {
        return new File(directory, vehicle.id.toString());
    }

    @Override
    public Map<String, ConnectionStatus> getVehicleHours(String earliestReportDate, String latestReportDate, Vehicle v) {
        if (v == null)
            return null;

        Connection connection = getConnection(v);
        if (connection == null)
            return null;

        try {
            return readVehicleHoursFromDB(connection, earliestReportDate, latestReportDate);
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Can't close sqlite connection! ", e);
            }
        }
    }
}
