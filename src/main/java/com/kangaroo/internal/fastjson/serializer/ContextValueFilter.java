package com.kangaroo.internal.fastjson.serializer;

/**
 * @since 1.2.9
 *
 */
public interface ContextValueFilter extends com.kangaroo.internal.fastjson.serializer.SerializeFilter {
    Object process(com.kangaroo.internal.fastjson.serializer.BeanContext context, Object object, String name, Object value);
}
