/*
package com.kangaroo.handler.context;

import com.kangaroo.EventDispacher;
import com.kangaroo.util.Validate;
import io.reactivex.Observer;

import java.util.function.Function;

class DefaultEventConsumerInit implements Function<AbstractRequestHandlerHolderContext.EventConsumerConfigurable, Observer<EventDispacher.Event>> {

    private final Observer<EventDispacher.Event> consumer;

    DefaultEventConsumerInit(Observer<EventDispacher.Event> initFunc) {
        Validate.notNull(initFunc);
        this.consumer = initFunc;
    }

    @Override
    public Observer<EventDispacher.Event> apply(AbstractRequestHandlerHolderContext.EventConsumerConfigurable configure) {
        return consumer;
    }
}
*/
