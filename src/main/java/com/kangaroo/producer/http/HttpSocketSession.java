package com.kangaroo.producer.http;

/**
 * Created by woonill on 9/18/16.
 */
public interface HttpSocketSession {

    String id();

    State state();

    State close();

    HttpSocketMessage payload(byte[] msg);

    HttpSocketMessageProducer producer();


    public interface State {

        public boolean isOpen();

        public boolean isClosed();


        public static final State OPEN = new State() {
            @Override
            public boolean isOpen() {
                return true;
            }

            @Override
            public boolean isClosed() {
                return false;
            }
        };


        public static final State CLOSED = new State() {
            @Override
            public boolean isOpen() {
                return false;
            }

            @Override
            public boolean isClosed() {
                return true;
            }
        };
    }
}
