package com.kangaroo.internal.fastjson.parser.deserializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import com.kangaroo.internal.fastjson.JSONException;

public class DefaultFieldDeserializer extends com.kangaroo.internal.fastjson.parser.deserializer.FieldDeserializer {

    protected com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer fieldValueDeserilizer;
    protected boolean            customDeserilizer     = false;

    public DefaultFieldDeserializer(com.kangaroo.internal.fastjson.parser.ParserConfig config, Class<?> clazz, com.kangaroo.internal.fastjson.util.FieldInfo fieldInfo){
        super(clazz, fieldInfo);
        com.kangaroo.internal.fastjson.annotation.JSONField annotation = fieldInfo.getAnnotation();
        if (annotation != null) {
            Class<?> deserializeUsing = annotation.deserializeUsing();
            customDeserilizer = deserializeUsing != null && deserializeUsing != Void.class;
        }
    }

    public com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer getFieldValueDeserilizer(com.kangaroo.internal.fastjson.parser.ParserConfig config) {
        if (fieldValueDeserilizer == null) {
            com.kangaroo.internal.fastjson.annotation.JSONField annotation = fieldInfo.getAnnotation();
            if (annotation != null && annotation.deserializeUsing() != Void.class) {
                Class<?> deserializeUsing = annotation.deserializeUsing();
                try {
                    fieldValueDeserilizer = (com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer) deserializeUsing.newInstance();
                } catch (Exception ex) {
                    throw new JSONException("create deserializeUsing ObjectDeserializer error", ex);
                }
            } else {
                fieldValueDeserilizer = config.getDeserializer(fieldInfo.fieldClass, fieldInfo.fieldType);
            }
        }

        return fieldValueDeserilizer;
    }

    @Override
    public void parseField(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Object object, Type objectType, Map<String, Object> fieldValues) {
        if (this.fieldValueDeserilizer == null) {
            getFieldValueDeserilizer(parser.getConfig());
        }

        com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer fieldValueDeserilizer = this.fieldValueDeserilizer;
        Type fieldType = fieldInfo.fieldType;
        if (objectType instanceof ParameterizedType) {
            com.kangaroo.internal.fastjson.parser.ParseContext objContext = parser.getContext();
            if (objContext != null) {
                objContext.type = objectType;
            }
            if (fieldType != objectType) {
                fieldType = com.kangaroo.internal.fastjson.util.FieldInfo.getFieldType(this.clazz, objectType, fieldType);
                fieldValueDeserilizer = parser.getConfig().getDeserializer(fieldType);
            }
        }

        // ContextObjectDeserializer
        Object value;
        if (fieldValueDeserilizer instanceof com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer && fieldInfo.parserFeatures != 0) {
            com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer javaBeanDeser = (com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer) fieldValueDeserilizer;
            value = javaBeanDeser.deserialze(parser, fieldType, fieldInfo.name, fieldInfo.parserFeatures);
        } else {
            if (this.fieldInfo.format != null && fieldValueDeserilizer instanceof ContextObjectDeserializer) {
                value = ((ContextObjectDeserializer) fieldValueDeserilizer) //
                                        .deserialze(parser,
                                                    fieldType,
                                                    fieldInfo.name,
                                                    fieldInfo.format,
                                                    fieldInfo.parserFeatures);
            } else {
                value = fieldValueDeserilizer.deserialze(parser, fieldType, fieldInfo.name);
            }
        }

        if (value instanceof byte[]
                && ("gzip".equals(fieldInfo.format) || "gzip,base64".equals(fieldInfo.format))) {
            byte[] bytes = (byte[]) value;
            GZIPInputStream gzipIn = null;
            try {
                gzipIn = new GZIPInputStream(new ByteArrayInputStream(bytes));

                ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                for (;;) {
                    byte[] buf = new byte[1024];
                    int len = gzipIn.read(buf);
                    if (len == -1) {
                        break;
                    }
                    if (len > 0) {
                        byteOut.write(buf, 0, len);
                    }
                }
                value = byteOut.toByteArray();

            } catch (IOException ex) {
                throw new JSONException("unzip bytes error.", ex);
            }
        }

        if (parser.getResolveStatus() == com.kangaroo.internal.fastjson.parser.DefaultJSONParser.NeedToResolve) {
            com.kangaroo.internal.fastjson.parser.DefaultJSONParser.ResolveTask task = parser.getLastResolveTask();
            task.fieldDeserializer = this;
            task.ownerContext = parser.getContext();
            parser.setResolveStatus(com.kangaroo.internal.fastjson.parser.DefaultJSONParser.NONE);
        } else {
            if (object == null) {
                fieldValues.put(fieldInfo.name, value);
            } else {
                setValue(object, value);
            }
        }
    }

    public int getFastMatchToken() {
        if (fieldValueDeserilizer != null) {
            return fieldValueDeserilizer.getFastMatchToken();
        }

        return com.kangaroo.internal.fastjson.parser.JSONToken.LITERAL_INT;
    }

    public void parseFieldUnwrapped(com.kangaroo.internal.fastjson.parser.DefaultJSONParser parser, Object object, Type objectType, Map<String, Object> fieldValues) {
        throw new JSONException("TODO");
    }
}
