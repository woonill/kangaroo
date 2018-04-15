package com.kangaroo.internal.fastjson.serializer;

import java.io.IOException;
import java.lang.reflect.Type;

public class AppendableSerializer implements com.kangaroo.internal.fastjson.serializer.ObjectSerializer {

    public final static AppendableSerializer instance = new AppendableSerializer();

    public void write(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        if (object == null) {
            com.kangaroo.internal.fastjson.serializer.SerializeWriter out = serializer.out;
            out.writeNull(com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullStringAsEmpty);
            return;
        }

        serializer.write(object.toString());
    }

}
