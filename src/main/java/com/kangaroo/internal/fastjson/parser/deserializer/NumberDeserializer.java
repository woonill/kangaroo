package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.Type;
import java.math.BigDecimal;

import com.kangaroo.internal.fastjson.JSONException;

public class NumberDeserializer implements ObjectDeserializer {

    public final static NumberDeserializer instance = new NumberDeserializer();

    @SuppressWarnings("unchecked")
    public <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type clazz, Object fieldName) {
        final com.kangaroo.internal.fastjson.parser.JSONLexer lexer = parser.lexer;
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT) {
            if (clazz == double.class || clazz  == Double.class) {
                String val = lexer.numberString();
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                return (T) Double.valueOf(Double.parseDouble(val));
            }
            
            long val = lexer.longValue();
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

            if (clazz == short.class || clazz == Short.class) {
                if (val > Short.MAX_VALUE || val < Short.MIN_VALUE) {
                    throw new JSONException("short overflow : " + val);
                }
                return (T) Short.valueOf((short) val);
            }

            if (clazz == byte.class || clazz == Byte.class) {
                if (val > Byte.MAX_VALUE || val < Byte.MIN_VALUE) {
                    throw new JSONException("short overflow : " + val);
                }

                return (T) Byte.valueOf((byte) val);
            }

            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) {
                return (T) Integer.valueOf((int) val);
            }
            return (T) Long.valueOf(val);
        }

        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_FLOAT) {
            if (clazz == double.class || clazz == Double.class) {
                String val = lexer.numberString();
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                return (T) Double.valueOf(Double.parseDouble(val));
            }

            BigDecimal val = lexer.decimalValue();
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

            if (clazz == short.class || clazz == Short.class) {
                if (val.compareTo(BigDecimal.valueOf(Short.MAX_VALUE)) > 0 || val.compareTo(BigDecimal.valueOf(Short.MIN_VALUE)) < 0) {
                    throw new JSONException("short overflow : " + val);
                }
                return (T) Short.valueOf(val.shortValue());
            }

            if (clazz == byte.class || clazz == Byte.class) {
                return (T) Byte.valueOf(val.byteValue());
            }

            return (T) val;
        }

        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.IDENTIFIER && "NaN".equals(lexer.stringVal())) {
            lexer.nextToken();
            Object nan = null;
            if (clazz == Double.class) {
                nan = Double.NaN;
            } else if (clazz == Float.class) {
                nan = Float.NaN;
            }
            return (T) nan;
        }

        Object value = parser.parse();

        if (value == null) {
            return null;
        }

        if (clazz == double.class || clazz == Double.class) {
            try {
                return (T) com.kangaroo.internal.fastjson.util.TypeUtils.castToDouble(value);
            } catch (Exception ex) {
                throw new JSONException("parseDouble error, field : " + fieldName, ex);
            }
        }

        if (clazz == short.class || clazz == Short.class) {
            try {
                return (T) com.kangaroo.internal.fastjson.util.TypeUtils.castToShort(value);
            } catch (Exception ex) {
                throw new JSONException("parseShort error, field : " + fieldName, ex);
            }
        }

        if (clazz == byte.class || clazz == Byte.class) {
            try {
                return (T) com.kangaroo.internal.fastjson.util.TypeUtils.castToByte(value);
            } catch (Exception ex) {
                throw new JSONException("parseByte error, field : " + fieldName, ex);
            }
        }

        return (T) com.kangaroo.internal.fastjson.util.TypeUtils.castToBigDecimal(value);
    }

    public int getFastMatchToken() {
        return com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT;
    }
}
