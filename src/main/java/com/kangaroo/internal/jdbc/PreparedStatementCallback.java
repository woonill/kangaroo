package com.kangaroo.internal.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementCallback {
    void doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException;
}
