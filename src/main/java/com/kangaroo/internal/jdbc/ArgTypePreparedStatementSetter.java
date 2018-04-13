package com.kangaroo.internal.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

public final class ArgTypePreparedStatementSetter implements PreparedStatementSetter {

    private final Object[] args;

    private final int[] argTypes;


    /**
     * Create a new ArgTypePreparedStatementSetter for the given arguments.
     *
     * @param args     the arguments to set
     * @param argTypes the corresponding SQL types of the arguments
     */
    public ArgTypePreparedStatementSetter(Object[] args, int[] argTypes) {
        if ((args != null && argTypes == null) || (args == null && argTypes != null) ||
                (args != null && args.length != argTypes.length)) {
            throw new DataAccessException("args and argTypes parameters must match");
        }
        this.args = args;
        this.argTypes = argTypes;
    }


    public void setValues(PreparedStatement ps) throws SQLException {
        int parameterPosition = 1;
        if (this.args != null) {
            for (int i = 0; i < this.args.length; i++) {
                Object arg = this.args[i];
                if (arg instanceof Collection && this.argTypes[i] != Types.ARRAY) {
                    Collection<?> entries = (Collection<?>) arg;
                    for (Iterator<?> it = entries.iterator(); it.hasNext(); ) {
                        Object entry = it.next();
                        if (entry instanceof Object[]) {
                            Object[] valueArray = ((Object[]) entry);
                            for (int k = 0; k < valueArray.length; k++) {
                                Object argValue = valueArray[k];
                                doSetValue(ps, parameterPosition, this.argTypes[i], argValue);
                                parameterPosition++;
                            }
                        } else {
                            doSetValue(ps, parameterPosition, this.argTypes[i], entry);
                            parameterPosition++;
                        }
                    }
                } else {
                    doSetValue(ps, parameterPosition, this.argTypes[i], arg);
                    parameterPosition++;
                }
            }
        }
    }

    protected void doSetValue(PreparedStatement ps, int parameterPosition, int argType, Object argValue)
            throws SQLException {
        StatementCreatorUtils.setParameterValue(ps, parameterPosition, argType, argValue);
    }
}