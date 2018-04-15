package com.kangaroo.internal.fastjson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JSONPObject implements com.kangaroo.internal.fastjson.serializer.JSONSerializable {
    public static String SECURITY_PREFIX = "/**/";
    private String             function;

    private static final int BrowserSecureMask = com.kangaroo.internal.fastjson.serializer.SerializerFeature.BrowserSecure.mask;

    private final List<Object> parameters = new ArrayList<Object>();

    public JSONPObject() {

    }

    public JSONPObject(String function) {
        this.function = function;
    }

    public String getFunction() {
        return function;
    }

    public void setFunction(String function) {
        this.function = function;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public void addParameter(Object parameter) {
        this.parameters.add(parameter);
    }

    public String toJSONString() {
        return toString();
    }

    public void write(com.kangaroo.internal.fastjson.serializer.JSONSerializer serializer, Object fieldName, Type fieldType, int features) throws IOException {
        com.kangaroo.internal.fastjson.serializer.SerializeWriter writer = serializer.out;

        if ((features & BrowserSecureMask) != 0 || (writer.isEnabled(BrowserSecureMask))) {
            writer.write(SECURITY_PREFIX);
        }

        writer.write(function);
        writer.write('(');
        for (int i = 0; i < parameters.size(); ++i) {
            if (i != 0) {
                writer.write(',');
            }
            serializer.write(parameters.get(i));
        }
        writer.write(')');
    }

    public String toString() {
        return JSON.toJSONString(this);
    }
}
