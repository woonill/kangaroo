package com.kangaroo.internal.jdbc;

import java.util.function.Function;


/**
 * Encapsulates a query parameter.
 */
final class SqlParameter {

    /**
     * Actual query parameter value to be encapsulated.
     */
    private final Object parameter;

    /**
     * Constructor.
     *
     * @param parameter
     */
    SqlParameter(Object parameter) {
        super();
        this.parameter = parameter;
    }

    /**
     * Returns the parameter value.
     *
     * @return
     */
    Object getValue() {
        return parameter;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Parameter [parameter=");
        builder.append(parameter);
        builder.append("]");
        return builder.toString();
    }

    /**
     * A conversion function for use in Observable.map().
     */
    static final Function<Object, SqlParameter> TO_PARAMETER = new Function<Object, SqlParameter>() {

        @Override
        public SqlParameter apply(Object parameter) {
            Conditions.checkFalse(parameter instanceof SqlParameter);
            return new SqlParameter(parameter);
        }
    };

}
