package com.kangaroo;


import java.util.Collection;
import java.util.List;


public interface Request extends java.io.Serializable {

    String path();
    byte[] payload();
    Object getAttribute(String key);
    Collection<Header> headers();
    String header(String key);

    List<Attachment> getAttachment();

    public static interface Context extends Global.Context{

        List<Attachment> getAttachment();
    }
}

