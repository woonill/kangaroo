package com.kangaroo;


import com.kangaroo.util.Validate;

import java.util.*;

import static com.kangaroo.Message.*;

public class DefaultResponse implements Response, java.io.Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private byte[] body;
    private int status = NOT_FOUND;
    private Type contentsType;
    private String requestId;

    private Throwable error;

    private Map<String, String> consVal = new HashMap<>();

    public static final DefaultResponse NONE = new DefaultResponse("".getBytes(), Type.none, NOT_FOUND);

    protected DefaultResponse() {
    }


    public DefaultResponse(byte[] obs, Type type) {
        this(obs, type, SUCCESS_STATUS_CODE);
    }

    public DefaultResponse(byte[] obs, Type type, int status) {
        Validate.notNull(type, "Response payload type required");
        Validate.notNull(obs, "Response payload requreid");
        this.body = obs;
        this.contentsType = type;
        this.status = status;
    }

    public DefaultResponse(Type none, int status) {

        this.body = "".getBytes();
        this.contentsType = none;
        this.status = status;
    }


    public byte[] body() {
        return body;
    }

    public int status() {
        return status;
    }

    public boolean isError() {
        return getError() != null;
    }

    public boolean isNone() {
        return Type.none.equals(this.contentsType);
    }

    public int getTypeCode() {
        return contentsType.code();
    }

    public Throwable getError() {
        return error;
    }

    public final String requestId() {
        return requestId;
    }


    public Optional<String> getHeaderVal(String name) {
        return Optional.ofNullable(this.consVal.get(name));
    }

    public String[] getHeaderKeys() {
        final Set<String> strings = consVal.keySet();
        return new LinkedList<String>(strings).toArray(new String[strings.size()]);
    }


    public final static class Builder {

//			private Request request;

        private String requestId = "";
        private Map<String, String> headers = new HashMap<>();
        private int statusCode = NOT_FOUND;
        private byte[] payload;
        private Type type = Type.text;
        private Throwable error;

        public Builder(Request request) {
            this.requestId = request.id();
        }

        public Builder() {
        }

        public Builder putHeader(String name, String val) {
            this.headers.put(name, val);
            return this;
        }

        public Builder statusCode(int code) {
            this.statusCode = code;
            return this;
        }

        public Builder payload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public Builder theType(Type type) {
            this.type = type;
            return this;
        }

        public Builder error(Throwable e) {
            this.error = e;
            return this;
        }

        public Response build() {

            if (payload == null || payload.length == 0) {
                this.payload = "".getBytes();
            }

            DefaultResponse re = new DefaultResponse(payload, type);
            re.requestId = this.requestId;
            re.status = this.statusCode;
            re.contentsType = this.type;
            re.consVal = this.headers;
            re.error = this.error;
            return re;
        }


        public static Response newNone(Request request) {
            return new DefaultResponse(Type.none, NOT_FOUND);
        }


    }
}