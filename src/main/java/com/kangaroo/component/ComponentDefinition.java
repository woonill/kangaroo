package com.kangaroo.component;

public final class ComponentDefinition {

    private final String name;
    private final Class<?> type;
    private final Object instance;

    public ComponentDefinition(String name, Class<?> type) {
        this(name, type, null);
    }

    public ComponentDefinition(String name, Class<?> type, Object instance) {
        this.name = name;
        this.type = type;
        this.instance = instance;
    }


    public String name() {
        return name;
    }

    public Class<?> type() {
        return type;
    }

    public Object instance() {
        return instance;
    }
}
