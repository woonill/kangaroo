package com.kangaroo.internal.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SqlTypeValue {

    int TYPE_UNKNOWN = Integer.MIN_VALUE;

    void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, String typeName) throws SQLException;
}
