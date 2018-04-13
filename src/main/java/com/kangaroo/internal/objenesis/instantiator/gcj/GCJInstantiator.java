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
package com.kangaroo.internal.objenesis.instantiator.gcj;

import java.lang.reflect.InvocationTargetException;

import com.kangaroo.internal.objenesis.ObjenesisException;
import com.kangaroo.internal.objenesis.instantiator.ObjectInstantiator;

/**
 * Instantiates a class by making a call to internal GCJ private methods. It is only supposed to
 * work on GCJ JVMs. This instantiator will not call any constructors.
 *
 * @author Leonardo Mesquita
 * @see ObjectInstantiator
 */
public class GCJInstantiator<T> extends GCJInstantiatorBase<T> {
    public GCJInstantiator(Class<T> type) {
        super(type);
    }

    @Override
    public T newInstance() {
        try {
            return type.cast(newObjectMethod.invoke(dummyStream, type, Object.class));
        } catch (RuntimeException e) {
            throw new ObjenesisException(e);
        } catch (IllegalAccessException e) {
            throw new ObjenesisException(e);
        } catch (InvocationTargetException e) {
            throw new ObjenesisException(e);
        }
    }
}
