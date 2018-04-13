package com.kangaroo;

import java.util.Map;
import java.util.Set;

public class Header implements java.io.Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String value;


    public static final String ACTION_TYPE_HEADER = "action-type";

    protected Header() {
        this("", "");
    }

    public Header(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public static Header[] toHeader(Map<String, String> headerValues) {
        if (headerValues == null
                || headerValues.isEmpty()) {
            throw new IllegalArgumentException("HeaderMap");
        }
        Set<Map.Entry<String, String>> mnames = headerValues.entrySet();
        Header[] headers = new Header[headerValues.size()];
        int i = 0;
        for (Map.Entry<String, String> ss : mnames) {
            headers[i++] = new Header(ss.getKey(), ss.getValue());
        }
        return headers;
    }
}