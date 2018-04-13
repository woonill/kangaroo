package com.kangaroo.internal.jdbc;

import com.kangaroo.util.tuple.Tuples;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public final class SDatabase {

    private DataSource ds;

    private SDatabase(DataSource ds2) {
        this.ds = ds2;
    }

    public QueryBuilder query(String sql) {
        return new QueryBuilder(sql);
    }

    public UpdaterBuilder updater(String sql) {
        return new UpdaterBuilder(sql);
    }

    public BatchUpdaterBuilder batchUpdater(String sql) {
        return new BatchUpdaterBuilder(sql);
    }


    private Connection newConnection() {
        return JdbcUtil.getConnection(ds);
    }

    public Session openSession() {
        return new Session(this.newConnection());
    }


    public static final class STransaction {

        private Logger logger = LoggerFactory.getLogger(this.getClass());
        private Connection con;
        private AtomicBoolean completed = new AtomicBoolean(false);
        private boolean _isRollback = false;
//		private boolean _isCommited = false;

        private STransaction(Connection con2) {
            this.con = con2;
        }

        public void commit() {

            if (this.completed.compareAndSet(false, true)) {
                if (!_isRollback) {
                    logger.debug("Start commit Transaction --------------------------------->>");
                    try {
                        this.con.commit();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        try {
                            logger.info("Start rollback now");
                            this.con.rollback();
                        } catch (SQLException e1) {
//							return Observable.error(e);
                            e1.printStackTrace();
                        }
                    }
                }
            }
            if (this._isRollback) {
                logger.debug("was Rollbacked transaction so do not commit it------------------------------------------>>");
            }
        }

        public boolean rollback(Throwable... error) {

            if (!completed.compareAndSet(false, true)) {
                throw new IllegalArgumentException("Transaction was completed");
            }
            _isRollback = true;
            try {
                this.con.rollback();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    public static final class Session {

        private final Logger logger = LoggerFactory.getLogger(this.getClass());
        private final Connection con;
        private AtomicBoolean _isOpenTransaction = new AtomicBoolean(false);
        private AtomicBoolean _isClosed = new AtomicBoolean(false);


        private Session(Connection con2) {
            this.con = con2;
        }

        public DatabaseOperatorBuilder getDatabaseOperatorBuilder() {

            if (_isClosed.get()) {
                throw new IllegalStateException("Session Closed");
            }
            return new DatabaseOperatorBuilder() {

                @Override
                public DatabaseUpdater buildUpdater(String updateSQL) {
                    return new DatabaseUpdater(updateSQL, new JdbcHandler(con));
                }

                @Override
                public DatabaseSelector buildQuery(String querySQL, Object... param) {
                    return new DatabaseSelector(querySQL, new JdbcHandler(con), param);
                }
            };
        }


        private STransaction transaction;

        public STransaction beginTransaction() {
            if (!_isOpenTransaction.compareAndSet(false, true)) {
                logger.info("Start Commit Transaction ");
                throw new IllegalStateException("Transaction started");
            }

            if (!JdbcUtil.isAutoCommit(con)) {
                throw new IllegalStateException("Connection is not AutoCommit");
            }
            try {
                con.setAutoCommit(false);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            this.transaction = new STransaction(this.con);
            return this.transaction;
        }

        public STransaction beginTransaction(int level) {
            if (!_isOpenTransaction.compareAndSet(false, true)) {
                logger.info("Start Commit Transaction ");
                throw new IllegalStateException("Transaction started");
            }

            if (!JdbcUtil.isAutoCommit(con)) {
                throw new IllegalStateException("Connection is not AutoCommit");
            }
            try {
                con.setAutoCommit(false);
                con.setTransactionIsolation(level);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            this.transaction = new STransaction(this.con);
            return this.transaction;
        }


        public boolean isBeginTransaction() {
            return this.transaction != null;
        }

        public Observable<Boolean> close() {

            if (!_isClosed.compareAndSet(false, true)) {
                throw new IllegalStateException("The Session closed");
            }

            logger.trace("Start Close Connection now");
            try {
                if (_isOpenTransaction.get()) {
                    if (transaction != null) {
//						logger.debug("Start commit Transaction now----------------------------------->>");
                        transaction.commit();
                    }
                    return Observable.just(true);
                }
            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    logger.debug("Start Close Connection now");
                    JdbcUtil.closeQuietly(this.con);
                }
            }
            return Observable.just(true);
        }
    }


    public final class QueryBuilder {

        private String sql;
        private List<Object> parameters = new LinkedList<Object>();

        private QueryBuilder(String sql) {
            this.sql = sql;
        }

        public QueryBuilder parameter(Object... params) {
            this.parameters.addAll(Arrays.asList(params));
            return this;
        }

        public <T> Observable<T> autoMap(Class<T> cls) {
            return get(JdbcUtil.autoMap(cls));
        }

        public <T> Observable<T> getAs(Class<T> mapClass) {
//			return this.get(JdbcUtil.autoMap(mapClass));
            return get(Tuples.single(mapClass));
        }

        public <T> Observable<T> get(Function<ResultSet, T> resultProcessor) {
            Session nsession = openSession();
            Object[] param = this.parameters.toArray(new Object[parameters.size()]);
            Observable<T> obs = nsession.getDatabaseOperatorBuilder().buildQuery(this.sql, param).processResult(resultProcessor);
            nsession.close();
            return obs;
        }
    }


    public final class UpdaterBuilder {

        private String sql;

        private UpdaterBuilder(String sql) {
            this.sql = sql;
        }

        public Observable<Integer> update(Object... parameter) {
            Session nsession = openSession();
            Observable<Integer> res = nsession.getDatabaseOperatorBuilder().buildUpdater(this.sql).update(parameter);
            nsession.close();
            return res;
        }
    }

    public final class BatchUpdaterBuilder {

        private String sql;

        private BatchUpdaterBuilder(String sql) {
            this.sql = sql;
        }

        public Observable<Boolean> update(BatchPreparedStatementSetter bpss) {
            Session nsession = openSession();
            Observable<Boolean> res = nsession.getDatabaseOperatorBuilder().buildUpdater(this.sql).batchUpdate(bpss);
            nsession.close();
            return res;
        }
    }


    public static final class DatabaseUpdater {


        private final String sql;
        private JdbcHandler jdbcHandler;

        private DatabaseUpdater(String sql, JdbcHandler jdbcHandler2) {
            this.sql = sql;
            this.jdbcHandler = jdbcHandler2;
        }


        public Observable<Boolean> batchUpdate(BatchPreparedStatementSetter pss) {
            return this.jdbcHandler.batchUpdate(this.sql, pss);
        }

        public Observable<Integer> update(Object... param) {
            return this.jdbcHandler.update(this.sql, param);
        }
    }


    public static SDatabase newDatabase(DataSource dataSource) {
        return new SDatabase(dataSource);
    }


    public static class DatabaseSelector {

        private String sql;
        private Object[] parameter;
        private JdbcHandler jdbcHandler;

        private DatabaseSelector(String sql, JdbcHandler handler, Object... parameters) {
            this.sql = sql;
            this.parameter = parameters;
            this.jdbcHandler = handler;
        }

        public <T> Observable<T> processResult(Function<ResultSet, T> prs) {
            return jdbcHandler.query(this.sql, prs, parameter);
        }

    }

    public abstract static class DatabaseOperatorBuilder {

        private DatabaseOperatorBuilder() {
        }

        public abstract DatabaseSelector buildQuery(String querySQL, Object... param);

        public abstract DatabaseUpdater buildUpdater(String updateSQL);
    }

    static class JdbcHandler {


        private Connection con;

        private JdbcHandler(Connection con) {
            this.con = con;
        }

        public <T> Observable<T> query(String sql, Function<ResultSet, T> rowProcessor, Object... parameters) {

            PreparedStatementSetter pss = this.createPreparedStatementSetter(parameters);
            PreparedStatement ps = this.createPreparedStatement(sql);
            ResultSet frs = null;
            try {
                pss.setValues(ps);
                frs = ps.executeQuery();
                List<T> tArray = new LinkedList<T>();
                while (frs.next()) {
                    T value = rowProcessor.apply(frs);
                    tArray.add(value);
                }
                return Observable.fromIterable(tArray);
            } catch (SQLException ex) {
//				ex.printStackTrace();
                return Observable.error(new SQLStateSQLExceptionTranslator().translate("PreparedStatementCallback", sql, ex));
            } finally {
                JdbcUtil.closeResultSet(frs);
                JdbcUtil.closeStatement(ps);
            }
        }


        public Observable<Integer> update(String sql, Object... parameters) {

            PreparedStatementSetter pss = this.createPreparedStatementSetter(parameters);
            PreparedStatement ps = this.createPreparedStatement(sql);
            try {
                pss.setValues(ps);
                int resultCount = ps.executeUpdate();
                return Observable.just(resultCount);
            } catch (SQLException e) {
//				e.printStackTrace();
                throw new IllegalArgumentException(e);
            } finally {
                JdbcUtil.closeStatement(ps);
            }
        }

        public Observable<Boolean> batchUpdate(String sql, BatchPreparedStatementSetter pss) {
            PreparedStatement ps = this.createPreparedStatement(sql);
            try {
                int batchSize = pss.getBatchSize();
                if (!JdbcUtil.supportsBatchUpdates(ps.getConnection())) {
                    throw new IllegalStateException("The JdbcDriver not support Batch");
                }
                for (int i = 0; i < batchSize; i++) {
                    pss.setValues(ps, i);
                    ps.addBatch();
                }
//				int[] rows = 
                ps.executeBatch();
                return Observable.just(true);
            } catch (SQLException e) {
                throw new IllegalArgumentException(e);
//				return Observable.error(e);
            } finally {
                JdbcUtil.closeStatement(ps);
            }
        }


        public PreparedStatement createPreparedStatement(String sql) {
            try {
                return con.prepareStatement(sql);
            } catch (SQLException e) {
                throw new IllegalArgumentException(e);
            }
        }

        public PreparedStatementSetter createPreparedStatementSetter(Object... parameters) {
            PreparedStatementSetter pss = getPreparedStatementSetter(parameters);
            return pss;
        }

        PreparedStatementSetter getPreparedStatementSetter(Object... parameters) {
            if (parameters != null && parameters.length < 1) {
                return PreparedStatementSetter.NONE;
            }
            return new ArgPreparedStatementSetter(parameters);
        }
    }
}
