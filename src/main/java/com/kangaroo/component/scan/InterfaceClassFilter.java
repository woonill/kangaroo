package com.kangaroo.component.scan;

import com.kangaroo.util.CUtils;

import java.util.function.Predicate;

public class InterfaceClassFilter implements Predicate<ClassMetadata> {

    private Class<?> interfaces;

    public InterfaceClassFilter(Class<?> inters) {
        this.interfaces = inters;
    }

    @Override
    public boolean test(ClassMetadata cmrv) {
        Class<?> clazz = CUtils.forName(cmrv.getClassName());
        return interfaces.isAssignableFrom(clazz);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\t " + interfaces.getName());
        return sb.toString();
    }
}
