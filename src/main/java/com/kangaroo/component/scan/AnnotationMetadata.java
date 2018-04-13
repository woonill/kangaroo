package com.kangaroo.component.scan;

public class AnnotationMetadata {

    private String clazz;
    private String annotaionClass;
    private String name;
    private Object value;

    public AnnotationMetadata(String className, String className2) {
        this.clazz = className;
        this.annotaionClass = className2;
    }

    public void setValue(String name2, Object value2) {
        this.name = name2;
        this.value = value2;
    }

    public String getAnnotaionClass() {
        return annotaionClass;
    }


    public String getClazz() {
        return clazz;
    }


    public String getName() {
        return name;
    }


    public Object getValue() {
        return value;
    }

}
