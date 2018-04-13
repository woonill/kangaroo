/*
package com.kangaroo.handler;


import com.kangaroo.ComponentContext;
import com.kangaroo.ComponentInjector;
import io.reactivex.Observable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public final class RequestHandlerConfigurable {


    private final String name;
    private final Observable<Class<?>> classes;
    private final ComponentContext componentContext;


    public RequestHandlerConfigurable(
            String name2,
            Observable<Class<?>> classes,
            ComponentContext componentContext) {
        this.name = name2;
        this.classes = classes;
        this.componentContext = componentContext;
    }


    public String name() {
        return name;
    }


    public final Observable<Class<?>> componentsClasses() {
        return classes;
    }

    public final ComponentInjector injector() {
        return componentContext.injector();
    }


    public final ComponentContext getComponentContext() {
        return componentContext;
    }


    public final List<Object> annoObjects(io.reactivex.functions.Predicate<Class<?>> filter) {

        List<Class<?>> resList = this.classes.filter(filter).toList().blockingGet();
        if (resList == null || resList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> obsList = new LinkedList<>();
        for (Class<?> obsClass : resList) {
            obsList.add(this.injector().toComponent(obsClass));
        }
        return obsList;
    }
}
*/
