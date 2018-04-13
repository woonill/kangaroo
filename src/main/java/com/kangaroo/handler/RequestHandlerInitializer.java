package com.kangaroo.handler;

import com.kangaroo.ComponentFactory;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;

public abstract class RequestHandlerInitializer {


    private String name;

    public RequestHandlerInitializer(String name){
        Objects.requireNonNull(name);
        this.name = name;
    }

    public String getName(){
        return name;
    }


    abstract public RequestHandler getHandler(RequestHandlerInitializer.Context context);

    public RequestHandlerInitializer[] getChildren(){
        return null;
    }


    public interface Context{

        ComponentFactory getComponentFactory();

        List<Object> annoObjects(Class<? extends Annotation> annotation);
    }
}
