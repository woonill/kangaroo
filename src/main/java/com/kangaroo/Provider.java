package com.kangaroo;

import com.kangaroo.util.Validate;

/**
 * Created by woonill on 7/14/16.
 */
public interface Provider<T> {
    T get();


    public static final class DefaultProvider<T> implements Provider<T> {


        private T val;


        public DefaultProvider(T val2) {
            Validate.notNull(val2);
            this.val = val2;
        }

        @Override
        public T get() {
            return val;
        }
    }

}
