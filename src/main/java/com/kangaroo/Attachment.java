package com.kangaroo;

import java.io.File;

public class Attachment implements java.io.Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private String name;
    private String fileName;
    private byte[] contents;
    private File file;

    protected Attachment() {
    }

    public Attachment(String name, byte[] bs) {
        this(name, "", bs, null);
    }

    public Attachment(
            String name,
            String submittedFileName,
            byte[] contents2,
            File file) {
        this.name = name;
        this.fileName = submittedFileName;
        this.contents = contents2;
        this.file = file;
    }

    public byte[] getPayload() {
        return this.contents;
    }

    public String toString() {
        return this.fileName;
    }


    public String getName() {
        return this.name;
    }

    public String getSubmitedName() {
        return this.fileName;
    }

}
