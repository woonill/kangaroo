package com.kangaroo.internal.fastjson.util;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import com.kangaroo.internal.fastjson.JSON;
import com.kangaroo.internal.fastjson.JSONArray;
import com.kangaroo.internal.fastjson.JSONAware;
import com.kangaroo.internal.fastjson.JSONException;
import com.kangaroo.internal.fastjson.JSONObject;
import com.kangaroo.internal.fastjson.JSONPath;
import com.kangaroo.internal.fastjson.JSONPathException;
import com.kangaroo.internal.fastjson.JSONReader;
import com.kangaroo.internal.fastjson.JSONStreamAware;
import com.kangaroo.internal.fastjson.JSONWriter;
import com.kangaroo.internal.fastjson.TypeReference;
import com.kangaroo.internal.fastjson.parser.DefaultJSONParser;
import com.kangaroo.internal.fastjson.parser.Feature;
import com.kangaroo.internal.fastjson.parser.JSONLexer;
import com.kangaroo.internal.fastjson.parser.JSONLexerBase;
import com.kangaroo.internal.fastjson.parser.JSONReaderScanner;
import com.kangaroo.internal.fastjson.parser.JSONScanner;
import com.kangaroo.internal.fastjson.parser.JSONToken;
import com.kangaroo.internal.fastjson.parser.ParseContext;
import com.kangaroo.internal.fastjson.parser.ParserConfig;
import com.kangaroo.internal.fastjson.parser.SymbolTable;
import com.kangaroo.internal.fastjson.parser.deserializer.AutowiredObjectDeserializer;
import com.kangaroo.internal.fastjson.parser.deserializer.DefaultFieldDeserializer;
import com.kangaroo.internal.fastjson.parser.deserializer.ExtraProcessable;
import com.kangaroo.internal.fastjson.parser.deserializer.ExtraProcessor;
import com.kangaroo.internal.fastjson.parser.deserializer.ExtraTypeProvider;
import com.kangaroo.internal.fastjson.parser.deserializer.FieldDeserializer;
import com.kangaroo.internal.fastjson.parser.deserializer.JavaBeanDeserializer;
import com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer;
import com.kangaroo.internal.fastjson.serializer.AfterFilter;
import com.kangaroo.internal.fastjson.serializer.BeanContext;
import com.kangaroo.internal.fastjson.serializer.BeforeFilter;
import com.kangaroo.internal.fastjson.serializer.JSONSerializer;
import com.kangaroo.internal.fastjson.serializer.LabelFilter;
import com.kangaroo.internal.fastjson.serializer.Labels;
import com.kangaroo.internal.fastjson.serializer.NameFilter;
import com.kangaroo.internal.fastjson.serializer.PropertyFilter;
import com.kangaroo.internal.fastjson.serializer.SerialContext;
import com.kangaroo.internal.fastjson.serializer.ValueFilter;

public class ASMClassLoader extends ClassLoader {

    private static java.security.ProtectionDomain DOMAIN;
    
    private static Map<String, Class<?>> classMapping = new HashMap<String, Class<?>>();

    static {
        DOMAIN = (java.security.ProtectionDomain) java.security.AccessController.doPrivileged(new PrivilegedAction<Object>() {

            public Object run() {
                return ASMClassLoader.class.getProtectionDomain();
            }
        });
        
        Class<?>[] jsonClasses = new Class<?>[] {JSON.class,
            JSONObject.class,
            JSONArray.class,
            JSONPath.class,
            JSONAware.class,
            JSONException.class,
            JSONPathException.class,
            JSONReader.class,
            JSONStreamAware.class,
            JSONWriter.class,
            TypeReference.class,
                    
            com.kangaroo.internal.fastjson.util.FieldInfo.class,
            com.kangaroo.internal.fastjson.util.TypeUtils.class,
            com.kangaroo.internal.fastjson.util.IOUtils.class,
            com.kangaroo.internal.fastjson.util.IdentityHashMap.class,
            ParameterizedTypeImpl.class,
            com.kangaroo.internal.fastjson.util.JavaBeanInfo.class,
                    
            com.kangaroo.internal.fastjson.serializer.ObjectSerializer.class,
            com.kangaroo.internal.fastjson.serializer.JavaBeanSerializer.class,
            com.kangaroo.internal.fastjson.serializer.SerializeFilterable.class,
            com.kangaroo.internal.fastjson.serializer.SerializeBeanInfo.class,
            JSONSerializer.class,
            com.kangaroo.internal.fastjson.serializer.SerializeWriter.class,
            com.kangaroo.internal.fastjson.serializer.SerializeFilter.class,
            Labels.class,
            LabelFilter.class,
            com.kangaroo.internal.fastjson.serializer.ContextValueFilter.class,
            AfterFilter.class,
            BeforeFilter.class,
            NameFilter.class,
            PropertyFilter.class,
            com.kangaroo.internal.fastjson.serializer.PropertyPreFilter.class,
            ValueFilter.class,
            com.kangaroo.internal.fastjson.serializer.SerializerFeature.class,
            com.kangaroo.internal.fastjson.serializer.ContextObjectSerializer.class,
            SerialContext.class,
            com.kangaroo.internal.fastjson.serializer.SerializeConfig.class,
                    
            JavaBeanDeserializer.class,
            ParserConfig.class,
            DefaultJSONParser.class,
            JSONLexer.class,
            JSONLexerBase.class,
            ParseContext.class,
            JSONToken.class,
            SymbolTable.class,
            Feature.class,
            JSONScanner.class,
            JSONReaderScanner.class,
                    
            AutowiredObjectDeserializer.class,
            ObjectDeserializer.class,
            ExtraProcessor.class,
            ExtraProcessable.class,
            ExtraTypeProvider.class,
            BeanContext.class,
            FieldDeserializer.class,
            DefaultFieldDeserializer.class,
        };
        
        for (Class<?> clazz : jsonClasses) {
            classMapping.put(clazz.getName(), clazz);
        }
    }
    
    public ASMClassLoader(){
        super(getParentClassLoader());
    }

    public ASMClassLoader(ClassLoader parent){
        super (parent);
    }

    static ClassLoader getParentClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                contextClassLoader.loadClass(JSON.class.getName());
                return contextClassLoader;
            } catch (ClassNotFoundException e) {
                // skip
            }
        }
        return JSON.class.getClassLoader();
    }

    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> mappingClass = classMapping.get(name);
        if (mappingClass != null) {
            return mappingClass;
        }
        
        try {
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            throw e;
        }
    }

    public Class<?> defineClassPublic(String name, byte[] b, int off, int len) throws ClassFormatError {
        Class<?> clazz = defineClass(name, b, off, len, DOMAIN);

        return clazz;
    }

    public boolean isExternalClass(Class<?> clazz) {
        ClassLoader classLoader = clazz.getClassLoader();

        if (classLoader == null) {
            return false;
        }

        ClassLoader current = this;
        while (current != null) {
            if (current == classLoader) {
                return false;
            }

            current = current.getParent();
        }

        return true;
    }

}
