package com.kangaroo.internal.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PreparedStatementSetter {

    void setValues(PreparedStatement ps) throws SQLException;


    public static final PreparedStatementSetter NONE = new PreparedStatementSetter() {
        @Override
        public void setValues(PreparedStatement ps) throws SQLException {
        }
    };


}
