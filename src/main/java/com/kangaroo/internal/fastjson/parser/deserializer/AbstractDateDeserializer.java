package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import com.kangaroo.internal.fastjson.JSONException;
import com.kangaroo.internal.fastjson.parser.DefaultJSONParser;

public abstract class AbstractDateDeserializer extends ContextObjectDeserializer implements com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer {

    public <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type clazz, Object fieldName) {
        return deserialze(parser, clazz, fieldName, null, 0);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type clazz, Object fieldName, String format, int features) {
        com.kangaroo.internal.fastjson.parser.JSONLexer lexer = parser.lexer;

        Object val;
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT) {
            val = lexer.longValue();
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
        } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
            String strVal = lexer.stringVal();
            
            if (format != null) {
                SimpleDateFormat simpleDateFormat = null;
                try {
                    simpleDateFormat = new SimpleDateFormat(format, com.kangaroo.internal.fastjson.JSON.defaultLocale);
                } catch (IllegalArgumentException ex) {
                    if (format.equals("yyyy-MM-ddTHH:mm:ss.SSS")) {
                        format = "yyyy-MM-dd'T'HH:mm:ss.SSS";
                        simpleDateFormat = new SimpleDateFormat(format);
                    } else  if (format.equals("yyyy-MM-ddTHH:mm:ss")) {
                        format = "yyyy-MM-dd'T'HH:mm:ss";
                        simpleDateFormat = new SimpleDateFormat(format);
                    }
                }

                try {
                    val = simpleDateFormat.parse(strVal);
                } catch (ParseException ex) {
                    if (format.equals("yyyy-MM-dd'T'HH:mm:ss.SSS") //
                            && strVal.length() == 19) {
                        try {
                            val = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(strVal);
                        } catch (ParseException ex2) {
                            // skip
                            val = null;
                        }
                    } else {
                        // skip
                        val = null;
                    }
                }
            } else {
                val = null;
            }
            
            if (val == null) {
                val = strVal;
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                
                if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowISO8601DateFormat)) {
                    com.kangaroo.internal.fastjson.parser.JSONScanner iso8601Lexer = new com.kangaroo.internal.fastjson.parser.JSONScanner(strVal);
                    if (iso8601Lexer.scanISO8601DateIfMatch()) {
                        val = iso8601Lexer.getCalendar().getTime();
                    }
                    iso8601Lexer.close();
                }
            }
        } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
            lexer.nextToken();
            val = null;
        } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE) {
            lexer.nextToken();
            
            String key;
            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                key = lexer.stringVal();
                
                if (com.kangaroo.internal.fastjson.JSON.DEFAULT_TYPE_KEY.equals(key)) {
                    lexer.nextToken();
                    parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.COLON);
                    
                    String typeName = lexer.stringVal();
                    Class<?> type = parser.getConfig().checkAutoType(typeName, null, lexer.getFeatures());
                    if (type != null) {
                        clazz = type;
                    }
                    
                    parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
                    parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                }
                
                lexer.nextTokenWithColon(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT);
            } else {
                throw new JSONException("syntax error");
            }
            
            long timeMillis;
            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT) {
                timeMillis = lexer.longValue();
                lexer.nextToken();
            } else {
                throw new JSONException("syntax error : " + lexer.tokenName());
            }
            
            val = timeMillis;
            
            parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE);
        } else if (parser.getResolveStatus() == com.kangaroo.internal.fastjson.parser.DefaultJSONParser.TypeNameRedirect) {
            parser.setResolveStatus(com.kangaroo.internal.fastjson.parser.DefaultJSONParser.NONE);
            parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                if (!"val".equals(lexer.stringVal())) {
                    throw new JSONException("syntax error");
                }
                lexer.nextToken();
            } else {
                throw new JSONException("syntax error");
            }

            parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.COLON);

            val = parser.parse();

            parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE);
        } else {
            val = parser.parse();
        }

        return (T) cast(parser, clazz, fieldName, val);
    }

    protected abstract <T> T cast(DefaultJSONParser parser, Type clazz, Object fieldName, Object value);
}
