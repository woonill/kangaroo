package com.kangaroo;

import java.util.concurrent.ExecutorService;

public interface Consumer<T extends Consumer.Context> {

    Proc<T> run(Producer... producers);

    public static interface Context extends Global.Context{


        ExecutorService getService();

        ComponentFactory getComponentFactory();
    }
}
