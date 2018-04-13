package com.kangaroo.handler.command.support;

import com.kangaroo.handler.RequestHandler;
import com.kangaroo.handler.RequestHandlerInitializer;
import com.kangaroo.handler.command.*;

import java.util.function.Function;

public class DefaultCommandRequestHandler extends CommandRequestHandler {

    public DefaultCommandRequestHandler(
            CommandHandler cHandler,
            Function<String, Class<? extends Command>> cmdMapper) {
        super(new DefaultCommandConverter2(cmdMapper), cHandler);
    }

    public DefaultCommandRequestHandler(
            CommandHandler cHandler, CommandConverter converter) {
        super(converter, cHandler);
    }


    abstract public static class DCRInitializer extends RequestHandlerInitializer {

        public DCRInitializer(String name) {
            super(name);
        }

        @Override
        public RequestHandler getHandler(RequestHandlerInitializer.Context context) {

            final CommandHandler commandHandler = getCommandHandler(context);
            final Function<String, Class<? extends Command>> commandMapper = this.getCommandMapper(commandHandler, context);
            return new DefaultCommandRequestHandler(commandHandler, commandMapper);
        }

        protected CommandHandler getCommandHandler(RequestHandlerInitializer.Context context) {
            return new DefaultCommandHandler();
        }

        abstract protected Function<String, Class<? extends Command>> getCommandMapper(
                CommandHandler commandHandler,
                RequestHandlerInitializer.Context context);
    }

}
