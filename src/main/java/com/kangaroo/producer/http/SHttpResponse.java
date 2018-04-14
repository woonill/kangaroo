package com.kangaroo.producer.http;

import com.kangaroo.Header;
import com.kangaroo.util.StrUtils;
import io.reactivex.Observable;

import java.util.*;

/**
 * Created by woonill on 9/24/15.
 */
public final class SHttpResponse {


    private byte[] payload;
    private int status;
    private List<Header> headers = new ArrayList<Header>();
    private HttpCookie cookie;

    public byte[] payload() {
        return payload;
    }

    public int status() {
        return status;
    }

    public Observable<Header> headers() {
        return Observable.fromIterable(headers);
    }

    public HttpCookie cookie() {
        return this.cookie;
    }

    public static class HttpResponseBuilder {

        private Map<String, String> headerMap = new HashMap<String, String>();
        private int status = 200;
        private String contensType;

        public HttpResponseBuilder() {
        }

        public HttpResponseBuilder header(String name, String val) {
            this.headerMap.put(name, val);
            return this;
        }

        public HttpResponseBuilder status(int status) {
            if (status > 0) {
                this.status = status;
            }
            return this;
        }

        private HttpCookie cookie;

        public HttpResponseBuilder setCookie(HttpCookie cookie) {
            this.cookie = cookie;
            return this;
        }

        public SHttpResponse build(byte[] bytes) {
            SHttpResponse shr = new SHttpResponse();
            shr.payload = bytes;
            shr.status = this.status;
            if (!StrUtils.isNull(this.contensType)) {
                shr.headers.add(new Header(HttpHeaderNames.CONTENT_TYPE, this.contensType));
            }

            if (!headerMap.isEmpty()) {
                final Set<Map.Entry<String, String>> entries = this.headerMap.entrySet();
                for (Map.Entry<String, String> entry : entries) {
                    shr.headers.add(new Header(entry.getKey(), entry.getValue()));
                }
            }

            if (this.cookie != null) {
                shr.cookie = this.cookie;
            }
            return shr;
        }

        public HttpResponseBuilder setContentType(String type) {
            this.contensType = type;
            return this;
        }


        public static HttpResponseBuilder newIns() {
            return new HttpResponseBuilder();
        }


        private static final List<String> HTTP_HEADERS = new ArrayList<String>();

        static {

            HTTP_HEADERS.add(HttpHeaderNames.ACCEPT);
            HTTP_HEADERS.add(HttpHeaderNames.ACCEPT_CHARSET);
            HTTP_HEADERS.add(HttpHeaderNames.ACCEPT_ENCODING);
            HTTP_HEADERS.add(HttpHeaderNames.ACCEPT_LANGUAGE);
            HTTP_HEADERS.add(HttpHeaderNames.ACCEPT_RANGES);
            HTTP_HEADERS.add(HttpHeaderNames.ACCEPT_PATCH);
            HTTP_HEADERS.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            HTTP_HEADERS.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS);
            HTTP_HEADERS.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS);
            HTTP_HEADERS.add(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN);
            HTTP_HEADERS.add(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS);
            HTTP_HEADERS.add(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE);
            HTTP_HEADERS.add(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS);
            HTTP_HEADERS.add(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
            HTTP_HEADERS.add(HttpHeaderNames.AGE);
            HTTP_HEADERS.add(HttpHeaderNames.ALLOW);
            HTTP_HEADERS.add(HttpHeaderNames.AUTHORIZATION);
            HTTP_HEADERS.add(HttpHeaderNames.CACHE_CONTROL);
            HTTP_HEADERS.add(HttpHeaderNames.CONNECTION);
            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_BASE);
            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_BASE);

            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_ENCODING);
            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_LANGUAGE);
//            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_LENGTH);
            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_LOCATION);
            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_TRANSFER_ENCODING);
            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_MD5);
            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_RANGE);
            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_TYPE);
            HTTP_HEADERS.add(HttpHeaderNames.COOKIE);
            HTTP_HEADERS.add(HttpHeaderNames.DATE);
            HTTP_HEADERS.add(HttpHeaderNames.ETAG);
            HTTP_HEADERS.add(HttpHeaderNames.EXPECT);
            HTTP_HEADERS.add(HttpHeaderNames.EXPIRES);
            HTTP_HEADERS.add(HttpHeaderNames.FROM);
            HTTP_HEADERS.add(HttpHeaderNames.HOST);
            HTTP_HEADERS.add(HttpHeaderNames.IF_MATCH);
            HTTP_HEADERS.add(HttpHeaderNames.IF_MODIFIED_SINCE);
            HTTP_HEADERS.add(HttpHeaderNames.IF_NONE_MATCH);
            HTTP_HEADERS.add(HttpHeaderNames.IF_RANGE);
            HTTP_HEADERS.add(HttpHeaderNames.IF_UNMODIFIED_SINCE);
            HTTP_HEADERS.add(HttpHeaderNames.LAST_MODIFIED);
            HTTP_HEADERS.add(HttpHeaderNames.LOCATION);
            HTTP_HEADERS.add(HttpHeaderNames.MAX_FORWARDS);
            HTTP_HEADERS.add(HttpHeaderNames.ORIGIN);
            HTTP_HEADERS.add(HttpHeaderNames.PRAGMA);
            HTTP_HEADERS.add(HttpHeaderNames.PROXY_AUTHENTICATE);
            HTTP_HEADERS.add(HttpHeaderNames.PROXY_AUTHORIZATION);
            HTTP_HEADERS.add(HttpHeaderNames.RANGE);
            HTTP_HEADERS.add(HttpHeaderNames.REFERER);
            HTTP_HEADERS.add(HttpHeaderNames.RETRY_AFTER);
            HTTP_HEADERS.add(HttpHeaderNames.SEC_WEBSOCKET_KEY1);
            HTTP_HEADERS.add(HttpHeaderNames.SEC_WEBSOCKET_KEY2);
            HTTP_HEADERS.add(HttpHeaderNames.SEC_WEBSOCKET_LOCATION);
            HTTP_HEADERS.add(HttpHeaderNames.SEC_WEBSOCKET_ORIGIN);
            HTTP_HEADERS.add(HttpHeaderNames.SEC_WEBSOCKET_PROTOCOL);
            HTTP_HEADERS.add(HttpHeaderNames.SEC_WEBSOCKET_VERSION);
            HTTP_HEADERS.add(HttpHeaderNames.SEC_WEBSOCKET_KEY);
            HTTP_HEADERS.add(HttpHeaderNames.SEC_WEBSOCKET_ACCEPT);
            HTTP_HEADERS.add(HttpHeaderNames.SERVER);
            HTTP_HEADERS.add(HttpHeaderNames.SET_COOKIE);
            HTTP_HEADERS.add(HttpHeaderNames.SET_COOKIE2);
            HTTP_HEADERS.add(HttpHeaderNames.TE);
            HTTP_HEADERS.add(HttpHeaderNames.TRAILER);
            HTTP_HEADERS.add(HttpHeaderNames.TRANSFER_ENCODING);
            HTTP_HEADERS.add(HttpHeaderNames.UPGRADE);
            HTTP_HEADERS.add(HttpHeaderNames.USER_AGENT);
            HTTP_HEADERS.add(HttpHeaderNames.VARY);
            HTTP_HEADERS.add(HttpHeaderNames.VIA);
            HTTP_HEADERS.add(HttpHeaderNames.WARNING);
            HTTP_HEADERS.add(HttpHeaderNames.WEBSOCKET_LOCATION);
            HTTP_HEADERS.add(HttpHeaderNames.WEBSOCKET_ORIGIN);
            HTTP_HEADERS.add(HttpHeaderNames.WEBSOCKET_PROTOCOL);
            HTTP_HEADERS.add(HttpHeaderNames.WWW_AUTHENTICATE);
            HTTP_HEADERS.add(HttpHeaderNames.CONTENT_DISPOSITION);
        }


        public static boolean isValid(String name2) {

            for (String name : HTTP_HEADERS) {

                if (name.equalsIgnoreCase(name2)) {
                    return true;
                }

            }
            return false;
        }


        public static final class HttpHeaderNames {
            /**
             * {@code "Accept"}
             */
            public static final String ACCEPT = "Accept";
            /**
             * {@code "Accept-Charset"}
             */
            public static final String ACCEPT_CHARSET = "Accept-Charset";
            /**
             * {@code "Accept-Encoding"}
             */
            public static final String ACCEPT_ENCODING = "Accept-Encoding";
            /**
             * {@code "Accept-Language"}
             */
            public static final String ACCEPT_LANGUAGE = "Accept-Language";
            /**
             * {@code "Accept-Ranges"}
             */
            public static final String ACCEPT_RANGES = "Accept-Ranges";
            /**
             * {@code "Accept-Patch"}
             */
            public static final String ACCEPT_PATCH = "Accept-Patch";
            /**
             * {@code "Access-Control-Allow-Credentials"}
             */
            public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
            /**
             * {@code "Access-Control-Allow-Headers"}
             */
            public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
            /**
             * {@code "Access-Control-Allow-Methods"}
             */
            public static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
            /**
             * {@code "Access-Control-Allow-Origin"}
             */
            public static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
            /**
             * {@code "Access-Control-Expose-Headers"}
             */
            public static final String ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
            /**
             * {@code "Access-Control-Max-Age"}
             */
            public static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
            /**
             * {@code "Access-Control-Request-Headers"}
             */
            public static final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
            /**
             * {@code "Access-Control-Request-Method"}
             */
            public static final String ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
            /**
             * {@code "Age"}
             */
            public static final String AGE = "Age";
            /**
             * {@code "Allow"}
             */
            public static final String ALLOW = "Allow";
            /**
             * {@code "Authorization"}
             */
            public static final String AUTHORIZATION = "Authorization";
            /**
             * {@code "Cache-Control"}
             */
            public static final String CACHE_CONTROL = "Cache-Control";
            /**
             * {@code "Connection"}
             */
            public static final String CONNECTION = "Connection";
            /**
             * {@code "Content-Base"}
             */
            public static final String CONTENT_BASE = "Content-Base";
            /**
             * {@code "Content-Encoding"}
             */
            public static final String CONTENT_ENCODING = "Content-Encoding";
            /**
             * {@code "Content-Language"}
             */
            public static final String CONTENT_LANGUAGE = "Content-Language";
            /**
             * {@code "Content-Length"}
             */
            public static final String CONTENT_LENGTH = "Content-Length";
            /**
             * {@code "Content-Location"}
             */
            public static final String CONTENT_LOCATION = "Content-Location";
            /**
             * {@code "Content-Transfer-Encoding"}
             */
            public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
            /**
             * {@code "Content-MD5"}
             */
            public static final String CONTENT_MD5 = "Content-MD5";
            /**
             * {@code "Content-Range"}
             */
            public static final String CONTENT_RANGE = "Content-Range";
            /**
             * {@code "Content-Type"}
             */
            public static final String CONTENT_TYPE = "Content-Type";

            public static final String CONTENT_DISPOSITION = "Content-Disposition";

            /**
             * {@code "Cookie"}
             */
            public static final String COOKIE = "Cookie";
            /**
             * {@code "Date"}
             */
            public static final String DATE = "Date";
            /**
             * {@code "ETag"}
             */
            public static final String ETAG = "ETag";
            /**
             * {@code "Expect"}
             */
            public static final String EXPECT = "Expect";
            /**
             * {@code "Expires"}
             */
            public static final String EXPIRES = "Expires";
            /**
             * {@code "From"}
             */
            public static final String FROM = "From";
            /**
             * {@code "Host"}
             */
            public static final String HOST = "Host";
            /**
             * {@code "If-Match"}
             */
            public static final String IF_MATCH = "If-Match";
            /**
             * {@code "If-Modified-Since"}
             */
            public static final String IF_MODIFIED_SINCE = "If-Modified-Since";
            /**
             * {@code "If-None-Match"}
             */
            public static final String IF_NONE_MATCH = "If-None-Match";
            /**
             * {@code "If-Range"}
             */
            public static final String IF_RANGE = "If-Range";
            /**
             * {@code "If-Unmodified-Since"}
             */
            public static final String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
            /**
             * {@code "Last-Modified"}
             */
            public static final String LAST_MODIFIED = "Last-Modified";
            /**
             * {@code "Location"}
             */
            public static final String LOCATION = "Location";
            /**
             * {@code "Max-Forwards"}
             */
            public static final String MAX_FORWARDS = "Max-Forwards";
            /**
             * {@code "Origin"}
             */
            public static final String ORIGIN = "Origin";
            /**
             * {@code "Pragma"}
             */
            public static final String PRAGMA = "Pragma";
            /**
             * {@code "Proxy-Authenticate"}
             */
            public static final String PROXY_AUTHENTICATE = "Proxy-Authenticate";
            /**
             * {@code "Proxy-Authorization"}
             */
            public static final String PROXY_AUTHORIZATION = "Proxy-Authorization";
            /**
             * {@code "Range"}
             */
            public static final String RANGE = "Range";
            /**
             * {@code "Referer"}
             */
            public static final String REFERER = "Referer";
            /**
             * {@code "Retry-After"}
             */
            public static final String RETRY_AFTER = "Retry-After";
            /**
             * {@code "Sec-WebSocket-Key1"}
             */
            public static final String SEC_WEBSOCKET_KEY1 = "Sec-WebSocket-Key1";
            /**
             * {@code "Sec-WebSocket-Key2"}
             */
            public static final String SEC_WEBSOCKET_KEY2 = "Sec-WebSocket-Key2";
            /**
             * {@code "Sec-WebSocket-Location"}
             */
            public static final String SEC_WEBSOCKET_LOCATION = "Sec-WebSocket-Location";
            /**
             * {@code "Sec-WebSocket-Origin"}
             */
            public static final String SEC_WEBSOCKET_ORIGIN = "Sec-WebSocket-Origin";
            /**
             * {@code "Sec-WebSocket-Protocol"}
             */
            public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
            /**
             * {@code "Sec-WebSocket-Version"}
             */
            public static final String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
            /**
             * {@code "Sec-WebSocket-Key"}
             */
            public static final String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
            /**
             * {@code "Sec-WebSocket-Accept"}
             */
            public static final String SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept";
            /**
             * {@code "Server"}
             */
            public static final String SERVER = "Server";
            /**
             * {@code "Set-Cookie"}
             */
            public static final String SET_COOKIE = "Set-Cookie";
            /**
             * {@code "Set-Cookie2"}
             */
            public static final String SET_COOKIE2 = "Set-Cookie2";
            /**
             * {@code "TE"}
             */
            public static final String TE = "TE";
            /**
             * {@code "Trailer"}
             */
            public static final String TRAILER = "Trailer";
            /**
             * {@code "Transfer-Encoding"}
             */
            public static final String TRANSFER_ENCODING = "Transfer-Encoding";
            /**
             * {@code "Upgrade"}
             */
            public static final String UPGRADE = "Upgrade";
            /**
             * {@code "User-Agent"}
             */
            public static final String USER_AGENT = "User-Agent";
            /**
             * {@code "Vary"}
             */
            public static final String VARY = "Vary";
            /**
             * {@code "Via"}
             */
            public static final String VIA = "Via";
            /**
             * {@code "Warning"}
             */
            public static final String WARNING = "Warning";
            /**
             * {@code "WebSocket-Location"}
             */
            public static final String WEBSOCKET_LOCATION = "WebSocket-Location";
            /**
             * {@code "WebSocket-Origin"}
             */
            public static final String WEBSOCKET_ORIGIN = "WebSocket-Origin";
            /**
             * {@code "WebSocket-Protocol"}
             */
            public static final String WEBSOCKET_PROTOCOL = "WebSocket-Protocol";
            /**
             * {@code "WWW-Authenticate"}
             */
            public static final String WWW_AUTHENTICATE = "WWW-Authenticate";

            private HttpHeaderNames() {
            }
        }
    }


    public static final class HttpCookieBuilder {

        private String value;
        private String domain;
        private String path;
        private long maxAge;
        private int version;

        private String name;

        private HttpCookieBuilder(String name) {
            this.name = name;
        }

        public HttpCookieBuilder value(String value) {
            this.value = value;
            return this;
        }


        public HttpCookieBuilder domain(String domain) {
            this.domain = domain;
            return this;
        }

        public HttpCookieBuilder path(String path) {
            this.path = path;
            return this;
        }

        public HttpCookieBuilder maxAge(long maxAge) {
            this.maxAge = maxAge;
            return this;
        }

        public HttpCookieBuilder version(int version) {
            this.version = version;
            return this;
        }


        public static HttpCookieBuilder create(String name) {
            return new HttpCookieBuilder(name);

        }

        public HttpCookie build() {
            HttpCookie hc = new HttpCookie();
            hc.name = this.name;
            hc.domain = this.domain;
            hc.path = this.path;
            hc.value = this.value;
            hc.version = this.version;
            hc.maxAge = this.maxAge;
            return hc;
        }
    }

    public static final class HttpCookie {

        private HttpCookie() {
        }

        private String name;
        private String value;
        private String domain;
        private String path;
        private long maxAge;
        private int version;

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getDomain() {
            return domain;
        }

        public String getPath() {
            return path;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public int getVersion() {
            return version;
        }

//        boolean isSecure();
//        boolean isHttpOnly();
//        String getCommentUrl();
//        boolean isDiscard();
//        String getComment();


//        void setValue(String value);
//        void setDomain(String domain);
//        void setPath(String uri);
//        void setComment(String comment);
//        void setMaxAge(long maxAge);
//        void setVersion(int version);
//        void setSecure(boolean secure);
//        void setHttpOnly(boolean httpOnly);
//        void setCommentUrl(String commentUrl);
//        void setDiscard(boolean discard);
//        void setPorts(int... ports);
//        void setPorts(Iterable<Integer> ports);

    }


}
