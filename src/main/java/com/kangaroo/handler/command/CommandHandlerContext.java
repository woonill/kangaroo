package com.kangaroo.handler.command;

import com.kangaroo.Attachment;
import com.kangaroo.ComponentContext;

import java.util.List;
import java.util.Map;

public interface CommandHandlerContext {


    Object prop(String key);

    ComponentContext components();

    List<Attachment> getAttachment();

    Map<String, Object> props();

/*    interface Progress {

        Object call(CommandHandlerContext context);
    }*/
}
