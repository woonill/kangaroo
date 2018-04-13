package com.kangaroo.component.scan;

public class DefaultClassFilter implements ClassFilter {

    private String[] annotation;

    DefaultClassFilter() {
    }

    public DefaultClassFilter(String... annotation) {
        this.annotation = annotation;
    }

    public DefaultClassFilter(Class<?>... classes) {
        this.annotation = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            Class<?> c = classes[i];
            this.annotation[i] = c.getName();
        }
    }


    @Override
    public boolean acceptable(ClassMetadata cmrv) {

        AnnotationMetadata[] anames = cmrv.getAnnotations();
        if (anames.length > 0) {
            for (AnnotationMetadata am : anames) {
                for (String sname : annotation) {
                    if (sname.equalsIgnoreCase(am.getAnnotaionClass())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb = sb.append(this.getClass().getName() + "in [\n");
        for (String str : this.annotation) {
            sb = sb.append("\t").append(str).append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }


}
