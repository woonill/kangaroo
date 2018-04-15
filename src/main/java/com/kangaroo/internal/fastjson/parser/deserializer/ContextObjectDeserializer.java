package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.Type;

public abstract class ContextObjectDeserializer implements com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer {
    public <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type type, Object fieldName) {
        return deserialze(parser, type, fieldName, null, 0);
    }
    
    public abstract <T> T deserialze(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Type type, Object fieldName, String format, int features);
}
