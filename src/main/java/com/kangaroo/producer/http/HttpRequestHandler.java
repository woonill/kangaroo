package com.kangaroo.producer.http;

import com.kangaroo.Request;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by woonill on 12/11/2016.
 */

public interface HttpRequestHandler {

    Observable<SHttpResponse> observe(Request request);

    HttpRequestHandler NONE_HANDLER = new HttpRequestHandler() {

        @Override
        public Observable<SHttpResponse> observe(Request request) {

            return Observable.create(new ObservableOnSubscribe<SHttpResponse>() {
                @Override
                public void subscribe(ObservableEmitter<SHttpResponse> observableEmitter) throws Exception {
                    final SHttpResponse res = SHttpResponse.HttpResponseBuilder.newIns().status(400).build("not found Handler".getBytes());
                    try {
                        observableEmitter.onNext(res);
                    } finally {
                        observableEmitter.onComplete();
                    }
                }
            });
        }
    };
}
