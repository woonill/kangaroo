package com.kangaroo.internal.jdbc;

import java.sql.SQLException;

public class BadSqlGrammarException extends DataAccessException {


    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String sql;


    /**
     * Constructor for BadSqlGrammarException.
     *
     * @param task name of current task
     * @param sql  the offending SQL statement
     * @param ex   the root cause
     */
    public BadSqlGrammarException(String task, String sql, SQLException ex) {
        super(task + "; bad SQL grammar [" + sql + "]", ex);
        this.sql = sql;
    }


    /**
     * Return the wrapped SQLException.
     */
    public SQLException getSQLException() {
        return (SQLException) getCause();
    }

    /**
     * Return the SQL that caused the problem.
     */
    public String getSql() {
        return this.sql;
    }
}
