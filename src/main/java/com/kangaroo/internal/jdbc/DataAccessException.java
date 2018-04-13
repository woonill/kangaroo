package com.kangaroo.internal.jdbc;

public class DataAccessException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public DataAccessException(String msg) {
        super(msg);
    }

    /**
     * Constructor for DataAccessException.
     *
     * @param msg   the detail message
     * @param cause the root cause (usually from using a underlying
     *              data access API such as JDBC)
     */
    public DataAccessException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
