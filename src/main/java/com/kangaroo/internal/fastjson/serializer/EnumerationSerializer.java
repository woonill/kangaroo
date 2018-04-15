package com.kangaroo.internal.fastjson.serializer;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Enumeration;


public class EnumerationSerializer implements com.kangaroo.internal.fastjson.serializer.ObjectSerializer {
    public static EnumerationSerializer instance = new EnumerationSerializer();
    
    public void write(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features) throws IOException {
        com.kangaroo.internal.fastjson.serializer.SerializeWriter out = serializer.out;

        if (object == null) {
            out.writeNull(com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullListAsEmpty);
            return;
        }
        
        Type elementType = null;
        if (out.isEnabled(com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteClassName)) {
            if (fieldType instanceof ParameterizedType) {
                ParameterizedType param = (ParameterizedType) fieldType;
                elementType = param.getActualTypeArguments()[0];
            }
        }
        
        Enumeration<?> e = (Enumeration<?>) object;
        
        com.kangaroo.internal.fastjson.serializer.SerialContext context = serializer.context;
        serializer.setContext(context, object, fieldName, 0);

        try {
            int i = 0;
            out.append('[');
            while (e.hasMoreElements()) {
                Object item = e.nextElement();
                if (i++ != 0) {
                    out.append(',');
                }

                if (item == null) {
                    out.writeNull();
                    continue;
                }

                com.kangaroo.internal.fastjson.serializer.ObjectSerializer itemSerializer = serializer.getObjectWriter(item.getClass());
                itemSerializer.write(serializer, item, i - 1, elementType, 0);
            }
            out.append(']');
        } finally {
            serializer.context = context;
        }
    }
}
