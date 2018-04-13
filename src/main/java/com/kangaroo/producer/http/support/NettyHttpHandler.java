package com.kangaroo.producer.http.support;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.reactivex.Observable;

/**
 * Created by woonill on 21/11/2016.
 */
public interface NettyHttpHandler {

    Observable<FullHttpResponse> observe(FullHttpRequest request);
}
