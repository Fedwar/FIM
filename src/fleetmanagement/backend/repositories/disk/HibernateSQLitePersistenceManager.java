package fleetmanagement.backend.repositories.disk;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;
import java.util.function.Function;

public class HibernateSQLitePersistenceManager implements PersistenceManager {

    protected SessionFactory factory;
    private final String[] schemaCommands;
    protected final File directory;
    private final String dbName;
    private static final Logger logger = Logger.getLogger(HibernateSQLitePersistenceManager.class);

    public HibernateSQLitePersistenceManager(File directory, Class persistedClass, String dbName) {
        this.directory = directory;

        this.dbName = dbName;
        Configuration configuration = new Configuration()
                .addAnnotatedClass(persistedClass)
//                .setProperty("hibernate.show_sql", "true")
                .setProperty("hibernate.connection.driver_class", "org.sqlite.JDBC")
                .setProperty("hibernate.dialect", "com.enigmabridge.hibernate.dialect.SQLiteDialect");
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(
                configuration.getProperties()).build();
        factory = configuration.buildSessionFactory(serviceRegistry);
        Dialect dialect = Dialect.getDialect(configuration.getProperties());
        schemaCommands = configuration.generateSchemaCreationScript(dialect);
    }

    protected void createSchema(Connection connection) {
        try {
            Statement statement = connection.createStatement();
            for (String sqlCommand : schemaCommands) {
                statement.executeUpdate(sqlCommand);
            }
        } catch (SQLException e) {
            logger.error("Can't create schema for " + dbName + " database! ", e);
        }
    }

    protected Connection getConnection(Object vehicleId) {
        File vehicleDir = getDatabaseDirectory(vehicleId);
        File databaseFile = new File(vehicleDir, dbName);
        boolean dbExists = databaseFile.exists();

        String url;
        try {
            url = "jdbc:sqlite:" + databaseFile.getCanonicalPath();
        } catch (IOException e) {
            logger.error("Can't make canonical path to database directory! " + vehicleDir.toString(), e);
            return null;
        }

        Connection conn;
        try {
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            logger.error("Can't connect to " + dbName + " database! ", e);
            return null;
        }

        if (!dbExists) {
            createSchema(conn);
        }

        return conn;
    }

    protected File getDatabaseDirectory(Object vehicleId) {
        return vehicleId == null ? directory : new File(directory, vehicleId.toString());
    }

    @Override
    public void transaction(Object vehicleId, Consumer<Session> action) {
        session(vehicleId, session -> {
            Transaction tx = session.beginTransaction();
            action.accept(session);
            tx.commit();
        });
    }

    @Override
    public void session(Object vehicleId, Consumer<Session> action) {
        connect(vehicleId, connection -> {
            Session session = factory.withOptions().connection(connection).openSession();
            action.accept(session);
            session.flush();
            session.close();
        });
    }

    @Override
    public void connect(Object vehicleId, Consumer<Connection> action) {
        try (Connection conn = getConnection(vehicleId)) {
            if (conn == null)
                return;
            action.accept(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public <T> T connect(Object vehicleId, Function<Connection, T> action) {
        try (Connection conn = getConnection(vehicleId)) {
            if (conn == null)
                return null;
            return action.apply(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public <T> T session(Object vehicleId, Function<Session, T> action) {
        return connect(vehicleId, connection -> {
            Session session = factory.withOptions().connection(connection).openSession();
            T apply = action.apply(session);
            session.flush();
            session.close();
            return apply;
        });
    }

}
