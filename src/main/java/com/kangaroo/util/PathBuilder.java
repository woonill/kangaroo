package com.kangaroo.util;


public final class PathBuilder {

    private String root;

    public PathBuilder(String root2) {
        this.init(root2);
        ;
    }

    public PathBuilder() {
        this("/");
    }

    private void init(String str) {
        if ("/".equalsIgnoreCase(str)) {
            this.root = str;
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (!str.startsWith("/")) {
            sb.append("/");
        }
        sb.append(str);
        this.root = sb.toString();
    }

    public PathBuilder append(String value2) {
        StringBuilder sb = new StringBuilder(this.root);
        int startIndex = 0;
        if (value2.startsWith("/")) {
            startIndex = 1;
        }
        String end = root.substring(root.length() - 1);
        if (!"/".equalsIgnoreCase(end)) {
            sb.append("/");
        }
        String nps = value2.substring(startIndex);
        sb.append(nps);
        return new PathBuilder(sb.toString());
    }

    @Override
    public String toString() {
        return this.root;
    }


    public static String removeS(String str) {
        String[] strArray = str.split("/");
        if (strArray != null && strArray.length > 0) {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (String s : strArray) {
                String ss = s.trim();
                if (!"*".equalsIgnoreCase(ss)
                        && !"**".equalsIgnoreCase(ss)) {
                    if (i > 0) {
                        sb.append("/");
                    }
                    sb.append(ss);
                }
                i++;
            }
            return sb.toString();
        }
        return str;


    }

    public static String build(String str, String string) {

        String[] strArray = str.split("/");
        if (strArray != null && strArray.length > 0) {
            String sb = removeS(str);
            return new PathBuilder(sb).append(string).toString();
        }
        return string;
    }
}
