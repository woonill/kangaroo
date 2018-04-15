/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kangaroo.internal.fastjson.parser;

import static com.kangaroo.internal.fastjson.parser.JSONLexer.EOI;
import static com.kangaroo.internal.fastjson.parser.JSONToken.*;

import java.io.Closeable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.kangaroo.internal.fastjson.JSONPath;
import com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer;
import com.kangaroo.internal.fastjson.serializer.LongCodec;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
public class DefaultJSONParser implements Closeable {

    public final Object                input;
    public final com.kangaroo.internal.fastjson.parser.SymbolTable symbolTable;
    protected com.kangaroo.internal.fastjson.parser.ParserConfig config;

    private final static Set<Class<?>> primitiveClasses   = new HashSet<Class<?>>();

    private String                     dateFormatPattern  = com.kangaroo.internal.fastjson.JSON.DEFFAULT_DATE_FORMAT;
    private DateFormat                 dateFormat;

    public final com.kangaroo.internal.fastjson.parser.JSONLexer lexer;

    protected com.kangaroo.internal.fastjson.parser.ParseContext context;

    private com.kangaroo.internal.fastjson.parser.ParseContext[]             contextArray;
    private int                        contextArrayIndex  = 0;

    private List<ResolveTask>          resolveTaskList;

    public final static int            NONE               = 0;
    public final static int            NeedToResolve      = 1;
    public final static int            TypeNameRedirect   = 2;

    public int                         resolveStatus      = NONE;

    private List<com.kangaroo.internal.fastjson.parser.deserializer.ExtraTypeProvider>    extraTypeProviders = null;
    private List<com.kangaroo.internal.fastjson.parser.deserializer.ExtraProcessor>       extraProcessors    = null;
    protected com.kangaroo.internal.fastjson.parser.deserializer.FieldTypeResolver fieldTypeResolver  = null;

    private boolean                    autoTypeEnable;
    private String[]                   autoTypeAccept     = null;

    protected transient com.kangaroo.internal.fastjson.serializer.BeanContext lastBeanContext;

    static {
        Class<?>[] classes = new Class[] {
                boolean.class,
                byte.class,
                short.class,
                int.class,
                long.class,
                float.class,
                double.class,

                Boolean.class,
                Byte.class,
                Short.class,
                Integer.class,
                Long.class,
                Float.class,
                Double.class,

                BigInteger.class,
                BigDecimal.class,
                String.class
        };

        for (Class<?> clazz : classes) {
            primitiveClasses.add(clazz);
        }
    }

    public String getDateFomartPattern() {
        return dateFormatPattern;
    }

    public DateFormat getDateFormat() {
        if (dateFormat == null) {
            dateFormat = new SimpleDateFormat(dateFormatPattern, lexer.getLocale());
            dateFormat.setTimeZone(lexer.getTimeZone());
        }
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormatPattern = dateFormat;
        this.dateFormat = null;
    }

    public void setDateFomrat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public DefaultJSONParser(String input){
        this(input, com.kangaroo.internal.fastjson.parser.ParserConfig.getGlobalInstance(), com.kangaroo.internal.fastjson.JSON.DEFAULT_PARSER_FEATURE);
    }

    public DefaultJSONParser(final String input, final com.kangaroo.internal.fastjson.parser.ParserConfig config){
        this(input, new com.kangaroo.internal.fastjson.parser.JSONScanner(input, com.kangaroo.internal.fastjson.JSON.DEFAULT_PARSER_FEATURE), config);
    }

    public DefaultJSONParser(final String input, final com.kangaroo.internal.fastjson.parser.ParserConfig config, int features){
        this(input, new com.kangaroo.internal.fastjson.parser.JSONScanner(input, features), config);
    }

    public DefaultJSONParser(final char[] input, int length, final com.kangaroo.internal.fastjson.parser.ParserConfig config, int features){
        this(input, new com.kangaroo.internal.fastjson.parser.JSONScanner(input, length, features), config);
    }

    public DefaultJSONParser(final com.kangaroo.internal.fastjson.parser.JSONLexer lexer){
        this(lexer, com.kangaroo.internal.fastjson.parser.ParserConfig.getGlobalInstance());
    }

    public DefaultJSONParser(final com.kangaroo.internal.fastjson.parser.JSONLexer lexer, final com.kangaroo.internal.fastjson.parser.ParserConfig config){
        this(null, lexer, config);
    }

    public DefaultJSONParser(final Object input, final com.kangaroo.internal.fastjson.parser.JSONLexer lexer, final com.kangaroo.internal.fastjson.parser.ParserConfig config){
        this.lexer = lexer;
        this.input = input;
        this.config = config;
        this.symbolTable = config.symbolTable;

        int ch = lexer.getCurrent();
        if (ch == '{') {
            lexer.next();
            ((JSONLexerBase) lexer).token = com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE;
        } else if (ch == '[') {
            lexer.next();
            ((JSONLexerBase) lexer).token = com.kangaroo.internal.fastjson.parser.JSONToken.LBRACKET;
        } else {
            lexer.nextToken(); // prime the pump
        }
    }

    public com.kangaroo.internal.fastjson.parser.SymbolTable getSymbolTable() {
        return symbolTable;
    }

    public String getInput() {
        if (input instanceof char[]) {
            return new String((char[]) input);
        }
        return input.toString();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final Object parseObject(final Map object, Object fieldName) {
        final com.kangaroo.internal.fastjson.parser.JSONLexer lexer = this.lexer;
        
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
            lexer.nextToken();
            return null;
        }
        
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
            lexer.nextToken();
            return object;
        }

        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING && lexer.stringVal().length() == 0) {
            lexer.nextToken();
            return object;
        }

        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE && lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
            throw new com.kangaroo.internal.fastjson.JSONException("syntax error, expect {, actual " + lexer.tokenName() + ", " + lexer.info());
        }

       com.kangaroo.internal.fastjson.parser.ParseContext context = this.context;
        try {
            Map map = object instanceof com.kangaroo.internal.fastjson.JSONObject ? ((com.kangaroo.internal.fastjson.JSONObject) object).getInnerMap() : object;

            boolean setContextFlag = false;
            for (;;) {
                lexer.skipWhitespace();
                char ch = lexer.getCurrent();
                if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowArbitraryCommas)) {
                    while (ch == ',') {
                        lexer.next();
                        lexer.skipWhitespace();
                        ch = lexer.getCurrent();
                    }
                }

                boolean isObjectKey = false;
                Object key;
                if (ch == '"') {
                    key = lexer.scanSymbol(symbolTable, '"');
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new com.kangaroo.internal.fastjson.JSONException("expect ':' at " + lexer.pos() + ", name " + key);
                    }
                } else if (ch == '}') {
                    lexer.next();
                    lexer.resetStringPosition();
                    lexer.nextToken();

                    if (!setContextFlag) {
                        if (this.context != null && fieldName == this.context.fieldName && object == this.context.object) {
                            context = this.context;
                        } else {
                            com.kangaroo.internal.fastjson.parser.ParseContext contextR = setContext(object, fieldName);
                            if (context == null) {
                                context = contextR;
                            }
                            setContextFlag = true;
                        }
                    }

                    return object;
                } else if (ch == '\'') {
                    if (!lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowSingleQuotes)) {
                        throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
                    }

                    key = lexer.scanSymbol(symbolTable, '\'');
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new com.kangaroo.internal.fastjson.JSONException("expect ':' at " + lexer.pos());
                    }
                } else if (ch == EOI) {
                    throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
                } else if (ch == ',') {
                    throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
                } else if ((ch >= '0' && ch <= '9') || ch == '-') {
                    lexer.resetStringPosition();
                    lexer.scanNumber();
                    try {
                        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT) {
                            key = lexer.integerValue();
                        } else {
                            key = lexer.decimalValue(true);
                        }
                        if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.NonStringKeyAsString)) {
                            key = key.toString();
                        }
                    } catch (NumberFormatException e) {
                        throw new com.kangaroo.internal.fastjson.JSONException("parse number key error" + lexer.info());
                    }
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new com.kangaroo.internal.fastjson.JSONException("parse number key error" + lexer.info());
                    }
                } else if (ch == '{' || ch == '[') {
                    lexer.nextToken();
                    key = parse();
                    isObjectKey = true;
                } else {
                    if (!lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowUnQuotedFieldNames)) {
                        throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
                    }

                    key = lexer.scanSymbolUnQuoted(symbolTable);
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new com.kangaroo.internal.fastjson.JSONException("expect ':' at " + lexer.pos() + ", actual " + ch);
                    }
                }

                if (!isObjectKey) {
                    lexer.next();
                    lexer.skipWhitespace();
                }

                ch = lexer.getCurrent();

                lexer.resetStringPosition();

                if (key == com.kangaroo.internal.fastjson.JSON.DEFAULT_TYPE_KEY
                        && !lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableSpecialKeyDetect)) {
                    String typeName = lexer.scanSymbol(symbolTable, '"');

                    if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.IgnoreAutoType)) {
                        continue;
                    }

                    Class<?> clazz = null;
                    if (object != null
                            && object.getClass().getName().equals(typeName)) {
                        clazz = object.getClass();
                    } else {
                        clazz = config.checkAutoType(typeName, null, lexer.getFeatures());
                    }

                    if (clazz == null) {
                        map.put(com.kangaroo.internal.fastjson.JSON.DEFAULT_TYPE_KEY, typeName);
                        continue;
                    }

                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                        try {
                            Object instance = null;
                            com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer deserializer = this.config.getDeserializer(clazz);
                            if (deserializer instanceof com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer) {
                                com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer javaBeanDeserializer = (com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer) deserializer;
                                instance = javaBeanDeserializer.createInstance(this, clazz);
                                
                                for (Object o : map.entrySet()) {
                                    Map.Entry entry = (Map.Entry) o;
                                    Object entryKey = entry.getKey();
                                    if (entryKey instanceof String) {
                                        com.kangaroo.internal.fastjson.parser.deserializer.FieldDeserializer fieldDeserializer = javaBeanDeserializer.getFieldDeserializer((String) entryKey);
                                        if (fieldDeserializer != null) {
                                            fieldDeserializer.setValue(instance, entry.getValue());
                                        }
                                    }
                                }
                            }

                            if (instance == null) {
                                if (clazz == Cloneable.class) {
                                    instance = new HashMap();
                                } else if ("java.util.Collections$EmptyMap".equals(typeName)) {
                                    instance = Collections.emptyMap();
                                } else {
                                    instance = clazz.newInstance();
                                }
                            }

                            return instance;
                        } catch (Exception e) {
                            throw new com.kangaroo.internal.fastjson.JSONException("create instance error", e);
                        }
                    }
                    
                    this.setResolveStatus(TypeNameRedirect);

                    if (this.context != null
                            && fieldName != null
                            && !(fieldName instanceof Integer)
                            && !(this.context.fieldName instanceof Integer)) {
                        this.popContext();
                    }
                    
                    if (object.size() > 0) {
                        Object newObj = com.kangaroo.internal.fastjson.util.TypeUtils.cast(object, clazz, this.config);
                        this.parseObject(newObj);
                        return newObj;
                    }

                    com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer deserializer = config.getDeserializer(clazz);
                    Class deserClass = deserializer.getClass();
                    if (com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer.class.isAssignableFrom(deserClass)
                            && deserClass != com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer.class
                            && deserClass != com.kangaroo.internal.fastjson.parser.deserializer.ThrowableDeserializer.class) {
                        this.setResolveStatus(NONE);
                    }
                    Object obj = deserializer.deserialze(this, clazz, fieldName);
                    return obj;
                }

                if (key == "$ref"
                        && context != null
                        && !lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableSpecialKeyDetect)) {
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                        String ref = lexer.stringVal();
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE);

                        Object refValue = null;
                        if ("@".equals(ref)) {
                            if (this.context != null) {
                                com.kangaroo.internal.fastjson.parser.ParseContext thisContext = this.context;
                                Object thisObj = thisContext.object;
                                if (thisObj instanceof Object[] || thisObj instanceof Collection<?>) {
                                    refValue = thisObj;
                                } else if (thisContext.parent != null) {
                                    refValue = thisContext.parent.object;
                                }
                            }
                        } else if ("..".equals(ref)) {
                            if (context.object != null) {
                                refValue = context.object;
                            } else {
                                addResolveTask(new ResolveTask(context, ref));
                                setResolveStatus(DefaultJSONParser.NeedToResolve);
                            }
                        } else if ("$".equals(ref)) {
                            com.kangaroo.internal.fastjson.parser.ParseContext rootContext = context;
                            while (rootContext.parent != null) {
                                rootContext = rootContext.parent;
                            }

                            if (rootContext.object != null) {
                                refValue = rootContext.object;
                            } else {
                                addResolveTask(new ResolveTask(rootContext, ref));
                                setResolveStatus(DefaultJSONParser.NeedToResolve);
                            }
                        } else {
                            addResolveTask(new ResolveTask(context, ref));
                            setResolveStatus(DefaultJSONParser.NeedToResolve);
                        }

                        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                            throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
                        }
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

                        return refValue;
                    } else {
                        throw new com.kangaroo.internal.fastjson.JSONException("illegal ref, " + com.kangaroo.internal.fastjson.parser.JSONToken.name(lexer.token()));
                    }
                }

                if (!setContextFlag) {
                    if (this.context != null && fieldName == this.context.fieldName && object == this.context.object) {
                        context = this.context;
                    } else {
                        com.kangaroo.internal.fastjson.parser.ParseContext contextR = setContext(object, fieldName);
                        if (context == null) {
                            context = contextR;
                        }
                        setContextFlag = true;
                    }
                }

                if (object.getClass() == com.kangaroo.internal.fastjson.JSONObject.class) {
                    if (key == null) {
                        key = "null";
                    }
                }

                Object value;
                if (ch == '"') {
                    lexer.scanString();
                    String strValue = lexer.stringVal();
                    value = strValue;

                    if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowISO8601DateFormat)) {
                        com.kangaroo.internal.fastjson.parser.JSONScanner iso8601Lexer = new com.kangaroo.internal.fastjson.parser.JSONScanner(strValue);
                        if (iso8601Lexer.scanISO8601DateIfMatch()) {
                            value = iso8601Lexer.getCalendar().getTime();
                        }
                        iso8601Lexer.close();
                    }

                    map.put(key, value);
                } else if (ch >= '0' && ch <= '9' || ch == '-') {
                    lexer.scanNumber();
                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT) {
                        value = lexer.integerValue();
                    } else {
                        value = lexer.decimalValue(lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.UseBigDecimal));
                    }

                    map.put(key, value);
                } else if (ch == '[') { // 减少嵌套，兼容android
                    lexer.nextToken();

                    com.kangaroo.internal.fastjson.JSONArray list = new com.kangaroo.internal.fastjson.JSONArray();

                    final boolean parentIsArray = fieldName != null && fieldName.getClass() == Integer.class;
//                    if (!parentIsArray) {
//                        this.setContext(context);
//                    }
                    if (fieldName == null) {
                        this.setContext(context);
                    }

                    this.parseArray(list, key);
                    
                    if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.UseObjectArray)) {
                        value = list.toArray();
                    } else {
                        value = list;
                    }
                    map.put(key, value);

                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                        lexer.nextToken();
                        return object;
                    } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                        continue;
                    } else {
                        throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
                    }
                } else if (ch == '{') { // 减少嵌套，兼容android
                    lexer.nextToken();

                    final boolean parentIsArray = fieldName != null && fieldName.getClass() == Integer.class;

                    Map input;
                    if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.CustomMapDeserializer)) {
                        com.kangaroo.internal.fastjson.parser.deserializer.MapDeserializer mapDeserializer = (com.kangaroo.internal.fastjson.parser.deserializer.MapDeserializer) config.getDeserializer(Map.class);
                        input = mapDeserializer.createMap(Map.class);
                    } else {
                        input = new com.kangaroo.internal.fastjson.JSONObject(lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.OrderedField));
                    }
                    com.kangaroo.internal.fastjson.parser.ParseContext ctxLocal = null;

                    if (!parentIsArray) {
                        ctxLocal = setContext(context, input, key);
                    }

                    Object obj = null;
                    boolean objParsed = false;
                    if (fieldTypeResolver != null) {
                        String resolveFieldName = key != null ? key.toString() : null;
                        Type fieldType = fieldTypeResolver.resolve(object, resolveFieldName);
                        if (fieldType != null) {
                            com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer fieldDeser = config.getDeserializer(fieldType);
                            obj = fieldDeser.deserialze(this, fieldType, key);
                            objParsed = true;
                        }
                    }
                    if (!objParsed) {
                        obj = this.parseObject(input, key);
                    }
                    
                    if (ctxLocal != null && input != obj) {
                        ctxLocal.object = object;
                    }

                    if (key != null) {
                        checkMapResolve(object, key.toString());
                    }
                    
                    map.put(key, obj);

                    if (parentIsArray) {
                        //setContext(context, obj, key);
                        setContext(obj, key);
                    }

                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                        lexer.nextToken();

                        setContext(context);
                        return object;
                    } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                        if (parentIsArray) {
                            this.popContext();
                        } else {
                            this.setContext(context);
                        }
                        continue;
                    } else {
                        throw new com.kangaroo.internal.fastjson.JSONException("syntax error, " + lexer.tokenName());
                    }
                } else {
                    lexer.nextToken();
                    value = parse();

                    map.put(key, value);

                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                        lexer.nextToken();
                        return object;
                    } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                        continue;
                    } else {
                        throw new com.kangaroo.internal.fastjson.JSONException("syntax error, position at " + lexer.pos() + ", name " + key);
                    }
                }

                lexer.skipWhitespace();
                ch = lexer.getCurrent();
                if (ch == ',') {
                    lexer.next();
                    continue;
                } else if (ch == '}') {
                    lexer.next();
                    lexer.resetStringPosition();
                    lexer.nextToken();

                    // this.setContext(object, fieldName);
                    this.setContext(value, key);

                    return object;
                } else {
                    throw new com.kangaroo.internal.fastjson.JSONException("syntax error, position at " + lexer.pos() + ", name " + key);
                }

            }
        } finally {
            this.setContext(context);
        }

    }

    public com.kangaroo.internal.fastjson.parser.ParserConfig getConfig() {
        return config;
    }

    public void setConfig(com.kangaroo.internal.fastjson.parser.ParserConfig config) {
        this.config = config;
    }

    // compatible
    @SuppressWarnings("unchecked")
    public <T> T parseObject(Class<T> clazz) {
        return (T) parseObject(clazz, null);
    }
    
    public <T> T parseObject(Type type) {
        return parseObject(type, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T parseObject(Type type, Object fieldName) {
        int token = lexer.token();
        if (token == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
            lexer.nextToken();
            return null;
        }

        if (token == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
            if (type == byte[].class) {
                byte[] bytes = lexer.bytesValue();
                lexer.nextToken();
                return (T) bytes;
            }

            if (type == char[].class) {
                String strVal = lexer.stringVal();
                lexer.nextToken();
                return (T) strVal.toCharArray();
            }
        }

        com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer derializer = config.getDeserializer(type);

        try {
            return (T) derializer.deserialze(this, type, fieldName);
        } catch (com.kangaroo.internal.fastjson.JSONException e) {
            throw e;
        } catch (Throwable e) {
            throw new com.kangaroo.internal.fastjson.JSONException(e.getMessage(), e);
        }
    }

    public <T> List<T> parseArray(Class<T> clazz) {
        List<T> array = new ArrayList<T>();
        parseArray(clazz, array);
        return array;
    }

    public void parseArray(Class<?> clazz, @SuppressWarnings("rawtypes") Collection array) {
        parseArray((Type) clazz, array);
    }

    @SuppressWarnings("rawtypes")
    public void parseArray(Type type, Collection array) {
        parseArray(type, array, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void parseArray(Type type, Collection array, Object fieldName) {
        int token = lexer.token();
        if (token == com.kangaroo.internal.fastjson.parser.JSONToken.SET || token == com.kangaroo.internal.fastjson.parser.JSONToken.TREE_SET) {
            lexer.nextToken();
            token = lexer.token();
        }

        if (token != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACKET) {
            throw new com.kangaroo.internal.fastjson.JSONException("exepct '[', but " + com.kangaroo.internal.fastjson.parser.JSONToken.name(token) + ", " + lexer.info());
        }

        com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer deserializer = null;
        if (int.class == type) {
            deserializer = com.kangaroo.internal.fastjson.serializer.IntegerCodec.instance;
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT);
        } else if (String.class == type) {
            deserializer = com.kangaroo.internal.fastjson.serializer.StringCodec.instance;
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
        } else {
            deserializer = config.getDeserializer(type);
            lexer.nextToken(deserializer.getFastMatchToken());
        }

        com.kangaroo.internal.fastjson.parser.ParseContext context = this.context;
        this.setContext(array, fieldName);
        try {
            for (int i = 0;; ++i) {
                if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowArbitraryCommas)) {
                    while (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                        lexer.nextToken();
                        continue;
                    }
                }

                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET) {
                    break;
                }

                if (int.class == type) {
                    Object val = com.kangaroo.internal.fastjson.serializer.IntegerCodec.instance.deserialze(this, null, null);
                    array.add(val);
                } else if (String.class == type) {
                    String value;
                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                        value = lexer.stringVal();
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                    } else {
                        Object obj = this.parse();
                        if (obj == null) {
                            value = null;
                        } else {
                            value = obj.toString();
                        }
                    }

                    array.add(value);
                } else {
                    Object val;
                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                        lexer.nextToken();
                        val = null;
                    } else {
                        val = deserializer.deserialze(this, type, i);
                    }
                    array.add(val);
                    checkListResolve(array);
                }

                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                    lexer.nextToken(deserializer.getFastMatchToken());
                    continue;
                }
            }
        } finally {
            this.setContext(context);
        }

        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
    }

    public Object[] parseArray(Type[] types) {
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
            return null;
        }

        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACKET) {
            throw new com.kangaroo.internal.fastjson.JSONException("syntax error : " + lexer.tokenName());
        }

        Object[] list = new Object[types.length];
        if (types.length == 0) {
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET);

            if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET) {
                throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
            }

            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
            return new Object[0];
        }

        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT);

        for (int i = 0; i < types.length; ++i) {
            Object value;

            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                value = null;
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
            } else {
                Type type = types[i];
                if (type == int.class || type == Integer.class) {
                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT) {
                        value = Integer.valueOf(lexer.intValue());
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                    } else {
                        value = this.parse();
                        value = com.kangaroo.internal.fastjson.util.TypeUtils.cast(value, type, config);
                    }
                } else if (type == String.class) {
                    if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
                        value = lexer.stringVal();
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                    } else {
                        value = this.parse();
                        value = com.kangaroo.internal.fastjson.util.TypeUtils.cast(value, type, config);
                    }
                } else {
                    boolean isArray = false;
                    Class<?> componentType = null;
                    if (i == types.length - 1) {
                        if (type instanceof Class) {
                            Class<?> clazz = (Class<?>) type;
                            isArray = clazz.isArray();
                            componentType = clazz.getComponentType();
                        }
                    }

                    // support varArgs
                    if (isArray && lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACKET) {
                        List<Object> varList = new ArrayList<Object>();

                        com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer derializer = config.getDeserializer(componentType);
                        int fastMatch = derializer.getFastMatchToken();

                        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET) {
                            for (;;) {
                                Object item = derializer.deserialze(this, type, null);
                                varList.add(item);

                                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                                    lexer.nextToken(fastMatch);
                                } else if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET) {
                                    break;
                                } else {
                                    throw new com.kangaroo.internal.fastjson.JSONException("syntax error :" + com.kangaroo.internal.fastjson.parser.JSONToken.name(lexer.token()));
                                }
                            }
                        }

                        value = com.kangaroo.internal.fastjson.util.TypeUtils.cast(varList, type, config);
                    } else {
                        com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer derializer = config.getDeserializer(type);
                        value = derializer.deserialze(this, type, i);
                    }
                }
            }
            list[i] = value;

            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET) {
                break;
            }

            if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                throw new com.kangaroo.internal.fastjson.JSONException("syntax error :" + com.kangaroo.internal.fastjson.parser.JSONToken.name(lexer.token()));
            }

            if (i == types.length - 1) {
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET);
            } else {
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT);
            }
        }

        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET) {
            throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
        }

        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

        return list;
    }

    public void parseObject(Object object) {
        Class<?> clazz = object.getClass();
        com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer beanDeser = null;
        com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer deserizer = config.getDeserializer(clazz);
        if (deserizer instanceof com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer) {
            beanDeser = (com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer) deserizer;
        }

        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE && lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
            throw new com.kangaroo.internal.fastjson.JSONException("syntax error, expect {, actual " + lexer.tokenName());
        }

        for (;;) {
            // lexer.scanSymbol
            String key = lexer.scanSymbol(symbolTable);

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

            com.kangaroo.internal.fastjson.parser.deserializer.FieldDeserializer fieldDeser = null;
            if (beanDeser != null) {
                fieldDeser = beanDeser.getFieldDeserializer(key);
            }
            
            if (fieldDeser == null) {
                if (!lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.IgnoreNotMatch)) {
                    throw new com.kangaroo.internal.fastjson.JSONException("setter not found, class " + clazz.getName() + ", property " + key);
                }

                lexer.nextTokenWithColon();
                parse(); // skip

                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                    lexer.nextToken();
                    return;
                }

                continue;
            } else {
                Class<?> fieldClass = fieldDeser.fieldInfo.fieldClass;
                Type fieldType = fieldDeser.fieldInfo.fieldType;
                Object fieldValue;
                if (fieldClass == int.class) {
                    lexer.nextTokenWithColon(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT);
                    fieldValue = com.kangaroo.internal.fastjson.serializer.IntegerCodec.instance.deserialze(this, fieldType, null);
                } else if (fieldClass == String.class) {
                    lexer.nextTokenWithColon(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
                    fieldValue = com.kangaroo.internal.fastjson.serializer.StringCodec.deserialze(this);
                } else if (fieldClass == long.class) {
                    lexer.nextTokenWithColon(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT);
                    fieldValue = LongCodec.instance.deserialze(this, fieldType, null);
                } else {
                    com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer fieldValueDeserializer = config.getDeserializer(fieldClass, fieldType);

                    lexer.nextTokenWithColon(fieldValueDeserializer.getFastMatchToken());
                    fieldValue = fieldValueDeserializer.deserialze(this, fieldType, null);
                }

                fieldDeser.setValue(object, fieldValue);
            }

            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                continue;
            }

            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                return;
            }
        }
    }

    public Object parseArrayWithType(Type collectionType) {
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
            lexer.nextToken();
            return null;
        }

        Type[] actualTypes = ((ParameterizedType) collectionType).getActualTypeArguments();

        if (actualTypes.length != 1) {
            throw new com.kangaroo.internal.fastjson.JSONException("not support type " + collectionType);
        }

        Type actualTypeArgument = actualTypes[0];

        if (actualTypeArgument instanceof Class) {
            List<Object> array = new ArrayList<Object>();
            this.parseArray((Class<?>) actualTypeArgument, array);
            return array;
        }

        if (actualTypeArgument instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) actualTypeArgument;

            // assert wildcardType.getUpperBounds().length == 1;
            Type upperBoundType = wildcardType.getUpperBounds()[0];

            // assert upperBoundType instanceof Class;
            if (Object.class.equals(upperBoundType)) {
                if (wildcardType.getLowerBounds().length == 0) {
                    // Collection<?>
                    return parse();
                } else {
                    throw new com.kangaroo.internal.fastjson.JSONException("not support type : " + collectionType);
                }
            }

            List<Object> array = new ArrayList<Object>();
            this.parseArray((Class<?>) upperBoundType, array);
            return array;

            // throw new JSONException("not support type : " +
            // collectionType);return parse();
        }

        if (actualTypeArgument instanceof TypeVariable) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) actualTypeArgument;
            Type[] bounds = typeVariable.getBounds();

            if (bounds.length != 1) {
                throw new com.kangaroo.internal.fastjson.JSONException("not support : " + typeVariable);
            }

            Type boundType = bounds[0];
            if (boundType instanceof Class) {
                List<Object> array = new ArrayList<Object>();
                this.parseArray((Class<?>) boundType, array);
                return array;
            }
        }

        if (actualTypeArgument instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) actualTypeArgument;

            List<Object> array = new ArrayList<Object>();
            this.parseArray(parameterizedType, array);
            return array;
        }

        throw new com.kangaroo.internal.fastjson.JSONException("TODO : " + collectionType);
    }

    public void acceptType(String typeName) {
        com.kangaroo.internal.fastjson.parser.JSONLexer lexer = this.lexer;

        lexer.nextTokenWithColon();

        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING) {
            throw new com.kangaroo.internal.fastjson.JSONException("type not match error");
        }

        if (typeName.equals(lexer.stringVal())) {
            lexer.nextToken();
            if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                lexer.nextToken();
            }
        } else {
            throw new com.kangaroo.internal.fastjson.JSONException("type not match error");
        }
    }

    public int getResolveStatus() {
        return resolveStatus;
    }

    public void setResolveStatus(int resolveStatus) {
        this.resolveStatus = resolveStatus;
    }

    public Object getObject(String path) {
        for (int i = 0; i < contextArrayIndex; ++i) {
            if (path.equals(contextArray[i].toString())) {
                return contextArray[i].object;
            }
        }

        return null;
    }

    @SuppressWarnings("rawtypes")
    public void checkListResolve(Collection array) {
        if (resolveStatus == NeedToResolve) {
            if (array instanceof List) {
                final int index = array.size() - 1;
                final List list = (List) array;
                ResolveTask task = getLastResolveTask();
                task.fieldDeserializer = new com.kangaroo.internal.fastjson.parser.deserializer.ResolveFieldDeserializer(this, list, index);
                task.ownerContext = context;
                setResolveStatus(DefaultJSONParser.NONE);
            } else {
                ResolveTask task = getLastResolveTask();
                task.fieldDeserializer  = new com.kangaroo.internal.fastjson.parser.deserializer.ResolveFieldDeserializer(array);
                task.ownerContext = context;
                setResolveStatus(DefaultJSONParser.NONE);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    public void checkMapResolve(Map object, Object fieldName) {
        if (resolveStatus == NeedToResolve) {
            com.kangaroo.internal.fastjson.parser.deserializer.ResolveFieldDeserializer fieldResolver = new com.kangaroo.internal.fastjson.parser.deserializer.ResolveFieldDeserializer(object, fieldName);
            ResolveTask task = getLastResolveTask();
            task.fieldDeserializer = fieldResolver;
            task.ownerContext = context;
            setResolveStatus(DefaultJSONParser.NONE);
        }
    }

    @SuppressWarnings("rawtypes")
    public Object parseObject(final Map object) {
        return parseObject(object, null);
    }

    public com.kangaroo.internal.fastjson.JSONObject parseObject() {
        com.kangaroo.internal.fastjson.JSONObject object = new com.kangaroo.internal.fastjson.JSONObject(lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.OrderedField));
        object = (com.kangaroo.internal.fastjson.JSONObject) parseObject(object);
        return object;
    }

    @SuppressWarnings("rawtypes")
    public final void parseArray(final Collection array) {
        parseArray(array, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public final void parseArray(final Collection array, Object fieldName) {
        final com.kangaroo.internal.fastjson.parser.JSONLexer lexer = this.lexer;

        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.SET || lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.TREE_SET) {
            lexer.nextToken();
        }

        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACKET) {
            throw new com.kangaroo.internal.fastjson.JSONException("syntax error, expect [, actual " + com.kangaroo.internal.fastjson.parser.JSONToken.name(lexer.token()) + ", pos "
                                    + lexer.pos() + ", fieldName " + fieldName);
        }

        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);

        com.kangaroo.internal.fastjson.parser.ParseContext context = this.context;
        this.setContext(array, fieldName);
        try {
            for (int i = 0;; ++i) {
                if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowArbitraryCommas)) {
                    while (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                        lexer.nextToken();
                        continue;
                    }
                }

                Object value;
                switch (lexer.token()) {
                    case LITERAL_INT:
                        value = lexer.integerValue();
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                        break;
                    case LITERAL_FLOAT:
                        if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.UseBigDecimal)) {
                            value = lexer.decimalValue(true);
                        } else {
                            value = lexer.decimalValue(false);
                        }
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                        break;
                    case LITERAL_STRING:
                        String stringLiteral = lexer.stringVal();
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

                        if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowISO8601DateFormat)) {
                            com.kangaroo.internal.fastjson.parser.JSONScanner iso8601Lexer = new com.kangaroo.internal.fastjson.parser.JSONScanner(stringLiteral);
                            if (iso8601Lexer.scanISO8601DateIfMatch()) {
                                value = iso8601Lexer.getCalendar().getTime();
                            } else {
                                value = stringLiteral;
                            }
                            iso8601Lexer.close();
                        } else {
                            value = stringLiteral;
                        }

                        break;
                    case TRUE:
                        value = Boolean.TRUE;
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                        break;
                    case FALSE:
                        value = Boolean.FALSE;
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                        break;
                    case LBRACE:
                        com.kangaroo.internal.fastjson.JSONObject object = new com.kangaroo.internal.fastjson.JSONObject(lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.OrderedField));
                        value = parseObject(object, i);
                        break;
                    case LBRACKET:
                        Collection items = new com.kangaroo.internal.fastjson.JSONArray();
                        parseArray(items, i);
                        if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.UseObjectArray)) {
                            value = items.toArray();
                        } else {
                            value = items;
                        }
                        break;
                    case NULL:
                        value = null;
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
                        break;
                    case UNDEFINED:
                        value = null;
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
                        break;
                    case RBRACKET:
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                        return;
                    case EOF:
                        throw new com.kangaroo.internal.fastjson.JSONException("unclosed jsonArray");
                    default:
                        value = parse();
                        break;
                }

                array.add(value);
                checkListResolve(array);

                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.COMMA) {
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_STRING);
                    continue;
                }
            }
        } finally {
            this.setContext(context);
        }
    }

    public com.kangaroo.internal.fastjson.parser.ParseContext getContext() {
        return context;
    }

    public List<ResolveTask> getResolveTaskList() {
        if (resolveTaskList == null) {
            resolveTaskList = new ArrayList<ResolveTask>(2);
        }
        return resolveTaskList;
    }

    public void addResolveTask(ResolveTask task) {
        if (resolveTaskList == null) {
            resolveTaskList = new ArrayList<ResolveTask>(2);
        }
        resolveTaskList.add(task);
    }

    public ResolveTask getLastResolveTask() {
        return resolveTaskList.get(resolveTaskList.size() - 1);
    }

    public List<com.kangaroo.internal.fastjson.parser.deserializer.ExtraProcessor> getExtraProcessors() {
        if (extraProcessors == null) {
            extraProcessors = new ArrayList<com.kangaroo.internal.fastjson.parser.deserializer.ExtraProcessor>(2);
        }
        return extraProcessors;
    }

    public List<com.kangaroo.internal.fastjson.parser.deserializer.ExtraTypeProvider> getExtraTypeProviders() {
        if (extraTypeProviders == null) {
            extraTypeProviders = new ArrayList<com.kangaroo.internal.fastjson.parser.deserializer.ExtraTypeProvider>(2);
        }
        return extraTypeProviders;
    }

    public com.kangaroo.internal.fastjson.parser.deserializer.FieldTypeResolver getFieldTypeResolver() {
        return fieldTypeResolver;
    }
    
    public void setFieldTypeResolver(com.kangaroo.internal.fastjson.parser.deserializer.FieldTypeResolver fieldTypeResolver) {
        this.fieldTypeResolver = fieldTypeResolver;
    }

    public void setContext(com.kangaroo.internal.fastjson.parser.ParseContext context) {
        if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableCircularReferenceDetect)) {
            return;
        }
        this.context = context;
    }

    public void popContext() {
        if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableCircularReferenceDetect)) {
            return;
        }

        this.context = this.context.parent;

        if (contextArrayIndex <= 0) {
            return;
        }

        contextArrayIndex--;
        contextArray[contextArrayIndex] = null;
    }

    public com.kangaroo.internal.fastjson.parser.ParseContext setContext(Object object, Object fieldName) {
        if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableCircularReferenceDetect)) {
            return null;
        }

        return setContext(this.context, object, fieldName);
    }

    public com.kangaroo.internal.fastjson.parser.ParseContext setContext(com.kangaroo.internal.fastjson.parser.ParseContext parent, Object object, Object fieldName) {
        if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableCircularReferenceDetect)) {
            return null;
        }

        this.context = new com.kangaroo.internal.fastjson.parser.ParseContext(parent, object, fieldName);
        addContext(this.context);

        return this.context;
    }

    private void addContext(com.kangaroo.internal.fastjson.parser.ParseContext context) {
        int i = contextArrayIndex++;
        if (contextArray == null) {
            contextArray = new com.kangaroo.internal.fastjson.parser.ParseContext[8];
        } else if (i >= contextArray.length) {
            int newLen = (contextArray.length * 3) / 2;
            com.kangaroo.internal.fastjson.parser.ParseContext[] newArray = new com.kangaroo.internal.fastjson.parser.ParseContext[newLen];
            System.arraycopy(contextArray, 0, newArray, 0, contextArray.length);
            contextArray = newArray;
        }
        contextArray[i] = context;
    }

    public Object parse() {
        return parse(null);
    }

    public Object parseKey() {
        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.IDENTIFIER) {
            String value = lexer.stringVal();
            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
            return value;
        }
        return parse(null);
    }

    public Object parse(Object fieldName) {
        final com.kangaroo.internal.fastjson.parser.JSONLexer lexer = this.lexer;
        switch (lexer.token()) {
            case SET:
                lexer.nextToken();
                HashSet<Object> set = new HashSet<Object>();
                parseArray(set, fieldName);
                return set;
            case TREE_SET:
                lexer.nextToken();
                TreeSet<Object> treeSet = new TreeSet<Object>();
                parseArray(treeSet, fieldName);
                return treeSet;
            case LBRACKET:
                com.kangaroo.internal.fastjson.JSONArray array = new com.kangaroo.internal.fastjson.JSONArray();
                parseArray(array, fieldName);
                if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.UseObjectArray)) {
                    return array.toArray();
                }
                return array;
            case LBRACE:
                com.kangaroo.internal.fastjson.JSONObject object = new com.kangaroo.internal.fastjson.JSONObject(lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.OrderedField));
                return parseObject(object, fieldName);
//            case LBRACE: {
//                Map<String, Object> map = lexer.isEnabled(Feature.OrderedField)
//                        ? new LinkedHashMap<String, Object>()
//                        : new HashMap<String, Object>();
//                Object obj = parseObject(map, fieldName);
//                if (obj != map) {
//                    return obj;
//                }
//                return new JSONObject(map);
//            }
            case LITERAL_INT:
                Number intValue = lexer.integerValue();
                lexer.nextToken();
                return intValue;
            case LITERAL_FLOAT:
                Object value = lexer.decimalValue(lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.UseBigDecimal));
                lexer.nextToken();
                return value;
            case LITERAL_STRING:
                String stringLiteral = lexer.stringVal();
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

                if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowISO8601DateFormat)) {
                    com.kangaroo.internal.fastjson.parser.JSONScanner iso8601Lexer = new com.kangaroo.internal.fastjson.parser.JSONScanner(stringLiteral);
                    try {
                        if (iso8601Lexer.scanISO8601DateIfMatch()) {
                            return iso8601Lexer.getCalendar().getTime();
                        }
                    } finally {
                        iso8601Lexer.close();
                    }
                }

                return stringLiteral;
            case NULL:
                lexer.nextToken();
                return null;
            case UNDEFINED:
                lexer.nextToken();
                return null;
            case TRUE:
                lexer.nextToken();
                return Boolean.TRUE;
            case FALSE:
                lexer.nextToken();
                return Boolean.FALSE;
            case NEW:
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.IDENTIFIER);

                if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.IDENTIFIER) {
                    throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
                }
                lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.LPAREN);

                accept(com.kangaroo.internal.fastjson.parser.JSONToken.LPAREN);
                long time = ((Number) lexer.integerValue()).longValue();
                accept(com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT);

                accept(com.kangaroo.internal.fastjson.parser.JSONToken.RPAREN);

                return new Date(time);
            case EOF:
                if (lexer.isBlankInput()) {
                    return null;
                }
                throw new com.kangaroo.internal.fastjson.JSONException("unterminated json string, " + lexer.info());
            case HEX:
                byte[] bytes = lexer.bytesValue();
                lexer.nextToken();
                return bytes;
            case IDENTIFIER:
                String identifier = lexer.stringVal();
                if ("NaN".equals(identifier)) {
                    lexer.nextToken();
                    return null;
                }
                throw new com.kangaroo.internal.fastjson.JSONException("syntax error, " + lexer.info());
            case ERROR:
            default:
                throw new com.kangaroo.internal.fastjson.JSONException("syntax error, " + lexer.info());
        }
    }

    public void config(com.kangaroo.internal.fastjson.parser.Feature feature, boolean state) {
        this.lexer.config(feature, state);
    }

    public boolean isEnabled(com.kangaroo.internal.fastjson.parser.Feature feature) {
        return lexer.isEnabled(feature);
    }

    public com.kangaroo.internal.fastjson.parser.JSONLexer getLexer() {
        return lexer;
    }

    public final void accept(final int token) {
        final com.kangaroo.internal.fastjson.parser.JSONLexer lexer = this.lexer;
        if (lexer.token() == token) {
            lexer.nextToken();
        } else {
            throw new com.kangaroo.internal.fastjson.JSONException("syntax error, expect " + com.kangaroo.internal.fastjson.parser.JSONToken.name(token) + ", actual "
                                    + com.kangaroo.internal.fastjson.parser.JSONToken.name(lexer.token()));
        }
    }

    public final void accept(final int token, int nextExpectToken) {
        final com.kangaroo.internal.fastjson.parser.JSONLexer lexer = this.lexer;
        if (lexer.token() == token) {
            lexer.nextToken(nextExpectToken);
        } else {
            throwException(token);
        }
    }
    
    public void throwException(int token) {
        throw new com.kangaroo.internal.fastjson.JSONException("syntax error, expect " + com.kangaroo.internal.fastjson.parser.JSONToken.name(token) + ", actual "
                                + com.kangaroo.internal.fastjson.parser.JSONToken.name(lexer.token()));
    }
    
    public void close() {
        final com.kangaroo.internal.fastjson.parser.JSONLexer lexer = this.lexer;

        try {
            if (lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AutoCloseSource)) {
                if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.EOF) {
                    throw new com.kangaroo.internal.fastjson.JSONException("not close json text, token : " + com.kangaroo.internal.fastjson.parser.JSONToken.name(lexer.token()));
                }
            }
        } finally {
            lexer.close();
        }
    }

    public Object resolveReference(String ref) {
        if(contextArray == null) {
            return null;
        }
        for (int i = 0; i < contextArray.length && i < contextArrayIndex; i++) {
            com.kangaroo.internal.fastjson.parser.ParseContext context = contextArray[i];
            if (context.toString().equals(ref)) {
                return context.object;
            }
        }
        return null;
    }

    public void handleResovleTask(Object value) {
        if (resolveTaskList == null) {
            return;
        }

        for (int i = 0, size = resolveTaskList.size(); i < size; ++i) {
            ResolveTask task = resolveTaskList.get(i);
            String ref = task.referenceValue;

            Object object = null;
            if (task.ownerContext != null) {
                object = task.ownerContext.object;
            }

            Object refValue;

            if (ref.startsWith("$")) {
                refValue = getObject(ref);
                if (refValue == null) {
                    try {
                        refValue = com.kangaroo.internal.fastjson.JSONPath.eval(value, ref);
                    } catch (com.kangaroo.internal.fastjson.JSONPathException ex) {
                        // skip
                    }
                }
            } else {
                refValue = task.context.object;
            }

            com.kangaroo.internal.fastjson.parser.deserializer.FieldDeserializer fieldDeser = task.fieldDeserializer;

            if (fieldDeser != null) {
                if (refValue != null
                        && refValue.getClass() == com.kangaroo.internal.fastjson.JSONObject.class
                        && fieldDeser.fieldInfo != null
                        && !Map.class.isAssignableFrom(fieldDeser.fieldInfo.fieldClass)) {
                    Object root = this.contextArray[0].object;
                    refValue = JSONPath.eval(root, ref);
                }

                fieldDeser.setValue(object, refValue);
            }
        }
    }

    public static class ResolveTask {

        public final com.kangaroo.internal.fastjson.parser.ParseContext context;
        public final String       referenceValue;
        public com.kangaroo.internal.fastjson.parser.deserializer.FieldDeserializer fieldDeserializer;
        public com.kangaroo.internal.fastjson.parser.ParseContext ownerContext;

        public ResolveTask(com.kangaroo.internal.fastjson.parser.ParseContext context, String referenceValue){
            this.context = context;
            this.referenceValue = referenceValue;
        }
    }
    
    public void parseExtra(Object object, String key) {
        final com.kangaroo.internal.fastjson.parser.JSONLexer lexer = this.lexer; // xxx
        lexer.nextTokenWithColon();
        Type type = null;
        
        if (extraTypeProviders != null) {
            for (com.kangaroo.internal.fastjson.parser.deserializer.ExtraTypeProvider extraProvider : extraTypeProviders) {
                type = extraProvider.getExtraType(object, key);
            }
        }
        Object value = type == null //
            ? parse() // skip
            : parseObject(type);
            
        if (object instanceof com.kangaroo.internal.fastjson.parser.deserializer.ExtraProcessable) {
            com.kangaroo.internal.fastjson.parser.deserializer.ExtraProcessable extraProcessable = ((com.kangaroo.internal.fastjson.parser.deserializer.ExtraProcessable) object);
            extraProcessable.processExtra(key, value);
            return;
        }

        if (extraProcessors != null) {
            for (com.kangaroo.internal.fastjson.parser.deserializer.ExtraProcessor process : extraProcessors) {
                process.processExtra(object, key, value);
            }
        }

        if (resolveStatus == NeedToResolve) {
            resolveStatus = NONE;
        }
    }

    public Object parse(com.kangaroo.internal.fastjson.parser.deserializer.PropertyProcessable object, Object fieldName) {
        if (lexer.token() != com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE) {
            String msg = "syntax error, expect {, actual " + lexer.tokenName();
            if (fieldName instanceof String) {
                msg += ", fieldName ";
                msg += fieldName;
            }
            msg += ", ";
            msg += lexer.info();

            com.kangaroo.internal.fastjson.JSONArray array = new com.kangaroo.internal.fastjson.JSONArray();
            parseArray(array, fieldName);

            if (array.size() == 1) {
                Object first = array.get(0);
                if (first instanceof com.kangaroo.internal.fastjson.JSONObject) {
                    return (com.kangaroo.internal.fastjson.JSONObject) first;
                }
            }

            throw new com.kangaroo.internal.fastjson.JSONException(msg);
        }

        com.kangaroo.internal.fastjson.parser.ParseContext context = this.context;
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
                    key = lexer.scanSymbol(symbolTable, '"');
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new com.kangaroo.internal.fastjson.JSONException("expect ':' at " + lexer.pos());
                    }
                } else if (ch == '}') {
                    lexer.next();
                    lexer.resetStringPosition();
                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                    return object;
                } else if (ch == '\'') {
                    if (!lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowSingleQuotes)) {
                        throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
                    }

                    key = lexer.scanSymbol(symbolTable, '\'');
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new com.kangaroo.internal.fastjson.JSONException("expect ':' at " + lexer.pos());
                    }
                } else {
                    if (!lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.AllowUnQuotedFieldNames)) {
                        throw new com.kangaroo.internal.fastjson.JSONException("syntax error");
                    }

                    key = lexer.scanSymbolUnQuoted(symbolTable);
                    lexer.skipWhitespace();
                    ch = lexer.getCurrent();
                    if (ch != ':') {
                        throw new com.kangaroo.internal.fastjson.JSONException("expect ':' at " + lexer.pos() + ", actual " + ch);
                    }
                }

                lexer.next();
                lexer.skipWhitespace();
                ch = lexer.getCurrent();

                lexer.resetStringPosition();

                if (key == com.kangaroo.internal.fastjson.JSON.DEFAULT_TYPE_KEY && !lexer.isEnabled(com.kangaroo.internal.fastjson.parser.Feature.DisableSpecialKeyDetect)) {
                    String typeName = lexer.scanSymbol(symbolTable, '"');

                    Class<?> clazz = config.checkAutoType(typeName, null, lexer.getFeatures());

                    if (Map.class.isAssignableFrom(clazz) ) {
                        lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                        if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                            lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                            return object;
                        }
                        continue;
                    }

                    ObjectDeserializer deserializer = config.getDeserializer(clazz);

                    lexer.nextToken(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);

                    setResolveStatus(DefaultJSONParser.TypeNameRedirect);

                    if (context != null && !(fieldName instanceof Integer)) {
                        popContext();
                    }

                    return (Map) deserializer.deserialze(this, clazz, fieldName);
                }

                Object value;
                lexer.nextToken();

                if (i != 0) {
                    setContext(context);
                }

                Type valueType = object.getType(key);

                if (lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.NULL) {
                    value = null;
                    lexer.nextToken();
                } else {
                    value = parseObject(valueType, key);
                }

                object.apply(key, value);

                setContext(context, value, key);
                setContext(context);

                final int tok = lexer.token();
                if (tok == com.kangaroo.internal.fastjson.parser.JSONToken.EOF || tok == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET) {
                    return object;
                }

                if (tok == com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE) {
                    lexer.nextToken();
                    return object;
                }
            }
        } finally {
            setContext(context);
        }
    }
}
