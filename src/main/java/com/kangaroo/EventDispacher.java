package com.kangaroo;

import java.util.UUID;
import java.util.concurrent.Future;

public interface EventDispacher {


    Future<?> submit(Event event);



    public static class Event implements java.io.Serializable {

        /**
         *
         */
        private static final long serialVersionUID = 3022637830851822876L;
        private String name;
        private Object target;
        private String id;

        Event() {
        }

        public Event(String id, String name, Object target2) {
            this.name = name;
            this.target = target2;
            this.id = id;
        }

        public Event(String name, Object target2) {
            this(UUID.randomUUID().toString(), name, target2);
        }

        public String getName() {
            return name;
        }

        public Object getTarget() {
            return target;
        }

        public String id() {
            return id;
        }


        public boolean isError() {
            return false;
        }
    }


    public static final class ErrorEvent extends Event {


        private Throwable error;

        public ErrorEvent(String id, String name, Object target2, Throwable te) {
            super(id, name, target2);
            this.error = te;
        }

        public ErrorEvent(String name, Object target2, Throwable te) {
            this(UUID.randomUUID().toString(), name, target2, te);
        }

        public ErrorEvent(Event event, Throwable te) {
            this(event.id, event.name, event.target, te);
        }

        public Throwable getError() {
            return error;
        }

        @Override
        public boolean isError() {
            return true;
        }

    }

}
