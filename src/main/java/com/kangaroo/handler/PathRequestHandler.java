package com.kangaroo.handler;

import com.kangaroo.Request;
import com.kangaroo.Response;
import com.kangaroo.util.PathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathRequestHandler implements RequestHandler {

    private final String[] pathSpec;
    private RequestHandler handler;
    //	private final PathMatcher pm = new PathMatcher();
    private PathMatcher pm = new PathMatcher();

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public PathRequestHandler(RequestHandler rh, String... pathSpec) {
        this.pathSpec = pathSpec;
        this.handler = rh;
    }

    @Override
    public Response handle(RequestHandlerContext context) {
        if (isMatchedPath(context.request())) {
            logger.debug("Start handle uri:" + context.request().uri() + " with class:" + this.handler.getClass().getName());
            return this.handler.handle(context);
        }
        return context.handle();
    }


    @Override
    public String toString() {
        return this.getClass().getName() + "#" + handler.toString();
    }

    protected boolean isMatchedPath(Request request) {
        logger.debug("Source uri:" + request);
        for (String path : this.pathSpec) {
            logger.debug("Source Path:" + path + " input uri:" + request.uri());
            if (pm.match(path, request.uri())) {
                logger.debug("Find matched handler:" + this.handler.getClass().getName());
                return true;
            }
        }
        return false;
    }
}