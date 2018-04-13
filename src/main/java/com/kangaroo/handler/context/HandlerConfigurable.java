/*
package com.kangaroo.handler.configure;

import com.kangaroo.ComponentInjector;
import io.reactivex.Observable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

*/
/**
 * Created by woonill on 2/26/16.
 *//*

public interface HandlerConfigurable {


    Observable<Class<?>> componentsClasses();

    ComponentInjector injector();

    List<Object> annoObjects(Function<Class<?>, Boolean> filter);


    public static final class DefaultHandlerConfigurable implements HandlerConfigurable {


        private Observable<Class<?>> classes;
        private ComponentInjector injector;

        public DefaultHandlerConfigurable(
                Observable<Class<?>> clases2,
                ComponentInjector injector2) {
            this.classes = clases2;
            this.injector = injector2;
        }


        @Override
        public Observable<Class<?>> componentsClasses() {
            return this.classes;
        }

        @Override
        public ComponentInjector injector() {
            return injector;
        }

        @Override
        public List<Object> annoObjects(Function<Class<?>, Boolean> filter) {
            List<Class<?>> resList = this.classes.filter((Class<?> clazz) -> {
                return filter.apply(clazz);
            } ).toList().blockingGet();
            if (resList == null || resList.isEmpty()) {
                return Collections.emptyList();
            }
            List<Object> obsList = new LinkedList<>();
            for (Class<?> obsClass : resList) {
                obsList.add(injector.toComponent(obsClass));
            }
            return obsList;
        }
    }

}
*/
