package com.kangaroo.producer.http;

/**
 * Created by woonill on 9/19/16.
 */
public interface HttpSocketMessageProducer {

    boolean produce(byte[] message);


    HttpSocketMessageProducer NONE = new HttpSocketMessageProducer() {

        @Override
        public boolean produce(byte[] message) {
            return false;
        }
    };
}
