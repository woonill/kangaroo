/**
 * COPYRIGHT & LICENSE
 * <p>
 * This code is Copyright (c) 2006 BEA Systems, inc. It is provided free, as-is and without any warranties for the purpose of
 * inclusion in Objenesis or any other open source project with a FSF approved license, as long as this notice is not
 * removed. There are no limitations on modifying or repackaging the code apart from this.
 * <p>
 * BEA does not guarantee that the code works, and provides no support for it. Use at your own risk.
 * <p>
 * Originally developed by Leonardo Mesquita. Copyright notice added by Henrik Ståhl, BEA JRockit Product Manager.
 */

package com.kangaroo.internal.objenesis.instantiator.jrockit;

import java.lang.reflect.Method;

import com.kangaroo.internal.objenesis.ObjenesisException;
import com.kangaroo.internal.objenesis.instantiator.sun.SunReflectionFactoryInstantiator;
import com.kangaroo.internal.objenesis.instantiator.ObjectInstantiator;

/**
 * Instantiates a class by making a call to internal JRockit private methods. It is only supposed to
 * work on JRockit 1.4.2 JVMs prior to release R25.1. From release R25.1 on, JRockit supports
 * sun.reflect.ReflectionFactory, making this "trick" unnecessary. This instantiator will not call
 * any constructors.
 *
 * @author Leonardo Mesquita
 * @see ObjectInstantiator
 * @see SunReflectionFactoryInstantiator
 */
public class JRockitLegacyInstantiator<T> implements ObjectInstantiator<T> {
    private static Method safeAllocObjectMethod = null;

    private static void initialize() {
        if (safeAllocObjectMethod == null) {
            Class<?> memSystem;
            try {
                memSystem = Class.forName("jrockit.vm.MemSystem");
                safeAllocObjectMethod = memSystem.getDeclaredMethod("safeAllocObject",
                        new Class[]{Class.class});
                safeAllocObjectMethod.setAccessible(true);
            } catch (RuntimeException e) {
                throw new ObjenesisException(e);
            } catch (ClassNotFoundException e) {
                throw new ObjenesisException(e);
            } catch (NoSuchMethodException e) {
                throw new ObjenesisException(e);
            }
        }
    }

    private final Class<T> type;

    public JRockitLegacyInstantiator(Class<T> type) {
        initialize();
        this.type = type;
    }

    public T newInstance() {
        try {
            return type.cast(safeAllocObjectMethod.invoke(null, type));
        } catch (Exception e) {
            throw new ObjenesisException(e);
        }
    }
}
