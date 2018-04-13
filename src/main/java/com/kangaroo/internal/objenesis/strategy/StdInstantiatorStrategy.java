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
package com.kangaroo.internal.objenesis.strategy;

import com.kangaroo.internal.objenesis.instantiator.ObjectInstantiator;
import com.kangaroo.internal.objenesis.instantiator.android.Android10Instantiator;
import com.kangaroo.internal.objenesis.instantiator.android.Android17Instantiator;
import com.kangaroo.internal.objenesis.instantiator.android.Android18Instantiator;
import com.kangaroo.internal.objenesis.instantiator.basic.AccessibleInstantiator;
import com.kangaroo.internal.objenesis.instantiator.basic.ObjectInputStreamInstantiator;
import com.kangaroo.internal.objenesis.instantiator.gcj.GCJInstantiator;
import com.kangaroo.internal.objenesis.instantiator.jrockit.JRockitLegacyInstantiator;
import com.kangaroo.internal.objenesis.instantiator.perc.PercInstantiator;
import com.kangaroo.internal.objenesis.instantiator.sun.SunReflectionFactoryInstantiator;
import com.kangaroo.internal.objenesis.instantiator.sun.UnsafeFactoryInstantiator;

import java.io.Serializable;

/**
 * Guess the best instantiator for a given class. The instantiator will instantiate the class
 * without calling any constructor. Currently, the selection doesn't depend on the class. It relies
 * on the
 * <ul>
 * <li>JVM version</li>
 * <li>JVM vendor</li>
 * <li>JVM vendor version</li>
 * </ul>
 * However, instantiators are stateful and so dedicated to their class.
 *
 * @author Henri Tremblay
 * @see ObjectInstantiator
 */
public class StdInstantiatorStrategy extends BaseInstantiatorStrategy {

    /**
     * Return an {@link ObjectInstantiator} allowing to create instance without any constructor being
     * called.
     *
     * @param type Class to instantiate
     * @return The ObjectInstantiator for the class
     */
    public <T> ObjectInstantiator<T> newInstantiatorOf(Class<T> type) {

        if (PlatformDescription.isThisJVM(PlatformDescription.HOTSPOT) || PlatformDescription.isThisJVM(PlatformDescription.OPENJDK)) {
            if (PlatformDescription.isGoogleAppEngine()) {
                if (Serializable.class.isAssignableFrom(type)) {
                    return new ObjectInputStreamInstantiator<T>(type);
                }
                return new AccessibleInstantiator<T>(type);
            }
            // The UnsafeFactoryInstantiator would also work. But according to benchmarks, it is 2.5
            // times slower. So I prefer to use this one
            return new SunReflectionFactoryInstantiator<T>(type);
        } else if (PlatformDescription.isThisJVM(PlatformDescription.JROCKIT)) {
            if (PlatformDescription.VM_VERSION.startsWith("1.4")) {
                // JRockit vendor version will be RXX where XX is the version
                // Versions prior to 26 need special handling
                // From R26 on, java.vm.version starts with R
                if (!PlatformDescription.VENDOR_VERSION.startsWith("R")) {
                    // On R25.1 and R25.2, ReflectionFactory should work. Otherwise, we must use the
                    // Legacy instantiator.
                    if (PlatformDescription.VM_INFO == null || !PlatformDescription.VM_INFO.startsWith("R25.1") || !PlatformDescription.VM_INFO.startsWith("R25.2")) {
                        return new JRockitLegacyInstantiator<T>(type);
                    }
                }
            }
            // After that, JRockit became compliant with HotSpot
            return new SunReflectionFactoryInstantiator<T>(type);
        } else if (PlatformDescription.isThisJVM(PlatformDescription.DALVIK)) {
            if (PlatformDescription.ANDROID_VERSION <= 10) {
                // Android 2.3 Gingerbread and lower
                return new Android10Instantiator<T>(type);
            }
            if (PlatformDescription.ANDROID_VERSION <= 17) {
                // Android 3.0 Honeycomb to 4.2 Jelly Bean
                return new Android17Instantiator<T>(type);
            }
            // Android 4.3 and higher (hopefully)
            return new Android18Instantiator<T>(type);
        } else if (PlatformDescription.isThisJVM(PlatformDescription.GNU)) {
            return new GCJInstantiator<T>(type);
        } else if (PlatformDescription.isThisJVM(PlatformDescription.PERC)) {
            return new PercInstantiator<T>(type);
        }

        // Fallback instantiator, should work with most modern JVM
        return new UnsafeFactoryInstantiator<T>(type);

    }
}
