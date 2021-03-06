package com.kangaroo.internal.fastjson.asm;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class TypeCollector {
    private static final Map<String, String> primitives = new HashMap<String, String>() {
        {
            put("int","I");
            put("boolean","Z");
            put("byte", "B");
            put("char","C");
            put("short","S");
            put("float","F");
            put("long","J");
            put("double","D");
        }
    };

    private final String methodName;

    private final Class<?>[] parameterTypes;

    protected com.kangaroo.internal.fastjson.asm.MethodCollector collector;

    public TypeCollector(String methodName, Class<?>[] parameterTypes) {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.collector = null;
    }

    protected com.kangaroo.internal.fastjson.asm.MethodCollector visitMethod(int access, String name, String desc) {
        if (collector != null) {
            return null;
        }

        if (!name.equals(methodName)) {
            return null;
        }

        com.kangaroo.internal.fastjson.asm.Type[] argTypes = com.kangaroo.internal.fastjson.asm.Type.getArgumentTypes(desc);
        int longOrDoubleQuantity = 0;
        for (com.kangaroo.internal.fastjson.asm.Type t : argTypes) {
            String className = t.getClassName();
            if (className.equals("long") || className.equals("double")) {
                longOrDoubleQuantity++;
            }
        }

        if (argTypes.length != this.parameterTypes.length) {
            return null;
        }
        for (int i = 0; i < argTypes.length; i++) {
            if (!correctTypeName(argTypes[i], this.parameterTypes[i].getName())) {
                return null;
            }
        }

        return collector = new com.kangaroo.internal.fastjson.asm.MethodCollector(
                Modifier.isStatic(access) ? 0 : 1,
                argTypes.length + longOrDoubleQuantity);
    }

    private boolean correctTypeName(com.kangaroo.internal.fastjson.asm.Type type, String paramTypeName) {
        String s = type.getClassName();
        // array notation needs cleanup.
        String braces = "";
        while (s.endsWith("[]")) {
            braces = braces + "[";
            s = s.substring(0, s.length() - 2);
        }
        if (!braces.equals("")) {
            if (primitives.containsKey(s)) {
                s = braces + primitives.get(s);
            } else {
                s = braces + "L" + s + ";";
            }
        }
        return s.equals(paramTypeName);
    }

    public String[] getParameterNamesForMethod() {
        if (collector == null || !collector.debugInfoPresent) {
            return new String[0];
        }
        return collector.getResult().split(",");
    }
}