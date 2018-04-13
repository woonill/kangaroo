package com.kangaroo.component.scan;


import com.kangaroo.util.Validate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class ClassFileIterator implements ResourceIterator {

    /**
     * files.
     */
    private List<File> files = new ArrayList<File>();

    /**
     * The index.
     */
    private int index = 0;

    /**
     * Instantiates a new class file iterator.
     *
     * @param file
     */
    public ClassFileIterator(File... file) {

        Validate.notEmpty(file);
        files.addAll(Arrays.asList(file));
    }

    // helper method to initialize the iterator
/*    private static void init(List<File> list, File dir, Filter filter) throws Exception {
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
            	init(list, files[i], filter);
            } else {
                if (filter == null || filter.accepts(files[i].getAbsolutePath())) {
                    list.add(files[i]);
                }
            }
        }
    }*/

    /* @see com.impetus.annovention.resource.ResourceIterator#next() */
    @Override
    public final InputStream next() {
        if (index >= files.size()) {
            return null;
        }
        File fp = (File) files.get(index++);
        try {
            return new FileInputStream(fp);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /* @see com.impetus.annovention.resource.ResourceIterator#close() */
    @Override
    public void close() {
        // DO Nothing
    }
}
