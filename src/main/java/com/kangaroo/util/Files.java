package com.kangaroo.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public final class Files {

    private static final PathMatcher apm = new PathMatcher();

    public static final String JAR_FILE = "jar";
    public static final String ZIP_FILE = "zip";

    public static String[] splitPath(File file, String s) {
        return file.getPath().replaceAll("\\\\", "/").split("/");
    }

    public static String[] splitPath(String filePath, String s) {
        return filePath.replaceAll("\\\\", "/").split("/");
    }


//	private static final Logger logger = SLoggerFactory.getInstance(Files.class);


    public interface FileVisitor {
        public void visit(File file);
    }


    public static String getSuffix(String fname) {
        int lasti = fname.lastIndexOf(".");
        if (lasti < 1) {
            return "";
        }
        lasti = lasti + 1;
        return fname.substring(lasti);
    }

    public static String getSuffix(File fname) {
        return getSuffix(fname.getName());
    }


    public static void creatTo(byte[] contents, File to) {

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(to);
            fos.write(contents);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static abstract class OnlyfileVisitor implements FileVisitor {


        @Override
        public void visit(File file) {
            if (!file.isHidden() && !file.isDirectory()) {
                doVisit(file);
            }
        }

        public abstract void doVisit(File file);

    }

    public static abstract class AsbstractFileReaderVisitor extends OnlyfileVisitor {

        @Override
        public void doVisit(File file) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int contents = 0;
                while ((contents = fis.read()) != -1) {
                    bos.write(contents);
                }
                visitFileContents(file, bos.toByteArray());
                bos.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public abstract void visitFileContents(File file, byte[] contents);


    }


    public static void copy(String source, String to) {
        File file = new File(source);
        File file2 = new File(to);
        copy(file, file2);
    }

    public interface JarFileVisitor {

        void visit(JarEntry je);
    }


    public static void acceptJar(File jf, JarFileVisitor fv) {
        try {
            acceptJar(new JarFile(jf), fv);
        } catch (IOException e) {
            throw new IllegalArgumentException("File support to JarFile error[" + e.getMessage() + "]");
        }
    }


    public static void acceptJar(JarFile jf, JarFileVisitor fv) {

        Enumeration<JarEntry> jarEntries = jf.entries();
        JarEntry je = null;
        while (jarEntries.hasMoreElements()) {
            je = jarEntries.nextElement();
            fv.visit(je);
        }
    }

//	public static void acceptTar(TarFile jf,JarFileVisitor fv){
//		
//		Enumeration<JarEntry> jarEntries = jf.entries();
//		JarEntry je = null;
//        while (jarEntries.hasMoreElements()) {
//        	je = jarEntries.nextElement();
//        	fv.visit(je);
//        }		
//	}	


    public static void copy(File file, File file2) {

        if (file.getName().endsWith(JAR_FILE)) {
            copyJar(file, file2);
        } else if (file.getName().endsWith(ZIP_FILE)) {
            copyJar(file, file2);
        } else {
            InputStream is = null;
            OutputStream os = null;
            try {
                is = new BufferedInputStream(new FileInputStream(file));
                os = new BufferedOutputStream(new FileOutputStream(file2));
                byte[] buffer = new byte[1024];
                while (true) {
                    int count = is.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    os.write(buffer, 0, count);
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }


    public static void decompressionZip(File jarfile, String to) {

        ZipFile zf;
        try {
            Validate.notNull(jarfile, "zip file reqired");
            if (!jarfile.getName().endsWith(Files.ZIP_FILE)) {
                throw new IllegalArgumentException("must be jar file");
            }
            File file = new File(to);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            zf = new ZipFile(jarfile);
            Enumeration<? extends ZipEntry> jarEntries = zf.entries();
            ZipEntry je = null;
            while (jarEntries.hasMoreElements()) {
                je = jarEntries.nextElement();
                String fileName = je.getName();
//                logger.debug("jarEntry name["+fileName+"]");
                String sfilePath = to + "/" + fileName;
                File temp = new File(sfilePath);
//                logger.debug("Sub file Path["+sfilePath+"]");
                if (je.isDirectory()) {
                    temp.mkdirs();
                } else {
                    OutputStream os = new FileOutputStream(temp);
                    InputStream is = zf.getInputStream(je);
                    int len = 0;
                    while ((len = is.read()) != -1) {
                        os.write(len);
                    }
                    os.close();
                    is.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void decompressionGZ(File dir, String to) {


        try {
            FileInputStream fis = new FileInputStream(dir);
            GZIPInputStream gis = new GZIPInputStream(fis);
            FileOutputStream fos = new FileOutputStream(to);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            //close resources
            fos.close();
            gis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void decompression(File jarfile, String to) {

        try {
            Validate.notNull(jarfile, "jar file reqired");
            if (!jarfile.getName().endsWith(Files.JAR_FILE)) {
                throw new IllegalArgumentException("must be jar file");
            }
            File file = new File(to);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
            }
            JarFile jf = new JarFile(jarfile);
            Enumeration<JarEntry> jarEntries = jf.entries();
            JarEntry je = null;
            while (jarEntries.hasMoreElements()) {
                je = jarEntries.nextElement();
                String fileName = je.getName();
//                logger.debug("jarEntry name["+fileName+"]");
                String sfilePath = to + "/" + fileName;
                File temp = new File(sfilePath);
//                logger.debug("Sub file Path["+sfilePath+"]");
                if (je.isDirectory()) {
                    System.out.println("make dir:" + sfilePath);
                    if (!temp.mkdir()) {
                        throw new IllegalArgumentException("can not create folder to:" + sfilePath);
                    }
                } else {

                    System.out.println("The file:" + temp.getPath());

                    OutputStream os = new FileOutputStream(temp);
                    InputStream is = jf.getInputStream(je);
                    int len = 0;
                    while ((len = is.read()) != -1) {
                        os.write(len);
                    }
                    os.close();
                    is.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void copyJar(File file, File file2) {
        JarFile jf;
        try {
            jf = new JarFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (file2.exists()) {
//			logger.debug("Find File so remove it first");
            file2.delete();
        }
        try {
//			logger.debug("Start Create new File["+file2.getName()+"] to uri["+file2.getPath()+"]");
            file2.createNewFile();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        JarOutputStream jsfos = null;
        try {
            jsfos = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(file2)));
            Enumeration<JarEntry> jarEntries = jf.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry entry = jarEntries.nextElement();

                jsfos.putNextEntry(entry);
                InputStream is = jf.getInputStream(entry);
                byte[] buffer = new byte[1024];
                while (true) {
                    int count = is.read(buffer);
                    if (count == -1) {
                        break;
                    } else {
                        jsfos.write(buffer, 0, count);
                    }
                }
                is.close();
                jsfos.closeEntry();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            if (jsfos != null) {
                try {
                    jsfos.finish();
                    jsfos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void accept(String[] paths, FileVisitor vf) {
        for (String path : paths) {
            File file = new File(path);
            Files.accept(file, vf);
        }
    }


    public static void accept(String[] files, final FileVisitor vf, final String pattern) {

        final FileVisitor wrapper = new FileVisitor() {
            @Override
            public void visit(File file) {
                String name = file.getName();
                if (apm.match(pattern, name)) {
                    vf.visit(file);
                }
            }
        };

        for (String path : files) {
            File file = new File(path);
            accept(file, wrapper);
        }
    }


    public static void accept(File[] files, final FileVisitor vf, final String pattern) {

        final FileVisitor wrapper = new FileVisitor() {
            @Override
            public void visit(File file) {
                String name = file.getName();
                if (apm.match(pattern, name)) {
                    vf.visit(file);
                }
            }
        };

        for (File file : files) {
            accept(file, wrapper);
        }
    }


    public static void accept(File file, FileVisitor fv) {

        if (file != null && file.exists()) {
            fv.visit(file);
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null && files.length > 0) {
                for (File sfile : files) {
                    accept(sfile, fv);
                }
            }
        }
    }


    public static void accept(File[] files, FileVisitor visitor) {
        for (File file : files) {
            accept(file, visitor);
        }
    }


    public static File[] readFiles(String string) {
        // TODO Auto-generated method stub
        File file = new File(string);
        return file.listFiles();
    }

    public static final URL getURL(String path) {
        try {
            return new File(path).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }


    public static File uriToFile(String logPath) {
        try {
            String f = URI.create(logPath).toURL().getFile();
            return new File(f);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Can not locate to[" + logPath + "]");
        }
    }


    public static void delete(String appHome) {
        File file = new File(appHome);

        if (!file.isHidden()) {
            if (file.isFile()) {
                file.delete();
            } else {
                Files.accept(file, new OnlyfileVisitor() {
                    @Override
                    public void doVisit(File file) {
                        file.delete();
                    }
                });
                Files.accept(file, new FileVisitor() {
                    @Override
                    public void visit(File file) {
                        file.delete();
                    }
                });
                file.delete();
            }
        }
    }


    public static boolean exts(String fPath) {
        if (StrUtils.isNull(fPath)) {
            return false;
        }
        File file = new File(fPath);
        return file.exists();
    }


    public static String readFileToString(String fpath) {


        byte[] fbyte = readFile(fpath);
        Charset cs = Charset.forName("UTF-8");
        ByteBuffer bb = ByteBuffer.allocate(fbyte.length);
        bb.put(fbyte);
        bb.flip();
        CharBuffer cb = cs.decode(bb);
        StringBuilder sb = new StringBuilder();
        char[] csp = cb.array();
        for (char c : csp) {
//		   System.out.println(c);
            sb.append(c);
        }
        return sb.toString().trim();
    }


    public static byte[] readFile(String fpath) {
        // TODO Auto-generated method stub
        File file = new File(fpath);
        if (!file.isFile()) {
            throw new IllegalArgumentException("file[" + fpath + "] is not file");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = fis.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("error", e);
        } catch (IOException e) {
            throw new IllegalArgumentException("error", e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ;
            }
        }

    }
}
