package com.kangaroo.handler.command;

import com.kangaroo.Request;

public interface CommandConverter {

    Command convert(Request request);


}
