package com.kangaroo;

import java.util.concurrent.Future;

/**
 * Created by woonill on 30/11/2016.
 */
public interface RunProc {


    Future<?> blocking();

    Future<?> halt();


}
