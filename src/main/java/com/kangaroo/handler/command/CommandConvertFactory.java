package com.kangaroo.handler.command;

import java.util.Map;

public interface CommandConvertFactory {

    CommandConverter create(Map<String, Class<? extends Command>> cmdMaps);


}
