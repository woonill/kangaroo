package com.kangaroo.handler.command;

public class CommandHandleException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public static final int ERROR = 500;
    private int errorCode = ERROR;

    public CommandHandleException(int errorCode) {
        this(errorCode, "");
    }

    public CommandHandleException(int errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }


    public CommandHandleException(String message) {
        super(message);
    }

    public CommandHandleException(int errorCode, Throwable t) {
        super(t);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
}