package com.kangaroo;

public interface ComponentFactory {

    <T> T get(T instance);
    <T> T toInstance(Class<T> type);



    public static final ComponentFactory NONE = new ComponentFactory() {
        @Override
        public <T> T get(T instance) {
            return null;
        }

        @Override
        public <T> T toInstance(Class<T> type) {
            return null;
        }
    };
}
