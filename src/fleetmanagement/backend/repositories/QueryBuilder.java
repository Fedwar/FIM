package fleetmanagement.backend.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;


public class QueryBuilder {

    private enum Type {
        INSERT, DELETE, SELECT, QUERY
    }

    private Connection connection;
    private String tableName;
    private StringBuilder query = new StringBuilder();
    private List<Object> params = new ArrayList<>();
    private int conditionsCount = 0;
    private List<String> insertFields = new ArrayList<>();
    private List<String> orderFields = new ArrayList<>();
    private Type queryType;
    private boolean isBuilt;

    public QueryBuilder(Connection connection) {
        this.connection = connection;
    }
    public QueryBuilder(Connection connection, String queryString) {
        this.connection = connection;
        query(queryString);
    }

    public QueryBuilder query(String queryString) {
        queryType = Type.QUERY;
        query= new StringBuilder().append(queryString);
        return this;
    }

    public QueryBuilder delete(String tableName) {
        queryType = Type.DELETE;
        this.tableName = tableName;
        query.append("DELETE FROM ").append(tableName).append(" ");
        return this;
    }

    public QueryBuilder selectAll(String tableName) {
        queryType = Type.SELECT;
        this.tableName = tableName;
        query.append("SELECT * FROM ").append(tableName).append(" ");
        return this;
    }

    public QueryBuilder insert(String tableName) {
        queryType = Type.INSERT;
        this.tableName = tableName;
        return this;
    }

    public QueryBuilder where() {
        query.append("WHERE ");
        return this;
    }

    public QueryBuilder orderField(String fieldName) {
        orderFields.add(fieldName);
        return this;
    }

    public QueryBuilder addParameter(Object value) {
        params.add(value);
        return this;
    }

    public QueryBuilder conditionEquals(String fieldName, Object value) {
        if (value == null) {
            addNullCondition(fieldName);
        } else {
            params.add(value);
            addCondition(fieldName, " = ? ");
        }
        return this;
    }

    public QueryBuilder conditionCustom(String fieldName, String condition, Object value) {
        if (value == null) {
            addNullCondition(fieldName);
        } else {
            params.add(value);
            addCondition(fieldName, " " + condition + " ? ");
        }
        return this;
    }

    public QueryBuilder conditionNotNull(String fieldName) {
        addCondition(fieldName, " is not null ");
        return this;
    }

    private QueryBuilder addNullCondition(String fieldName) {
        addCondition(fieldName, " is null ");
        return this;
    }

    private QueryBuilder addCondition(String fieldName, String condition) {
        if (conditionsCount != 0)
            query.append(" and ");
        query.append(fieldName).append(condition);
        conditionsCount++;
        return this;
    }

    public QueryBuilder insertField(String fieldName, Object value) {
        insertFields.add(fieldName);
        params.add(value);
        return this;
    }

    private void buildInsertQuery() {
        query.append("INSERT INTO ").append(tableName)
                .append(" (")
                .append(String.join(",", insertFields))
                .append(") VALUES (");
        for (int i = 0; i < insertFields.size(); i++) {
            query.append(i == 0 ? "?" : ",?");
        }
        query.append(")");

    }

    private void build() {
        if (!isBuilt) {
            if (queryType == Type.INSERT) {
                buildInsertQuery();
            }
            if (queryType == Type.SELECT) {
                if (!orderFields.isEmpty()) {
                    query.append(" ORDER BY ").append(String.join(",", orderFields));
                }
            }
            isBuilt = true;
        }
    }

    @Override
    public String toString() {
        build();
        return query.toString();
    }

    public PreparedStatement prepareStatement() throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(toString());
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param == null) {
                stmt.setNull(i + 1, 0);
            } else if (param instanceof String) {
                stmt.setString(i + 1, (String) param);
            } else if (param instanceof Integer) {
                stmt.setInt(i + 1, (Integer) param);
            } else if (param instanceof Long) {
                stmt.setLong(i + 1, (Long) param);
            } else if (param instanceof ZonedDateTime) {
                stmt.setTimestamp(i + 1, Timestamp.valueOf(((ZonedDateTime) param).toLocalDateTime()));
            } else if (param instanceof Enum<?>){
                stmt.setString(i + 1, param.toString());
            }
        }
        return stmt;
    }

    public void executeUpdate() throws SQLException {
        PreparedStatement preparedStatement = prepareStatement();
        preparedStatement.executeUpdate();
        preparedStatement.close();
    }



}
