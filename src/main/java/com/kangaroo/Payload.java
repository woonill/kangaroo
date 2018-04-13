package com.kangaroo;

public interface Payload {


    void write(Writer writer);
    void read(Reader reader);

    public interface Writer{

    }

    public interface Reader{
    }
}
