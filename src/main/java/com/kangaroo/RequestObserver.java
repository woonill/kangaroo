package com.kangaroo;

import io.reactivex.Observable;

/**
 * Created by woonill on 6/28/15.
 */
public interface RequestObserver {

    Observable<Response> observe(Request request);

}
