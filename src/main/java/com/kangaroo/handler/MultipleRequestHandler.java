package com.kangaroo.handler;

import com.kangaroo.DefaultResponse;
import com.kangaroo.Message;
import com.kangaroo.Request;
import com.kangaroo.Response;
import com.kangaroo.util.CUtils;
import com.kangaroo.util.PathBuilder;
import com.kangaroo.util.PathMatcher;
import com.kangaroo.util.Validate;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

import static com.kangaroo.util.CUtils.MethodMeta;
import static com.kangaroo.util.CUtils.Parameter;

/**
 * Created by woonill on 2/21/16.
 */
public abstract class MultipleRequestHandler implements RequestHandler {


    private static PathMatcher apm = new PathMatcher();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final List<UriMethodMeta> uriMethodMetas;

    public static String getRootPath(String path) {
        return path.split("/")[1];
    }

    protected MultipleRequestHandler() {
        this.uriMethodMetas = this.initMethodMetas();
    }


    public static final Function<RequestHandlerContext, Response> NONE_HANDLER = new Function<RequestHandlerContext, Response>() {
        @Override
        public Response apply(RequestHandlerContext context) {
            return context.handle();
        }
    };

    static final public String getSubPath(String inPath, String rootPath) {

//        System.out.println("In path:"+inPath+" RootPath:"+rootPath);

        int sIndex = inPath.indexOf(rootPath);
        if (sIndex < 1) {
            return null;
        }
        inPath = inPath.substring(sIndex);
        inPath = inPath.startsWith("/") ? inPath : ("/" + inPath);
        String sPath = new PathBuilder(inPath).toString();
        return sPath;
    }

    @Override
    public Response handle(RequestHandlerContext context) {

        logger.debug("Start hander Query Request");

        Request request = context.request();

        UriMethodMeta methodMeta = getUriMethodMeta(request);
        if (methodMeta != null) {

            final Object[] parameter = getInputParameter(methodMeta, request);
            try {
                final Object returnVal = methodMeta.getCaller().apply(parameter);
                logger.debug("The Reponse Object:" + returnVal);
//                final Response response = context.responser().objectToResponse(returnVal);
                return new DefaultResponse.Builder(context.request()).error(te).build();
//                logger.debug("Start response now:" + response.status() + " The type:" + Message.Type.get(response.getTypeCode()));
//                return response;
            } catch (Throwable te) {
                te.printStackTrace();
                return new DefaultResponse.Builder(context.request()).statusCode(400).error(te).build();
            }

        }

        logger.info("not supported path handler for:" + request.path());

        return context.handle();
    }


    public UriMethodMeta getUriMethodMeta(Request request) {

        for (UriMethodMeta meta : this.uriMethodMetas) {
            String sPath = getSubPath(request.path(), meta.getRootPath());
            logger.debug("Sub Uri Path:" + sPath + "  with root path:" + meta.getRootPath());
            if (sPath != null) {
                if (MultipleRequestHandler.apm.match(meta.getPath(), sPath)) {
                    return meta;
                }
            }
        }
        return null;
    }


    protected Object[] getInputParameter(UriMethodMeta meta, Request request) {

        String sPath = getSubPath(request.path(), meta.getRootPath());
        Map<String, String> pamaps = MultipleRequestHandler.apm.extractUriTemplateVariables(meta.getPath(), sPath);
        Map<String, Object> omaps = new HashMap<String, Object>();
        Set<String> skey = pamaps.keySet();
        Iterator<String> iskey = skey.iterator();
        while (iskey.hasNext()) {
            String key = iskey.next();
            omaps.put(key, pamaps.get(key));
        }
        omaps.put(Request.class.getSimpleName(), request);
        List<Object> lparsm = new ArrayList<Object>();
        for (CUtils.Parameter p : meta.parameters) {
            Object op = omaps.get(p.getName());
            if (op == null) {
                throw new IllegalArgumentException("Parameter not matched to[" + p.getName() + "][" + p.getClass().getName());
            }
            logger.debug("Parameter[" + p.getName() + "]value[" + op);
            lparsm.add(op);
        }
        logger.debug("Will invock[" + meta.getMethodMeta().getMethod().getName() + ']');
        return lparsm.toArray(new Object[lparsm.size()]);
    }

    public Map<String, String> getParameter(String s, String s2) {
        return apm.extractUriTemplateVariables(s, s2);
    }

    public boolean isMatched(String s, String s2) {
        return apm.match(s, s2);
    }

    protected abstract List<UriMethodMeta> initMethodMetas();

    public static class MultipleRequestHandlerInitializer extends RequestHandlerInitializer {

//        private Logger logger = LoggerFactory.getLogger(this.getClass());

        public MultipleRequestHandlerInitializer(String name) {
            super(name);
        }

        @Override
        public RequestHandler getHandler(RequestHandlerInitializer.Context context) {

            final MethodRequestHandlerWrapper wrapper = initUriRequestHandler(context);
            return new MultipleRequestHandler() {
                @Override
                protected List<UriMethodMeta> initMethodMetas() {
                    return wrapper.metas;
                }
            };
        }

        protected MethodRequestHandlerWrapper initUriRequestHandler(RequestHandlerInitializer.Context context) {

            List<Object> handlers = context.annoObjects(UriRequestHandler.class);
//                    (Class<?> aClass) -> {
//                        return aClass.getClass().getAnnotation(UriRequestHandler.class) != null;
//                    }
//            );
            if (handlers == null || handlers.isEmpty()) {
                throw new NullPointerException("UiRequestHandlers");
            }
            return new MethodRequestHandlerWrapper(handlers.toArray(new Object[handlers.size()]));
        }
    }


    public static final class MethodRequestHandlerWrapper {


        private Object[] handlers;
        private List<UriMethodMeta> metas;


        public MethodRequestHandlerWrapper(Object... handlers2) {
            Validate.notEmpty(handlers2, "Handler is empty");
            this.handlers = handlers2;
            this.metas = this.getUriMethod(handlers2);
        }

        public int size() {
            return metas.size();
        }

        public List<UriMethodMeta> metas() {
            return Collections.synchronizedList(metas);
        }

        protected List<UriMethodMeta> getUriMethod(Object... handlers) {

            Validate.notEmpty(handlers, "URIHandler is empty");
            List<UriMethodMeta> ruiCall = new ArrayList<UriMethodMeta>();
            for (Object obs : handlers) {
                Class<?> cls = obs.getClass();
                UriRequestHandler urh = cls.getAnnotation(UriRequestHandler.class);
                if (urh != null) {
                    PathBuilder bp = new PathBuilder(urh.value());
                    Method[] ms = obs.getClass().getMethods();
                    if (ms != null && ms.length > 0) {
                        for (Method m : ms) {
                            UriRequestHandler murh = m.getAnnotation(UriRequestHandler.class);
                            if (murh != null) {
                                String fpath = bp.append(murh.value()).toString();
                                List<CUtils.Parameter> parameters = getUriMethodParameter(m);
                                UriMethodMeta umm = new UriMethodMeta(fpath, new MethodMeta(m, obs), parameters);
                                ruiCall.add(umm);
                            }
                        }
                    }
                }
            }
            return ruiCall;
        }

        protected List<CUtils.Parameter> getUriMethodParameter(Method m) {

            List<CUtils.Parameter> pArray = new ArrayList<CUtils.Parameter>();
            Type[] types = m.getGenericParameterTypes();
            Annotation[][] aas = m.getParameterAnnotations();
            int count = 0;
            for (Type type : types) {
                Annotation[] as = aas[count];
                if (as.length < 1) {
                    Class<?> type2 = m.getParameterTypes()[count];
                    if (!Request.class.isAssignableFrom(type2)) {
                        return null;
                    }
                    String sname = Request.class.getSimpleName();
                    Parameter p = new Parameter(sname, count, type2);
                    pArray.add(p);
                } else {
                    for (Annotation ap : as) {
                        if (!PathVar.class.isAssignableFrom(ap.annotationType())) {
                            return null;
                        }
                        PathVar pv = (PathVar) ap;
                        Parameter p = new Parameter(pv.value(), count, type.getClass());
                        pArray.add(p);
                    }
                }
                count++;
            }
            return pArray;
        }


    }

    public static class UriMethodMeta {

        private String path;
        private MethodMeta methodMeta;
        private String rootPath;
        private List<CUtils.Parameter> parameters;


        private final Function<Object[], Object> caller;

        public UriMethodMeta(String fpath, MethodMeta methodMeta2, List<Parameter> parameters2) {
            this.path = fpath;
            this.methodMeta = methodMeta2;
            this.rootPath = MultipleRequestHandler.getRootPath(fpath);
            this.parameters = parameters2;
            this.caller = getCaller(methodMeta2);
        }

        public String getPath() {
            return path;
        }

        public MethodMeta getMethodMeta() {
            return methodMeta;
        }

        public Method getMethod() {
            return methodMeta.getMethod();
        }

        public Object getTarget() {
            return methodMeta.getTarget();
        }

        public String getRootPath() {
            return rootPath;
        }

        public Function<Object[], Object> getCaller() {
            return caller;
        }


        public static final Function<Object[], Object> getCaller(MethodMeta methodMeta) {

            return new Function<Object[], Object>() {
                @Override
                public Object apply(Object[] pass) {
                    return CUtils.invokeMethod(methodMeta.getMethod(), methodMeta.getTarget(), pass);
                }
            };
        }

    }

}