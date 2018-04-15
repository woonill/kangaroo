package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.Type;

public interface FieldTypeResolver extends com.kangaroo.internal.fastjson.parser.deserializer.ParseProcess {
    Type resolve(Object object, String fieldName);
}
