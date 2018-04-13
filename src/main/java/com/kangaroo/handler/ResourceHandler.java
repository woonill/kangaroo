package com.kangaroo.handler;

import com.kangaroo.util.Files;
import com.kangaroo.util.ObjectUtil;
import com.kangaroo.util.Validate;
import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by woonill on 8/26/15.
 */
public final class ResourceHandler {

//    private Map<String,File> fileMap = new HashMap<String,File>();

    private final SResource[] resources;
    private final static Logger logger = LoggerFactory.getLogger(ResourceHandler.class);

    private String[] suffix;


    public ResourceHandler(SResource[] allFiles, String... suffix2) {
//        fileMap.putAll(allFiles);
        Validate.notEmpty(allFiles, "Resource is empty");
        this.resources = allFiles;
        this.suffix = suffix2;
    }

    public Observable<SResource> resources() {
        return Observable.fromArray(resources);
    }

    public SResource getResource(String requestURI) {


        for (SResource resource : this.resources) {
//            logger.debug("The Resource path:"+resource.getPath()+" : ----------------------------------");
//            logger.debug("Start path:"+startPath);
            if (resource.getPath().equalsIgnoreCase(requestURI)) {
//                long id = resource.file.lastModified()
                return resource;
            }
        }
        return null;
    }


    public static class SResource {

        private String path;
        private File file;
        private byte[] contents;
        private String header;

        private SResource(String path, File file) {
            this(path, file, null);
        }


        private SResource(String path, File file, byte[] contents) {
            this.path = path;
            this.file = file;
            this.contents = contents;
            this.header = path.split("/")[1];
        }

        public String getPath() {
            return path;
        }

        public String rootPath() {
            return header;
        }

        public String name() {
            return file.getName();
        }


        public String contentsToString() {
            return this.contentsToString("UTF-8");
        }


        public String contentsToString(String ctype) {
            try {
                return new String(this.getContents(), ctype);
            } catch (UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e);
            }
        }


        private long lastModify = 0;
        private AtomicBoolean updated = new AtomicBoolean(false);


        public boolean exists() {
            return file.exists();
        }

        public byte[] getContents() {

            logger.info("Start get Contents now");

            if (this.contents != null) {
                File nfile = new File(this.file.getPath());
                if (lastModify != nfile.lastModified()) {
                    if (updated.compareAndSet(false, true)) {
                        return this.loadData();
                    }
                }
                return this.contents;
            }
            return this.loadData();
        }

        private byte[] loadData() {

            this.file = new File(this.file.getPath());
            logger.info("Create new File");
            logger.info("Current modify:" + this.lastModify);
            logger.info("New modify:" + file.lastModified());
            this.lastModify = this.file.lastModified();
            this.contents = Files.readFile(this.file.getPath());

            this.updated.set(false);

            return contents;

        }
    }


    public static final String[] DEFAULT_SUFFIX = new String[]{
            "html", "htm"
    };

    public static ResourceHandler newHandler(URL url, String... strings) {
        final String[] res_sufix = ObjectUtil.isEmpty(strings) ? DEFAULT_SUFFIX : strings;
        SResource[] resources = getResource(url, res_sufix);
        return new ResourceHandler(resources, res_sufix);
    }


    public static SResource[] getResource(URL url, String... strings) {
        return getResource(new File(url.getFile()), strings);
    }

    public static SResource[] getResource(File file, String... strings) {

        final String rootPath = file.getPath();
        String[] sps = Files.splitPath(file, "/");
//        String[] sps = rootPath.split(separator);
        final String lastOne = sps[sps.length - 1];
        int i = 0;
        if (sps.length > 0) {
            for (String str : sps) {
                if (lastOne.equalsIgnoreCase(str)) {
                    i++;
                }
            }
        }

        final Integer ips = new Integer(i);
        final List<SResource> resources = new LinkedList<SResource>();
//        final Observable<String> sts = Observable.create(res_sufix);

        Files.accept(new File[]{file}, new Files.OnlyfileVisitor() {
            @Override
            public void doVisit(File file) {
                String[] sps2 = Files.splitPath(file, "/");
                StringBuilder sLastOne = new StringBuilder("/").append(lastOne);
                int j = 1;
                boolean start = false;
                for (String str : sps2) {
                    if (!start) {
                        if (lastOne.equalsIgnoreCase(str)) {
                            if (ips == j) {
                                start = true;
                            } else {
                                j++;
                            }
                        }
                    } else {
                        sLastOne.append("/").append(str);
                    }
                }
                for (String str : strings) {
                    if (file.getName().lastIndexOf(str) > 0) {
                        resources.add(new SResource(sLastOne.toString(), file));
                        break;
                    }
                }
            }
        });
        logger.info(" Find:" + resources.size() + " resource");
        return resources.toArray(new SResource[resources.size()]);

    }
}

