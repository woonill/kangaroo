/*
 * Copyright 1999-2017 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kangaroo.internal.fastjson.serializer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

import com.kangaroo.internal.fastjson.JSON;
import com.kangaroo.internal.fastjson.JSONException;
import com.kangaroo.internal.fastjson.PropertyNamingStrategy;
import com.kangaroo.internal.fastjson.annotation.JSONField;
import com.kangaroo.internal.fastjson.util.FieldInfo;
import com.kangaroo.internal.fastjson.util.TypeUtils;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
public class JavaBeanSerializer extends SerializeFilterable implements com.kangaroo.internal.fastjson.serializer.ObjectSerializer {
    // serializers
    protected final com.kangaroo.internal.fastjson.serializer.FieldSerializer[] getters;
    protected final com.kangaroo.internal.fastjson.serializer.FieldSerializer[] sortedGetters;
    
    protected com.kangaroo.internal.fastjson.serializer.SerializeBeanInfo beanInfo;

    private transient volatile long[] hashArray;
    private transient volatile short[] hashArrayMapping;
    
    public JavaBeanSerializer(Class<?> beanType){
        this(beanType, (Map<String, String>) null);
    }

    public JavaBeanSerializer(Class<?> beanType, String... aliasList){
        this(beanType, createAliasMap(aliasList));
    }

    static Map<String, String> createAliasMap(String... aliasList) {
        Map<String, String> aliasMap = new HashMap<String, String>();
        for (String alias : aliasList) {
            aliasMap.put(alias, alias);
        }

        return aliasMap;
    }

    /**
     * @since 1.2.42
     */
    public Class<?> getType() {
        return beanInfo.beanType;
    }

    public JavaBeanSerializer(Class<?> beanType, Map<String, String> aliasMap){
        this(TypeUtils.buildBeanInfo(beanType, aliasMap, null));
    }
    
    public JavaBeanSerializer(com.kangaroo.internal.fastjson.serializer.SerializeBeanInfo beanInfo) {
        this.beanInfo = beanInfo;
        
        sortedGetters = new com.kangaroo.internal.fastjson.serializer.FieldSerializer[beanInfo.sortedFields.length];
        for (int i = 0; i < sortedGetters.length; ++i) {
            sortedGetters[i] = new com.kangaroo.internal.fastjson.serializer.FieldSerializer(beanInfo.beanType, beanInfo.sortedFields[i]);
        }
        
        if (beanInfo.fields == beanInfo.sortedFields) {
            getters = sortedGetters;
        } else {
            getters = new com.kangaroo.internal.fastjson.serializer.FieldSerializer[beanInfo.fields.length];
            for (int i = 0; i < getters.length; ++i) {
                getters[i] = getFieldSerializer(beanInfo.fields[i].name);
            }
        }
    }

    public void writeDirectNonContext(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, //
                                      Object object, //
                                      Object fieldName, //
                                      Type fieldType, //
                                      int features) throws IOException {
        write(serializer, object, fieldName, fieldType, features);
    }
    
    public void writeAsArray(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, //
                             Object object, //
                             Object fieldName, //
                             Type fieldType, //
                             int features) throws IOException {
        write(serializer, object, fieldName, fieldType, features);
    }
    
    public void writeAsArrayNonContext(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, //
                                       Object object, //
                                       Object fieldName, //
                                       Type fieldType, //
                                       int features) throws IOException {
        write(serializer, object, fieldName, fieldType, features);
    }

    public void write(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, //
                      Object object, //
                      Object fieldName, //
                      Type fieldType, //
                      int features) throws IOException {
        write(serializer, object, fieldName, fieldType, features, false);
    }

    public void writeNoneASM(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, //
                             Object object, //
                             Object fieldName, //
                             Type fieldType, //
                             int features) throws IOException {
        write(serializer, object, fieldName, fieldType, features, false);
    }

    protected void write(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, //
                         Object object, //
                         Object fieldName, //
                         Type fieldType, //
                         int features,
                         boolean unwrapped
    ) throws IOException {
        com.kangaroo.internal.fastjson.serializer.SerializeWriter out = serializer.out;

        if (object == null) {
            out.writeNull();
            return;
        }

        if (writeReference(serializer, object, features)) {
            return;
        }

        final com.kangaroo.internal.fastjson.serializer.FieldSerializer[] getters;

        if (out.sortField) {
            getters = this.sortedGetters;
        } else {
            getters = this.getters;
        }

        com.kangaroo.internal.fastjson.serializer.SerialContext parent = serializer.context;
        if (!this.beanInfo.beanType.isEnum()) {
            serializer.setContext(parent, object, fieldName, this.beanInfo.features, features);
        }

        final boolean writeAsArray = isWriteAsArray(serializer, features);

        try {
            final char startSeperator = writeAsArray ? '[' : '{';
            final char endSeperator = writeAsArray ? ']' : '}';
            if (!unwrapped) {
                out.append(startSeperator);
            }

            if (getters.length > 0 && out.isEnabled(com.kangaroo.internal.fastjson.serializer.SerializerFeature.PrettyFormat)) {
                serializer.incrementIndent();
                serializer.println();
            }

            boolean commaFlag = false;

            if ((this.beanInfo.features & com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteClassName.mask) != 0
                ||(features & com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteClassName.mask) != 0
                || serializer.isWriteClassName(fieldType, object)) {
                Class<?> objClass = object.getClass();

                final Type type;
                if (objClass != fieldType && fieldType instanceof WildcardType) {
                    type = TypeUtils.getClass(fieldType);
                } else {
                    type = fieldType;
                }

                if (objClass != type) {
                    writeClassName(serializer, beanInfo.typeKey, object);
                    commaFlag = true;
                }
            }

            char seperator = commaFlag ? ',' : '\0';

            final boolean directWritePrefix = out.quoteFieldNames && !out.useSingleQuotes;
            char newSeperator = this.writeBefore(serializer, object, seperator);
            commaFlag = newSeperator == ',';

            final boolean skipTransient = out.isEnabled(com.kangaroo.internal.fastjson.serializer.SerializerFeature.SkipTransientField);
            final boolean ignoreNonFieldGetter = out.isEnabled(com.kangaroo.internal.fastjson.serializer.SerializerFeature.IgnoreNonFieldGetter);

            for (int i = 0; i < getters.length; ++i) {
                com.kangaroo.internal.fastjson.serializer.FieldSerializer fieldSerializer = getters[i];

                Field field = fieldSerializer.fieldInfo.field;
                FieldInfo fieldInfo = fieldSerializer.fieldInfo;
                String fieldInfoName = fieldInfo.name;
                Class<?> fieldClass = fieldInfo.fieldClass;

                if (skipTransient) {
                    if (field != null) {
                        if (fieldInfo.fieldTransient) {
                            continue;
                        }
                    }
                }

                if (ignoreNonFieldGetter) {
                    if (field == null) {
                        continue;
                    }
                }

                boolean notApply = false;
                if ((!this.applyName(serializer, object, fieldInfoName)) //
                    || !this.applyLabel(serializer, fieldInfo.label)) {
                    if (writeAsArray) {
                        notApply = true;
                    } else {
                        continue;
                    }
                }

                if (beanInfo.typeKey != null
                        && fieldInfoName.equals(beanInfo.typeKey)
                        && serializer.isWriteClassName(fieldType, object)) {
                    continue;
                }

                Object propertyValue;

                if (notApply) {
                    propertyValue = null;
                } else {
                    try {
                        propertyValue = fieldSerializer.getPropertyValueDirect(object);
                    } catch (InvocationTargetException ex) {
                        if (out.isEnabled(com.kangaroo.internal.fastjson.serializer.SerializerFeature.IgnoreErrorGetter)) {
                            propertyValue = null;
                        } else {
                            throw ex;
                        }
                    }
                }

                if (!this.apply(serializer, object, fieldInfoName, propertyValue)) {
                    continue;
                }

                if (fieldClass == String.class && "trim".equals(fieldInfo.format)) {
                    if (propertyValue != null) {
                        propertyValue = ((String) propertyValue).trim();
                    }
                }

                String key = fieldInfoName;
                key = this.processKey(serializer, object, key, propertyValue);

                Object originalValue = propertyValue;
                propertyValue = this.processValue(serializer, fieldSerializer.fieldContext, object, fieldInfoName,
                                                        propertyValue);

                if (propertyValue == null) {
                    int serialzeFeatures = fieldInfo.serialzeFeatures;
                    if (beanInfo.jsonType != null) {
                        serialzeFeatures |= com.kangaroo.internal.fastjson.serializer.SerializerFeature.of(beanInfo.jsonType.serialzeFeatures());
                    }
                    // beanInfo.jsonType
                    if (fieldClass == Boolean.class) {
                        int defaultMask = com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullBooleanAsFalse.mask;
                        final int mask = defaultMask | com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue.mask;
                        if ((!writeAsArray) && (serialzeFeatures & mask) == 0 && (out.features & mask) == 0) {
                            continue;
                        } else if ((serialzeFeatures & defaultMask) != 0 || (out.features & defaultMask) != 0) {
                            propertyValue = false;
                        }
                    } else if (fieldClass == String.class) {
                        int defaultMask = com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullStringAsEmpty.mask;
                        final int mask = defaultMask | com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue.mask;
                        if ((!writeAsArray) && (serialzeFeatures & mask) == 0 && (out.features & mask) == 0) {
                            continue;
                        } else if ((serialzeFeatures & defaultMask) != 0 || (out.features & defaultMask) != 0) {
                            propertyValue = "";
                        }
                    } else if (Number.class.isAssignableFrom(fieldClass)) {
                        int defaultMask = com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullNumberAsZero.mask;
                        final int mask = defaultMask | com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue.mask;
                        if ((!writeAsArray) && (serialzeFeatures & mask) == 0 && (out.features & mask) == 0) {
                            continue;
                        } else if ((serialzeFeatures & defaultMask) != 0 || (out.features & defaultMask) != 0) {
                            propertyValue = 0;
                        }
                    } else if (Collection.class.isAssignableFrom(fieldClass)) {
                        int defaultMask = com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullListAsEmpty.mask;
                        final int mask = defaultMask | com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue.mask;
                        if ((!writeAsArray) && (serialzeFeatures & mask) == 0 && (out.features & mask) == 0) {
                            continue;
                        } else if ((serialzeFeatures & defaultMask) != 0 || (out.features & defaultMask) != 0) {
                            propertyValue = Collections.emptyList();
                        }
                    } else if ((!writeAsArray) && (!fieldSerializer.writeNull) && !out.isEnabled(com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue.mask)){
                        continue;
                    }
                }

                if (propertyValue != null  //
                        && (out.notWriteDefaultValue //
                        || (fieldInfo.serialzeFeatures & com.kangaroo.internal.fastjson.serializer.SerializerFeature.NotWriteDefaultValue.mask) != 0 //
                        || (beanInfo.features & com.kangaroo.internal.fastjson.serializer.SerializerFeature.NotWriteDefaultValue.mask) != 0 //
                        )) {
                    Class<?> fieldCLass = fieldInfo.fieldClass;
                    if (fieldCLass == byte.class && propertyValue instanceof Byte
                        && ((Byte) propertyValue).byteValue() == 0) {
                        continue;
                    } else if (fieldCLass == short.class && propertyValue instanceof Short
                               && ((Short) propertyValue).shortValue() == 0) {
                        continue;
                    } else if (fieldCLass == int.class && propertyValue instanceof Integer
                               && ((Integer) propertyValue).intValue() == 0) {
                        continue;
                    } else if (fieldCLass == long.class && propertyValue instanceof Long
                               && ((Long) propertyValue).longValue() == 0L) {
                        continue;
                    } else if (fieldCLass == float.class && propertyValue instanceof Float
                               && ((Float) propertyValue).floatValue() == 0F) {
                        continue;
                    } else if (fieldCLass == double.class && propertyValue instanceof Double
                               && ((Double) propertyValue).doubleValue() == 0D) {
                        continue;
                    } else if (fieldCLass == boolean.class && propertyValue instanceof Boolean
                               && !((Boolean) propertyValue).booleanValue()) {
                        continue;
                    }
                }

                if (commaFlag) {
                    if (fieldInfo.unwrapped
                            && propertyValue instanceof Map
                            && ((Map) propertyValue).size() == 0) {
                        continue;
                    }

                    out.write(',');
                    if (out.isEnabled(com.kangaroo.internal.fastjson.serializer.SerializerFeature.PrettyFormat)) {
                        serializer.println();
                    }
                }

                if (key != fieldInfoName) {
                    if (!writeAsArray) {
                        out.writeFieldName(key, true);
                    }

                    serializer.write(propertyValue);
                } else if (originalValue != propertyValue) {
                    if (!writeAsArray) {
                        fieldSerializer.writePrefix(serializer);
                    }
                    serializer.write(propertyValue);
                } else {
                    if (!writeAsArray) {
                        if (!fieldInfo.unwrapped) {
                            if (directWritePrefix) {
                                out.write(fieldInfo.name_chars, 0, fieldInfo.name_chars.length);
                            } else {
                                fieldSerializer.writePrefix(serializer);
                            }
                        }
                    }

                    if (!writeAsArray) {
                        JSONField fieldAnnotation = fieldInfo.getAnnotation();
                        if (fieldClass == String.class && (fieldAnnotation == null || fieldAnnotation.serializeUsing() == Void.class)) {
                            if (propertyValue == null) {
                                if ((out.features & com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullStringAsEmpty.mask) != 0
                                    || (fieldSerializer.features & com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullStringAsEmpty.mask) != 0) {
                                    out.writeString("");
                                } else {
                                    out.writeNull();
                                }
                            } else {
                                String propertyValueString = (String) propertyValue;

                                if (out.useSingleQuotes) {
                                    out.writeStringWithSingleQuote(propertyValueString);
                                } else {
                                    out.writeStringWithDoubleQuote(propertyValueString, (char) 0);
                                }
                            }
                        } else {
                            if (fieldInfo.unwrapped
                                    && propertyValue instanceof Map
                                    && ((Map) propertyValue).size() == 0) {
                                commaFlag = false;
                                continue;
                            }

                            fieldSerializer.writeValue(serializer, propertyValue);
                        }
                    } else {
                        fieldSerializer.writeValue(serializer, propertyValue);
                    }
                }

                boolean fieldUnwrappedNull = false;
                if (fieldInfo.unwrapped
                        && propertyValue instanceof Map) {
                    Map map = ((Map) propertyValue);
                    if (map.size() == 0) {
                        fieldUnwrappedNull = true;
                    } else if (!serializer.isEnabled(com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue)){
                        boolean hasNotNull = false;
                        for (Object value : map.values()) {
                            if (value != null) {
                                hasNotNull = true;
                                break;
                            }
                        }
                        if (!hasNotNull) {
                            fieldUnwrappedNull = true;
                        }
                    }
                }

                if (!fieldUnwrappedNull) {
                    commaFlag = true;
                }
            }

            this.writeAfter(serializer, object, commaFlag ? ',' : '\0');

            if (getters.length > 0 && out.isEnabled(com.kangaroo.internal.fastjson.serializer.SerializerFeature.PrettyFormat)) {
                serializer.decrementIdent();
                serializer.println();
            }

            if (!unwrapped) {
                out.append(endSeperator);
            }
        } catch (Exception e) {
            String errorMessage = "write javaBean error, fastjson version " + JSON.VERSION;
            if (object != null) {
                errorMessage += ", class " + object.getClass().getName();
            }
            if (fieldName != null) {
                errorMessage += ", fieldName : " + fieldName;
            }
            if (e.getMessage() != null) {
                errorMessage += (", " + e.getMessage());
            }

            throw new JSONException(errorMessage, e);
        } finally {
            serializer.context = parent;
        }
    }

    protected void writeClassName(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, String typeKey, Object object) {
        if (typeKey == null) {
            typeKey = serializer.config.typeKey;
        }
        serializer.out.writeFieldName(typeKey, false);
        String typeName = this.beanInfo.typeName;
        if (typeName == null) {
            Class<?> clazz = object.getClass();

            if (TypeUtils.isProxy(clazz)) {
                clazz = clazz.getSuperclass();
            }

            typeName = clazz.getName();
        }
        serializer.write(typeName);
    }

    public boolean writeReference(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, Object object, int fieldFeatures) {
        com.kangaroo.internal.fastjson.serializer.SerialContext context = serializer.context;
        int mask = com.kangaroo.internal.fastjson.serializer.SerializerFeature.DisableCircularReferenceDetect.mask;
        if (context == null || (context.features & mask) != 0 || (fieldFeatures & mask) != 0) {
            return false;
        }

        if (serializer.references != null && serializer.references.containsKey(object)) {
            serializer.writeReference(object);
            return true;
        } else {
            return false;
        }
    }
    
    protected boolean isWriteAsArray(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer) {
        return isWriteAsArray(serializer, 0);   
    }

    protected boolean isWriteAsArray(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, int fieldFeatrues) {
        final int mask = com.kangaroo.internal.fastjson.serializer.SerializerFeature.BeanToArray.mask;
        return (beanInfo.features & mask) != 0 //
                || serializer.out.beanToArray //
                || (fieldFeatrues & mask) != 0;
    }
    
    public Object getFieldValue(Object object, String key) {
        com.kangaroo.internal.fastjson.serializer.FieldSerializer fieldDeser = getFieldSerializer(key);
        if (fieldDeser == null) {
            throw new JSONException("field not found. " + key);
        }
        
        try {
            return fieldDeser.getPropertyValue(object);
        } catch (InvocationTargetException ex) {
            throw new JSONException("getFieldValue error." + key, ex);
        } catch (IllegalAccessException ex) {
            throw new JSONException("getFieldValue error." + key, ex);
        }
    }

    public Object getFieldValue(Object object, String key, long keyHash, boolean throwFieldNotFoundException) {
        com.kangaroo.internal.fastjson.serializer.FieldSerializer fieldDeser = getFieldSerializer(keyHash);
        if (fieldDeser == null) {
            if (throwFieldNotFoundException) {
                throw new JSONException("field not found. " + key);
            }
            return null;
        }

        try {
            return fieldDeser.getPropertyValue(object);
        } catch (InvocationTargetException ex) {
            throw new JSONException("getFieldValue error." + key, ex);
        } catch (IllegalAccessException ex) {
            throw new JSONException("getFieldValue error." + key, ex);
        }
    }

    public com.kangaroo.internal.fastjson.serializer.FieldSerializer getFieldSerializer(String key) {
        if (key == null) {
            return null;
        }

        int low = 0;
        int high = sortedGetters.length - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;

            String fieldName = sortedGetters[mid].fieldInfo.name;

            int cmp = fieldName.compareTo(key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return sortedGetters[mid]; // key found
            }
        }

        return null; // key not found.
    }

    public com.kangaroo.internal.fastjson.serializer.FieldSerializer getFieldSerializer(long hash) {
        PropertyNamingStrategy[] namingStrategies = null;
        if (this.hashArray == null) {
            namingStrategies = PropertyNamingStrategy.values();

            long[] hashArray = new long[sortedGetters.length * namingStrategies.length];
            int index = 0;
            for (int i = 0; i < sortedGetters.length; i++) {
                String name = sortedGetters[i].fieldInfo.name;
                hashArray[index++] = TypeUtils.fnv1a_64(name);

                for (int j = 0; j < namingStrategies.length; j++) {
                    String name_t = namingStrategies[j].translate(name);
                    if (name.equals(name_t)) {
                        continue;
                    }
                    hashArray[index++] = TypeUtils.fnv1a_64(name_t);
                }
            }
            Arrays.sort(hashArray, 0, index);

            this.hashArray = new long[index];
            System.arraycopy(hashArray, 0, this.hashArray, 0, index);
        }

        int pos = Arrays.binarySearch(hashArray, hash);
        if (pos < 0) {
            return null;
        }

        if (hashArrayMapping == null) {
            if (namingStrategies == null) {
                namingStrategies = PropertyNamingStrategy.values();
            }

            short[] mapping = new short[hashArray.length];
            Arrays.fill(mapping, (short) -1);
            for (int i = 0; i < sortedGetters.length; i++) {
                String name = sortedGetters[i].fieldInfo.name;

                int p = Arrays.binarySearch(hashArray
                        , TypeUtils.fnv1a_64(name));
                if (p >= 0) {
                    mapping[p] = (short) i;
                }

                for (int j = 0; j < namingStrategies.length; j++) {
                    String name_t = namingStrategies[j].translate(name);
                    if (name.equals(name_t)) {
                        continue;
                    }

                    int p_t = Arrays.binarySearch(hashArray
                            , TypeUtils.fnv1a_64(name_t));
                    if (p_t >= 0) {
                        mapping[p_t] = (short) i;
                    }
                }
            }
            hashArrayMapping = mapping;
        }

        int getterIndex = hashArrayMapping[pos];
        if (getterIndex != -1) {
            return sortedGetters[getterIndex];
        }

        return null; // key not found.
    }

    public List<Object> getFieldValues(Object object) throws Exception {
        List<Object> fieldValues = new ArrayList<Object>(sortedGetters.length);
        for (com.kangaroo.internal.fastjson.serializer.FieldSerializer getter : sortedGetters) {
            fieldValues.add(getter.getPropertyValue(object));
        }

        return fieldValues;
    }

    // for jsonpath deepSet
    public List<Object> getObjectFieldValues(Object object) throws Exception {
        List<Object> fieldValues = new ArrayList<Object>(sortedGetters.length);
        for (com.kangaroo.internal.fastjson.serializer.FieldSerializer getter : sortedGetters) {
            Class fieldClass = getter.fieldInfo.fieldClass;
            if (fieldClass.isPrimitive()) {
                continue;
            }
            if (fieldClass.getName().startsWith("java.lang.")) {
                continue;
            }
            fieldValues.add(getter.getPropertyValue(object));
        }

        return fieldValues;
    }
    
    public int getSize(Object object) throws Exception {
        int size = 0;
        for (com.kangaroo.internal.fastjson.serializer.FieldSerializer getter : sortedGetters) {
            Object value = getter.getPropertyValueDirect(object);
            if (value != null) {
                size ++;
            }
        }
        return size;
    }
    
    public Map<String, Object> getFieldValuesMap(Object object) throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>(sortedGetters.length);
        
        for (com.kangaroo.internal.fastjson.serializer.FieldSerializer getter : sortedGetters) {
            map.put(getter.fieldInfo.name, getter.getPropertyValue(object));
        }
        
        return map;
    }

    protected com.kangaroo.internal.fastjson.serializer.BeanContext getBeanContext(int orinal) {
        return sortedGetters[orinal].fieldContext;
    }
    
    protected Type getFieldType(int ordinal) {
        return sortedGetters[ordinal].fieldInfo.fieldType;
    }
    
    protected char writeBefore(com.kangaroo.internal.fastjson.serializer.JSONSerializer jsonBeanDeser, //
                               Object object, char seperator) {
        
        if (jsonBeanDeser.beforeFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.BeforeFilter beforeFilter : jsonBeanDeser.beforeFilters) {
                seperator = beforeFilter.writeBefore(jsonBeanDeser, object, seperator);
            }
        }
        
        if (this.beforeFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.BeforeFilter beforeFilter : this.beforeFilters) {
                seperator = beforeFilter.writeBefore(jsonBeanDeser, object, seperator);
            }
        }
        
        return seperator;
    }
    
    protected char writeAfter(com.kangaroo.internal.fastjson.serializer.JSONSerializer jsonBeanDeser, //
                              Object object, char seperator) {
        if (jsonBeanDeser.afterFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.AfterFilter afterFilter : jsonBeanDeser.afterFilters) {
                seperator = afterFilter.writeAfter(jsonBeanDeser, object, seperator);
            }
        }
        
        if (this.afterFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.AfterFilter afterFilter : this.afterFilters) {
                seperator = afterFilter.writeAfter(jsonBeanDeser, object, seperator);
            }
        }
        
        return seperator;
    }
    
    protected boolean applyLabel(com.kangaroo.internal.fastjson.serializer.JSONSerializer jsonBeanDeser, String label) {
        if (jsonBeanDeser.labelFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.LabelFilter propertyFilter : jsonBeanDeser.labelFilters) {
                if (!propertyFilter.apply(label)) {
                    return false;
                }
            }
        }
        
        if (this.labelFilters != null) {
            for (com.kangaroo.internal.fastjson.serializer.LabelFilter propertyFilter : this.labelFilters) {
                if (!propertyFilter.apply(label)) {
                    return false;
                }
            }
        }
        
        return true;
    }
}
