package com.kangaroo.handler.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class DefaultCommandHandler implements CommandHandler {


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Object handle(Command command, CommandHandlerContext context) {
        if (AbstractCommand.class.isAssignableFrom(command.getClass())) {
            context.components().injector().get(command);
            logger.info("Find AbstractCommand start handle it and to Response");
            AbstractCommand scommand = ((AbstractCommand) command);
            try {
                return scommand.execute(context.props(), context.getAttachment());
            } catch (Throwable te) {
                scommand.onError(te);
                throw te;
            } finally {
                try {
                    scommand.onCompleted();
                } catch (Throwable te) {
                    te.printStackTrace();
                }
            }
        }
        logger.warn("not supported :" + command);
        return null;
    }
}
