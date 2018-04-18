package com.kangaroo.handler.command;

import com.kangaroo.Attachment;
import com.kangaroo.ComponentFactory;

import java.util.List;
import java.util.Map;

public interface CommandHandlerContext {


    Object prop(String key);

    ComponentFactory components();

    List<Attachment> getAttachment();

    Map<String, Object> props();

}
