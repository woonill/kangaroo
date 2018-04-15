package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.Type;

import com.kangaroo.internal.fastjson.JSONException;

public class TimeDeserializer implements ObjectDeserializer {

    public final static TimeDeserializer instance = new TimeDeserializer();

    @SuppressWarnings("unchecked")
    public <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type clazz, Object fieldName) {
        com.kangaroo.internal.fastjson.parser.JSONLexer lexer = parser.lexer;
        
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
            
            if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                throw new JSONException("syntax error");
            }
            
            lexer.nextTokenWithColon(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT);
            
            if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT) {
                throw new JSONException("syntax error");
            }
            
            long time = lexer.longValue();
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE);
            if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                throw new JSONException("syntax error");
            }
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
            
            return (T) new java.sql.Time(time);
        }
        
        Object val = parser.parse();

        if (val == null) {
            return null;
        }

        if (val instanceof java.sql.Time) {
            return (T) val;
        } else if (val instanceof Number) {
            return (T) new java.sql.Time(((Number) val).longValue());
        } else if (val instanceof String) {
            String strVal = (String) val;
            if (strVal.length() == 0) {
                return null;
            }
            
            long longVal;
            com.kangaroo.internal.fastjson.parser.JSONScanner dateLexer = new com.kangaroo.internal.fastjson.parser.JSONScanner(strVal);
            if (dateLexer.scanISO8601DateIfMatch()) {
                longVal = dateLexer.getCalendar().getTimeInMillis();
            } else {
                boolean isDigit = true;
                for (int i = 0; i< strVal.length(); ++i) {
                    char ch = strVal.charAt(i);
                    if (ch < '0' || ch > '9') {
                        isDigit = false;
                        break;
                    }
                }
                if (!isDigit) {
                    dateLexer.close();
                    return (T) java.sql.Time.valueOf(strVal);    
                }
                
                longVal = Long.parseLong(strVal);
            }
            dateLexer.close();
            return (T) new java.sql.Time(longVal);
        }
        
        throw new JSONException("parse error");
    }

    public int getFastMatchToken() {
        return com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT;
    }
}
