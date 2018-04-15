package com.kangaroo.internal.fastjson.serializer;

import java.io.IOException;
import java.lang.reflect.Type;

public class ToStringSerializer implements com.kangaroo.internal.fastjson.serializer.ObjectSerializer {

    public static final ToStringSerializer instance = new ToStringSerializer();

    @Override
    public void write(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, Object object, Object fieldName, Type fieldType,
                      int features) throws IOException {
        com.kangaroo.internal.fastjson.serializer.SerializeWriter out = serializer.out;

        if (object == null) {
            out.writeNull();
            return;
        }

        String strVal = object.toString();
        out.writeString(strVal);
    }

}
