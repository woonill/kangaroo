package com.kangaroo.internal.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ArgPreparedStatementSetter implements PreparedStatementSetter {

    private final Object[] args;


    /**
     * Create a new ArgPreparedStatementSetter for the given arguments.
     *
     * @param args the arguments to set
     */
    public ArgPreparedStatementSetter(Object[] args) {
        this.args = args;
    }


    @Override
    public void setValues(PreparedStatement ps) throws SQLException {
        if (this.args != null) {
            for (int i = 0; i < this.args.length; i++) {
                Object arg = this.args[i];
                doSetValue(ps, i + 1, arg);
            }
        }
    }

    /**
     * Set the value for prepared statements specified parameter index using the passed in value.
     * This method can be overridden by sub-classes if needed.
     *
     * @param ps                the PreparedStatement
     * @param parameterPosition index of the parameter position
     * @param argValue          the value to set
     * @throws SQLException
     */
    protected void doSetValue(PreparedStatement ps, int parameterPosition, Object argValue) throws SQLException {
        StatementCreatorUtils.setParameterValue(ps, parameterPosition, SqlTypeValue.TYPE_UNKNOWN, argValue);
    }
}
