/**
 * Copyright 2006-2015 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kangaroo.internal.objenesis.instantiator.sun;

import com.kangaroo.internal.objenesis.ObjenesisException;
import com.kangaroo.internal.objenesis.instantiator.ObjectInstantiator;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Instantiates an object, WITHOUT calling it's constructor, using
 * sun.misc.Unsafe.allocateInstance(). Unsafe and its methods are implemented by most
 * modern JVMs.
 *
 * @author Henri Tremblay
 * @see ObjectInstantiator
 */
@SuppressWarnings("restriction")
public class UnsafeFactoryInstantiator<T> implements ObjectInstantiator<T> {

    private static Unsafe unsafe;
    private final Class<T> type;

    public UnsafeFactoryInstantiator(Class<T> type) {
        if (unsafe == null) {
            Field f;
            try {
                f = Unsafe.class.getDeclaredField("theUnsafe");
            } catch (NoSuchFieldException e) {
                throw new ObjenesisException(e);
            }
            f.setAccessible(true);
            try {
                unsafe = (Unsafe) f.get(null);
            } catch (IllegalAccessException e) {
                throw new ObjenesisException(e);
            }
        }
        this.type = type;
    }

    public T newInstance() {
        try {
            return type.cast(unsafe.allocateInstance(type));
        } catch (InstantiationException e) {
            throw new ObjenesisException(e);
        }
    }
}