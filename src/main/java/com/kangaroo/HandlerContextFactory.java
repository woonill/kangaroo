package com.kangaroo;

public
interface HandlerContextFactory<T extends Global.Context>{

    T build(Consumer.Context csContext);
}