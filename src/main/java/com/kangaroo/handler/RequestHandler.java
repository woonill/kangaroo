package com.kangaroo.handler;


import com.kangaroo.Response;


public interface RequestHandler {


    Response handle(RequestHandlerContext rhc);


/*
    default Observer<EventDispacher.Event> getEventObserver() {
        return null;
    }
    default void init(RequestHandlerContext context) {
    }
*/

    RequestHandler NONE = new RequestHandler() {
        @Override
        public Response handle(RequestHandlerContext srhc) {
            return srhc.handle();
        }
    };
}


