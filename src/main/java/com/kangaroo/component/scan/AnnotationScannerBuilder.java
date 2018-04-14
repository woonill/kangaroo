package com.kangaroo.component.scan;


import com.kangaroo.util.CUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AnnotationScannerBuilder {

    private static AnnotationScannerBuilder defaultInstance = new AnnotationScannerBuilder();

    private AnnotationScannerBuilder() {
    }

    public static AnnotationScannerBuilder ins() {
        return defaultInstance;
    }

    public List<Class<?>> scan(Predicate<ClassMetadata> cFilter, URL rootUrl) {
        AsmAnnotationBeanReader aabr = new AsmAnnotationBeanReader(cFilter, rootUrl);
        return aabr.scanAnnotationsBean();
    }

    public List<Object> findComponents(Predicate<ClassMetadata> cf, URL from) {
        List<Class<?>> classes = AnnotationScannerBuilder.ins().scan(cf, from);
        List<Object> nObs = new ArrayList<Object>();
        if (classes != null && classes.size() > 0) {
            for (Class<?> clazz : classes) {
                Object obs = CUtils.getInstance(clazz);
                nObs.add(obs);
            }
        }
        return nObs;
    }

}
