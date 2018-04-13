package com.kangaroo.internal.jdbc;

import com.kangaroo.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class SQLStateSQLExceptionTranslator {
    /**
     * Logger available to subclasses
     */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private SQLExceptionTranslator fallbackTranslator;
    private static final Set<String> BAD_SQL_GRAMMAR_CODES = new HashSet<String>(8);

    private static final Set<String> DATA_INTEGRITY_VIOLATION_CODES = new HashSet<String>(8);

    private static final Set<String> DATA_ACCESS_RESOURCE_FAILURE_CODES = new HashSet<String>(8);

    private static final Set<String> TRANSIENT_DATA_ACCESS_RESOURCE_CODES = new HashSet<String>(8);

    private static final Set<String> CONCURRENCY_FAILURE_CODES = new HashSet<String>(4);


    static {
        BAD_SQL_GRAMMAR_CODES.add("07");    // Dynamic SQL error
        BAD_SQL_GRAMMAR_CODES.add("21");    // Cardinality violation
        BAD_SQL_GRAMMAR_CODES.add("2A");    // Syntax error direct SQL
        BAD_SQL_GRAMMAR_CODES.add("37");    // Syntax error dynamic SQL
        BAD_SQL_GRAMMAR_CODES.add("42");    // General SQL syntax error
        BAD_SQL_GRAMMAR_CODES.add("65");    // Oracle: unknown identifier
        BAD_SQL_GRAMMAR_CODES.add("S0");    // MySQL uses this - from ODBC error codes?

        DATA_INTEGRITY_VIOLATION_CODES.add("01");    // Data truncation
        DATA_INTEGRITY_VIOLATION_CODES.add("02");    // No data found
        DATA_INTEGRITY_VIOLATION_CODES.add("22");    // Value out of range
        DATA_INTEGRITY_VIOLATION_CODES.add("23");    // Integrity constraint violation
        DATA_INTEGRITY_VIOLATION_CODES.add("27");    // Triggered data change violation
        DATA_INTEGRITY_VIOLATION_CODES.add("44");    // With check violation

        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("08");     // Connection exception
        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("53");     // PostgreSQL: insufficient resources (e.g. disk full)
        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("54");     // PostgreSQL: program limit exceeded (e.g. statement too complex)
        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("57");     // DB2: out-of-memory exception / database not started
        DATA_ACCESS_RESOURCE_FAILURE_CODES.add("58");     // DB2: unexpected system error

        TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("JW");     // Sybase: internal I/O error
        TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("JZ");     // Sybase: unexpected I/O error
        TRANSIENT_DATA_ACCESS_RESOURCE_CODES.add("S1");     // DB2: communication failure

        CONCURRENCY_FAILURE_CODES.add("40");    // Transaction rollback
        CONCURRENCY_FAILURE_CODES.add("61");    // Oracle: deadlock
    }


    protected DataAccessException doTranslate(String task, String sql, SQLException ex) {
        String sqlState = getSqlState(ex);
        if (sqlState != null && sqlState.length() >= 2) {
            String classCode = sqlState.substring(0, 2);
            if (logger.isDebugEnabled()) {
                logger.debug("Extracted SQL state class '" + classCode + "' from value '" + sqlState + "'");
            }
            if (BAD_SQL_GRAMMAR_CODES.contains(classCode)) {
                return new BadSqlGrammarException(task, sql, ex);
            } else if (DATA_INTEGRITY_VIOLATION_CODES.contains(classCode)) {
                return new DataAccessException(buildMessage(task, sql, ex), ex);
            } else if (DATA_ACCESS_RESOURCE_FAILURE_CODES.contains(classCode)) {
                return new DataAccessException(buildMessage(task, sql, ex), ex);
            } else if (TRANSIENT_DATA_ACCESS_RESOURCE_CODES.contains(classCode)) {
                return new DataAccessException(buildMessage(task, sql, ex), ex);
            } else if (CONCURRENCY_FAILURE_CODES.contains(classCode)) {
                return new DataAccessException(buildMessage(task, sql, ex), ex);
            }
        }
        return null;
    }

    /**
     * Gets the SQL state code from the supplied {@link SQLException exception}.
     * <p>Some JDBC drivers nest the actual exception from a batched update, so we
     * might need to dig down into the nested exception.
     *
     * @param ex the exception from which the {@link SQLException#getSQLState() SQL state}
     *           is to be extracted
     * @return the SQL state code
     */
    private String getSqlState(SQLException ex) {
        String sqlState = ex.getSQLState();
        if (sqlState == null) {
            SQLException nestedEx = ex.getNextException();
            if (nestedEx != null) {
                sqlState = nestedEx.getSQLState();
            }
        }
        return sqlState;
    }


    /**
     * Override the default SQL state fallback translator
     * (typically a {@link SQLStateSQLExceptionTranslator}).
     */
    public void setFallbackTranslator(SQLExceptionTranslator fallback) {
        this.fallbackTranslator = fallback;
    }

    /**
     * Return the fallback exception translator, if any.
     */
    public SQLExceptionTranslator getFallbackTranslator() {
        return this.fallbackTranslator;
    }


    /**
     * Pre-checks the arguments, calls {@link #doTranslate}, and invokes the
     * {@link #getFallbackTranslator() fallback translator} if necessary.
     */
    public DataAccessException translate(String task, String sql, SQLException ex) {
        Validate.notNull(ex, "Cannot translate a null SQLException");
        if (task == null) {
            task = "";
        }
        if (sql == null) {
            sql = "";
        }
        DataAccessException dex = doTranslate(task, sql, ex);
        if (dex != null) {
            return dex;
        }
        SQLExceptionTranslator fallback = getFallbackTranslator();
        if (fallback != null) {
            return fallback.translate(task, sql, ex);
        }
        return new DataAccessException(buildMessage(task, sql, ex));
    }

    protected String buildMessage(String task, String sql, SQLException ex) {
        return task + "; SQL [" + sql + "]; " + ex.getMessage();
    }
}
