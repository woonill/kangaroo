package com.kangaroo.internal.jdbc;

import com.kangaroo.util.CUtils;
import com.kangaroo.internal.io.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public final class JdbcUtil {

    private static Logger logger = LoggerFactory.getLogger(JdbcUtil.class.getName());


    public static final Object NULL_CLOB = new Object();
    public static final Object NULL_BLOB = new Object();
    public static final Object NULL_NUMBER = new Object();


    public static boolean supportsBatchUpdates(Connection con) {
        try {
            DatabaseMetaData dbmd = con.getMetaData();
            if (dbmd != null) {
                if (dbmd.supportsBatchUpdates()) {
                    logger.info("JDBC driver supports batch updates");
                    return true;
                } else {
                    logger.warn("JDBC driver does not support batch updates");
                }
            }
        } catch (SQLException ex) {
            logger.error("JDBC driver 'supportsBatchUpdates' method threw exception", ex);
        } catch (AbstractMethodError err) {
            logger.error("JDBC driver does not support JDBC 2.0 'supportsBatchUpdates' method", err);
        }
        return false;
    }


//	public static String getSql(Object sqlProvider) {
//		if (sqlProvider instanceof SqlProvider) {
//			return ((SqlProvider) sqlProvider).getSql();
//		}
//		else {
//			return null;
//		}
//	}	


    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException ex) {
                logger.error("Could not close JDBC Statement", ex);
            } catch (Throwable ex) {
                // We don't trust the JDBC driver: It might throw RuntimeException or Error.
                logger.trace("Unexpected exception on closing JDBC Statement", ex);
            }
        }
    }

    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                logger.error("Could not close JDBC ResultSet", ex);
            } catch (Throwable ex) {
                // We don't trust the JDBC driver: It might throw RuntimeException or Error.
                logger.error("Unexpected exception on closing JDBC ResultSet", ex);
            }
        }
    }


    public static Connection getConnection(DataSource ds) {
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            throw new IllegalArgumentException("error to get Jdbc Connection", e);
        }

    }


    static int parametersCount(String sql) {
        // TODO account for ? characters in string constants
        return countOccurrences(sql, '?');
    }

    /**
     * Returns the number of occurrences of a character in a string.
     *
     * @param haystack
     * @param needle
     * @return
     */
    private static int countOccurrences(String haystack, char needle) {
        int count = 0;
        for (int i = 0; i < haystack.length(); i++) {
            if (haystack.charAt(i) == needle)
                count++;
        }
        return count;
    }

    /**
     * Cancels then closes a {@link PreparedStatement} and logs exceptions
     * without throwing. Does nothing if ps is null.
     *
     * @param ps
     */
    static void closeQuietly(PreparedStatement ps) {
        try {
            boolean isClosed;
            try {
                if (ps != null)
                    isClosed = ps.isClosed();
                else
                    isClosed = true;
            } catch (SQLException e) {
                logger.debug(e.getMessage());
                isClosed = true;
            }
            if (ps != null && !isClosed) {
                try {
                    ps.cancel();
                    logger.debug("cancelled {}", ps);
                } catch (SQLException e) {
                    logger.debug(e.getMessage());
                }
                ps.close();
                logger.debug("closed {}", ps);
            }
        } catch (SQLException e) {
            logger.debug(e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    /**
     * Closes a {@link Connection} and logs exceptions without throwing. Does
     * nothing if connection is null.
     *
     * @param connection
     */
    static void closeQuietly(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.debug("closed {}", connection);
            }
        } catch (SQLException e) {
            logger.debug(e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    /**
     * Closes a {@link Connection} only if the connection is in auto commit mode
     * and logs exceptions without throwing. Does nothing if connection is null.
     *
     * @param connection
     */
    static boolean closeQuietlyIfAutoCommit(Connection connection) {
        try {
            if (connection != null && !connection.isClosed() && connection.getAutoCommit()) {
                closeQuietly(connection);
                return true;
            } else
                return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Commits a {@link Connection} and logs exceptions without throwing.
     *
     * @param connection
     */
    static void commit(Connection connection) {
        if (connection != null)
            try {
                connection.commit();
                logger.debug("committed");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Rolls back a {@link Connection} and logs exceptions without throwing.
     *
     * @param connection
     */
    static void rollback(Connection connection) {
        if (connection != null)
            try {
                connection.rollback();
                logger.debug("rolled back");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
    }

    /**
     * Closes a {@link ResultSet} and logs exceptions without throwing.
     *
     * @param rs
     */
    static void closeQuietly(ResultSet rs) {
        try {
            if (rs != null && !rs.isClosed()) {
                rs.close();
                logger.debug("closed {}", rs);
            }
        } catch (SQLException e) {
            logger.debug(e.getMessage(), e);
        } catch (RuntimeException e) {
            logger.debug(e.getMessage(), e);
        }
    }

    /**
     * Returns true if and only if {@link Connection} is in auto commit mode.
     *
     * @param con
     * @return
     */
    static boolean isAutoCommit(Connection con) {
        try {
            return con.getAutoCommit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the empty list whenever called.
     */
    static Function<Integer, List<SqlParameter>> TO_EMPTY_PARAMETER_LIST = new Function<Integer, List<SqlParameter>>() {
        @Override
        public List<SqlParameter> apply(Integer n) {
            return Collections.emptyList();
        }

        ;
    };

    /**
     * Returns a function that converts the ResultSet column values into
     * parameters to the constructor (with number of parameters equals the
     * number of columns) of type <code>cls</code> then returns an instance of
     *
     * @param cls
     * @return
     */
    static <T> Function<ResultSet, T> autoMap(final Class<T> cls) {
        return new Function<ResultSet, T>() {
            @Override
            public T apply(ResultSet rs) {
                return autoMap(rs, cls);
            }
        };
    }

    /**
     * Converts the ResultSet column values into parameters to the constructor
     * (with number of parameters equals the number of columns) of type
     * <code>T</code> then returns an instance of type <code>T</code>. See See
     *
     * @param cls the class of the resultant instance
     * @return an automapped instance
     */
    @SuppressWarnings("unchecked")
    static <T> T autoMap(ResultSet rs, Class<T> cls) {
        try {
            int n = rs.getMetaData().getColumnCount();
            for (Constructor<?> c : cls.getDeclaredConstructors()) {
                if (n == c.getParameterTypes().length) {
                    return autoMap(rs, (Constructor<T>) c);
                }
            }

            T bean = (T) CUtils.getInstance(cls);
            //modify by woonill at 2015.06.02 for User Beans set Method
            return autoMapBeans(rs, bean);
//            throw new RuntimeException("constructor with number of parameters=" + n
//                    + "  not found in " + cls);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Converts the Result column values into parameters to the given Beans
     */

    static <T> T autoMapBeans(ResultSet rs, T bean) {

        String cname = "";
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columns = rsmd.getColumnCount();
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] proDescrtptors = beanInfo.getPropertyDescriptors();

            boolean find = false;
            for (int i = 1; i <= columns; i++) {
                find = false;
                cname = rsmd.getColumnLabel(i);
                if (proDescrtptors != null && proDescrtptors.length > 0) {
                    for (PropertyDescriptor propDesc : proDescrtptors) {
                        if (propDesc.getName().equalsIgnoreCase(cname)) {
                            Method wMethods = propDesc.getWriteMethod();
//			            	Method rMethod = propDesc.getReadMethod();
//			            	Object obs = getObject(rs,rMethod.getReturnType(),i);
//			            	Object arg = autoMap(obs,rMethod.getReturnType());

                            Class<?> pType = wMethods.getParameterTypes()[0];
                            Object obs = getObject(rs, pType, i);
                            Object arg = autoMap(obs, pType);

                            try {
                                wMethods.invoke(bean, arg);
                            } catch (Throwable te) {
                                logger.error("Can not invock method:" + wMethods.getName() + "  object:" + obs + " args:" + arg + "  Class.ParameterType:" + pType + " ColumnIndex:" + 1);
                                throw te;
                            }
                            find = true;
                            break;
                        }
                    }
                }
                if (!find) {
                    logger.error("Not found:" + cname + " property on Bean:" + bean.getClass().getName());
                    throw new IllegalArgumentException("Not found:" + cname + " property on Bean:" + bean.getClass().getName());
                }
            }
            return bean;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            throw new RuntimeException("problem with parameters=" + cname
                    + ", rs types=" + getRowInfo(rs)
                    + ". Be sure not to use primitives in a constructor when calling autoMap().", e);
        }
    }

    /**
     * Returns debugging info about the types of a list of objects.
     *
     * @param list
     * @return
     */
    private static String getTypeInfo(List<Object> list) {

        StringBuilder s = new StringBuilder();
        for (Object o : list) {
            if (s.length() > 0)
                s.append(", ");
            if (o == null)
                s.append("null");
            else {
                s.append(o.getClass().getName());
                s.append("=");
                s.append(o);
            }
        }
        return s.toString();
    }

    private static String getRowInfo(ResultSet rs) {
        StringBuilder s = new StringBuilder();
        try {
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                String name = md.getColumnName(i);
                String type = md.getColumnClassName(i);
                if (s.length() > 0)
                    s.append(", ");
                s.append(name);
                s.append("=");
                s.append(type);
            }
        } catch (SQLException e1) {
            throw new RuntimeException(e1);
        }
        return s.toString();
    }


    /**
     * Converts from java.sql Types to common java types like java.util.Date and
     *
     * @param o
     * @param cls
     * @return
     */
    public static Object autoMap(Object o, Class<?> cls) {
        if (o == null)
            return o;
        else if (cls.isAssignableFrom(o.getClass())) {
            return o;
        } else {
            if (o instanceof Date) {
                Date d = (Date) o;
                if (cls.isAssignableFrom(Long.class))
                    return d.getTime();
                else if (cls.isAssignableFrom(BigInteger.class))
                    return BigInteger.valueOf(d.getTime());
                else
                    return o;
            } else if (o instanceof Timestamp) {
                Timestamp t = (Timestamp) o;
                if (cls.isAssignableFrom(Long.class))
                    return t.getTime();
                else if (cls.isAssignableFrom(BigInteger.class))
                    return BigInteger.valueOf(t.getTime());
                else
                    return o;
            } else if (o instanceof Time) {
                Time t = (Time) o;
                if (cls.isAssignableFrom(Long.class))
                    return t.getTime();
                else if (cls.isAssignableFrom(BigInteger.class))
                    return BigInteger.valueOf(t.getTime());
                else
                    return o;
            } else if (o instanceof Blob && cls.isAssignableFrom(byte[].class)) {
                return toBytes((Blob) o);
            } else if (o instanceof Clob && cls.isAssignableFrom(String.class)) {
                return toString((Clob) o);
            } else if (o instanceof BigInteger && cls.isAssignableFrom(Long.class)) {
                return ((BigInteger) o).longValue();
            } else if (o instanceof BigInteger && cls.isAssignableFrom(Integer.class)) {
                return ((BigInteger) o).intValue();
            } else if (o instanceof BigInteger && cls.isAssignableFrom(Double.class)) {
                return ((BigInteger) o).doubleValue();
            } else if (o instanceof BigInteger && cls.isAssignableFrom(Float.class)) {
                return ((BigInteger) o).floatValue();
            } else if (o instanceof BigInteger && cls.isAssignableFrom(Short.class)) {
                return ((BigInteger) o).shortValue();
            } else if (o instanceof BigInteger && cls.isAssignableFrom(BigDecimal.class)) {
                return new BigDecimal((BigInteger) o);
            } else if (o instanceof BigDecimal && cls.isAssignableFrom(Double.class)) {
                return ((BigDecimal) o).doubleValue();
            } else if (o instanceof BigDecimal && cls.isAssignableFrom(Integer.class)) {
                return ((BigDecimal) o).toBigInteger().intValue();
            } else if (o instanceof BigDecimal && cls.isAssignableFrom(Float.class)) {
                return ((BigDecimal) o).floatValue();
            } else if (o instanceof BigDecimal && cls.isAssignableFrom(Short.class)) {
                return ((BigDecimal) o).toBigInteger().shortValue();
            } else if (o instanceof BigDecimal && cls.isAssignableFrom(Long.class)) {
                return ((BigDecimal) o).toBigInteger().longValue();
            } else if (o instanceof BigDecimal && cls.isAssignableFrom(BigInteger.class)) {
                return ((BigDecimal) o).toBigInteger();
            } else if ((o instanceof Short || o instanceof Integer || o instanceof Long)
                    && cls.isAssignableFrom(BigInteger.class)) {
                return new BigInteger(o.toString());
            } else if (o instanceof Number && cls.isAssignableFrom(BigDecimal.class)) {
                return new BigDecimal(o.toString());
            } else if (o instanceof Number && cls.isAssignableFrom(Short.class))
                return ((Number) o).shortValue();
            else if (o instanceof Number && cls.isAssignableFrom(Integer.class))
                return ((Number) o).intValue();
            else if (o instanceof Number && cls.isAssignableFrom(Integer.class))
                return ((Number) o).intValue();
            else if (o instanceof Number && cls.isAssignableFrom(Long.class))
                return ((Number) o).longValue();
            else if (o instanceof Number && cls.isAssignableFrom(Float.class))
                return ((Number) o).floatValue();
            else if (o instanceof Number && cls.isAssignableFrom(Double.class))
                return ((Number) o).doubleValue();
            else
                return o;
        }
    }

    public static <T> Object mapObject(final ResultSet rs, Class<T> cls, int i) {
        return autoMap(getObject(rs, cls, i), cls);
    }

    private static <T> Object getObject(final ResultSet rs, Class<T> cls, int i) {
        try {
            if (rs.getObject(i) == null) {
                return null;
            }

            final int type = rs.getMetaData().getColumnType(i);
            // TODO java.util.Calendar support
            // TODO XMLGregorian Calendar support

            if (type == Types.NVARCHAR) {
                return rs.getString(i);
            } else if (type == Types.DATE)
                return rs.getDate(i, Calendar.getInstance());
            else if (type == Types.TIME)
                return rs.getTime(i, Calendar.getInstance());
            else if (type == Types.TIMESTAMP)
                return rs.getTimestamp(i, Calendar.getInstance());
            else if (type == Types.CLOB && cls.equals(String.class)) {
                return toString(rs.getClob(i));
            } else if (type == Types.CLOB && Reader.class.isAssignableFrom(cls)) {
                Clob c = rs.getClob(i);
                Reader r = c.getCharacterStream();
                return createFreeOnCloseReader(c, r);
            } else if (type == Types.BLOB && cls.equals(byte[].class)) {
                return toBytes(rs.getBlob(i));
            } else if (type == Types.BLOB && InputStream.class.isAssignableFrom(cls)) {
                final Blob b = rs.getBlob(i);
                final InputStream is = rs.getBlob(i).getBinaryStream();
                return createFreeOnCloseInputStream(b, is);
            } else
                return rs.getObject(i);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Converts the ResultSet column values into parameters to the given
     * constructor (with number of parameters equals the number of columns) of
     * type <code>T</code> then returns an instance of type <code>T</code>. See
     *
     * @param rs the result set row
     * @param c  constructor to use for instantiation
     * @return automapped instance
     */
    private static <T> T autoMap(ResultSet rs, Constructor<T> c) {
        Class<?>[] types = c.getParameterTypes();
        List<Object> list = new ArrayList<Object>();
        for (int i = 0; i < types.length; i++) {
            list.add(autoMap(getObject(rs, types[i], i + 1), types[i]));
        }
        try {
            return newInstance(c, list);
        } catch (RuntimeException e) {
            throw new RuntimeException("problem with parameters=" + getTypeInfo(list)
                    + ", rs types=" + getRowInfo(rs)
                    + ". Be sure not to use primitives in a constructor when calling autoMap().", e);
        }
    }

    /**
     * @param c          constructor to use
     * @param parameters constructor parameters
     * @return
     */
    @SuppressWarnings("unchecked")
    private static <T> T newInstance(Constructor<?> c, List<Object> parameters) {
        try {
            return (T) c.newInstance(parameters.toArray());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Returns the bytes of a {@link Blob} and frees the blob resource.
     *
     * @param b blob
     * @return
     */

    private static byte[] toBytes(Blob b) {
        try {
            InputStream is = b.getBinaryStream();
            byte[] result = IOUtil.toByteArray(is);
            is.close();
            b.free();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Returns the String of a {@link Clob} and frees the clob resource.
     *
     * @param c
     * @return
     */
    private static String toString(Clob c) {
        try {
            Reader reader = c.getCharacterStream();
            String result = IOUtil.toString(reader);
            reader.close();
            c.free();
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Automatically frees the blob (<code>blob.free()</code>) once the blob
     * {@link InputStream} is closed.
     *
     * @param blob
     * @param is
     * @return
     */
    private static InputStream createFreeOnCloseInputStream(final Blob blob, final InputStream is) {
        return new InputStream() {

            @Override
            public int read() throws IOException {
                return is.read();
            }

            @Override
            public void close() throws IOException {
                try {
                    is.close();
                } finally {
                    try {
                        blob.free();
                    } catch (SQLException e) {
                        logger.debug(e.getMessage());
                    }
                }
            }
        };
    }

    /**
     * Automatically frees the clob (<code>Clob.free()</code>) once the clob
     * Reader is closed.
     *
     * @param clob
     * @param reader
     * @return
     */
    private static Reader createFreeOnCloseReader(final Clob clob, final Reader reader) {
        return new Reader() {

            @Override
            public void close() throws IOException {
                try {
                    reader.close();
                } finally {
                    try {
                        clob.free();
                    } catch (SQLException e) {
                        logger.debug(e.getMessage());
                    }
                }
            }

            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return reader.read(cbuf, off, len);
            }
        };
    }

    /**
     * Sets parameters for the {@link PreparedStatement}.
     *
     * @param ps
     * @param params
     * @throws SQLException
     */
    static void setParameters(PreparedStatement ps, List<SqlParameter> params) throws SQLException {
        for (int i = 1; i <= params.size(); i++) {
            Object o = params.get(i - 1).getValue();
            try {
                if (o == null)
                    ps.setObject(i, null);
                else if (o == NULL_CLOB)
                    ps.setNull(i, Types.CLOB);
                else if (o == NULL_BLOB)
                    ps.setNull(i, Types.BLOB);
                else {
                    Class<?> cls = o.getClass();
                    if (Clob.class.isAssignableFrom(cls)) {
                        setClob(ps, i, o, cls);
                    } else if (Blob.class.isAssignableFrom(cls)) {
                        setBlob(ps, i, o, cls);
                    } else if (Calendar.class.isAssignableFrom(cls)) {
                        Calendar cal = (Calendar) o;
                        Timestamp t = new Timestamp(cal.getTimeInMillis());
                        ps.setTimestamp(i, t, cal);
                    } else if (Time.class.isAssignableFrom(cls)) {
                        Calendar cal = Calendar.getInstance();
                        ps.setTime(i, (Time) o, cal);
                    } else if (Timestamp.class.isAssignableFrom(cls)) {
                        Calendar cal = Calendar.getInstance();
                        ps.setTimestamp(i, (Timestamp) o, cal);
                    } else if (Date.class.isAssignableFrom(cls)) {
                        Calendar cal = Calendar.getInstance();
                        ps.setDate(i, (Date) o, cal);
                    } else if (java.util.Date.class.isAssignableFrom(cls)) {
                        Calendar cal = Calendar.getInstance();
                        java.util.Date date = (java.util.Date) o;
                        ps.setTimestamp(i, new Timestamp(date.getTime()), cal);
                    } else
                        ps.setObject(i, o);
                }
            } catch (SQLException e) {
                logger.debug("{} when setting ps.setObject({},{})", e.getMessage(), i, o);
                throw e;
            }
        }
    }

    /**
     * Sets a blob parameter for the prepared statement.
     *
     * @param ps
     * @param i
     * @param o
     * @param cls
     * @throws SQLException
     */
    private static void setBlob(PreparedStatement ps, int i, Object o, Class<?> cls)
            throws SQLException {
        final InputStream is;
        if (o instanceof byte[]) {
            is = new ByteArrayInputStream((byte[]) o);
        } else if (o instanceof InputStream)
            is = (InputStream) o;
        else
            throw new RuntimeException("cannot insert parameter of type " + cls
                    + " into blob column " + i);
        Blob c = ps.getConnection().createBlob();
        OutputStream os = c.setBinaryStream(1);
        copy(is, os);
        ps.setBlob(i, c);
    }

    /**
     * Sets the clob parameter for the prepared statement.
     *
     * @param ps
     * @param i
     * @param o
     * @param cls
     * @throws SQLException
     */
    private static void setClob(PreparedStatement ps, int i, Object o, Class<?> cls)
            throws SQLException {
        final Reader r;
        if (o instanceof String)
            r = new StringReader((String) o);
        else if (o instanceof Reader)
            r = (Reader) o;
        else
            throw new RuntimeException("cannot insert parameter of type " + cls
                    + " into clob column " + i);
        Clob c = ps.getConnection().createClob();
        Writer w = c.setCharacterStream(1);
        copy(r, w);
        ps.setClob(i, c);
    }

    /**
     * Copies a {@link Reader} to a {@link Writer}.
     *
     * @param input
     * @param output
     * @return
     */
    private static int copy(Reader input, Writer output) {
        try {
            return IOUtil.copy(input, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Copies an {@link InputStream} to an {@link OutputStream}.
     *
     * @param input
     * @param output
     * @return
     */
    private static int copy(InputStream input, OutputStream output) {
        try {
            return IOUtil.copy(input, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a function that reads a {@link Reader} into a String.
     */
    public static final Function<Reader, String> READER_TO_STRING = new Function<Reader, String>() {
        @Override
        public String apply(Reader r) {
            try {
                return IOUtil.toString(r);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    };


    public static abstract class RowProcessFunc<R> implements Function<ResultSet, R> {

        @Override
        public R apply(ResultSet in) {
            try {
                return process(in);
            } catch (SQLException e) {
                throw new IllegalArgumentException(e);
            }
        }

        abstract protected R process(ResultSet in) throws SQLException;
    }
}


