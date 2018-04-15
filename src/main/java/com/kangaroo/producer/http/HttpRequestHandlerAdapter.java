package com.kangaroo.producer.http;

import com.kangaroo.Request;
import com.kangaroo.RequestObserver;
import com.kangaroo.Response;
import com.kangaroo.util.ObjectUtil;
import com.kangaroo.util.Validate;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.observers.DefaultObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

/**
 * Created by woonill on 5/30/16.
 */
public abstract class HttpRequestHandlerAdapter implements HttpRequestHandler {


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BiFunction<Request, Response, SHttpResponse> adapterFunc;

 /*   public HttpRequestHandlerAdapter() {
        this(new HttpResponseAdapter());
    }*/

    public HttpRequestHandlerAdapter(BiFunction<Request, Response, SHttpResponse> adapterFunc2) {

        Validate.notNull(adapterFunc2, "HttpResponse handler func is required");
        this.adapterFunc = adapterFunc2;
    }

    abstract protected Observable<Response> subscribe(Request request);

    @Override
    final public Observable<SHttpResponse> observe(final Request request) {


        return Observable.create(new ObservableOnSubscribe<SHttpResponse>() {
            @Override
            public void subscribe(ObservableEmitter<SHttpResponse> observableEmitter) throws Exception {
                HttpRequestHandlerAdapter.this.subscribe(request)
                    .map((Response in) -> {
                        return adapterFunc.apply(request, in);
                    }).subscribe(new DefaultObserver<SHttpResponse>() {
                        @Override
                        public void onNext(SHttpResponse event) {
                            observableEmitter.onNext(event);
                        }

                        @Override
                        public void onError(Throwable te) {
                            logger.error(ObjectUtil.errorToString(te));
                            observableEmitter.onError(te);
                        }

                        @Override
                        public void onComplete() {
                            observableEmitter.onComplete();
                        }
                    });
            }
        });

    }


    public static final class DefaultHttpRequestSubscriberAdapter extends HttpRequestHandlerAdapter {

        private RequestObserver subscribers;

        public DefaultHttpRequestSubscriberAdapter(BiFunction<Request, Response, SHttpResponse> resHandler, RequestObserver in) {
            super(resHandler);
            this.subscribers = in;
        }

/*        public DefaultHttpRequestSubscriberAdapter(RequestObserver in) {
            super();
            this.subscribers = in;
        }*/

        @Override
        protected Observable<Response> subscribe(Request request) {
            return subscribers.observe(request);
        }
    }
}
