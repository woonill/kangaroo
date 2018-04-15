package com.kangaroo.internal.fastjson.serializer;


@Deprecated
public class JSONSerializerMap extends com.kangaroo.internal.fastjson.serializer.SerializeConfig {
    public final boolean put(Class<?> clazz, com.kangaroo.internal.fastjson.serializer.ObjectSerializer serializer) {
        return super.put(clazz, serializer);
    }
}
