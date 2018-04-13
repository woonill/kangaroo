package com.kangaroo;


import io.reactivex.Observable;
import io.reactivex.functions.Predicate;

import java.util.LinkedList;
import java.util.List;


/**
 * Created by woonill on 9/29/15.
 */
public class Message implements java.io.Serializable {

    private static final long serialVersionUID = 7183413944690728229L;
    private byte[] payload;
    private List<Header> headers = new LinkedList<Header>();


    public static final int REDIRECT_STATUS = 301;
    public static final int SUCCESS_STATUS_CODE = 200;
    public static final int BAD_REQUEST = 400;
    public static final int NOT_FOUND = 404;


    public static final String REDIRECT_LOCATION_FIELD = "Location";


    public Message(byte[] payload, Header... value) {
        this.payload = payload;
        if (value != null && value.length > 0) {
            for (Header header : value) {
                this.addHeader(header.name(), (String) header.value());
            }
        }
    }

    private Message addHeader(String string, String value) {
        this.headers.add(new Header(string, value));
        return this;
    }


    public String header(String key) {
        for (Header header : this.headers) {
            if (header.name().equalsIgnoreCase(key)) {
                return (String) header.value();
            }
        }
        return "";
    }

    public byte[] payload() {
        return this.payload;
    }


    public enum Type {

        error(-1, "error"),
        none(0, "none"),
        byte_array(1, "byte"),
        text(2, "txt"),
        html(3, "html"),
        htm(4, "htm"),
        xml(5, "xml"),
        js(6, "js"),
        json(7, "json"),
        css(8, "css"),
        scss(9, "scss"),
        svg(10, "svg"),
        woff(11, "woff"),
        eot(12, "eot"),
        jsp(13, "jsp"),
        asp(14, "asp"),


        pdf(20, "pdf"),
        word(21, "doc"),
        nword(22, "docx"),
        excel(23, "xls"),
        xlsx(24, "xlsx"),
        ppt(25, "ppt"),
        flash(26, "swf"),
        map(30, "map"),


        jpg(40, "jpg"),
        png(41, "png"),
        bmp(42, "bmp"),
        jpeg(43, "jpeg"),
        gif(44, "gif"),
        tif(45, "tif"),
        tiff(46, "tif"),
        ico(47, "ico"),

        zip(60, "zip"),
        gz(61, "gz"),
        tar(62, "tar");


        private int code;
        private String name;

        private Type(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        public static Type get(String sname) {

            return Observable.fromArray(Type.values())
                            .filter(new Predicate<Type>() {
                                @Override
                                public boolean test(Type type) {
                                    return type.name.equalsIgnoreCase(sname);
                                }
                            })
                            .blockingFirst();
        }

        public static Observable<Type> observable() {
            return Observable.fromArray(Type.values());
        }

        public static Type get(int code) {
            return Observable.fromArray(Type.values())
                    .filter(new Predicate<Type>() {
                        @Override
                        public boolean test(Type in) {
                            return in.code == code;
                        }
                    })
                    .blockingFirst();

        }

        public int code() {
            return this.code;
        }


    }
}
