package com.kangaroo;

import java.util.Map;

public interface Global {

    public static interface Context{

        Map<String,Object> props();
    }
}
