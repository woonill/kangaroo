package com.kangaroo;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultRequest implements Request{


    /**
     * @author woonill
     */
    private static final long serialVersionUID = 4584652672257781915L;

    public static final String SID = "session_id";


    private String path;
    private byte[] payload;
    private List<Header> headers = new LinkedList<Header>();

    public static final String REQUEST_TYPE_NAME = "contents-type";

    private Map<String, Object> attributeMap = new HashMap<>();
    private AtomicBoolean mapPlayloaded = new AtomicBoolean(false);
    private Map<String, Object> mapPayload = null;

    public final static String ATTCHMENT_FIELD = "_attachment";
    public final static String HEADER_FIELD = "_req-header";


    private String id = UUID.randomUUID().toString();

    protected DefaultRequest() {
    }

    public final String id() {
        return id;
    }

    public DefaultRequest(String path, byte[] payload) {
        this.path = path;
        this.payload = payload;
    }


    public DefaultRequest(String path, byte[] payload, Header... value) {
        this.path = path;
        this.payload = payload;
        if (value != null && value.length > 0) {
            for (Header header : value) {
                this.addHeader(header.name(), (String) header.value());
            }
        }
    }

    public DefaultRequest(
            String requestURI,
            byte[] payload2,
            Header[] headers2,
            Attachment[] attachments) {

        this.path = requestURI;
        this.payload = payload2;

        if (attachments != null) {
            this.attributeMap.put(ATTCHMENT_FIELD, attachments);
        }
//		this.attachments.addAll(attachments);
        this.headers.addAll(Arrays.asList(headers2));
        this.attributeMap.put(HEADER_FIELD, this.headers);
    }


    private Request addHeader(String string, String value) {
        this.headers.add(new Header(string, value));
        return this;
    }

    public String uri() {
        return this.path;
    }
    public byte[] payload() {
        return this.payload;
    }
    public Object getAttribute(String key) {
        return attributeMap.get(key);
    }
    public Collection<Header> headers() {
        return Collections.unmodifiableCollection(this.headers);
    }

    public String header(String key) {
        for (Header header : this.headers) {
            if (header.name().equalsIgnoreCase(key)) {
                return (String) header.value();
            }
        }
        return "";
    }

    public List<Attachment> getAttachment() {
        Attachment[] attachments = (Attachment[]) getAttribute(DefaultRequest.ATTCHMENT_FIELD);
        if (attachments == null) {
            return Collections.EMPTY_LIST;
        }
        return Arrays.asList(attachments);
    }

}
