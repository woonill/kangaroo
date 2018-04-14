package com.kangaroo;

import java.io.Serializable;
import java.util.Map;

public interface Global {

    public static interface Context{

        Map<String,Object> props();
    }


    public static interface ContextFactory<T extends Global.Context>{

        T build(Consumer.Context csContext);
    }


    public static interface Writer{
        <T> T write(WriterAdapter<T> adapter);
    }


    public static interface WriterAdapter<T>{

        <T>  T toJSON(Object object);
    }


    public static interface SObject extends Serializable{


        <T> T to(WriterAdapter<T> adapter);
    }
}
