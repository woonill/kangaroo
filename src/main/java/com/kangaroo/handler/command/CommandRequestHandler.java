package com.kangaroo.handler.command;


import com.kangaroo.*;
import com.kangaroo.handler.*;
import com.kangaroo.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CommandRequestHandler implements RequestHandler {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CommandConverter commandConverter;
    private final CommandHandler handler;


    CommandRequestHandler() {
        throw new IllegalStateException("No instance!");
    }

    public CommandRequestHandler(CommandHandler cHandler) {
        this(new DefaultCommandConverter(), cHandler);
    }

    public CommandRequestHandler(CommandConverter cc2, CommandHandler cHandler) {
        this.commandConverter = cc2;
        this.handler = cHandler;
    }


    protected final Logger logger() {
        return this.logger;
    }


    @Override
    public Response handle(RequestHandlerContext context) {

        logger().debug("Start Command handler request ");
//		logger().info("Attachment size:"+request.attachments().toList().size());

        Request request = context.request();
        Command command = commandConverter.convert(request);

        if (command == null) {
            logger().warn("not found command");
            return context.handle();
        }
        CommandHandlerContext chc = toCommandHandlerContext(context);

        try {
            Object rm = this.handler.handle(command, chc);
//			return context.responser().response(JSON.toJSONBytes(rm),Message.Type.json);
//            return context.responser().objectToResponse(rm);
            return context.getResponseFactory(request).response(rm);
        } catch (Exception te) {
            try {
                Object obs = this.handler.onError(te, command, chc);
//                return context.responser().objectToResponse(obs);
                return context.getResponseFactory(request).response(obs);
            } catch (Exception e) {
                onError(e, command, chc);
                throw new RuntimeException(e);
            }
        }
    }

    protected void onError(
            Throwable exception,
            Command command,
            CommandHandlerContext context) {
        exception.printStackTrace();
    }


    protected CommandHandlerContext toCommandHandlerContext(RequestHandlerContext context) {
        return new DefaultCommandHandlerContext(context);
    }

    static final class DefaultCommandHandlerContext implements CommandHandlerContext {

        private RequestHandlerContext reqContext;

        public DefaultCommandHandlerContext(RequestHandlerContext context) {
            this.reqContext = context;
        }

        @Override
        public Object prop(String key) {
            return reqContext.props().get(key);
        }

        @Override
        public ComponentContext components() {
            return reqContext.getComponents();
        }

        @Override
        public List<Attachment> getAttachment() {
            return reqContext.request().getAttachment();
        }

        @Override
        public Map<String, Object> props() {
            return reqContext.props();
        }

    }


    public static final class DefaultCommandConverter2 extends DefaultCommandConverter {

        private Function<String, Class<? extends Command>> cmdMapper;

        public DefaultCommandConverter2(Function<String, Class<? extends Command>> cmdMapper) {
            Validate.notNull(cmdMapper);
            this.cmdMapper = cmdMapper;
        }

        @Override
        protected Command toCommand2(CommandWrapper wrapper) {
            final Class<? extends Command> cmdClazz = cmdMapper.apply(wrapper.name());
            if (cmdClazz == null) {
                logger().warn("not found command:" + wrapper.name());
                throw new IllegalArgumentException("error not found command:" + wrapper.name());
            }
            logger().debug("ToCommand:" + cmdClazz.getName());
            return wrapper.toCommand(cmdClazz);
        }
    }

    abstract public static class Init extends RequestHandlerInitializer {

        public Init(String name) {
            super(name);
        }

        @Override
        public RequestHandler getHandler(RequestHandlerInitializer.Context context) {
            return new CommandRequestHandler(this.getConverter(context), getCommandHandler(context));
        }

        abstract protected CommandHandler getCommandHandler(RequestHandlerInitializer.Context context);

        protected CommandConverter getConverter(RequestHandlerInitializer.Context context) {
            return new DefaultCommandConverter();
        }
    }
}
