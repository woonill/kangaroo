package com.kangaroo.internal.fastjson.serializer;

public class PascalNameFilter implements com.kangaroo.internal.fastjson.serializer.NameFilter {

    public String process(Object source, String name, Object value) {
        if (name == null || name.length() == 0) {
            return name;
        }
        
        char[] chars = name.toCharArray();
        chars[0]= Character.toUpperCase(chars[0]);
        
        String pascalName = new String(chars);
        return pascalName;
    }
}
