package com.kangaroo.handler;

import com.kangaroo.Message;
import com.kangaroo.Request;
import com.kangaroo.Response;
import com.kangaroo.util.Files;
import com.kangaroo.util.Validate;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * Created by woonill on 10/16/15.
 */
public class ResourceHandlerInitializer extends RequestHandlerInitializer {

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    private final String fpath;
    //    private final String sfsnames;
    private final Function<String, String> uriMapper;


    public ResourceHandlerInitializer(String name,String fileRootPath) {
        super(name);

        Validate.notNull(fileRootPath, "File path flied name is null");
        this.fpath = fileRootPath;
        this.uriMapper = this.getUriMapper(fpath);
    }


    @Override
    public final RequestHandler getHandler(ResourceHandlerInitializer.Context configer) {
        logger.debug("init requesthandle info --------------------------------------------");

        String[] fsnames = defaultFilterTypes();
        logger.debug("Get resource from:" + fpath);
        logger.debug("Filter file :" + Observable.fromArray(fsnames).toString());

        LoggerFactory.getLogger(this.getClass()).info("Input file path:" + fpath);
        ResourceHandler srh = ResourceHandler.newHandler(Files.getURL(fpath), fsnames);
        return new RequestHandler() {

            @Override
            public Response handle(RequestHandlerContext handleContext) {

                logger.debug("Handler html request:" + handleContext.request().path());
                Request request = handleContext.request();
                String resPath = handleContext.request().path().indexOf("?") > 0 ? getLastPath(request.path()) : request.path();
                logger.debug("ResPath:" + resPath);

                String startPath = uriMapper.apply(resPath);
                logger.debug("Start path:" + startPath);
                final ResourceHandler.SResource resource = srh.getResource(startPath);
                if (resource != null) {
                    byte[] contents = resource.getContents();
                    String prefix = resource.name().substring(resource.name().lastIndexOf(".") + 1);
                    Message.Type type = Message.Type.get(prefix);
                    return handleContext.responser().response(contents, type);
                }
                return handleContext.handle();
            }
        };
    }

    public static final String getLastPath(String path) {

        path = path.substring(0, path.indexOf("?"));
        return path;
    }

    protected String[] defaultFilterTypes() {
        return new String[]{"html", "htm", "js", "css", "png", "jpg", "gif", "woff", "ttf", "ico", "jpeg", "txt"};
    }

    protected Function<String, String> getUriMapper(String fpath) {

        String[] spp = fpath.split("/");
        String rootPath = spp[spp.length - 1];

        return new Function<String, String>() {
            @Override
            public String apply(String resPath) {

//                logger.info("The static head path:" + rootPath);
                return "/" + resPath.substring(resPath.indexOf(rootPath));
            }
        };
    }

}