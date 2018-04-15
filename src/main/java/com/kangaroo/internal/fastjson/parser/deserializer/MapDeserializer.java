package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.kangaroo.internal.fastjson.JSONArray;
import com.kangaroo.internal.fastjson.JSONException;
import com.kangaroo.internal.fastjson.JSONObject;

public class MapDeserializer implements ObjectDeserializer {
    public static MapDeserializer instance = new MapDeserializer();
    
    
    @SuppressWarnings("unchecked")
    public <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type type, Object fieldName) {
        if (type == JSONObject.class && parser.getFieldTypeResolver() == null) {
            return (T) parser.parseObject();
        }
        
        final com.kangaroo.internal.fastjson.parser.JSONLexer lexer = parser.lexer;
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
            return null;
        }

        Map<Object, Object> map = createMap(type);

        com.kangaroo.internal.fastjson.parser.ParseContext context = parser.getContext();

        try {
            parser.setContext(context, map, fieldName);
            return (T) deserialze(parser, type, fieldName, map);
        } finally {
            parser.setContext(context);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Object deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type type, Object fieldName, Map map) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type keyType = parameterizedType.getActualTypeArguments()[0];
            Type valueType = null;
            if(map.getClass().getName().equals("org.springframework.util.LinkedMultiValueMap")){
                valueType = List.class;
            }else{
                valueType = parameterizedType.getActualTypeArguments()[1];
            }
            if (String.class == keyType) {
                return parseMap(parser, (Map<String, Object>) map, valueType, fieldName);
            } else {
                return parseMap(parser, map, keyType, valueType, fieldName);
            }
        } else {
            return parser.parseObject(map, fieldName);
        }
    }
    
    @SuppressWarnings("rawtypes")
    public static Map parseMap(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Map<String, Object> map, Type valueType, Object fieldName) {
        com.kangaroo.internal.fastjson.parser.JSONLexer lexer = parser.lexer;

        int token = lexer.token();
        if (token != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE) {
            String msg = "syntax error, expect {, actual " + lexer.tokenName();
            if (fieldName instanceof String) {
                msg += ", fieldName ";
                msg += fieldName;
            }
            msg += ", ";
            msg += lexer.info();

            if (token != com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                JSONArray array = new JSONArray();
                parser.parseArray(array, fieldName);

                if (array.size() == 1) {
                    Object first = array.get(0);
                    if (first instanceof JSONObject) {
                        return (JSONObject) first;
                    }
                }
            }

            throw new JSONException(msg);
        }

        com.kangaroo.internal.fastjson.parser.ParseContext context = parser.getContext();
        try {
            for (int i = 0;;++i) {
                lexer.skipWhitespace();
                char ch = lexer.getCurrent();
                if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowArbitraryCommas)) {
                    while (ch == ',') {
                        lexer.next();
                        lexer.skipWhitespace();
                        ch = lexer.getCurrent();
                    }
                }

                String key;
                if (ch == '"') {
                    key = lexer.scanSymbol(parser.getSymbolTable(), '"');
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new JSONException("expect ':' at " + lexer.pos());
                    }
                } else if (ch == '}') {
                    lexer.next();
                    lexer.resetStringPosition();
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                    return map;
                } else if (ch == '\'') {
                    if (!lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowSingleQuotes)) {
                        throw new JSONException("syntax error");
                    }

                    key = lexer.scanSymbol(parser.getSymbolTable(), '\'');
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new JSONException("expect ':' at " + lexer.pos());
                    }
                } else {
                    if (!lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowUnQuotedFieldNames)) {
                        throw new JSONException("syntax error");
                    }

                    key = lexer.scanSymbolUnQuoted(parser.getSymbolTable());
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new JSONException("expect ':' at " + lexer.pos() + ", actual " + ch);
                    }
                }

                lexer.next();
                lexer.skipWhitespace();
                ch = lexer.getCurrent();

                lexer.resetStringPosition();

                if (key == com.kangaroo.internal.fastjson.JSON.DEFAULT_TYPE_KEY && !lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableSpecialKeyDetect)) {
                    String typeName = lexer.scanSymbol(parser.getSymbolTable(), '"');
                    final com.kangaroo.internal.fastjson.parser.ParserConfig config = parser.getConfig();

                    Class<?> clazz = config.checkAutoType(typeName, null, lexer.getFeatures());

                    if (Map.class.isAssignableFrom(clazz) ) {
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                            return map;
                        }
                        continue;
                    }

                    ObjectDeserializer deserializer = config.getDeserializer(clazz);

                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

                    parser.setResolveStatus(com.kangaroo.internal.fastjson.parser.DefaultJSONParser.TypeNameRedirect);

                    if (context != null && !(fieldName instanceof Integer)) {
                        parser.popContext();
                    }

                    return (Map) deserializer.deserialze(parser, clazz, fieldName);
                }

                Object value;
                lexer.nextToken();

                if (i != 0) {
                    parser.setContext(context);
                }
                
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    value = null;
                    lexer.nextToken();
                } else {
                    value = parser.parseObject(valueType, key);
                }

                map.put(key, value);
                parser.checkMapResolve(map, key);

                parser.setContext(context, value, key);
                parser.setContext(context);

                final int tok = lexer.token();
                if (tok == com.kangaroo.internal.fastjson.parser.JSONToken.EOF || tok == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET) {
                    return map;
                }

                if (tok == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                    lexer.nextToken();
                    return map;
                }
            }
        } finally {
            parser.setContext(context);
        }

    }
    
    public static Object parseMap(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Map<Object, Object> map, Type keyType, Type valueType,
                                  Object fieldName) {
        com.kangaroo.internal.fastjson.parser.JSONLexer lexer = parser.lexer;

        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE && lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
            throw new JSONException("syntax error, expect {, actual " + lexer.tokenName());
        }

        ObjectDeserializer keyDeserializer = parser.getConfig().getDeserializer(keyType);
        ObjectDeserializer valueDeserializer = parser.getConfig().getDeserializer(valueType);
        lexer.nextToken(keyDeserializer.getFastMatchToken());

        com.kangaroo.internal.fastjson.parser.ParseContext context = parser.getContext();
        try {
            for (;;) {
                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                    break;
                }

                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING //
                    && lexer.isRef() //
                    && !lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableSpecialKeyDetect) //
                ) {
                    Object object = null;

                    lexer.nextTokenWithColon(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                        String ref = lexer.stringVal();
                        if ("..".equals(ref)) {
                            com.kangaroo.internal.fastjson.parser.ParseContext parentContext = context.parent;
                            object = parentContext.object;
                        } else if ("$".equals(ref)) {
                            com.kangaroo.internal.fastjson.parser.ParseContext rootContext = context;
                            while (rootContext.parent != null) {
                                rootContext = rootContext.parent;
                            }

                            object = rootContext.object;
                        } else {
                            parser.addResolveTask(new com.kangaroo.internal.fastjson.parser.DefaultJSONParser.ResolveTask(context, ref));
                            parser.setResolveStatus(com.kangaroo.internal.fastjson.parser.DefaultJSONParser.NeedToResolve);
                        }
                    } else {
                        throw new JSONException("illegal ref, " + com.kangaroo.internal.fastjson.parser.JSONToken.name(lexer.token()));
                    }

                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE);
                    if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                        throw new JSONException("illegal ref");
                    }
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

                    // parser.setContext(context, map, fieldName);
                    // parser.setContext(context);

                    return object;
                }

                if (map.size() == 0 //
                    && lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING //
                    && com.kangaroo.internal.fastjson.JSON.DEFAULT_TYPE_KEY.equals(lexer.stringVal()) //
                    && !lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableSpecialKeyDetect)) {
                    lexer.nextTokenWithColon(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                        lexer.nextToken();
                        return map;
                    }
                    lexer.nextToken(keyDeserializer.getFastMatchToken());
                }

                Object key = keyDeserializer.deserialze(parser, keyType, null);

                if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.COLON) {
                    throw new JSONException("syntax error, expect :, actual " + lexer.token());
                }

                lexer.nextToken(valueDeserializer.getFastMatchToken());

                Object value = valueDeserializer.deserialze(parser, valueType, key);
                parser.checkMapResolve(map, key);

                map.put(key, value);

                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                    lexer.nextToken(keyDeserializer.getFastMatchToken());
                }
            }
        } finally {
            parser.setContext(context);
        }

        return map;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Map<Object, Object> createMap(Type type) {
        if (type == Properties.class) {
            return new Properties();
        }

        if (type == Hashtable.class) {
            return new Hashtable();
        }

        if (type == IdentityHashMap.class) {
            return new IdentityHashMap();
        }

        if (type == SortedMap.class || type == TreeMap.class) {
            return new TreeMap();
        }

        if (type == ConcurrentMap.class || type == ConcurrentHashMap.class) {
            return new ConcurrentHashMap();
        }
        
        if (type == Map.class || type == HashMap.class) {
            return new HashMap();
        }
        
        if (type == LinkedHashMap.class) {
            return new LinkedHashMap();
        }

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;

            Type rawType = parameterizedType.getRawType();
            if (EnumMap.class.equals(rawType)) {
                Type[] actualArgs = parameterizedType.getActualTypeArguments();
                return new EnumMap((Class) actualArgs[0]);
            }

            return createMap(rawType);
        }

        Class<?> clazz = (Class<?>) type;
        if (clazz.isInterface()) {
            throw new JSONException("unsupport type " + type);
        }
        
        try {
            return (Map<Object, Object>) clazz.newInstance();
        } catch (Exception e) {
            throw new JSONException("unsupport type " + type, e);
        }
    }
    

    public int getFastMatchToken() {
        return com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE;
    }
}
