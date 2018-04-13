package com.kangaroo.internal.jdbc;

import java.sql.SQLException;


public interface SQLExceptionTranslator {

    DataAccessException translate(String task, String sql, SQLException ex);

}
