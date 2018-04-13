package com.kangaroo.component.scan;


import java.util.function.Predicate;

public class FilterImpl implements Predicate<String> {
    /**
     * The ignored packages.
     */
    private transient String[] ignoredPackages = {"javax", "java", "sun", "com.sun", "javassist"};

    /* @see com.impetus.annovention.Filter#accepts(java.lang.String) */
    @Override
    public final boolean test(String filename) {
        if (filename.endsWith(".class")) {
            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }
            if (!ignoreScan(filename.replace('/', '.'))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param intf
     * @return
     */
    private boolean ignoreScan(String intf) {
        for (String ignored : ignoredPackages) {
            if (intf.startsWith(ignored + ".")) {
                return true;
            }
        }
        return false;
    }
}
