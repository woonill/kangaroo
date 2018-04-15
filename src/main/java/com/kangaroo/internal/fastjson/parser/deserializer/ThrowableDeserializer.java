package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.kangaroo.internal.fastjson.JSONException;

public class ThrowableDeserializer extends com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer {

    public ThrowableDeserializer(com.kangaroo.internal.fastjson.parser.ParserConfig mapping, Class<?> clazz){
        super(mapping, clazz, clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type type, Object fieldName) {
        com.kangaroo.internal.fastjson.parser.JSONLexer lexer = parser.lexer;
        
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
            lexer.nextToken();
            return null;
        }

        if (parser.getResolveStatus() == com.kangaroo.internal.fastjson.parser.DefaultJSONParser.TypeNameRedirect) {
            parser.setResolveStatus(com.kangaroo.internal.fastjson.parser.DefaultJSONParser.NONE);
        } else {
            if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE) {
                throw new JSONException("syntax error");
            }
        }

        Throwable cause = null;
        Class<?> exClass = null;
        
        if (type != null && type instanceof Class) {
        	Class<?> clazz = (Class<?>) type;
        	if (Throwable.class.isAssignableFrom(clazz)) {
        		exClass = clazz;
        	}
        }
        
        String message = null;
        StackTraceElement[] stackTrace = null;
        Map<String, Object> otherValues = null;


        for (;;) {
            // lexer.scanSymbol
            String key = lexer.scanSymbol(parser.getSymbolTable());

            if (key == null) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                    break;
                }
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                    if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowArbitraryCommas)) {
                        continue;
                    }
                }
            }

            lexer.nextTokenWithColon(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);

            if (com.kangaroo.internal.fastjson.JSON.DEFAULT_TYPE_KEY.equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                    String exClassName = lexer.stringVal();
                    exClass = parser.getConfig().checkAutoType(exClassName, Throwable.class, lexer.getFeatures());
                } else {
                    throw new JSONException("syntax error");
                }
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
            } else if ("message".equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    message = null;
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                    message = lexer.stringVal();
                } else {
                    throw new JSONException("syntax error");
                }
                lexer.nextToken();
            } else if ("cause".equals(key)) {
                cause = deserialze(parser, null, "cause");
            } else if ("stackTrace".equals(key)) {
                stackTrace = parser.parseObject(StackTraceElement[].class);
            } else {
                if (otherValues == null) {
                    otherValues = new HashMap<String, Object>();
                }
                otherValues.put(key, parser.parse());
            }

            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                break;
            }
        }

        Throwable ex = null;
        if (exClass == null) {
            ex = new Exception(message, cause);
        } else {
            if (!Throwable.class.isAssignableFrom(exClass)) {
                throw new JSONException("type not match, not Throwable. " + exClass.getName());
            }

            try {
                ex = createException(message, cause, exClass);
                if (ex == null) {
                    ex = new Exception(message, cause);
                }
            } catch (Exception e) {
                throw new JSONException("create instance error", e);
            }
        }

        if (stackTrace != null) {
            ex.setStackTrace(stackTrace);
        }

        if (otherValues != null) {
            com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer exBeanDeser = null;

            if (exClass != null) {
                if (exClass == clazz) {
                    exBeanDeser = this;
                } else {
                    com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer exDeser = parser.getConfig().getDeserializer(exClass);
                    if (exDeser instanceof com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer) {
                        exBeanDeser = (com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer) exDeser;
                    }
                }
            }

            if (exBeanDeser != null) {
                for (Map.Entry<String, Object> entry : otherValues.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    com.kangaroo.internal.fastjson.parser.deserializer.FieldDeserializer fieldDeserializer = exBeanDeser.getFieldDeserializer(key);
                    if (fieldDeserializer != null) {
                        fieldDeserializer.setValue(ex, value);
                    }
                }
            }
        }

        return (T) ex;
    }

    private Throwable createException(String message, Throwable cause, Class<?> exClass) throws Exception {
        Constructor<?> defaultConstructor = null;
        Constructor<?> messageConstructor = null;
        Constructor<?> causeConstructor = null;
        for (Constructor<?> constructor : exClass.getConstructors()) {
        	Class<?>[] types = constructor.getParameterTypes();
            if (types.length == 0) {
                defaultConstructor = constructor;
                continue;
            }

            if (types.length == 1 && types[0] == String.class) {
                messageConstructor = constructor;
                continue;
            }

            if (types.length == 2 && types[0] == String.class && types[1] == Throwable.class) {
                causeConstructor = constructor;
                continue;
            }
        }

        if (causeConstructor != null) {
            return (Throwable) causeConstructor.newInstance(message, cause);
        }

        if (messageConstructor != null) {
            return (Throwable) messageConstructor.newInstance(message);
        }

        if (defaultConstructor != null) {
            return (Throwable) defaultConstructor.newInstance();
        }

        return null;
    }

    public int getFastMatchToken() {
        return com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE;
    }
}
