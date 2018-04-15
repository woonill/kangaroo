package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.Type;

import com.kangaroo.internal.fastjson.JSONException;

public class StackTraceElementDeserializer implements com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer {

    public final static StackTraceElementDeserializer instance = new StackTraceElementDeserializer();

    @SuppressWarnings({ "unchecked", "unused" })
    public <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type type, Object fieldName) {
        com.kangaroo.internal.fastjson.parser.JSONLexer lexer = parser.lexer;
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
            lexer.nextToken();
            return null;
        }

        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE && lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
            throw new JSONException("syntax error: " + com.kangaroo.internal.fastjson.parser.JSONToken.name(lexer.token()));
        }

        String declaringClass = null;
        String methodName = null;
        String fileName = null;
        int lineNumber = 0;
        String moduleName = null;
        String moduleVersion = null;
        String classLoaderName = null;

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
            if ("className".equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    declaringClass = null;
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                    declaringClass = lexer.stringVal();
                } else {
                    throw new JSONException("syntax error");
                }
            } else if ("methodName".equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    methodName = null;
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                    methodName = lexer.stringVal();
                } else {
                    throw new JSONException("syntax error");
                }
            } else if ("fileName".equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    fileName = null;
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                    fileName = lexer.stringVal();
                } else {
                    throw new JSONException("syntax error");
                }
            } else if ("lineNumber".equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    lineNumber = 0;
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT) {
                    lineNumber = lexer.intValue();
                } else {
                    throw new JSONException("syntax error");
                }
            } else if ("nativeMethod".equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.TRUE) {
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.FALSE) {
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                } else {
                    throw new JSONException("syntax error");
                }
            } else if (key == com.kangaroo.internal.fastjson.JSON.DEFAULT_TYPE_KEY) {
               if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                    String elementType = lexer.stringVal();
                    if (!elementType.equals("java.lang.StackTraceElement")) {
                        throw new JSONException("syntax error : " + elementType);    
                    }
                } else {
                    if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                        throw new JSONException("syntax error");
                    }
                }
            } else if ("moduleName".equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    moduleName = null;
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                    moduleName = lexer.stringVal();
                } else {
                    throw new JSONException("syntax error");
                }
            } else if ("moduleVersion".equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    moduleVersion = null;
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                    moduleVersion = lexer.stringVal();
                } else {
                    throw new JSONException("syntax error");
                }
            } else if ("classLoaderName".equals(key)) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    classLoaderName = null;
                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                    classLoaderName = lexer.stringVal();
                } else {
                    throw new JSONException("syntax error");
                }
            } else {
                throw new JSONException("syntax error : " + key);
            }

            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                break;
            }
        }
        return (T) new StackTraceElement(declaringClass, methodName, fileName, lineNumber);
    }

    public int getFastMatchToken() {
        return com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE;
    }
}
