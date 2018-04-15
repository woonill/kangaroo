package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.Type;

/**
 * @author wenshao[szujobs@hotmail.com]
 * @since 1.1.34
 */
public interface ExtraTypeProvider extends com.kangaroo.internal.fastjson.parser.deserializer.ParseProcess {

    Type getExtraType(Object object, String key);
}
