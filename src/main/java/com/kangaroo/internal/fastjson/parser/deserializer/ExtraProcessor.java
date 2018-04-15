package com.kangaroo.internal.fastjson.parser.deserializer;

/**
 * 
 * @author wenshao[szujobs@hotmail.com]
 * @since 1.1.34
 */
public interface ExtraProcessor extends com.kangaroo.internal.fastjson.parser.deserializer.ParseProcess {

    void processExtra(Object object, String key, Object value);
}
