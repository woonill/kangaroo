package com.kangaroo.handler.command;

import java.util.Map;

public interface CommandWrapper extends Command {


    String name();

    Map<String, Object> toMap();

    <T extends Command> T toCommand(Class<T> cmdClass);
}
