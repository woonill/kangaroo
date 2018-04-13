package com.kangaroo.handler;

import com.kangaroo.Response;

public interface ResponseObserver {


    Response observe(RequestHandlerContext context, Object res);
}
