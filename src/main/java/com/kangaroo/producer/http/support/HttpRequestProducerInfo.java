package com.kangaroo.producer.http.support;

import com.kangaroo.util.StrUtils;
import io.netty.util.NetUtil;

/**
 * Created by woonill on 10/11/15.
 */
public final class HttpRequestProducerInfo {


    private Integer port;
    private Integer maxPostLength;
    private Integer acceptor;
    private Integer workerPool;
    private String inetHost;
    private boolean supportHttp2 = false;
    private boolean supportSocket = false;


    private HttpRequestProducerInfo() {
    }

    public Integer port() {
        return port;
    }

    public int maxPostLength() {

        if (maxPostLength == null) {
            return 1024 * 100;
        }
        return maxPostLength.intValue();


    }

    public int acceptor(int deVal) {
        if (acceptor == null) {
            if (deVal > 0) {
                return deVal;
            }
            return 0;
        }
        return acceptor.intValue();
    }

    public int workerPool(int defaultVal) {
        if (workerPool == null) {
            if (defaultVal > 0) {
                return defaultVal;
            }
            return 0;
        }
        return workerPool.intValue();
    }

    private int socketBacklog = 50;
    private boolean socketKeepAlive = false;

    public int socketBacklog() {

        return socketBacklog;
    }

    public boolean socketKeepAlive() {
        return this.socketKeepAlive;

    }


    public boolean isSupportSocket() {
        return this.supportSocket;
    }


    public String inetHost() {

        if (StrUtils.isEmpty(inetHost)) {
            return "127.0.0.1";
        }

        if (!NetUtil.isValidIpV4Address(this.inetHost)) {
            throw new IllegalArgumentException("Input Host:" + this.inetHost + " is not avalid Ip4");
        }
        return this.inetHost;
    }

//    private  boolean isSupportSocket = false;


    public static class HttpRequestProducerInfoBuilder {

//        Runtime.getRuntime().availableProcessors()*2

        public static final String ENV_PROPERTY = "producer.env";


        private Integer acceptor;
        private Integer workerPool;
        private Integer port = 8080;
        private Integer maxPostLength = new Integer(65536);
        private String inetHost = "127.0.0.1";
        private boolean supportHttp2 = false;
        private int socketBacklog = 50;
        private boolean socketkeepAlive = true;
        private boolean socketKeepAlive;


        public HttpRequestProducerInfoBuilder setPort(Integer port) {
            this.port = port;
            return this;
        }


        public HttpRequestProducerInfoBuilder setSupportHttp2(boolean supportHttp2) {
            this.supportHttp2 = supportHttp2;
            return this;
        }


        public HttpRequestProducerInfoBuilder setAcceptor(Integer acceptor) {
            this.acceptor = acceptor == null ? 0 : acceptor.intValue();
            return this;
        }

        public HttpRequestProducerInfoBuilder setWorkerPool(Integer workerPool) {
            this.workerPool = workerPool == null ? 0 : workerPool.intValue();
            return this;
        }


        public HttpRequestProducerInfoBuilder setSocketBacklog(int socketBacklog) {
            this.socketBacklog = socketBacklog;
            return this;
        }

        public HttpRequestProducerInfo build() {
            HttpRequestProducerInfo hrp = new HttpRequestProducerInfo();
            hrp.port = this.port;
            hrp.maxPostLength = this.maxPostLength;
            hrp.acceptor = this.acceptor;
            hrp.workerPool = this.workerPool;
            hrp.supportHttp2 = this.supportHttp2;
            hrp.socketBacklog = this.socketBacklog < 50 ? 50 : this.socketBacklog;
            hrp.socketKeepAlive = this.socketkeepAlive;
            return hrp;
        }

        public static HttpRequestProducerInfoBuilder newIns() {
            return new HttpRequestProducerInfoBuilder();
        }

        public HttpRequestProducerInfoBuilder setMaxPostLength(Integer maxPostLength) {
            if (maxPostLength != null) {
                this.maxPostLength = maxPostLength;
            }
            return this;
        }

        public HttpRequestProducerInfoBuilder inetHost(String inetHost) {
            this.inetHost = inetHost;
            return this;
        }

        public HttpRequestProducerInfoBuilder setSocketKeepAlive(boolean socketKeepAlive) {
            this.socketKeepAlive = socketKeepAlive;
            return this;
        }

    }
}
