package com.kangaroo.internal.fastjson.support.hsf;

import java.lang.reflect.Method;

public interface MethodLocator {
    Method findMethod(String[] types);
}
