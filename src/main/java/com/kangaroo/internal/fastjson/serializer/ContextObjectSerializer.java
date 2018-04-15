package com.kangaroo.internal.fastjson.serializer;

import java.io.IOException;

public interface ContextObjectSerializer extends com.kangaroo.internal.fastjson.serializer.ObjectSerializer {
    void write(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, //
               Object object, //
               com.kangaroo.internal.fastjson.serializer.BeanContext context) throws IOException;
}
