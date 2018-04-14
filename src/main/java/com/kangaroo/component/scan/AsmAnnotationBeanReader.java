package com.kangaroo.component.scan;

import com.kangaroo.util.CUtils;
import com.kangaroo.util.Files;
import org.springframework.asm.ClassReader;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class AsmAnnotationBeanReader {

    private Predicate<ClassMetadata> classFilter;
    private URL rootUrl;

    public AsmAnnotationBeanReader(Predicate<ClassMetadata> cf, URL croot) {
        this.classFilter = cf;
        this.rootUrl = croot;
    }


    public List<ResourceIterator> getResourceIterator(URL url, Predicate<String> filter) throws Exception {

        if (!url.getProtocol().equals("file")) {
            throw new IOException("Unable to understand protocol: " + url.getProtocol());
        }

        String filePath = URLDecoder.decode(url.getPath(), "UTF-8");
        File f = new File(filePath);
        if (!f.exists()) return null;


        List<ResourceIterator> fileList = new LinkedList<>();
        if (f.isDirectory()) {

            Files.accept(f, new Files.AsbstractFileReaderVisitor() {
                @Override
                public void visitFileContents(File file, byte[] bytes) {
                    if (file.isFile()) {

                        int li = file.getName().lastIndexOf(".");
                        if (li > 0) {
                            String prefix = file.getName().substring(li + 1);
                            if ("class".equalsIgnoreCase(prefix) || "jar".equalsIgnoreCase(prefix)) {
                                final ResourceIterator resourceIterator = newResourceIterator(file, filter);
                                if (resourceIterator != null) {
                                    fileList.add(resourceIterator);
                                }
                            }
                        }
                    }
                }
            });
//                List<File> fileList = AsmAnnotationBeanReader.initFiles(f,filter);
//            System.out.println("Is directory:"+fileList.size());
            return fileList;

        }
        fileList.add(newResourceIterator(f, filter));
        return fileList;
    }

    private ResourceIterator newResourceIterator(File file, Predicate<String> filter) {

        try {

//                System.out.println("File:"+file.getName());
            if (file.getName().endsWith(".class")) {
                return new ClassFileIterator(file);
            }
//                System.out.println("Start parse jar");
            URL url = file.toURL();
            String urlString = url.toString();
            if (urlString.endsWith("!/")) {
                urlString = urlString.substring(4);
                urlString = urlString.substring(0, urlString.length() - 2);
                url = new URL(urlString);
            }

            if (!urlString.endsWith("/")) {
                return new JarFileIterator(url.openStream(), filter);
            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return null;
    }


    public final Class<?> readClass(InputStream is, Predicate<ClassMetadata> classFilter) {
        try {
            DataInputStream dstream = new DataInputStream(new BufferedInputStream(is));
            try {
                ClassMetadataReadingVisitor cmrv = new ClassMetadataReadingVisitor();
                ClassReader cf = new ClassReader(dstream);
                cf.accept(cmrv, ClassReader.SKIP_DEBUG);
                if (classFilter != null && classFilter.test(cmrv)) {
                    return CUtils.forName(cmrv.getClassName(), null);
                }
            } finally {
                dstream.close();
                is.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return null;
    }

    public List<Class<?>> scanAnnotationsBean() {
        // TODO Auto-generated method stub
        List<Class<?>> clazzes = new ArrayList<Class<?>>();
        try {
            List<ResourceIterator> itrs = this.getResourceIterator(rootUrl, new FilterImpl());
            if (itrs != null && !itrs.isEmpty()) {

                for (ResourceIterator itr : itrs) {
                    InputStream is = null;
                    while ((is = itr.next()) != null) {
                        // make a data input stream
                        DataInputStream dstream = new DataInputStream(new BufferedInputStream(is));
                        try {
                            ClassMetadataReadingVisitor cmrv = new ClassMetadataReadingVisitor();
                            ClassReader cf = new ClassReader(dstream);
                            cf.accept(cmrv, ClassReader.SKIP_DEBUG);
                            if (classFilter.test(cmrv)) {
                                Class<?> clazz = CUtils.forName(cmrv.getClassName(), null);
                                clazzes.add(clazz);
                            }
                        } finally {
                            dstream.close();
                            is.close();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clazzes;
    }
}
