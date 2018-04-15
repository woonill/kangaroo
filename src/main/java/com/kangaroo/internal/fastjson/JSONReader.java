package com.kangaroo.internal.fastjson;

import java.io.Closeable;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class JSONReader implements Closeable {

    private final com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser;
    private JSONStreamContext       context;

    public JSONReader(Reader reader){
        this(reader, new com.kangaroo.internal.fastjson.parser.Feature[0]);
    }
    
    public JSONReader(Reader reader, com.kangaroo.internal.fastjson.parser.Feature... features){
        this(new com.kangaroo.internal.fastjson.parser.JSONReaderScanner(reader));
        for (com.kangaroo.internal.fastjson.parser.Feature feature : features) {
            this.config(feature, true);
        }
    }

    public JSONReader(com.kangaroo.internal.fastjson.parser.JSONLexer lexer){
        this(new com.kangaroo.internal.fastjson.parser.DefaultJSONParser(lexer));
    }

    public JSONReader(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser){
        this.parser = parser;
    }
    
    public void setTimzeZone(TimeZone timezone) {
        this.parser.lexer.setTimeZone(timezone);
    }
    
    public void setLocale(Locale locale) {
        this.parser.lexer.setLocale(locale);
    }

    public void config(com.kangaroo.internal.fastjson.parser.Feature feature, boolean state) {
        this.parser.config(feature, state);
    }
    
    public Locale getLocal() {
        return this.parser.lexer.getLocale();
    }
    
    public TimeZone getTimzeZone() {
        return this.parser.lexer.getTimeZone();
    }

    public void startObject() {
        if (context == null) {
            context = new JSONStreamContext(null, JSONStreamContext.StartObject);
        } else {
            startStructure();
            context = new JSONStreamContext(context, JSONStreamContext.StartObject);
        }

        this.parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.LBRACE, com.kangaroo.internal.fastjson.parser.JSONToken.IDENTIFIER);
    }

    public void endObject() {
        this.parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE);
        endStructure();
    }

    public void startArray() {
        if (context == null) {
            context = new JSONStreamContext(null, JSONStreamContext.StartArray);
        } else {
            startStructure();

            context = new JSONStreamContext(context, JSONStreamContext.StartArray);
        }
        this.parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.LBRACKET);
    }

    public void endArray() {
        this.parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET);
        endStructure();
    }

    private void startStructure() {
        final int state = context.state;
        switch (state) {
            case JSONStreamContext.PropertyKey:
                parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.COLON);
                break;
            case JSONStreamContext.PropertyValue:
            case JSONStreamContext.ArrayValue:
                parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                break;
            case JSONStreamContext.StartArray:
            case JSONStreamContext.StartObject:
                break;
            default:
                throw new JSONException("illegal state : " + context.state);
        }
    }

    private void endStructure() {
        context = context.parent;

        if (context == null) {
            return;
        }
        
        final int state = context.state;
        int newState = -1;
        switch (state) {
            case JSONStreamContext.PropertyKey:
                newState = JSONStreamContext.PropertyValue;
                break;
            case JSONStreamContext.StartArray:
                newState = JSONStreamContext.ArrayValue;
                break;
            case JSONStreamContext.PropertyValue:
            case JSONStreamContext.StartObject:
                newState = JSONStreamContext.PropertyKey;
                break;
            default:
                break;
        }
        if (newState != -1) {
            context.state = newState;
        }
    }

    public boolean hasNext() {
        if (context == null) {
            throw new JSONException("context is null");
        }

        final int token = parser.lexer.token();
        final int state = context.state;
        switch (state) {
            case JSONStreamContext.StartArray:
            case JSONStreamContext.ArrayValue:
                return token != com.kangaroo.internal.fastjson.parser.JSONToken.RBRACKET;
            case JSONStreamContext.StartObject:
            case JSONStreamContext.PropertyValue:
                return token != com.kangaroo.internal.fastjson.parser.JSONToken.RBRACE;
            default:
                throw new JSONException("illegal state : " + state);
        }
    }

    public int peek() {
        return parser.lexer.token();
    }

    public void close() {
        parser.close();
    }

    public Integer readInteger() {
        Object object;
        if (context == null) {
            object = parser.parse();
        } else {
            readBefore();
            object = parser.parse();
            readAfter();
        }

        return com.kangaroo.internal.fastjson.util.TypeUtils.castToInt(object);
    }

    public Long readLong() {
        Object object;
        if (context == null) {
            object = parser.parse();
        } else {
            readBefore();
            object = parser.parse();
            readAfter();
        }

        return com.kangaroo.internal.fastjson.util.TypeUtils.castToLong(object);
    }

    public String readString() {
        Object object;
        if (context == null) {
            object = parser.parse();
        } else {
            readBefore();
            com.kangaroo.internal.fastjson.parser.JSONLexer lexer = parser.lexer;
            if (context.state == JSONStreamContext.StartObject && lexer.token() == com.kangaroo.internal.fastjson.parser.JSONToken.IDENTIFIER) {
                object = lexer.stringVal();
                lexer.nextToken();
            } else {
                object = parser.parse();
            }
            readAfter();
        }

        return com.kangaroo.internal.fastjson.util.TypeUtils.castToString(object);
    }
    
    public <T> T readObject(TypeReference<T> typeRef) {
        return readObject(typeRef.getType());
    }

    public <T> T readObject(Type type) {
        if (context == null) {
            return parser.parseObject(type);
        }

        readBefore();
        T object = parser.parseObject(type);
        readAfter();
        return object;
    }

    public <T> T readObject(Class<T> type) {
        if (context == null) {
            return parser.parseObject(type);
        }

        readBefore();
        T object = parser.parseObject(type);
        readAfter();
        return object;
    }

    public void readObject(Object object) {
        if (context == null) {
            parser.parseObject(object);
            return;
        }

        readBefore();
        parser.parseObject(object);
        readAfter();
    }

    public Object readObject() {
        if (context == null) {
            return parser.parse();
        }

        readBefore();
        Object object;
        switch (context.state) {
            case JSONStreamContext.StartObject:
            case JSONStreamContext.PropertyValue:
                object = parser.parseKey();
                break;
            default:
                object = parser.parse();
                break;
        }

        readAfter();
        return object;
    }

    @SuppressWarnings("rawtypes")
    public Object readObject(Map object) {
        if (context == null) {
            return parser.parseObject(object);
        }

        readBefore();
        Object value = parser.parseObject(object);
        readAfter();
        return value;
    }

    private void readBefore() {
        int state = context.state;
        // before
        switch (state) {
            case JSONStreamContext.PropertyKey:
                parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.COLON);
                break;
            case JSONStreamContext.PropertyValue:
                parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA, com.kangaroo.internal.fastjson.parser.JSONToken.IDENTIFIER);
                break;
            case JSONStreamContext.ArrayValue:
                parser.accept(com.kangaroo.internal.fastjson.parser.JSONToken.COMMA);
                break;
            case JSONStreamContext.StartObject:
                break;
            case JSONStreamContext.StartArray:
                break;
            default:
                throw new JSONException("illegal state : " + state);
        }
    }

    private void readAfter() {
        int state = context.state;
        int newStat = -1;
        switch (state) {
            case JSONStreamContext.StartObject:
                newStat = JSONStreamContext.PropertyKey;
                break;
            case JSONStreamContext.PropertyKey:
                newStat = JSONStreamContext.PropertyValue;
                break;
            case JSONStreamContext.PropertyValue:
                newStat = JSONStreamContext.PropertyKey;
                break;
            case JSONStreamContext.ArrayValue:
                break;
            case JSONStreamContext.StartArray:
                newStat = JSONStreamContext.ArrayValue;
                break;
            default:
                throw new JSONException("illegal state : " + state);
        }
        if (newStat != -1) {
            context.state = newStat;
        }
    }

}
