package com.kangaroo;

public interface SHandler<T,R,C extends Global.Context> {

    R handle(T event,C context);
}
