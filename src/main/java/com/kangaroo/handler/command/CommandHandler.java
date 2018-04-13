package com.kangaroo.handler.command;

public interface CommandHandler<R> {


    R handle(Command command, CommandHandlerContext context);


    default R onError(
            Exception error,
            Command command,
            CommandHandlerContext context) throws Exception {
        throw error;
    }
}
