package com.kangaroo.handler;


import com.kangaroo.ComponentFactory;
import com.kangaroo.Request;
import com.kangaroo.Response;

import java.util.Map;

public interface RequestHandlerContext {

    Response handle();

    Request request();

    ComponentFactory getComponents();

    Response handle(Map<String, Object> props);

    Map<String, Object> props();

    Response.Factory getResponseFactory(Request request);
}
