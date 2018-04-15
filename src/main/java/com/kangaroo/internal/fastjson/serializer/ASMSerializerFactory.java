package com.kangaroo.internal.fastjson.serializer;

import com.kangaroo.internal.fastjson.JSONException;
import com.kangaroo.internal.fastjson.annotation.JSONField;
import com.kangaroo.internal.fastjson.annotation.JSONType;
import com.kangaroo.internal.fastjson.parser.ParserConfig;
import com.kangaroo.internal.fastjson.util.ASMClassLoader;
import com.kangaroo.internal.fastjson.util.ASMUtils;
import com.kangaroo.internal.fastjson.util.FieldInfo;
import com.kangaroo.internal.fastjson.util.TypeUtils;

import java.io.Serializable;
import java.lang.reflect.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.kangaroo.internal.fastjson.util.ASMUtils.desc;
import static com.kangaroo.internal.fastjson.util.ASMUtils.type;

public class ASMSerializerFactory implements com.kangaroo.internal.fastjson.asm.Opcodes {

    protected final ASMClassLoader classLoader             = new ASMClassLoader();

    private final AtomicLong       seed                    = new AtomicLong();

    static final String            JSONSerializer           = type(com.kangaroo.internal.fastjson.serializer.JSONSerializer.class);
    static final String            ObjectSerializer         = type(com.kangaroo.internal.fastjson.serializer.ObjectSerializer.class);
    static final String            ObjectSerializer_desc    = "L" + ObjectSerializer + ";";
    static final String            SerializeWriter          = type(com.kangaroo.internal.fastjson.serializer.SerializeWriter.class);
    static final String            SerializeWriter_desc     = "L" + SerializeWriter + ";";
    static final String            JavaBeanSerializer       = type(com.kangaroo.internal.fastjson.serializer.JavaBeanSerializer.class);
    static final String            JavaBeanSerializer_desc  = "L" + type(com.kangaroo.internal.fastjson.serializer.JavaBeanSerializer.class) + ";";
    static final String            SerialContext_desc       = desc(com.kangaroo.internal.fastjson.serializer.SerialContext.class);
    static final String            SerializeFilterable_desc = desc(com.kangaroo.internal.fastjson.serializer.SerializeFilterable.class);

    static class Context {

        static final int              serializer     = 1;
        static final int              obj            = 2;
        static final int              paramFieldName = 3;
        static final int              paramFieldType = 4;
        static final int              features       = 5;
        static int                    fieldName      = 6;
        static int                    original       = 7;
        static int                    processValue   = 8;

        private final FieldInfo[]       getters;
        private final String            className;
        private final com.kangaroo.internal.fastjson.serializer.SerializeBeanInfo beanInfo;
        private final boolean           writeDirect;

        private Map<String, Integer>    variants       = new HashMap<String, Integer>();
        private int                     variantIndex   = 9;
        private final boolean           nonContext;

        public Context(FieldInfo[] getters, //
                       com.kangaroo.internal.fastjson.serializer.SerializeBeanInfo beanInfo, //
                       String className, //
                       boolean writeDirect, //
                       boolean nonContext){
            this.getters = getters;
            this.className = className;
            this.beanInfo = beanInfo;
            this.writeDirect = writeDirect;
            this.nonContext = nonContext || beanInfo.beanType.isEnum();
        }

        public int var(String name) {
            Integer i = variants.get(name);
            if (i == null) {
                variants.put(name, variantIndex++);
            }
            i = variants.get(name);
            return i.intValue();
        }

        public int var(String name, int increment) {
            Integer i = variants.get(name);
            if (i == null) {
                variants.put(name, variantIndex);
                variantIndex += increment;
            }
            i = variants.get(name);
            return i.intValue();
        }
        
        public int getFieldOrinal(String name) {
            int fieldIndex = -1;
            for (int i = 0, size = getters.length; i < size; ++i) {
                FieldInfo item = getters[i];
                if (item.name.equals(name)) {
                    fieldIndex = i;
                    break;
                }
            }
            return fieldIndex;
        }
    }

    public com.kangaroo.internal.fastjson.serializer.JavaBeanSerializer createJavaBeanSerializer(com.kangaroo.internal.fastjson.serializer.SerializeBeanInfo beanInfo) throws Exception {
        Class<?> clazz = beanInfo.beanType;
        if (clazz.isPrimitive()) {
            throw new JSONException("unsupportd class " + clazz.getName());
        }

        JSONType jsonType = TypeUtils.getAnnotation(clazz, JSONType.class);

        FieldInfo[] unsortedGetters = beanInfo.fields;

        for (FieldInfo fieldInfo : unsortedGetters) {
            if (fieldInfo.field == null //
                && fieldInfo.method != null //
                && fieldInfo.method.getDeclaringClass().isInterface()) {
                return new com.kangaroo.internal.fastjson.serializer.JavaBeanSerializer(beanInfo);
            }
        }

        FieldInfo[] getters = beanInfo.sortedFields;

        boolean nativeSorted = beanInfo.sortedFields == beanInfo.fields;

        if (getters.length > 256) {
            return new com.kangaroo.internal.fastjson.serializer.JavaBeanSerializer(beanInfo);
        }

        for (FieldInfo getter : getters) {
            if (!ASMUtils.checkName(getter.getMember().getName())) {
                return new com.kangaroo.internal.fastjson.serializer.JavaBeanSerializer(beanInfo);
            }
        }

        String className = "ASMSerializer_" + seed.incrementAndGet() + "_" + clazz.getSimpleName();
        String classNameType;
        String classNameFull;
        Package pkg = ASMSerializerFactory.class.getPackage();
        if (pkg != null) {
            String packageName = pkg.getName();
            classNameType = packageName.replace('.', '/') + "/" + className;
            classNameFull = packageName + "." + className;
        } else {
            classNameType = className;
            classNameFull = className;
        }

        String packageName = ASMSerializerFactory.class.getPackage().getName();


        com.kangaroo.internal.fastjson.asm.ClassWriter cw = new com.kangaroo.internal.fastjson.asm.ClassWriter();
        cw.visit(V1_5 //
                 , ACC_PUBLIC + ACC_SUPER //
                 , classNameType //
                 , JavaBeanSerializer //
                 , new String[] { ObjectSerializer } //
        );

        for (FieldInfo fieldInfo : getters) {
            if (fieldInfo.fieldClass.isPrimitive() //
                //|| fieldInfo.fieldClass.isEnum() //
                || fieldInfo.fieldClass == String.class) {
                continue;
            }

            new com.kangaroo.internal.fastjson.asm.FieldWriter(cw, ACC_PUBLIC, fieldInfo.name + "_asm_fieldType", "Ljava/lang/reflect/Type;") //
                                                                                                           .visitEnd();

            if (List.class.isAssignableFrom(fieldInfo.fieldClass)) {
                new com.kangaroo.internal.fastjson.asm.FieldWriter(cw, ACC_PUBLIC, fieldInfo.name + "_asm_list_item_ser_",
                                ObjectSerializer_desc) //
                                                       .visitEnd();
            }

            new com.kangaroo.internal.fastjson.asm.FieldWriter(cw, ACC_PUBLIC, fieldInfo.name + "_asm_ser_", ObjectSerializer_desc) //
                                                                                                        .visitEnd();
        }

        com.kangaroo.internal.fastjson.asm.MethodVisitor mw = new com.kangaroo.internal.fastjson.asm.MethodWriter(cw, ACC_PUBLIC, "<init>", "(" + desc(com.kangaroo.internal.fastjson.serializer.SerializeBeanInfo.class) + ")V", null, null);
        mw.visitVarInsn(ALOAD, 0);
        mw.visitVarInsn(ALOAD, 1);
        mw.visitMethodInsn(INVOKESPECIAL, JavaBeanSerializer, "<init>", "(" + desc(com.kangaroo.internal.fastjson.serializer.SerializeBeanInfo.class) + ")V");

        // init _asm_fieldType
        for (int i = 0; i < getters.length; ++i) {
            FieldInfo fieldInfo = getters[i];
            if (fieldInfo.fieldClass.isPrimitive() //
//                || fieldInfo.fieldClass.isEnum() //
                || fieldInfo.fieldClass == String.class) {
                continue;
            }

            mw.visitVarInsn(ALOAD, 0);

            if (fieldInfo.method != null) {
                mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(fieldInfo.declaringClass)));
                mw.visitLdcInsn(fieldInfo.method.getName());
                mw.visitMethodInsn(INVOKESTATIC, type(ASMUtils.class), "getMethodType",
                                   "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Type;");

            } else {
                mw.visitVarInsn(ALOAD, 0);
                mw.visitLdcInsn(i);
                mw.visitMethodInsn(INVOKESPECIAL, JavaBeanSerializer, "getFieldType", "(I)Ljava/lang/reflect/Type;");
            }

            mw.visitFieldInsn(PUTFIELD, classNameType, fieldInfo.name + "_asm_fieldType", "Ljava/lang/reflect/Type;");
        }

        mw.visitInsn(RETURN);
        mw.visitMaxs(4, 4);
        mw.visitEnd();

        boolean DisableCircularReferenceDetect = false;
        if (jsonType != null) {
            for (com.kangaroo.internal.fastjson.serializer.SerializerFeature featrues : jsonType.serialzeFeatures()) {
                if (featrues == com.kangaroo.internal.fastjson.serializer.SerializerFeature.DisableCircularReferenceDetect) {
                    DisableCircularReferenceDetect = true;
                    break;
                }
            }
        }

        // 0 write
        // 1 writeNormal
        // 2 writeNonContext
        for (int i = 0; i < 3; ++i) {
            String methodName;
            boolean nonContext = DisableCircularReferenceDetect;
            boolean writeDirect = false;
            if (i == 0) {
                methodName = "write";
                writeDirect = true;
            } else if (i == 1) {
                methodName = "writeNormal";
            } else {
                writeDirect = true;
                nonContext = true;
                methodName = "writeDirectNonContext";
            }

            Context context = new Context(getters, beanInfo, classNameType, writeDirect,
                                          nonContext);

            mw = new com.kangaroo.internal.fastjson.asm.MethodWriter(cw, //
                                  ACC_PUBLIC, //
                                  methodName, //
                                  "(L" + JSONSerializer
                                              + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V", //
                                  null, //
                                  new String[] { "java/io/IOException" } //
            );

            {
                com.kangaroo.internal.fastjson.asm.Label endIf_ = new com.kangaroo.internal.fastjson.asm.Label();
                mw.visitVarInsn(ALOAD, Context.obj);
                //serializer.writeNull();
                mw.visitJumpInsn(IFNONNULL, endIf_);
                mw.visitVarInsn(ALOAD, Context.serializer);
                mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer,
                        "writeNull", "()V");

                mw.visitInsn(RETURN);
                mw.visitLabel(endIf_);
            }

            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitFieldInsn(GETFIELD, JSONSerializer, "out", SerializeWriter_desc);
            mw.visitVarInsn(ASTORE, context.var("out"));

            if ((!nativeSorted) //
                && !context.writeDirect) {

                if (jsonType == null || jsonType.alphabetic()) {
                    com.kangaroo.internal.fastjson.asm.Label _else = new com.kangaroo.internal.fastjson.asm.Label();

                    mw.visitVarInsn(ALOAD, context.var("out"));
                    mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "isSortField", "()Z");

                    mw.visitJumpInsn(IFNE, _else);
                    mw.visitVarInsn(ALOAD, 0);
                    mw.visitVarInsn(ALOAD, 1);
                    mw.visitVarInsn(ALOAD, 2);
                    mw.visitVarInsn(ALOAD, 3);
                    mw.visitVarInsn(ALOAD, 4);
                    mw.visitVarInsn(ILOAD, 5);
                    mw.visitMethodInsn(INVOKEVIRTUAL, classNameType,
                                       "writeUnsorted", "(L" + JSONSerializer
                                                        + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                    mw.visitInsn(RETURN);

                    mw.visitLabel(_else);
                }
            }

            // isWriteDoubleQuoteDirect
            if (context.writeDirect && !nonContext) {
                com.kangaroo.internal.fastjson.asm.Label _direct = new com.kangaroo.internal.fastjson.asm.Label();
                com.kangaroo.internal.fastjson.asm.Label _directElse = new com.kangaroo.internal.fastjson.asm.Label();

                mw.visitVarInsn(ALOAD, 0);
                mw.visitVarInsn(ALOAD, Context.serializer);
                mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "writeDirect", "(L" + JSONSerializer + ";)Z");
                mw.visitJumpInsn(IFNE, _directElse);

                mw.visitVarInsn(ALOAD, 0);
                mw.visitVarInsn(ALOAD, 1);
                mw.visitVarInsn(ALOAD, 2);
                mw.visitVarInsn(ALOAD, 3);
                mw.visitVarInsn(ALOAD, 4);
                mw.visitVarInsn(ILOAD, 5);
                mw.visitMethodInsn(INVOKEVIRTUAL, classNameType,
                                   "writeNormal", "(L" + JSONSerializer
                                                  + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                mw.visitInsn(RETURN);

                mw.visitLabel(_directElse);
                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.DisableCircularReferenceDetect.mask);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "isEnabled", "(I)Z");
                mw.visitJumpInsn(IFEQ, _direct);

                mw.visitVarInsn(ALOAD, 0);
                mw.visitVarInsn(ALOAD, 1);
                mw.visitVarInsn(ALOAD, 2);
                mw.visitVarInsn(ALOAD, 3);
                mw.visitVarInsn(ALOAD, 4);
                mw.visitVarInsn(ILOAD, 5);
                mw.visitMethodInsn(INVOKEVIRTUAL, classNameType, "writeDirectNonContext",
                                   "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                mw.visitInsn(RETURN);

                mw.visitLabel(_direct);
            }

            mw.visitVarInsn(ALOAD, Context.obj); // obj
            mw.visitTypeInsn(CHECKCAST, type(clazz)); // serializer
            mw.visitVarInsn(ASTORE, context.var("entity")); // obj
            generateWriteMethod(clazz, mw, getters, context);
            mw.visitInsn(RETURN);
            mw.visitMaxs(7, context.variantIndex + 2);
            mw.visitEnd();
        }

        if (!nativeSorted) {
            // sortField support
            Context context = new Context(getters, beanInfo, classNameType, false,
                                          DisableCircularReferenceDetect);

            mw = new com.kangaroo.internal.fastjson.asm.MethodWriter(cw, ACC_PUBLIC, "writeUnsorted",
                                  "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V",
                                  null, new String[] { "java/io/IOException" });

            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitFieldInsn(GETFIELD, JSONSerializer, "out", SerializeWriter_desc);
            mw.visitVarInsn(ASTORE, context.var("out"));

            mw.visitVarInsn(ALOAD, Context.obj); // obj
            mw.visitTypeInsn(CHECKCAST, type(clazz)); // serializer
            mw.visitVarInsn(ASTORE, context.var("entity")); // obj

            generateWriteMethod(clazz, mw, unsortedGetters, context);

            mw.visitInsn(RETURN);
            mw.visitMaxs(7, context.variantIndex + 2);
            mw.visitEnd();
        }

        // 0 writeAsArray
        // 1 writeAsArrayNormal
        // 2 writeAsArrayNonContext
        for (int i = 0; i < 3; ++i) {
            String methodName;
            boolean nonContext = DisableCircularReferenceDetect;
            boolean writeDirect = false;
            if (i == 0) {
                methodName = "writeAsArray";
                writeDirect = true;
            } else if (i == 1) {
                methodName = "writeAsArrayNormal";
            } else {
                writeDirect = true;
                nonContext = true;
                methodName = "writeAsArrayNonContext";
            }

            Context context = new Context(getters, beanInfo, classNameType, writeDirect,
                                          nonContext);

            mw = new com.kangaroo.internal.fastjson.asm.MethodWriter(cw, ACC_PUBLIC, methodName,
                                  "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V",
                                  null, new String[] { "java/io/IOException" });

            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitFieldInsn(GETFIELD, JSONSerializer, "out", SerializeWriter_desc);
            mw.visitVarInsn(ASTORE, context.var("out"));

            mw.visitVarInsn(ALOAD, Context.obj); // obj
            mw.visitTypeInsn(CHECKCAST, type(clazz)); // serializer
            mw.visitVarInsn(ASTORE, context.var("entity")); // obj
            generateWriteAsArray(clazz, mw, getters, context);
            mw.visitInsn(RETURN);
            mw.visitMaxs(7, context.variantIndex + 2);
            mw.visitEnd();
        }

        byte[] code = cw.toByteArray();

        Class<?> serializerClass = classLoader.defineClassPublic(classNameFull, code, 0, code.length);
        Constructor<?> constructor = serializerClass.getConstructor(com.kangaroo.internal.fastjson.serializer.SerializeBeanInfo.class);
        Object instance = constructor.newInstance(beanInfo);

        return (com.kangaroo.internal.fastjson.serializer.JavaBeanSerializer) instance;
    }

    private void generateWriteAsArray(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo[] getters,
                                      Context context) throws Exception {

        com.kangaroo.internal.fastjson.asm.Label nonPropertyFilters_ = new com.kangaroo.internal.fastjson.asm.Label();
        mw.visitVarInsn(ALOAD, Context.serializer);
        mw.visitVarInsn(ALOAD, 0);
        mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "hasPropertyFilters", "(" + SerializeFilterable_desc + ")Z");
        mw.visitJumpInsn(IFNE, nonPropertyFilters_);
        mw.visitVarInsn(ALOAD, 0);
        mw.visitVarInsn(ALOAD, 1);
        mw.visitVarInsn(ALOAD, 2);
        mw.visitVarInsn(ALOAD, 3);
        mw.visitVarInsn(ALOAD, 4);
        mw.visitVarInsn(ILOAD, 5);
        mw.visitMethodInsn(INVOKESPECIAL, JavaBeanSerializer,
                "writeNoneASM", "(L" + JSONSerializer
                        + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
        mw.visitInsn(RETURN);

        mw.visitLabel(nonPropertyFilters_);
        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(BIPUSH, '[');
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

        int size = getters.length;

        if (size == 0) {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(BIPUSH, ']');
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            return;
        }

        for (int i = 0; i < size; ++i) {
            final char seperator = (i == size - 1) ? ']' : ',';

            FieldInfo fieldInfo = getters[i];
            Class<?> fieldClass = fieldInfo.fieldClass;

            mw.visitLdcInsn(fieldInfo.name);
            mw.visitVarInsn(ASTORE, Context.fieldName);

            if (fieldClass == byte.class //
                || fieldClass == short.class //
                || fieldClass == int.class) {

                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitInsn(DUP);
                _get(mw, context, fieldInfo);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeInt", "(I)V");
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            } else if (fieldClass == long.class) {
                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitInsn(DUP);
                _get(mw, context, fieldInfo);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeLong", "(J)V");
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            } else if (fieldClass == float.class) {
                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitInsn(DUP);
                _get(mw, context, fieldInfo);
                mw.visitInsn(ICONST_1);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFloat", "(FZ)V");
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            } else if (fieldClass == double.class) {
                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitInsn(DUP);
                _get(mw, context, fieldInfo);
                mw.visitInsn(ICONST_1);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeDouble", "(DZ)V");
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            } else if (fieldClass == boolean.class) {
                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitInsn(DUP);
                _get(mw, context, fieldInfo);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(Z)V");
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            } else if (fieldClass == char.class) {
                mw.visitVarInsn(ALOAD, context.var("out"));
                _get(mw, context, fieldInfo); // Character.toString(value)
                mw.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "toString", "(C)Ljava/lang/String;");
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeString", "(Ljava/lang/String;C)V");

            } else if (fieldClass == String.class) {
                mw.visitVarInsn(ALOAD, context.var("out"));
                _get(mw, context, fieldInfo);
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeString", "(Ljava/lang/String;C)V");
            } else if (fieldClass.isEnum()) {
                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitInsn(DUP);
                _get(mw, context, fieldInfo);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeEnum", "(Ljava/lang/Enum;)V");
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            } else if (List.class.isAssignableFrom(fieldClass)) {
                Type fieldType = fieldInfo.fieldType;

                Type elementType;
                if (fieldType instanceof Class) {
                    elementType = Object.class;
                } else {
                    elementType = ((ParameterizedType) fieldType).getActualTypeArguments()[0];
                }

                Class<?> elementClass = null;
                if (elementType instanceof Class<?>) {
                    elementClass = (Class<?>) elementType;

                    if (elementClass == Object.class) {
                        elementClass = null;
                    }
                }
                
                _get(mw, context, fieldInfo);
                mw.visitTypeInsn(CHECKCAST, "java/util/List"); // cast
                mw.visitVarInsn(ASTORE, context.var("list"));

                if (elementClass == String.class //
                    && context.writeDirect) {
                    mw.visitVarInsn(ALOAD, context.var("out"));
                    mw.visitVarInsn(ALOAD, context.var("list"));
                    mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(Ljava/util/List;)V");
                } else {
                    com.kangaroo.internal.fastjson.asm.Label nullEnd_ = new com.kangaroo.internal.fastjson.asm.Label(), nullElse_ = new com.kangaroo.internal.fastjson.asm.Label();

                    mw.visitVarInsn(ALOAD, context.var("list"));
                    mw.visitJumpInsn(IFNONNULL, nullElse_);

                    mw.visitVarInsn(ALOAD, context.var("out"));
                    mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeNull", "()V");
                    mw.visitJumpInsn(GOTO, nullEnd_);

                    mw.visitLabel(nullElse_);

                    mw.visitVarInsn(ALOAD, context.var("list"));
                    mw.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
                    mw.visitVarInsn(ISTORE, context.var("size"));

                    mw.visitVarInsn(ALOAD, context.var("out"));
                    mw.visitVarInsn(BIPUSH, '[');
                    mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

                    com.kangaroo.internal.fastjson.asm.Label for_ = new com.kangaroo.internal.fastjson.asm.Label(), forFirst_ = new com.kangaroo.internal.fastjson.asm.Label(), forEnd_ = new com.kangaroo.internal.fastjson.asm.Label();

                    mw.visitInsn(ICONST_0);
                    mw.visitVarInsn(ISTORE, context.var("i"));

                    // for (; i < list.size() -1; ++i) {
                    mw.visitLabel(for_);
                    mw.visitVarInsn(ILOAD, context.var("i"));
                    mw.visitVarInsn(ILOAD, context.var("size"));
                    mw.visitJumpInsn(IF_ICMPGE, forEnd_); // i < list.size - 1

                    mw.visitVarInsn(ILOAD, context.var("i"));
                    mw.visitJumpInsn(IFEQ, forFirst_); // i < list.size - 1

                    mw.visitVarInsn(ALOAD, context.var("out"));
                    mw.visitVarInsn(BIPUSH, ',');
                    mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

                    mw.visitLabel(forFirst_);

                    mw.visitVarInsn(ALOAD, context.var("list"));
                    mw.visitVarInsn(ILOAD, context.var("i"));
                    mw.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");
                    mw.visitVarInsn(ASTORE, context.var("list_item"));

                    com.kangaroo.internal.fastjson.asm.Label forItemNullEnd_ = new com.kangaroo.internal.fastjson.asm.Label(), forItemNullElse_ = new com.kangaroo.internal.fastjson.asm.Label();

                    mw.visitVarInsn(ALOAD, context.var("list_item"));
                    mw.visitJumpInsn(IFNONNULL, forItemNullElse_);

                    mw.visitVarInsn(ALOAD, context.var("out"));
                    mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeNull", "()V");
                    mw.visitJumpInsn(GOTO, forItemNullEnd_);

                    mw.visitLabel(forItemNullElse_);

                    com.kangaroo.internal.fastjson.asm.Label forItemClassIfEnd_ = new com.kangaroo.internal.fastjson.asm.Label(), forItemClassIfElse_ = new com.kangaroo.internal.fastjson.asm.Label();
                    if (elementClass != null && Modifier.isPublic(elementClass.getModifiers())) {
                        mw.visitVarInsn(ALOAD, context.var("list_item"));
                        mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                        mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(elementClass)));
                        mw.visitJumpInsn(IF_ACMPNE, forItemClassIfElse_);

                        _getListFieldItemSer(context, mw, fieldInfo, elementClass);
                        mw.visitVarInsn(ASTORE, context.var("list_item_desc"));

                        com.kangaroo.internal.fastjson.asm.Label instanceOfElse_ = new com.kangaroo.internal.fastjson.asm.Label(), instanceOfEnd_ = new com.kangaroo.internal.fastjson.asm.Label();

                        if (context.writeDirect) {
                            mw.visitVarInsn(ALOAD, context.var("list_item_desc"));
                            mw.visitTypeInsn(INSTANCEOF, JavaBeanSerializer);
                            mw.visitJumpInsn(IFEQ, instanceOfElse_);

                            mw.visitVarInsn(ALOAD, context.var("list_item_desc"));
                            mw.visitTypeInsn(CHECKCAST, JavaBeanSerializer); // cast
                            mw.visitVarInsn(ALOAD, Context.serializer);
                            mw.visitVarInsn(ALOAD, context.var("list_item")); // object
                            if (context.nonContext) { // fieldName
                                mw.visitInsn(ACONST_NULL);
                            } else {
                                mw.visitVarInsn(ILOAD, context.var("i"));
                                mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf",
                                                   "(I)Ljava/lang/Integer;");
                            }
                            mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(elementClass))); // fieldType
                            mw.visitLdcInsn(fieldInfo.serialzeFeatures); // features
                            mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "writeAsArrayNonContext", //
                                               "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                            mw.visitJumpInsn(GOTO, instanceOfEnd_);

                            mw.visitLabel(instanceOfElse_);
                        }

                        mw.visitVarInsn(ALOAD, context.var("list_item_desc"));
                        mw.visitVarInsn(ALOAD, Context.serializer);
                        mw.visitVarInsn(ALOAD, context.var("list_item")); // object
                        if (context.nonContext) { // fieldName
                            mw.visitInsn(ACONST_NULL);
                        } else {
                            mw.visitVarInsn(ILOAD, context.var("i"));
                            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
                        }
                        mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(elementClass))); // fieldType
                        mw.visitLdcInsn(fieldInfo.serialzeFeatures); // features
                        mw.visitMethodInsn(INVOKEINTERFACE, ObjectSerializer, "write", //
                                           "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                        mw.visitLabel(instanceOfEnd_);
                        mw.visitJumpInsn(GOTO, forItemClassIfEnd_);
                    }

                    mw.visitLabel(forItemClassIfElse_);
                    mw.visitVarInsn(ALOAD, Context.serializer);
                    mw.visitVarInsn(ALOAD, context.var("list_item"));
                    if (context.nonContext) {
                        mw.visitInsn(ACONST_NULL);
                    } else {
                        mw.visitVarInsn(ILOAD, context.var("i"));
                        mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
                    }
                    if (elementClass != null && Modifier.isPublic(elementClass.getModifiers())) {
                        mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc((Class<?>) elementType)));
                        mw.visitLdcInsn(fieldInfo.serialzeFeatures);
                        mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFieldName",
                                           "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                    } else {
                        mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFieldName",
                                           "(Ljava/lang/Object;Ljava/lang/Object;)V");
                    }
                    mw.visitLabel(forItemClassIfEnd_);
                    mw.visitLabel(forItemNullEnd_);

                    mw.visitIincInsn(context.var("i"), 1);
                    mw.visitJumpInsn(GOTO, for_);

                    mw.visitLabel(forEnd_);

                    mw.visitVarInsn(ALOAD, context.var("out"));
                    mw.visitVarInsn(BIPUSH, ']');
                    mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

                    mw.visitLabel(nullEnd_);
                }

                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            } else {
                com.kangaroo.internal.fastjson.asm.Label notNullEnd_ = new com.kangaroo.internal.fastjson.asm.Label(), notNullElse_ = new com.kangaroo.internal.fastjson.asm.Label();

                _get(mw, context, fieldInfo);
                mw.visitInsn(DUP);
                mw.visitVarInsn(ASTORE, context.var("field_" + fieldInfo.fieldClass.getName()));
                mw.visitJumpInsn(IFNONNULL, notNullElse_);

                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeNull", "()V");
                mw.visitJumpInsn(GOTO, notNullEnd_);

                mw.visitLabel(notNullElse_);

                com.kangaroo.internal.fastjson.asm.Label classIfEnd_ = new com.kangaroo.internal.fastjson.asm.Label(), classIfElse_ = new com.kangaroo.internal.fastjson.asm.Label();
                mw.visitVarInsn(ALOAD, context.var("field_" + fieldInfo.fieldClass.getName()));
                mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(fieldClass)));
                mw.visitJumpInsn(IF_ACMPNE, classIfElse_);

                _getFieldSer(context, mw, fieldInfo);
                mw.visitVarInsn(ASTORE, context.var("fied_ser"));

                com.kangaroo.internal.fastjson.asm.Label instanceOfElse_ = new com.kangaroo.internal.fastjson.asm.Label(), instanceOfEnd_ = new com.kangaroo.internal.fastjson.asm.Label();
                if (context.writeDirect && Modifier.isPublic(fieldClass.getModifiers())) {
                    mw.visitVarInsn(ALOAD, context.var("fied_ser"));
                    mw.visitTypeInsn(INSTANCEOF, JavaBeanSerializer);
                    mw.visitJumpInsn(IFEQ, instanceOfElse_);

                    mw.visitVarInsn(ALOAD, context.var("fied_ser"));
                    mw.visitTypeInsn(CHECKCAST, JavaBeanSerializer); // cast
                    mw.visitVarInsn(ALOAD, Context.serializer);
                    mw.visitVarInsn(ALOAD, context.var("field_" + fieldInfo.fieldClass.getName()));
                    mw.visitVarInsn(ALOAD, Context.fieldName);
                    mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(fieldClass))); // fieldType
                    mw.visitLdcInsn(fieldInfo.serialzeFeatures); // features
                    mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "writeAsArrayNonContext", //
                                       "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                    mw.visitJumpInsn(GOTO, instanceOfEnd_);

                    mw.visitLabel(instanceOfElse_);
                }
                mw.visitVarInsn(ALOAD, context.var("fied_ser"));
                mw.visitVarInsn(ALOAD, Context.serializer);
                mw.visitVarInsn(ALOAD, context.var("field_" + fieldInfo.fieldClass.getName()));
                mw.visitVarInsn(ALOAD, Context.fieldName);
                mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(fieldClass))); // fieldType
                mw.visitLdcInsn(fieldInfo.serialzeFeatures); // features
                mw.visitMethodInsn(INVOKEINTERFACE, ObjectSerializer, "write", //
                                   "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                mw.visitLabel(instanceOfEnd_);
                mw.visitJumpInsn(GOTO, classIfEnd_);

                mw.visitLabel(classIfElse_);
                String format = fieldInfo.getFormat();

                mw.visitVarInsn(ALOAD, Context.serializer);
                mw.visitVarInsn(ALOAD, context.var("field_" + fieldInfo.fieldClass.getName()));
                if (format != null) {
                    mw.visitLdcInsn(format);
                    mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFormat",
                                       "(Ljava/lang/Object;Ljava/lang/String;)V");
                } else {
                    mw.visitVarInsn(ALOAD, Context.fieldName);
                    if (fieldInfo.fieldType instanceof Class<?> //
                        && ((Class<?>) fieldInfo.fieldType).isPrimitive()) {
                        mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFieldName",
                                           "(Ljava/lang/Object;Ljava/lang/Object;)V");
                    } else {
                        mw.visitVarInsn(ALOAD, 0); // this
                        mw.visitFieldInsn(GETFIELD, context.className, fieldInfo.name + "_asm_fieldType",
                                          "Ljava/lang/reflect/Type;");
                        mw.visitLdcInsn(fieldInfo.serialzeFeatures);

                        mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFieldName",
                                           "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                    }
                }
                mw.visitLabel(classIfEnd_);
                mw.visitLabel(notNullEnd_);
                

                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitVarInsn(BIPUSH, seperator);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            }
        }
    }

    private void generateWriteMethod(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo[] getters,
                                     Context context) throws Exception {

        // if (serializer.containsReference(object)) {
        com.kangaroo.internal.fastjson.asm.Label end = new com.kangaroo.internal.fastjson.asm.Label();

        int size = getters.length;

        if (!context.writeDirect) {
            // pretty format not byte code optimized
            com.kangaroo.internal.fastjson.asm.Label endSupper_ = new com.kangaroo.internal.fastjson.asm.Label();
            com.kangaroo.internal.fastjson.asm.Label supper_ = new com.kangaroo.internal.fastjson.asm.Label();
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.PrettyFormat.mask);
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "isEnabled", "(I)Z");
            mw.visitJumpInsn(IFNE, supper_);

            boolean hasMethod = false;
            for (FieldInfo getter : getters) {
                if (getter.method != null) {
                    hasMethod = true;
                }
            }

            if (hasMethod) {
                mw.visitVarInsn(ALOAD, context.var("out"));
                mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.IgnoreErrorGetter.mask);
                mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "isEnabled", "(I)Z");
                mw.visitJumpInsn(IFEQ, endSupper_);
            } else {
                mw.visitJumpInsn(GOTO, endSupper_);
            }

            mw.visitLabel(supper_);
            mw.visitVarInsn(ALOAD, 0);
            mw.visitVarInsn(ALOAD, 1);
            mw.visitVarInsn(ALOAD, 2);
            mw.visitVarInsn(ALOAD, 3);
            mw.visitVarInsn(ALOAD, 4);
            mw.visitVarInsn(ILOAD, 5);
            mw.visitMethodInsn(INVOKESPECIAL, JavaBeanSerializer,
                               "write", "(L" + JSONSerializer
                                        + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
            mw.visitInsn(RETURN);

            mw.visitLabel(endSupper_);
        }

        if (!context.nonContext) {
            com.kangaroo.internal.fastjson.asm.Label endRef_ = new com.kangaroo.internal.fastjson.asm.Label();

            // /////
            mw.visitVarInsn(ALOAD, 0); // this
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, Context.obj);
            mw.visitVarInsn(ILOAD, Context.features);
            mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "writeReference",
                               "(L" + JSONSerializer + ";Ljava/lang/Object;I)Z");

            mw.visitJumpInsn(IFEQ, endRef_);

            mw.visitInsn(RETURN);

            mw.visitLabel(endRef_);
        }

        final String writeAsArrayMethodName;

        if (context.writeDirect) {
            if (context.nonContext) {
                writeAsArrayMethodName = "writeAsArrayNonContext";
            } else {
                writeAsArrayMethodName = "writeAsArray";
            }
        } else {
            writeAsArrayMethodName = "writeAsArrayNormal";
        }

        if ((context.beanInfo.features & com.kangaroo.internal.fastjson.serializer.SerializerFeature.BeanToArray.mask) == 0) {
            com.kangaroo.internal.fastjson.asm.Label endWriteAsArray_ = new com.kangaroo.internal.fastjson.asm.Label();

            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.BeanToArray.mask);
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "isEnabled", "(I)Z");
            mw.visitJumpInsn(IFEQ, endWriteAsArray_);

            // /////
            mw.visitVarInsn(ALOAD, 0); // this
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, 2); // obj
            mw.visitVarInsn(ALOAD, 3); // fieldObj
            mw.visitVarInsn(ALOAD, 4); // fieldType
            mw.visitVarInsn(ILOAD, 5); // features
            mw.visitMethodInsn(INVOKEVIRTUAL, //
                               context.className, //
                               writeAsArrayMethodName, //
                               "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");

            mw.visitInsn(RETURN);

            mw.visitLabel(endWriteAsArray_);
        } else {
            mw.visitVarInsn(ALOAD, 0); // this
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, 2); // obj
            mw.visitVarInsn(ALOAD, 3); // fieldObj
            mw.visitVarInsn(ALOAD, 4); // fieldType
            mw.visitVarInsn(ILOAD, 5); // features
            mw.visitMethodInsn(INVOKEVIRTUAL, //
                               context.className, //
                               writeAsArrayMethodName, //
                               "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
            mw.visitInsn(RETURN);
        }

        if (!context.nonContext) {
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "getContext", "()" + SerialContext_desc);
            mw.visitVarInsn(ASTORE, context.var("parent"));

            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, context.var("parent"));
            mw.visitVarInsn(ALOAD, Context.obj);
            mw.visitVarInsn(ALOAD, Context.paramFieldName);
            mw.visitLdcInsn(context.beanInfo.features);
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "setContext",
                               "(" + SerialContext_desc + "Ljava/lang/Object;Ljava/lang/Object;I)V");
        }

        boolean writeClasName = (context.beanInfo.features & com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteClassName.mask) != 0;

        // SEPERATO
        if (writeClasName || !context.writeDirect) {
            com.kangaroo.internal.fastjson.asm.Label end_ = new com.kangaroo.internal.fastjson.asm.Label();
            com.kangaroo.internal.fastjson.asm.Label else_ = new com.kangaroo.internal.fastjson.asm.Label();
            com.kangaroo.internal.fastjson.asm.Label writeClass_ = new com.kangaroo.internal.fastjson.asm.Label();

            if (!writeClasName) {
                mw.visitVarInsn(ALOAD, Context.serializer);
                mw.visitVarInsn(ALOAD, Context.paramFieldType);
                mw.visitVarInsn(ALOAD, Context.obj);
                mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "isWriteClassName",
                        "(Ljava/lang/reflect/Type;Ljava/lang/Object;)Z");
                mw.visitJumpInsn(IFEQ, else_);
            }

            // IFNULL
            mw.visitVarInsn(ALOAD, Context.paramFieldType);
            mw.visitVarInsn(ALOAD, Context.obj);
            mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
            mw.visitJumpInsn(IF_ACMPEQ, else_);

            mw.visitLabel(writeClass_);
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(BIPUSH, '{');
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            
            mw.visitVarInsn(ALOAD, 0);
            mw.visitVarInsn(ALOAD, Context.serializer);
            if (context.beanInfo.typeKey != null) {
                mw.visitLdcInsn(context.beanInfo.typeKey);
            } else {
                mw.visitInsn(ACONST_NULL);
            }
            mw.visitVarInsn(ALOAD, Context.obj);

            mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "writeClassName", "(L" + JSONSerializer + ";Ljava/lang/String;Ljava/lang/Object;)V");
            mw.visitVarInsn(BIPUSH, ',');
            mw.visitJumpInsn(GOTO, end_);

            mw.visitLabel(else_);
            mw.visitVarInsn(BIPUSH, '{');

            mw.visitLabel(end_);
        } else {
            mw.visitVarInsn(BIPUSH, '{');
        }

        mw.visitVarInsn(ISTORE, context.var("seperator"));

        if (!context.writeDirect) {
            _before(mw, context);
        }

        if (!context.writeDirect) {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "isNotWriteDefaultValue", "()Z");
            mw.visitVarInsn(ISTORE, context.var("notWriteDefaultValue"));

            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, 0);
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "checkValue", "(" + SerializeFilterable_desc + ")Z");
            mw.visitVarInsn(ISTORE, context.var("checkValue"));

            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, 0);
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "hasNameFilters", "(" + SerializeFilterable_desc + ")Z");
            mw.visitVarInsn(ISTORE, context.var("hasNameFilters"));
        }

        for (int i = 0; i < size; ++i) {
            FieldInfo property = getters[i];
            Class<?> propertyClass = property.fieldClass;

            mw.visitLdcInsn(property.name);
            mw.visitVarInsn(ASTORE, Context.fieldName);

            if (propertyClass == byte.class //
                || propertyClass == short.class //
                || propertyClass == int.class) {
                _int(clazz, mw, property, context, context.var(propertyClass.getName()), 'I');
            } else if (propertyClass == long.class) {
                _long(clazz, mw, property, context);
            } else if (propertyClass == float.class) {
                _float(clazz, mw, property, context);
            } else if (propertyClass == double.class) {
                _double(clazz, mw, property, context);
            } else if (propertyClass == boolean.class) {
                _int(clazz, mw, property, context, context.var("boolean"), 'Z');
            } else if (propertyClass == char.class) {
                _int(clazz, mw, property, context, context.var("char"), 'C');
            } else if (propertyClass == String.class) {
                _string(clazz, mw, property, context);
            } else if (propertyClass == BigDecimal.class) {
                _decimal(clazz, mw, property, context);
            } else if (List.class.isAssignableFrom(propertyClass)) {
                _list(clazz, mw, property, context);
            } else if (propertyClass.isEnum()) {
                _enum(clazz, mw, property, context);
            } else {
                _object(clazz, mw, property, context);
            }
        }

        if (!context.writeDirect) {
            _after(mw, context);
        }

        com.kangaroo.internal.fastjson.asm.Label _else = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label _end_if = new com.kangaroo.internal.fastjson.asm.Label();

        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitIntInsn(BIPUSH, '{');
        mw.visitJumpInsn(IF_ICMPNE, _else);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(BIPUSH, '{');
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

        mw.visitLabel(_else);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(BIPUSH, '}');
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

        mw.visitLabel(_end_if);
        mw.visitLabel(end);

        if (!context.nonContext) {
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, context.var("parent"));
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "setContext", "(" + SerialContext_desc + ")V");
        }

    }

    private void _object(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context) {
        com.kangaroo.internal.fastjson.asm.Label _end = new com.kangaroo.internal.fastjson.asm.Label();

        _nameApply(mw, property, context, _end);
        _get(mw, context, property);
        mw.visitVarInsn(ASTORE, context.var("object"));

        _filters(mw, property, context, _end);

        _writeObject(mw, property, context, _end);

        mw.visitLabel(_end);
    }

    private void _enum(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo fieldInfo, Context context) {
        com.kangaroo.internal.fastjson.asm.Label _not_null = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label _end_if = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label _end = new com.kangaroo.internal.fastjson.asm.Label();

        _nameApply(mw, fieldInfo, context, _end);
        _get(mw, context, fieldInfo);
        mw.visitTypeInsn(CHECKCAST, "java/lang/Enum"); // cast
        mw.visitVarInsn(ASTORE, context.var("enum"));

        _filters(mw, fieldInfo, context, _end);

        mw.visitVarInsn(ALOAD, context.var("enum"));
        mw.visitJumpInsn(IFNONNULL, _not_null);
        _if_write_null(mw, fieldInfo, context);
        mw.visitJumpInsn(GOTO, _end_if);

        mw.visitLabel(_not_null);

       if (context.writeDirect) {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(ILOAD, context.var("seperator"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitVarInsn(ALOAD, context.var("enum"));
            mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Enum", "name", "()Ljava/lang/String;");
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldValueStringWithDoubleQuote",
                               "(CLjava/lang/String;Ljava/lang/String;)V");
        } else {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(ILOAD, context.var("seperator"));
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
            
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitInsn(ICONST_0);
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldName", "(Ljava/lang/String;Z)V");
            
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, context.var("enum"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc((Class<?>) fieldInfo.fieldClass)));
            mw.visitLdcInsn(fieldInfo.serialzeFeatures);
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFieldName",
                    "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
        }

        _seperator(mw, context);

        mw.visitLabel(_end_if);
        mw.visitLabel(_end);
    }
    
    private void _int(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context, int var, char type) {
        com.kangaroo.internal.fastjson.asm.Label end_ = new com.kangaroo.internal.fastjson.asm.Label();

        _nameApply(mw, property, context, end_);
        _get(mw, context, property);
        mw.visitVarInsn(ISTORE, var);

        _filters(mw, property, context, end_);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitVarInsn(ALOAD, Context.fieldName);
        mw.visitVarInsn(ILOAD, var);

        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldValue", "(CLjava/lang/String;" + type + ")V");

        _seperator(mw, context);

        mw.visitLabel(end_);
    }

    private void _long(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context) {
        com.kangaroo.internal.fastjson.asm.Label end_ = new com.kangaroo.internal.fastjson.asm.Label();

        _nameApply(mw, property, context, end_);
        _get(mw, context, property);
        mw.visitVarInsn(LSTORE, context.var("long", 2));

        _filters(mw, property, context, end_);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitVarInsn(ALOAD, Context.fieldName);
        mw.visitVarInsn(LLOAD, context.var("long", 2));
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldValue", "(CLjava/lang/String;J)V");

        _seperator(mw, context);

        mw.visitLabel(end_);
    }
    
    private void _float(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context) {
        com.kangaroo.internal.fastjson.asm.Label end_ = new com.kangaroo.internal.fastjson.asm.Label();

        _nameApply(mw, property, context, end_);
        _get(mw, context, property);
        mw.visitVarInsn(FSTORE, context.var("float"));

        _filters(mw, property, context, end_);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitVarInsn(ALOAD, Context.fieldName);
        mw.visitVarInsn(FLOAD, context.var("float"));
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldValue", "(CLjava/lang/String;F)V");

        _seperator(mw, context);

        mw.visitLabel(end_);
    }

    private void _double(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context) {
        com.kangaroo.internal.fastjson.asm.Label end_ = new com.kangaroo.internal.fastjson.asm.Label();

        _nameApply(mw, property, context, end_);
        _get(mw, context, property);
        mw.visitVarInsn(DSTORE, context.var("double", 2));

        _filters(mw, property, context, end_);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitVarInsn(ALOAD, Context.fieldName);
        mw.visitVarInsn(DLOAD, context.var("double", 2));
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldValue", "(CLjava/lang/String;D)V");

        _seperator(mw, context);

        mw.visitLabel(end_);
    }
    
    private void _get(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, Context context, FieldInfo fieldInfo) {
        Method method = fieldInfo.method;
        if (method != null) {
            mw.visitVarInsn(ALOAD, context.var("entity"));
            Class<?> declaringClass = method.getDeclaringClass();
            mw.visitMethodInsn(declaringClass.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL, type(declaringClass), method.getName(), desc(method));
            if (!method.getReturnType().equals(fieldInfo.fieldClass)) {
                mw.visitTypeInsn(CHECKCAST, type(fieldInfo.fieldClass)); // cast
            }
        } else {
            mw.visitVarInsn(ALOAD, context.var("entity"));
            Field field = fieldInfo.field;
            mw.visitFieldInsn(GETFIELD, type(fieldInfo.declaringClass), field.getName(),
                              desc(field.getType()));
            if (!field.getType().equals(fieldInfo.fieldClass)) {
                mw.visitTypeInsn(CHECKCAST, type(fieldInfo.fieldClass)); // cast
            }
        }
    }

    private void _decimal(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context) {
        com.kangaroo.internal.fastjson.asm.Label end_ = new com.kangaroo.internal.fastjson.asm.Label();

        _nameApply(mw, property, context, end_);
        _get(mw, context, property);
        mw.visitVarInsn(ASTORE, context.var("decimal"));

        _filters(mw, property, context, end_);

        com.kangaroo.internal.fastjson.asm.Label if_ = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label else_ = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label endIf_ = new com.kangaroo.internal.fastjson.asm.Label();

        mw.visitLabel(if_);

        // if (decimalValue == null) {
        mw.visitVarInsn(ALOAD, context.var("decimal"));
        mw.visitJumpInsn(IFNONNULL, else_);
        _if_write_null(mw, property, context);
        mw.visitJumpInsn(GOTO, endIf_);

        mw.visitLabel(else_); // else { out.writeFieldValue(seperator, fieldName, fieldValue)

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitVarInsn(ALOAD, Context.fieldName);
        mw.visitVarInsn(ALOAD, context.var("decimal"));
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldValue",
                           "(CLjava/lang/String;Ljava/math/BigDecimal;)V");

        _seperator(mw, context);
        mw.visitJumpInsn(GOTO, endIf_);

        mw.visitLabel(endIf_);

        mw.visitLabel(end_);
    }

    private void _string(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context) {
        com.kangaroo.internal.fastjson.asm.Label end_ = new com.kangaroo.internal.fastjson.asm.Label();

        if (property.name.equals(context.beanInfo.typeKey)) {
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, Context.paramFieldType);
            mw.visitVarInsn(ALOAD, Context.obj);
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "isWriteClassName",
                    "(Ljava/lang/reflect/Type;Ljava/lang/Object;)Z");
            mw.visitJumpInsn(IFNE, end_);
        }

        _nameApply(mw, property, context, end_);
        _get(mw, context, property);
        mw.visitVarInsn(ASTORE, context.var("string"));

        _filters(mw, property, context, end_);

        com.kangaroo.internal.fastjson.asm.Label else_ = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label endIf_ = new com.kangaroo.internal.fastjson.asm.Label();

        // if (value == null) {
        mw.visitVarInsn(ALOAD, context.var("string"));
        mw.visitJumpInsn(IFNONNULL, else_);

        _if_write_null(mw, property, context);

        mw.visitJumpInsn(GOTO, endIf_);

        mw.visitLabel(else_); // else { out.writeFieldValue(seperator, fieldName, fieldValue)


        if ("trim".equals(property.format)) {
            mw.visitVarInsn(ALOAD, context.var("string"));
            mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "trim", "()Ljava/lang/String;");
            mw.visitVarInsn(ASTORE, context.var("string"));
        }

        if (context.writeDirect) {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(ILOAD, context.var("seperator"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitVarInsn(ALOAD, context.var("string"));
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldValueStringWithDoubleQuoteCheck",
                               "(CLjava/lang/String;Ljava/lang/String;)V");
        } else {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(ILOAD, context.var("seperator"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitVarInsn(ALOAD, context.var("string"));
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldValue",
                               "(CLjava/lang/String;Ljava/lang/String;)V");
        }
        _seperator(mw, context);

        mw.visitLabel(endIf_);

        mw.visitLabel(end_);
    }

    private void _list(Class<?> clazz, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo fieldInfo, Context context) {
        Type propertyType = fieldInfo.fieldType;

        Type elementType = TypeUtils.getCollectionItemType(propertyType);

        Class<?> elementClass = null;
        if (elementType instanceof Class<?>) {
            elementClass = (Class<?>) elementType;
        }
        
        if (elementClass == Object.class //
            || elementClass == Serializable.class) {
            elementClass = null;
        }

        com.kangaroo.internal.fastjson.asm.Label end_ = new com.kangaroo.internal.fastjson.asm.Label(), else_ = new com.kangaroo.internal.fastjson.asm.Label(), endIf_ = new com.kangaroo.internal.fastjson.asm.Label();

        _nameApply(mw, fieldInfo, context, end_);
        _get(mw, context, fieldInfo);
        mw.visitTypeInsn(CHECKCAST, "java/util/List"); // cast
        mw.visitVarInsn(ASTORE, context.var("list"));

        _filters(mw, fieldInfo, context, end_);

        mw.visitVarInsn(ALOAD, context.var("list"));
        mw.visitJumpInsn(IFNONNULL, else_);
        _if_write_null(mw, fieldInfo, context);
        mw.visitJumpInsn(GOTO, endIf_);

        mw.visitLabel(else_); // else {

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

        _writeFieldName(mw, context);

        //
        mw.visitVarInsn(ALOAD, context.var("list"));
        mw.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
        mw.visitVarInsn(ISTORE, context.var("size"));

        com.kangaroo.internal.fastjson.asm.Label _else_3 = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label _end_if_3 = new com.kangaroo.internal.fastjson.asm.Label();

        mw.visitVarInsn(ILOAD, context.var("size"));
        mw.visitInsn(ICONST_0);
        mw.visitJumpInsn(IF_ICMPNE, _else_3);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitLdcInsn("[]");
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(Ljava/lang/String;)V");

        mw.visitJumpInsn(GOTO, _end_if_3);

        mw.visitLabel(_else_3);

        if (!context.nonContext) {
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, context.var("list"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "setContext", "(Ljava/lang/Object;Ljava/lang/Object;)V");
        }

        if (elementType == String.class //
            && context.writeDirect) {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(ALOAD, context.var("list"));
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(Ljava/util/List;)V");
        } else {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(BIPUSH, '[');
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

            com.kangaroo.internal.fastjson.asm.Label for_ = new com.kangaroo.internal.fastjson.asm.Label(), forFirst_ = new com.kangaroo.internal.fastjson.asm.Label(), forEnd_ = new com.kangaroo.internal.fastjson.asm.Label();

            mw.visitInsn(ICONST_0);
            mw.visitVarInsn(ISTORE, context.var("i"));

            // for (; i < list.size() -1; ++i) {
            mw.visitLabel(for_);
            mw.visitVarInsn(ILOAD, context.var("i"));
            mw.visitVarInsn(ILOAD, context.var("size"));
            mw.visitJumpInsn(IF_ICMPGE, forEnd_); // i < list.size - 1

            mw.visitVarInsn(ILOAD, context.var("i"));
            mw.visitJumpInsn(IFEQ, forFirst_); // i < list.size - 1

            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(BIPUSH, ',');
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

            mw.visitLabel(forFirst_);

            mw.visitVarInsn(ALOAD, context.var("list"));
            mw.visitVarInsn(ILOAD, context.var("i"));
            mw.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");
            mw.visitVarInsn(ASTORE, context.var("list_item"));

            com.kangaroo.internal.fastjson.asm.Label forItemNullEnd_ = new com.kangaroo.internal.fastjson.asm.Label(), forItemNullElse_ = new com.kangaroo.internal.fastjson.asm.Label();

            mw.visitVarInsn(ALOAD, context.var("list_item"));
            mw.visitJumpInsn(IFNONNULL, forItemNullElse_);

            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeNull", "()V");
            mw.visitJumpInsn(GOTO, forItemNullEnd_);

            mw.visitLabel(forItemNullElse_);

            com.kangaroo.internal.fastjson.asm.Label forItemClassIfEnd_ = new com.kangaroo.internal.fastjson.asm.Label(), forItemClassIfElse_ = new com.kangaroo.internal.fastjson.asm.Label();
            if (elementClass != null && Modifier.isPublic(elementClass.getModifiers())) {
                mw.visitVarInsn(ALOAD, context.var("list_item"));
                mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
                mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(elementClass)));
                mw.visitJumpInsn(IF_ACMPNE, forItemClassIfElse_);

                _getListFieldItemSer(context, mw, fieldInfo, elementClass);
                // mw.visitInsn(DUP);
                mw.visitVarInsn(ASTORE, context.var("list_item_desc"));

                com.kangaroo.internal.fastjson.asm.Label instanceOfElse_ = new com.kangaroo.internal.fastjson.asm.Label(), instanceOfEnd_ = new com.kangaroo.internal.fastjson.asm.Label();

                if (context.writeDirect) {
                    String writeMethodName = context.nonContext && context.writeDirect ? //
                        "writeDirectNonContext" //
                        : "write";
                    mw.visitVarInsn(ALOAD, context.var("list_item_desc"));
                    mw.visitTypeInsn(INSTANCEOF, JavaBeanSerializer);
                    mw.visitJumpInsn(IFEQ, instanceOfElse_);

                    mw.visitVarInsn(ALOAD, context.var("list_item_desc"));
                    mw.visitTypeInsn(CHECKCAST, JavaBeanSerializer); // cast
                    mw.visitVarInsn(ALOAD, Context.serializer);
                    mw.visitVarInsn(ALOAD, context.var("list_item")); // object
                    if (context.nonContext) { // fieldName
                        mw.visitInsn(ACONST_NULL);
                    } else {
                        mw.visitVarInsn(ILOAD, context.var("i"));
                        mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
                    }
                    mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(elementClass))); // fieldType
                    mw.visitLdcInsn(fieldInfo.serialzeFeatures); // features
                    mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, writeMethodName, //
                                       "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
                    mw.visitJumpInsn(GOTO, instanceOfEnd_);

                    mw.visitLabel(instanceOfElse_);
                }
                mw.visitVarInsn(ALOAD, context.var("list_item_desc"));
                mw.visitVarInsn(ALOAD, Context.serializer);
                mw.visitVarInsn(ALOAD, context.var("list_item")); // object
                if (context.nonContext) { // fieldName
                    mw.visitInsn(ACONST_NULL);
                } else {
                    mw.visitVarInsn(ILOAD, context.var("i"));
                    mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
                }
                mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(elementClass))); // fieldType
                mw.visitLdcInsn(fieldInfo.serialzeFeatures); // features
                mw.visitMethodInsn(INVOKEINTERFACE, ObjectSerializer, "write", //
                                   "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");

                mw.visitLabel(instanceOfEnd_);
                mw.visitJumpInsn(GOTO, forItemClassIfEnd_);
            }

            mw.visitLabel(forItemClassIfElse_);

            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, context.var("list_item"));
            if (context.nonContext) {
                mw.visitInsn(ACONST_NULL);
            } else {
                mw.visitVarInsn(ILOAD, context.var("i"));
                mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            }

            if (elementClass != null && Modifier.isPublic(elementClass.getModifiers())) {
                mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc((Class<?>) elementType)));
                mw.visitLdcInsn(fieldInfo.serialzeFeatures);
                mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFieldName",
                                   "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
            } else {
                mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFieldName",
                                   "(Ljava/lang/Object;Ljava/lang/Object;)V");
            }

            mw.visitLabel(forItemClassIfEnd_);
            mw.visitLabel(forItemNullEnd_);

            mw.visitIincInsn(context.var("i"), 1);
            mw.visitJumpInsn(GOTO, for_);

            mw.visitLabel(forEnd_);

            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(BIPUSH, ']');
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");
        }

        {
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "popContext", "()V");
        }

        mw.visitLabel(_end_if_3);

        _seperator(mw, context);

        mw.visitLabel(endIf_);

        mw.visitLabel(end_);
    }

    private void _filters(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context, com.kangaroo.internal.fastjson.asm.Label _end) {
        if (property.fieldTransient) {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.SkipTransientField.mask);
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "isEnabled", "(I)Z");
            mw.visitJumpInsn(IFNE, _end);
        }

        _notWriteDefault(mw, property, context, _end);

        if (context.writeDirect) {
            return;
        }

        _apply(mw, property, context);
        mw.visitJumpInsn(IFEQ, _end);

        _processKey(mw, property, context);

        _processValue(mw, property, context, _end);
    }

    private void _nameApply(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context, com.kangaroo.internal.fastjson.asm.Label _end) {
        if (!context.writeDirect) {
            mw.visitVarInsn(ALOAD, 0);
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, Context.obj);
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "applyName",
                               "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/String;)Z");
            mw.visitJumpInsn(IFEQ, _end);

            _labelApply(mw, property, context, _end);
        }

        if (property.field == null) {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.IgnoreNonFieldGetter.mask);
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "isEnabled", "(I)Z");

            // if true
            mw.visitJumpInsn(IFNE, _end);
        }
    }

    private void _labelApply(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context, com.kangaroo.internal.fastjson.asm.Label _end) {
        mw.visitVarInsn(ALOAD, 0); // this
        mw.visitVarInsn(ALOAD, Context.serializer);
        mw.visitLdcInsn(property.label);
        mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "applyLabel",
                           "(L" + JSONSerializer + ";Ljava/lang/String;)Z");
        mw.visitJumpInsn(IFEQ, _end);
    }

    private void _writeObject(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo fieldInfo, Context context, com.kangaroo.internal.fastjson.asm.Label _end) {
        String format = fieldInfo.getFormat();
        Class<?> fieldClass = fieldInfo.fieldClass;

        com.kangaroo.internal.fastjson.asm.Label notNull_ = new com.kangaroo.internal.fastjson.asm.Label();

        // if (obj == null)
        if (context.writeDirect) {
            mw.visitVarInsn(ALOAD, context.var("object"));
        } else {
            mw.visitVarInsn(ALOAD, Context.processValue);
        }
        mw.visitInsn(DUP);
        mw.visitVarInsn(ASTORE, context.var("object"));
        mw.visitJumpInsn(IFNONNULL, notNull_);
        _if_write_null(mw, fieldInfo, context);
        mw.visitJumpInsn(GOTO, _end);

        mw.visitLabel(notNull_);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

        _writeFieldName(mw, context);

        com.kangaroo.internal.fastjson.asm.Label classIfEnd_ = new com.kangaroo.internal.fastjson.asm.Label(), classIfElse_ = new com.kangaroo.internal.fastjson.asm.Label();
        if (Modifier.isPublic(fieldClass.getModifiers()) //
            && !ParserConfig.isPrimitive2(fieldClass) //
        ) {
            mw.visitVarInsn(ALOAD, context.var("object"));
            mw.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(fieldClass)));
            mw.visitJumpInsn(IF_ACMPNE, classIfElse_);

            _getFieldSer(context, mw, fieldInfo);
            mw.visitVarInsn(ASTORE, context.var("fied_ser"));

            com.kangaroo.internal.fastjson.asm.Label instanceOfElse_ = new com.kangaroo.internal.fastjson.asm.Label(), instanceOfEnd_ = new com.kangaroo.internal.fastjson.asm.Label();
            mw.visitVarInsn(ALOAD, context.var("fied_ser"));
            mw.visitTypeInsn(INSTANCEOF, JavaBeanSerializer);
            mw.visitJumpInsn(IFEQ, instanceOfElse_);

            boolean disableCircularReferenceDetect = (fieldInfo.serialzeFeatures & com.kangaroo.internal.fastjson.serializer.SerializerFeature.DisableCircularReferenceDetect.mask) != 0;
            boolean fieldBeanToArray = (fieldInfo.serialzeFeatures & com.kangaroo.internal.fastjson.serializer.SerializerFeature.BeanToArray.mask) != 0;
            String writeMethodName;
            if (disableCircularReferenceDetect || (context.nonContext && context.writeDirect)) {
                writeMethodName = fieldBeanToArray ? "writeAsArrayNonContext" : "writeDirectNonContext";
            } else {
                writeMethodName = fieldBeanToArray ? "writeAsArray" : "write";
            }
            
            mw.visitVarInsn(ALOAD, context.var("fied_ser"));
            mw.visitTypeInsn(CHECKCAST, JavaBeanSerializer); // cast
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, context.var("object"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitVarInsn(ALOAD, 0);
            mw.visitFieldInsn(GETFIELD, context.className, fieldInfo.name + "_asm_fieldType",
                              "Ljava/lang/reflect/Type;");
            mw.visitLdcInsn(fieldInfo.serialzeFeatures); // features
            mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, writeMethodName, //
                               "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
            mw.visitJumpInsn(GOTO, instanceOfEnd_);

            mw.visitLabel(instanceOfElse_);

            mw.visitVarInsn(ALOAD, context.var("fied_ser"));
            mw.visitVarInsn(ALOAD, Context.serializer);
            mw.visitVarInsn(ALOAD, context.var("object"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitVarInsn(ALOAD, 0);
            mw.visitFieldInsn(GETFIELD, context.className, fieldInfo.name + "_asm_fieldType",
                              "Ljava/lang/reflect/Type;");
            mw.visitLdcInsn(fieldInfo.serialzeFeatures); // features
            mw.visitMethodInsn(INVOKEINTERFACE, ObjectSerializer, "write", //
                               "(L" + JSONSerializer + ";Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");

            mw.visitLabel(instanceOfEnd_);
            mw.visitJumpInsn(GOTO, classIfEnd_);
        }

        mw.visitLabel(classIfElse_);

        mw.visitVarInsn(ALOAD, Context.serializer);
        if (context.writeDirect) {
            mw.visitVarInsn(ALOAD, context.var("object"));
        } else {
            mw.visitVarInsn(ALOAD, Context.processValue);
        }
        if (format != null) {
            mw.visitLdcInsn(format);
            mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFormat",
                               "(Ljava/lang/Object;Ljava/lang/String;)V");
        } else {
            mw.visitVarInsn(ALOAD, Context.fieldName);
            if (fieldInfo.fieldType instanceof Class<?> //
                && ((Class<?>) fieldInfo.fieldType).isPrimitive()) {
                mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFieldName",
                                   "(Ljava/lang/Object;Ljava/lang/Object;)V");
            } else {
                if (fieldInfo.fieldClass == String.class) {
                    mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(String.class)));
                } else {
                    mw.visitVarInsn(ALOAD, 0);
                    mw.visitFieldInsn(GETFIELD, context.className, fieldInfo.name + "_asm_fieldType",
                                      "Ljava/lang/reflect/Type;");
                }
                mw.visitLdcInsn(fieldInfo.serialzeFeatures);

                mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "writeWithFieldName",
                                   "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/reflect/Type;I)V");
            }
        }
        mw.visitLabel(classIfEnd_);

        _seperator(mw, context);
    }

    private void _before(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, Context context) {
        mw.visitVarInsn(ALOAD, 0); // this
        mw.visitVarInsn(ALOAD, Context.serializer);
        mw.visitVarInsn(ALOAD, Context.obj);
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "writeBefore",
                           "(L" + JSONSerializer + ";Ljava/lang/Object;C)C");
        mw.visitVarInsn(ISTORE, context.var("seperator"));
    }

    private void _after(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, Context context) {
        mw.visitVarInsn(ALOAD, 0); // this
        mw.visitVarInsn(ALOAD, Context.serializer);
        mw.visitVarInsn(ALOAD, 2); // obj
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "writeAfter",
                           "(L" + JSONSerializer + ";Ljava/lang/Object;C)C");
        mw.visitVarInsn(ISTORE, context.var("seperator"));
    }

    private void _notWriteDefault(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context, com.kangaroo.internal.fastjson.asm.Label _end) {
        if (context.writeDirect) {
            return;
        }

        com.kangaroo.internal.fastjson.asm.Label elseLabel = new com.kangaroo.internal.fastjson.asm.Label();

        mw.visitVarInsn(ILOAD, context.var("notWriteDefaultValue"));
        mw.visitJumpInsn(IFEQ, elseLabel);

        Class<?> propertyClass = property.fieldClass;
        if (propertyClass == boolean.class) {
            mw.visitVarInsn(ILOAD, context.var("boolean"));
            mw.visitJumpInsn(IFEQ, _end);
        } else if (propertyClass == byte.class) {
            mw.visitVarInsn(ILOAD, context.var("byte"));
            mw.visitJumpInsn(IFEQ, _end);
        } else if (propertyClass == short.class) {
            mw.visitVarInsn(ILOAD, context.var("short"));
            mw.visitJumpInsn(IFEQ, _end);
        } else if (propertyClass == int.class) {
            mw.visitVarInsn(ILOAD, context.var("int"));
            mw.visitJumpInsn(IFEQ, _end);
        } else if (propertyClass == long.class) {
            mw.visitVarInsn(LLOAD, context.var("long"));
            mw.visitInsn(LCONST_0);
            mw.visitInsn(LCMP);
            mw.visitJumpInsn(IFEQ, _end);
        } else if (propertyClass == float.class) {
            mw.visitVarInsn(FLOAD, context.var("float"));
            mw.visitInsn(FCONST_0);
            mw.visitInsn(FCMPL);
            mw.visitJumpInsn(IFEQ, _end);
        } else if (propertyClass == double.class) {
            mw.visitVarInsn(DLOAD, context.var("double"));
            mw.visitInsn(DCONST_0);
            mw.visitInsn(DCMPL);
            mw.visitJumpInsn(IFEQ, _end);
        }

        mw.visitLabel(elseLabel);
    }

    private void _apply(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context) {
        Class<?> propertyClass = property.fieldClass;

        mw.visitVarInsn(ALOAD, 0); // this
        mw.visitVarInsn(ALOAD, Context.serializer);
        mw.visitVarInsn(ALOAD, Context.obj);
        mw.visitVarInsn(ALOAD, Context.fieldName);

        if (propertyClass == byte.class) {
            mw.visitVarInsn(ILOAD, context.var("byte"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
        } else if (propertyClass == short.class) {
            mw.visitVarInsn(ILOAD, context.var("short"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
        } else if (propertyClass == int.class) {
            mw.visitVarInsn(ILOAD, context.var("int"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
        } else if (propertyClass == char.class) {
            mw.visitVarInsn(ILOAD, context.var("char"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
        } else if (propertyClass == long.class) {
            mw.visitVarInsn(LLOAD, context.var("long", 2));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
        } else if (propertyClass == float.class) {
            mw.visitVarInsn(FLOAD, context.var("float"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
        } else if (propertyClass == double.class) {
            mw.visitVarInsn(DLOAD, context.var("double", 2));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
        } else if (propertyClass == boolean.class) {
            mw.visitVarInsn(ILOAD, context.var("boolean"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
        } else if (propertyClass == BigDecimal.class) {
            mw.visitVarInsn(ALOAD, context.var("decimal"));
        } else if (propertyClass == String.class) {
            mw.visitVarInsn(ALOAD, context.var("string"));
        } else if (propertyClass.isEnum()) {
            mw.visitVarInsn(ALOAD, context.var("enum"));
        } else if (List.class.isAssignableFrom(propertyClass)) {
            mw.visitVarInsn(ALOAD, context.var("list"));
        } else {
            mw.visitVarInsn(ALOAD, context.var("object"));
        }
        mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer,
                           "apply", "(L" + JSONSerializer
                                    + ";Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Z");
    }

    private void _processValue(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo fieldInfo, Context context, com.kangaroo.internal.fastjson.asm.Label _end) {
        com.kangaroo.internal.fastjson.asm.Label processKeyElse_ = new com.kangaroo.internal.fastjson.asm.Label();

        Class<?> fieldClass = fieldInfo.fieldClass;

        if (fieldClass.isPrimitive()) {
            com.kangaroo.internal.fastjson.asm.Label checkValueEnd_ = new com.kangaroo.internal.fastjson.asm.Label();
            mw.visitVarInsn(ILOAD, context.var("checkValue"));
            mw.visitJumpInsn(IFNE, checkValueEnd_);

            mw.visitInsn(ACONST_NULL);
            mw.visitInsn(DUP);
            mw.visitVarInsn(ASTORE, Context.original);
            mw.visitVarInsn(ASTORE, Context.processValue);
            mw.visitJumpInsn(GOTO, processKeyElse_);

            mw.visitLabel(checkValueEnd_);
        }

        mw.visitVarInsn(ALOAD, 0);
        mw.visitVarInsn(ALOAD, Context.serializer);
        mw.visitVarInsn(ALOAD, 0);
        mw.visitLdcInsn(context.getFieldOrinal(fieldInfo.name));
        mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "getBeanContext", "(I)" + desc(com.kangaroo.internal.fastjson.serializer.BeanContext.class));
        mw.visitVarInsn(ALOAD, Context.obj);
        mw.visitVarInsn(ALOAD, Context.fieldName);

        String valueDesc = "Ljava/lang/Object;";
        if (fieldClass == byte.class) {
            mw.visitVarInsn(ILOAD, context.var("byte"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
            mw.visitInsn(DUP);
            mw.visitVarInsn(ASTORE, Context.original);
        } else if (fieldClass == short.class) {
            mw.visitVarInsn(ILOAD, context.var("short"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
            mw.visitInsn(DUP);
            mw.visitVarInsn(ASTORE, Context.original);
        } else if (fieldClass == int.class) {
            mw.visitVarInsn(ILOAD, context.var("int"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            mw.visitInsn(DUP);
            mw.visitVarInsn(ASTORE, Context.original);
        } else if (fieldClass == char.class) {
            mw.visitVarInsn(ILOAD, context.var("char"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
            mw.visitInsn(DUP);
            mw.visitVarInsn(ASTORE, Context.original);
        } else if (fieldClass == long.class) {
            mw.visitVarInsn(LLOAD, context.var("long", 2));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
            mw.visitInsn(DUP);
            mw.visitVarInsn(ASTORE, Context.original);
        } else if (fieldClass == float.class) {
            mw.visitVarInsn(FLOAD, context.var("float"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
            mw.visitInsn(DUP);
            mw.visitVarInsn(ASTORE, Context.original);
        } else if (fieldClass == double.class) {
            mw.visitVarInsn(DLOAD, context.var("double", 2));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
            mw.visitInsn(DUP);
            mw.visitVarInsn(ASTORE, Context.original);
        } else if (fieldClass == boolean.class) {
            mw.visitVarInsn(ILOAD, context.var("boolean"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
            mw.visitInsn(DUP);
            mw.visitVarInsn(ASTORE, Context.original);
        } else if (fieldClass == BigDecimal.class) {
            mw.visitVarInsn(ALOAD, context.var("decimal"));
            mw.visitVarInsn(ASTORE, Context.original);
            mw.visitVarInsn(ALOAD, Context.original);
        } else if (fieldClass == String.class) {
            mw.visitVarInsn(ALOAD, context.var("string"));
            mw.visitVarInsn(ASTORE, Context.original);
            mw.visitVarInsn(ALOAD, Context.original);
        } else if (fieldClass.isEnum()) {
            mw.visitVarInsn(ALOAD, context.var("enum"));
            mw.visitVarInsn(ASTORE, Context.original);
            mw.visitVarInsn(ALOAD, Context.original);
        } else if (List.class.isAssignableFrom(fieldClass)) {
            mw.visitVarInsn(ALOAD, context.var("list"));
            mw.visitVarInsn(ASTORE, Context.original);
            mw.visitVarInsn(ALOAD, Context.original);
        } else {
            mw.visitVarInsn(ALOAD, context.var("object"));
            mw.visitVarInsn(ASTORE, Context.original);
            mw.visitVarInsn(ALOAD, Context.original);
        }

        mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer, "processValue",
                           "(L" + JSONSerializer  + ";" //
                                                                          + desc(com.kangaroo.internal.fastjson.serializer.BeanContext.class) //
                                                                          + "Ljava/lang/Object;Ljava/lang/String;" //
                                                                          + valueDesc + ")Ljava/lang/Object;");

        mw.visitVarInsn(ASTORE, Context.processValue);

        mw.visitVarInsn(ALOAD, Context.original);
        mw.visitVarInsn(ALOAD, Context.processValue);
        mw.visitJumpInsn(IF_ACMPEQ, processKeyElse_);
        _writeObject(mw, fieldInfo, context, _end);
        mw.visitJumpInsn(GOTO, _end);

        mw.visitLabel(processKeyElse_);
    }

    private void _processKey(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo property, Context context) {
        com.kangaroo.internal.fastjson.asm.Label _else_processKey = new com.kangaroo.internal.fastjson.asm.Label();

        mw.visitVarInsn(ILOAD, context.var("hasNameFilters"));
        mw.visitJumpInsn(IFEQ, _else_processKey);

        Class<?> propertyClass = property.fieldClass;

        mw.visitVarInsn(ALOAD, 0);
        mw.visitVarInsn(ALOAD, Context.serializer);
        mw.visitVarInsn(ALOAD, Context.obj);
        mw.visitVarInsn(ALOAD, Context.fieldName);

        if (propertyClass == byte.class) {
            mw.visitVarInsn(ILOAD, context.var("byte"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
        } else if (propertyClass == short.class) {
            mw.visitVarInsn(ILOAD, context.var("short"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
        } else if (propertyClass == int.class) {
            mw.visitVarInsn(ILOAD, context.var("int"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
        } else if (propertyClass == char.class) {
            mw.visitVarInsn(ILOAD, context.var("char"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
        } else if (propertyClass == long.class) {
            mw.visitVarInsn(LLOAD, context.var("long", 2));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
        } else if (propertyClass == float.class) {
            mw.visitVarInsn(FLOAD, context.var("float"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
        } else if (propertyClass == double.class) {
            mw.visitVarInsn(DLOAD, context.var("double", 2));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
        } else if (propertyClass == boolean.class) {
            mw.visitVarInsn(ILOAD, context.var("boolean"));
            mw.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
        } else if (propertyClass == BigDecimal.class) {
            mw.visitVarInsn(ALOAD, context.var("decimal"));
        } else if (propertyClass == String.class) {
            mw.visitVarInsn(ALOAD, context.var("string"));
        } else if (propertyClass.isEnum()) {
            mw.visitVarInsn(ALOAD, context.var("enum"));
        } else if (List.class.isAssignableFrom(propertyClass)) {
            mw.visitVarInsn(ALOAD, context.var("list"));
        } else {
            mw.visitVarInsn(ALOAD, context.var("object"));
        }

        mw.visitMethodInsn(INVOKEVIRTUAL, JavaBeanSerializer,
                           "processKey", "(L" + JSONSerializer
                                         + ";Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;");

        mw.visitVarInsn(ASTORE, Context.fieldName);

        mw.visitLabel(_else_processKey);
    }

    private void _if_write_null(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo fieldInfo, Context context) {
        Class<?> propertyClass = fieldInfo.fieldClass;

        com.kangaroo.internal.fastjson.asm.Label _if = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label _else = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label _write_null = new com.kangaroo.internal.fastjson.asm.Label();
        com.kangaroo.internal.fastjson.asm.Label _end_if = new com.kangaroo.internal.fastjson.asm.Label();

        mw.visitLabel(_if);

        JSONField annotation = fieldInfo.getAnnotation();
        int features = 0;
        if (annotation != null) {
            features = com.kangaroo.internal.fastjson.serializer.SerializerFeature.of(annotation.serialzeFeatures());
        }
        JSONType jsonType = context.beanInfo.jsonType;
        if (jsonType != null) {
            features |= com.kangaroo.internal.fastjson.serializer.SerializerFeature.of(jsonType.serialzeFeatures());
        }

        int writeNullFeatures;
        if (propertyClass == String.class) {
            writeNullFeatures = com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue.getMask()
                    | com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullStringAsEmpty.getMask();
        } else if (Number.class.isAssignableFrom(propertyClass)) {
            writeNullFeatures = com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue.getMask()
                    | com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullNumberAsZero.getMask();
        } else if (Collection.class.isAssignableFrom(propertyClass)) {
            writeNullFeatures = com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue.getMask()
                    | com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullListAsEmpty.getMask();
        } else if (Boolean.class == propertyClass) {
            writeNullFeatures = com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteMapNullValue.getMask()
                    | com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullBooleanAsFalse.getMask();
        } else {
            writeNullFeatures = com.kangaroo.internal.fastjson.serializer.SerializerFeature.WRITE_MAP_NULL_FEATURES;
        }

        if ((features & writeNullFeatures) == 0) {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitLdcInsn(writeNullFeatures);
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "isEnabled", "(I)Z");
            mw.visitJumpInsn(IFEQ, _else);
        }

        mw.visitLabel(_write_null);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitVarInsn(ILOAD, context.var("seperator"));
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "write", "(I)V");

        _writeFieldName(mw, context);

        mw.visitVarInsn(ALOAD, context.var("out"));
        mw.visitLdcInsn(features);
        // features

        if (propertyClass == String.class || propertyClass == Character.class) {
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullStringAsEmpty.mask);
        } else if (Number.class.isAssignableFrom(propertyClass)) {
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullNumberAsZero.mask);
        } else if (propertyClass == Boolean.class) {
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullBooleanAsFalse.mask);
        } else if (Collection.class.isAssignableFrom(propertyClass) || propertyClass.isArray()) {
            mw.visitLdcInsn(com.kangaroo.internal.fastjson.serializer.SerializerFeature.WriteNullListAsEmpty.mask);
        } else {
            mw.visitLdcInsn(0);
        }
        mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeNull", "(II)V");

        // seperator = ',';
        _seperator(mw, context);

        mw.visitJumpInsn(GOTO, _end_if);

        mw.visitLabel(_else);

        mw.visitLabel(_end_if);
    }

    private void _writeFieldName(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, Context context) {
        if (context.writeDirect) {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldNameDirect", "(Ljava/lang/String;)V");
        } else {
            mw.visitVarInsn(ALOAD, context.var("out"));
            mw.visitVarInsn(ALOAD, Context.fieldName);
            mw.visitInsn(ICONST_0);
            mw.visitMethodInsn(INVOKEVIRTUAL, SerializeWriter, "writeFieldName", "(Ljava/lang/String;Z)V");
        }
    }

    private void _seperator(com.kangaroo.internal.fastjson.asm.MethodVisitor mw, Context context) {
        mw.visitVarInsn(BIPUSH, ',');
        mw.visitVarInsn(ISTORE, context.var("seperator"));
    }

    private void _getListFieldItemSer(Context context, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo fieldInfo, Class<?> itemType) {
        com.kangaroo.internal.fastjson.asm.Label notNull_ = new com.kangaroo.internal.fastjson.asm.Label();
        mw.visitVarInsn(ALOAD, 0);
        mw.visitFieldInsn(GETFIELD, context.className, fieldInfo.name + "_asm_list_item_ser_",
                          ObjectSerializer_desc);
        mw.visitJumpInsn(IFNONNULL, notNull_);

        mw.visitVarInsn(ALOAD, 0); // this
        mw.visitVarInsn(ALOAD, Context.serializer);
        mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(itemType)));
        mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "getObjectWriter",
                           "(Ljava/lang/Class;)" + ObjectSerializer_desc);

        mw.visitFieldInsn(PUTFIELD, context.className, fieldInfo.name + "_asm_list_item_ser_",
                          ObjectSerializer_desc);

        mw.visitLabel(notNull_);

        mw.visitVarInsn(ALOAD, 0);
        mw.visitFieldInsn(GETFIELD, context.className, fieldInfo.name + "_asm_list_item_ser_",
                          ObjectSerializer_desc);
    }

    private void _getFieldSer(Context context, com.kangaroo.internal.fastjson.asm.MethodVisitor mw, FieldInfo fieldInfo) {
        com.kangaroo.internal.fastjson.asm.Label notNull_ = new com.kangaroo.internal.fastjson.asm.Label();
        mw.visitVarInsn(ALOAD, 0);
        mw.visitFieldInsn(GETFIELD, context.className, fieldInfo.name + "_asm_ser_", ObjectSerializer_desc);
        mw.visitJumpInsn(IFNONNULL, notNull_);

        mw.visitVarInsn(ALOAD, 0); // this
        mw.visitVarInsn(ALOAD, Context.serializer);
        mw.visitLdcInsn(com.kangaroo.internal.fastjson.asm.Type.getType(desc(fieldInfo.fieldClass)));
        mw.visitMethodInsn(INVOKEVIRTUAL, JSONSerializer, "getObjectWriter",
                           "(Ljava/lang/Class;)" + ObjectSerializer_desc);

        mw.visitFieldInsn(PUTFIELD, context.className, fieldInfo.name + "_asm_ser_", ObjectSerializer_desc);

        mw.visitLabel(notNull_);

        mw.visitVarInsn(ALOAD, 0);
        mw.visitFieldInsn(GETFIELD, context.className, fieldInfo.name + "_asm_ser_", ObjectSerializer_desc);
    }
}
