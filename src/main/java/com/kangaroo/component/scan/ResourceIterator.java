package com.kangaroo.component.scan;

import java.io.InputStream;

interface ResourceIterator {

    /**
     * Please close after use.
     *
     * @return null if no more streams left to iterate on
     */
    InputStream next();

    /**
     * Close.
     */
    void close();


    static public final ResourceIterator NONE = new ResourceIterator() {
        @Override
        public InputStream next() {
            return null;
        }

        @Override
        public void close() {

        }
    };
}

