package com.kangaroo.handler;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface UriRequestHandler {


    public String value();


}
