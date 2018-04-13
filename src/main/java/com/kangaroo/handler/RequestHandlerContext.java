package com.kangaroo.handler;


import com.kangaroo.ComponentContext;
import com.kangaroo.Request;
import com.kangaroo.Response;

import java.util.Map;

public interface RequestHandlerContext {

    Response handle();

    Request request();

    ComponentContext getComponents();

    ComponentContext getComponents(String name);

    Response handle(Map<String, Object> props);

    Map<String, Object> props();
}
