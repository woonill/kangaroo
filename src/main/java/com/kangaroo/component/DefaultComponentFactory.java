package com.kangaroo.component;

import com.kangaroo.ComponentFactory;

public class DefaultComponentFactory implements ComponentFactory {

    @Override
    public <T> T get(T instance) {
        return null;
    }

    @Override
    public <T> T toInstance(Class<T> type) {
        return null;
    }
}
