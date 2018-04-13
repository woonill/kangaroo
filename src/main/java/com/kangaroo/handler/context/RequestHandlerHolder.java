package com.kangaroo.handler.context;

import com.kangaroo.handler.PathRequestHandler;
import com.kangaroo.handler.RequestHandler;
import com.kangaroo.util.ObjectUtil;
import com.kangaroo.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by woonill on 7/27/15.
 */
public final class RequestHandlerHolder {

    private String name;
    private String[] mappingUris;
    private RequestHandler handler;
    private RequestHandlerHolder[] children;

    private RequestHandlerHolder(
            String name,
            RequestHandler handler,
            String[] mappingUris,
            RequestHandlerHolder... rhhChildren) {

//        Validate.notEmpty(mappingUris,"Request uris is required");

        this.name = name;
        this.handler = handler;
        this.children = rhhChildren;
        this.mappingUris = initUris(mappingUris, rhhChildren);
    }

    static String[] initUris(String[] uris, RequestHandlerHolder... children) {
        if (ObjectUtil.isEmpty(uris)) {
            if (ObjectUtil.isEmpty(children)) {
                throw new IllegalArgumentException("Request mapping uris is null so children must not be Empty");
            }
            List<String> strs = new LinkedList<>();
            for (RequestHandlerHolder cho : children) {
                String[] strings = cho.mappingUris;
                strs.addAll(Arrays.asList(strings));
            }
            return strs.toArray(new String[strs.size()]);
        }
        return uris;
    }

    public RequestHandlerHolder(String name, RequestHandler handler, RequestHandlerHolder... rhhChildren) {
        this(name, handler, new String[]{}, rhhChildren);
    }


    public RequestHandlerHolder(String name, RequestHandler handler, String... uris) {
        this(name, handler, uris,(RequestHandlerHolder[]) null);
    }

    public String name() {
        return name;
    }

    public RequestHandler handler() {
        return new PathRequestHandler(this.handler, this.mappingUris);
    }

    public RequestHandler targetHandler() {
        return this.handler;
    }

    public String[] mappingUris() {
        return this.mappingUris;
    }

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private void addChild(RequestHandlerHolder holder) {
        synchronized (this) {
            List<RequestHandlerHolder> all = new LinkedList<>(Arrays.asList(this.children));
            all.add(holder);
            this.children = all.toArray(new RequestHandlerHolder[all.size()]);
        }

    }


    RequestHandlerHolderPipeline toPipeline() {

        return new RequestHandlerHolderPipeline() {

            boolean _last = false;
            boolean _pipeLast = false;
            boolean isFirst = true;
            int index = 0;
            RequestHandlerHolderPipeline cPipe = null;

            @Override
            public boolean isLast() {
                return _last && _pipeLast;
            }

            @Override
            public RequestHandlerHolder next() {

                if (this.isLast()) {
                    throw new IllegalArgumentException("no element");
                }

                RequestHandlerHolder _current = null;
                if (isFirst) {
                    _current = RequestHandlerHolder.this;
                    isFirst = false;
                } else {
//                    logger.debug("----------sss---------------------------------------------------:"+_current.name);
//                    logger.info("The Children name:"+Arrays.toString(children));
                    if (index <= (children.length - 1)) {
                        _last = false;
                        _pipeLast = false;
                        this.cPipe = children[index++].toPipeline();
                    }
                    _current = this.cPipe.next();
                    _last = cPipe.isLast();
                    if (index >= children.length) {
                        _pipeLast = true;
                    }
                }

                if (ObjectUtil.isEmpty(children)) {
                    logger.debug("Children is empty for:" + _current.name + " is First:" + isFirst);
                    _last = true;
                    _pipeLast = true;
                }
//                logger.debug("Children is empty for:"+_current.name+" is First:"+this.isLast());

                return _current;
            }
        };
    }

    public RequestHandlerHolder[] children() {
        return this.children;
    }


    public String mappingUrisToString() {

        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String str : this.mappingUris) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(str);
            i++;
        }
        return sb.toString();
    }


    public static final class RequestHandlerHolderPipelineSComposite implements RequestHandlerHolderPipeline {


        private RequestHandlerHolder[] rootHolders;

        boolean isLast = false;
        int index = 0;
        RequestHandlerHolderPipeline current;
        RequestHandlerHolder currentHolder;


        public RequestHandlerHolderPipelineSComposite(RequestHandlerHolder... rootHolders2) {
            Validate.notEmpty(rootHolders2, "Handler empty");
            this.rootHolders = rootHolders2;
        }

        @Override
        public boolean isLast() {
            return isLast && this._lastPipe;
        }

        private boolean _lastPipe = false;

        @Override
        public RequestHandlerHolder next() {

            if (isLast()) {
                throw new IllegalArgumentException("End error");
            }
            if (index <= (rootHolders.length - 1)) {

                if (isLast || index == 0) {
                    isLast = false;
                    _lastPipe = false;
                    this.current = rootHolders[index++].toPipeline();
                }

            }
            this.currentHolder = current.next();
            this.isLast = current.isLast();
            if (index >= rootHolders.length) {
                this._lastPipe = true;
            }
            return this.currentHolder;
        }
    }

    /**
     * Created by woonill on 10/13/15.
     */
    public interface RequestHandlerHolderPipeline {

        boolean isLast();

        RequestHandlerHolder next();
    }
}
