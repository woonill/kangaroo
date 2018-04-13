package com.kangaroo.internal.jdbc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public final class StatementCreatorUtils {

    private static final Logger logger = LoggerFactory.getLogger(StatementCreatorUtils.class);

    private static Map<Class<?>, Integer> javaTypeToSqlTypeMap = new HashMap<Class<?>, Integer>(32);

    static {
		/* JDBC 3.0 only - not compatible with e.g. MySQL at present
		javaTypeToSqlTypeMap.put(boolean.class, new Integer(Types.BOOLEAN));
		javaTypeToSqlTypeMap.put(Boolean.class, new Integer(Types.BOOLEAN));
		*/
        javaTypeToSqlTypeMap.put(byte.class, Types.TINYINT);
        javaTypeToSqlTypeMap.put(Byte.class, Types.TINYINT);
        javaTypeToSqlTypeMap.put(short.class, Types.SMALLINT);
        javaTypeToSqlTypeMap.put(Short.class, Types.SMALLINT);
        javaTypeToSqlTypeMap.put(int.class, Types.INTEGER);
        javaTypeToSqlTypeMap.put(Integer.class, Types.INTEGER);
        javaTypeToSqlTypeMap.put(long.class, Types.BIGINT);
        javaTypeToSqlTypeMap.put(Long.class, Types.BIGINT);
        javaTypeToSqlTypeMap.put(BigInteger.class, Types.BIGINT);
        javaTypeToSqlTypeMap.put(float.class, Types.FLOAT);
        javaTypeToSqlTypeMap.put(Float.class, Types.FLOAT);
        javaTypeToSqlTypeMap.put(double.class, Types.DOUBLE);
        javaTypeToSqlTypeMap.put(Double.class, Types.DOUBLE);
        javaTypeToSqlTypeMap.put(BigDecimal.class, Types.DECIMAL);
        javaTypeToSqlTypeMap.put(java.sql.Date.class, Types.DATE);
        javaTypeToSqlTypeMap.put(java.sql.Time.class, Types.TIME);
        javaTypeToSqlTypeMap.put(java.sql.Timestamp.class, Types.TIMESTAMP);
        javaTypeToSqlTypeMap.put(Blob.class, Types.BLOB);
        javaTypeToSqlTypeMap.put(Clob.class, Types.CLOB);
    }


    /**
     * Derive a default SQL type from the given Java type.
     *
     * @param javaType the Java type to translate
     * @return the corresponding SQL type, or <code>null</code> if none found
     */
    public static int javaTypeToSqlParameterType(Class<?> javaType) {
        Integer sqlType = javaTypeToSqlTypeMap.get(javaType);
        if (sqlType != null) {
            return sqlType;
        }
        if (Number.class.isAssignableFrom(javaType)) {
            return Types.NUMERIC;
        }
        if (isStringValue(javaType)) {
            return Types.VARCHAR;
        }
        if (isDateValue(javaType) || Calendar.class.isAssignableFrom(javaType)) {
            return Types.TIMESTAMP;
        }
        return SqlTypeValue.TYPE_UNKNOWN;
    }

    public static void setParameterValue(
            PreparedStatement ps, int paramIndex, int sqlType, Object inValue)
            throws SQLException {

        setParameterValueInternal(ps, paramIndex, sqlType, null, null, inValue);
    }

    public static void setParameterValue(
            PreparedStatement ps, int paramIndex, int sqlType, String typeName, Object inValue)
            throws SQLException {

        setParameterValueInternal(ps, paramIndex, sqlType, typeName, null, inValue);
    }

    private static void setParameterValueInternal(
            PreparedStatement ps, int paramIndex, int sqlType, String typeName, Integer scale, Object inValue)
            throws SQLException {

        String typeNameToUse = typeName;
        int sqlTypeToUse = sqlType;
        Object inValueToUse = inValue;

        // override type info?
//		if (inValue instanceof SqlParameterValue) {
//			SqlParameterValue parameterValue = (SqlParameterValue) inValue;
//			if (logger.isDebugEnabled()) {
//				logger.debug("Overriding typeinfo with runtime info from SqlParameterValue: column index " + paramIndex +
//						", SQL type " + parameterValue.getSqlType() +
//						", Type name " + parameterValue.getTypeName());
//			}
//			if (parameterValue.getSqlType() != SqlTypeValue.TYPE_UNKNOWN) {
//				sqlTypeToUse = parameterValue.getSqlType();
//			}
//			if (parameterValue.getTypeName() != null) {
//				typeNameToUse = parameterValue.getTypeName();
//			}
//			inValueToUse = parameterValue.getValue();
//		}
        if (logger.isTraceEnabled()) {
            logger.trace("Setting SQL statement parameter value: column index " + paramIndex +
                    ", parameter value [" + inValueToUse +
                    "], value class [" + (inValueToUse != null ? inValueToUse.getClass().getName() : "null") +
                    "], SQL type " + (sqlTypeToUse == SqlTypeValue.TYPE_UNKNOWN ? "unknown" : Integer.toString(sqlTypeToUse)));
        }
        if (inValueToUse == null) {
            setNull(ps, paramIndex, sqlTypeToUse, typeNameToUse);
        } else {
            setValue(ps, paramIndex, sqlTypeToUse, typeNameToUse, scale, inValueToUse);
        }
    }

    /**
     * Set the specified PreparedStatement parameter to null,
     * respecting database-specific peculiarities.
     */
    private static void setNull(PreparedStatement ps, int paramIndex, int sqlType, String typeName)
            throws SQLException {

        if (sqlType == SqlTypeValue.TYPE_UNKNOWN) {
            boolean useSetObject = false;
            sqlType = Types.NULL;
            try {
                DatabaseMetaData dbmd = ps.getConnection().getMetaData();
                String databaseProductName = dbmd.getDatabaseProductName();
                String jdbcDriverName = dbmd.getDriverName();
                if (databaseProductName.startsWith("Informix") ||
                        jdbcDriverName.startsWith("Microsoft SQL Server")) {
                    useSetObject = true;
                } else if (databaseProductName.startsWith("DB2") ||
                        jdbcDriverName.startsWith("jConnect") ||
                        jdbcDriverName.startsWith("SQLServer") ||
                        jdbcDriverName.startsWith("Apache Derby")) {
                    sqlType = Types.VARCHAR;
                }
            } catch (Throwable ex) {
                logger.debug("Could not check database or driver name", ex);
            }
            if (useSetObject) {
                ps.setObject(paramIndex, null);
            } else {
                ps.setNull(paramIndex, sqlType);
            }
        } else if (typeName != null) {
            ps.setNull(paramIndex, sqlType, typeName);
        } else {
            ps.setNull(paramIndex, sqlType);
        }
    }

    private static void setValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName,
                                 Integer scale, Object inValue) throws SQLException {

        if (inValue instanceof SqlTypeValue) {
            ((SqlTypeValue) inValue).setTypeValue(ps, paramIndex, sqlType, typeName);
        } else if (sqlType == Types.VARCHAR || sqlType == Types.LONGVARCHAR ||
                (sqlType == Types.CLOB && isStringValue(inValue.getClass()))) {
            ps.setString(paramIndex, inValue.toString());
        } else if (sqlType == Types.DECIMAL || sqlType == Types.NUMERIC) {
            if (inValue instanceof BigDecimal) {
                ps.setBigDecimal(paramIndex, (BigDecimal) inValue);
            } else if (scale != null) {
                ps.setObject(paramIndex, inValue, sqlType, scale);
            } else {
                ps.setObject(paramIndex, inValue, sqlType);
            }
        } else if (sqlType == Types.DATE) {
            if (inValue instanceof java.util.Date) {
                if (inValue instanceof java.sql.Date) {
                    ps.setDate(paramIndex, (java.sql.Date) inValue);
                } else {
                    ps.setDate(paramIndex, new java.sql.Date(((java.util.Date) inValue).getTime()));
                }
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setDate(paramIndex, new java.sql.Date(cal.getTime().getTime()), cal);
            } else {
                ps.setObject(paramIndex, inValue, Types.DATE);
            }
        } else if (sqlType == Types.TIME) {
            if (inValue instanceof java.util.Date) {
                if (inValue instanceof java.sql.Time) {
                    ps.setTime(paramIndex, (java.sql.Time) inValue);
                } else {
                    ps.setTime(paramIndex, new java.sql.Time(((java.util.Date) inValue).getTime()));
                }
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setTime(paramIndex, new java.sql.Time(cal.getTime().getTime()), cal);
            } else {
                ps.setObject(paramIndex, inValue, Types.TIME);
            }
        } else if (sqlType == Types.TIMESTAMP) {
            if (inValue instanceof java.util.Date) {
                if (inValue instanceof java.sql.Timestamp) {
                    ps.setTimestamp(paramIndex, (java.sql.Timestamp) inValue);
                } else {
                    ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
                }
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
            } else {
                ps.setObject(paramIndex, inValue, Types.TIMESTAMP);
            }
        } else if (sqlType == SqlTypeValue.TYPE_UNKNOWN) {
            if (isStringValue(inValue.getClass())) {
                ps.setString(paramIndex, inValue.toString());
            } else if (isDateValue(inValue.getClass())) {
                ps.setTimestamp(paramIndex, new java.sql.Timestamp(((java.util.Date) inValue).getTime()));
            } else if (inValue instanceof Calendar) {
                Calendar cal = (Calendar) inValue;
                ps.setTimestamp(paramIndex, new java.sql.Timestamp(cal.getTime().getTime()), cal);
            } else {
                // Fall back to generic setObject call without SQL type specified.
                ps.setObject(paramIndex, inValue);
            }
        } else {
            // Fall back to generic setObject call with SQL type specified.
            ps.setObject(paramIndex, inValue, sqlType);
        }
    }

    /**
     * Check whether the given value can be treated as a String value.
     */
    private static boolean isStringValue(Class<?> inValueType) {
        // Consider any CharSequence (including StringBuffer and StringBuilder) as a String.
        return (CharSequence.class.isAssignableFrom(inValueType) ||
                StringWriter.class.isAssignableFrom(inValueType));
    }

    private static boolean isDateValue(Class<?> inValueType) {
        return (java.util.Date.class.isAssignableFrom(inValueType) &&
                !(java.sql.Date.class.isAssignableFrom(inValueType) ||
                        java.sql.Time.class.isAssignableFrom(inValueType) ||
                        java.sql.Timestamp.class.isAssignableFrom(inValueType)));
    }

}
