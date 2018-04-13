package com.kangaroo.handler.command;

import com.kangaroo.util.CUtils;
import com.kangaroo.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Created by woonill on 1/4/16.
 */


public final class MultiCommandHandler implements CommandHandler<Object> {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private Map<String, SAutoCommandHandler> handlerMap;
    private Function<String, Class<? extends Command>> commandMapper;

    public MultiCommandHandler(List<Object> objects) {
        this.handlerMap = get(objects);
        this.commandMapper = SAutoCommandHandler.toCommandMapper(handlerMap);
    }

    @Override
    public Object handle(Command command, CommandHandlerContext context) {

//        String cmdName = command.name();
        String cmdName = command.getClass().getName();
        final SAutoCommandHandler sAutoCommandHandler = handlerMap.get(cmdName);
        if (sAutoCommandHandler == null) {
//            context.goError(new CommandHandleException(400,"Unsupported Command:"+cmdName));
            throw new CommandHandleException(400, "Unsupported Command:" + cmdName);
        }
        return sAutoCommandHandler.handle(command, context);
    }

    public Function<String, Class<? extends Command>> getCommandMapper() {
        return commandMapper;
    }


    static final Map<String, SAutoCommandHandler> get(List<Object> handlers) {


        Map<String, SAutoCommandHandler> handlerMap = new HashMap<>();
        for (Object handler : handlers) {

            final Method[] methods = handler.getClass().getDeclaredMethods();
            for (Method m : methods) {

                final AutoCommandHandler annotation = m.getAnnotation(AutoCommandHandler.class);
                if (annotation != null) {

                    final Class<?>[] parameterTypes = m.getParameterTypes();

                    if (parameterTypes != null
                            && parameterTypes.length == 2
                            && Command.class.isAssignableFrom(parameterTypes[0])
                            && CommandHandlerContext.class.isAssignableFrom(parameterTypes[1])) {

                        Class<? extends Command> commandClass = (Class<? extends Command>) parameterTypes[0];
                        handlerMap.put(commandClass.getName(), new SAutoCommandHandler(annotation.value(), handler, m, commandClass));
                    } else
                        throw new IllegalStateException("AutoCommandHandler.Method.Parameter type must be use CommnadContext");
                }
            }
        }
        return handlerMap;
    }


    private static final class SAutoCommandHandler implements CommandHandler<Object> {


        private Method action;
        private Object handler;
        private Class<? extends Command> commandType;
        private String name;

        public SAutoCommandHandler(String name, Object handler2, Method m, Class<? extends Command> commandType) {

            Validate.notNull(handler2);
            Validate.notNull(m);
            Validate.notNull(commandType);
            this.handler = handler2;
            this.action = m;
            this.commandType = commandType;
            this.name = name;
        }


        public Object handle(Command command, CommandHandlerContext context) {
            return CUtils.invokeMethod(action, handler, new Object[]{context});
        }

        public static Function<String, Class<? extends Command>> toCommandMapper(Map<String, SAutoCommandHandler> handlerMap) {

            final Collection<SAutoCommandHandler> handlers = handlerMap.values();

            return new Function<String, Class<? extends Command>>() {
                @Override
                public Class<? extends Command> apply(String s) {
                    for (SAutoCommandHandler handler : handlers) {
                        if (handler.name.equalsIgnoreCase(s)) {
                            return handler.commandType;
                        }
                    }
                    return null;
                }
            };
        }
    }
}
