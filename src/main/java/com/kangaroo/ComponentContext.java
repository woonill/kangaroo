package com.kangaroo;

import io.reactivex.Observable;


public interface ComponentContext {


    <T> Observable<T> components();


    ComponentFactory injector();

    <T> T getComponent(String name);

    <T> T getComponent(Class<T> requiredType);


    ComponentContext NONE = new ComponentContext() {

        private final ComponentFactory injector = ComponentFactory.NONE;


        @Override
        public <T> Observable<T> components() {
            return Observable.empty();
        }

        @Override
        public ComponentFactory injector() {
            return injector;
        }

        @Override
        public <T> T getComponent(String name) {
            throw new NullPointerException("null:"+name);
        }

        @Override
        public <T> T getComponent(Class<T> requiredType) {
            throw new NullPointerException("null:"+requiredType);
        }
    };
}
