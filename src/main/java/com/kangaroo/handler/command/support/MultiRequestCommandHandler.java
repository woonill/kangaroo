package com.kangaroo.handler.command.support;

import com.kangaroo.handler.RequestHandler;
import com.kangaroo.handler.RequestHandlerInitializer;
import com.kangaroo.handler.command.Command;
import com.kangaroo.handler.command.CommandRequestHandler;
import com.kangaroo.handler.command.MultiCommandHandler;

import java.util.List;
import java.util.function.Function;

public final class MultiRequestCommandHandler extends CommandRequestHandler {

    private Function<String, Class<? extends Command>> commandMapper;

    public MultiRequestCommandHandler(MultiCommandHandler cHandler) {
        super(new DefaultCommandConverter2(cHandler.getCommandMapper()), cHandler);

    }


    abstract static public class MRCInitializer extends RequestHandlerInitializer {

        public MRCInitializer(String name) {
            super(name);
        }

        @Override
        public RequestHandler getHandler(RequestHandlerInitializer.Context context) {
            final List<Object> objectList = this.getCommandHandlerObject(context);
            if (objectList == null || objectList.isEmpty()) {
                throw new IllegalArgumentException("CommandHandler null");
            }
            final MultiCommandHandler commandHandler = new MultiCommandHandler(objectList);
            return new MultiRequestCommandHandler(commandHandler);
        }

        abstract protected List<Object> getCommandHandlerObject(RequestHandlerInitializer.Context context);
    }
}
