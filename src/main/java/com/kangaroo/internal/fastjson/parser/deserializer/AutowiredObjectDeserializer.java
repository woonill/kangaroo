package com.kangaroo.internal.fastjson.parser.deserializer;

import java.lang.reflect.Type;
import java.util.Set;


public interface AutowiredObjectDeserializer extends com.kangaroo.internal.fastjson.parser.deserializer.ObjectDeserializer {
	Set<Type> getAutowiredFor();
}
