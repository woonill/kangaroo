package com.kangaroo.handler.command;

import com.kangaroo.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractCommand implements Command {

    /**
     * @author woonill
     * @since 2012.1.1
     */
    private Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    protected final Logger logger() {
        return this.logger;
    }

    private Map<String, Object> props = new HashMap<>();
    private List<Attachment> attachmentList = new LinkedList<>();

    public final Object execute(
            Map<String, Object> props,
            List<Attachment> attachments) {

        if (props != null && !props.isEmpty()) {
            logger.debug("Setting props");
            this.props.putAll(props);
        }

        if (attachments != null && !attachments.isEmpty()) {
            logger.debug("Setting Attachment list");
            this.attachmentList.addAll(attachments);
        }
        return this.doExecute();
    }

    protected final Object getAttribute(String key) {
        return props.get(key);
    }

    @SuppressWarnings("unchecked")
    protected List<Attachment> attachments() {
        return Collections.synchronizedList(this.attachmentList);
    }

    abstract protected Object doExecute();


    public Map<String, Object> getProps() {
        return this.props;
    }

    private Throwable error;

    protected void onError(Throwable te) {
        this.error = te;
    }

    public Throwable getError() {
        return error;
    }

    @Deprecated
    protected void preDestory() {
    }

    public void onCompleted() {
        this.preDestory();
    }

}
