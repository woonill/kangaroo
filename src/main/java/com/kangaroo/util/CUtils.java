package com.kangaroo.util;

import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;


public final class CUtils {

    public static final String ARRAY_SUFFIX = "[]";

    /**
     * Prefix for internal array class names: "["
     */
    private static final String INTERNAL_ARRAY_PREFIX = "[";

    /**
     * Prefix for internal non-primitive array class names: "[L"
     */
    private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

    /** The package separator character '.' */
//	private static final char PACKAGE_SEPARATOR = '.';

    /** The inner class separator character '$' */
//	private static final char INNER_CLASS_SEPARATOR = '$';
    /**
     * The CGLIB class separator character "$$"
     */

    public static final String CGLIB_CLASS_SEPARATOR = "$$";
    /**
     * The ".class" file suffix
     */
    public static final String CLASS_FILE_SUFFIX = ".class";


    /**
     * The package separator character '.'
     */
    private static final char PACKAGE_SEPARATOR = '.';

    /** The path separator character '/' */
//	private static final char PATH_SEPARATOR = '/';

    /**
     * The inner class separator character '$'
     */
    private static final char INNER_CLASS_SEPARATOR = '$';

    /**
     * The CGLIB class separator character "$$"
     */


//	private static final Log logger = LogFactory.getLog(WClassUtil.class.getName());
    private static ClassLoader defaultClassLoader = null;
    public static final char INNER_CLASS_SEPARATOR_CHAR = '$';


    private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new HashMap<Class<?>, Class<?>>(8);
    private static final Map<Class<?>, Class<?>> primitiveTypeToWrapperMap = new HashMap<Class<?>, Class<?>>(8);
    private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<String, Class<?>>(32);
    private static final Map<String, Class<?>> commonClassCache = new HashMap<String, Class<?>>(32);


    public static byte[] toByteArray(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray();
            oos.close();
            bos.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return bytes;
    }

    public static class ClassLoaderObjectInputStream extends ObjectInputStream {
        private ClassLoader classLoader;

        public ClassLoaderObjectInputStream(InputStream is) throws IOException {
            this(Thread.currentThread().getContextClassLoader(), is);
        }

        public ClassLoaderObjectInputStream(ClassLoader classLoader, InputStream in) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                String name = desc.getName();
                return Class.forName(name, false, classLoader);
            } catch (ClassNotFoundException e) {
                return super.resolveClass(desc);
            }
        }
    }


    public static Object toObject(byte[] res) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(res);
            ObjectInputStream ois = new ClassLoaderObjectInputStream(bis);
            Object obj = ois.readObject();
            ois.close();
            bis.close();
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static {
        primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
        primitiveWrapperTypeMap.put(Byte.class, byte.class);
        primitiveWrapperTypeMap.put(Character.class, char.class);
        primitiveWrapperTypeMap.put(Double.class, double.class);
        primitiveWrapperTypeMap.put(Float.class, float.class);
        primitiveWrapperTypeMap.put(Integer.class, int.class);
        primitiveWrapperTypeMap.put(Long.class, long.class);
        primitiveWrapperTypeMap.put(Short.class, short.class);

        for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
            primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
            registerCommonClasses(entry.getKey());
        }

        Set<Class<?>> primitiveTypes = new HashSet<Class<?>>(32);
        primitiveTypes.addAll(primitiveWrapperTypeMap.values());
        primitiveTypes.addAll(Arrays.asList(new Class<?>[]{
                boolean[].class, byte[].class, char[].class, double[].class,
                float[].class, int[].class, long[].class, short[].class}));
        primitiveTypes.add(void.class);
        for (Class<?> primitiveType : primitiveTypes) {
            primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
        }

        registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
                Float[].class, Integer[].class, Long[].class, Short[].class);
        registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
                Object.class, Object[].class, Class.class, Class[].class);
        registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
                Error.class, StackTraceElement.class, StackTraceElement[].class);
    }


    public static boolean isBaseType(Class<?> type) {
        return commonClassCache.get(type) != null;
    }

    private static void registerCommonClasses(Class<?>... commonClasses) {
        for (Class<?> clazz : commonClasses) {
            commonClassCache.put(clazz.getName(), clazz);
        }
    }

    public static ClassLoader getDefaultClassLoader() {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = CUtils.class.getClassLoader();
        }
        return cl;
    }

    public static void setClassLoader(ClassLoader cl) {
        defaultClassLoader = cl;
    }


    public static Class<?> forName(String string) {
        return forName(string, getDefaultClassLoader());
    }

    public static Class<?> forName(String name, ClassLoader classLoader) {
        Validate.notNull(name, "Name must not be null");

        Class<?> clazz = resolvePrimitiveClassName(name);
        if (clazz == null) {
            clazz = commonClassCache.get(name);
        }
        if (clazz != null) {
            return clazz;
        }

        // "java.lang.String[]" style arrays
        if (name.endsWith(ARRAY_SUFFIX)) {
            String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
            Class<?> elementClass = forName(elementClassName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[Ljava.lang.String;" style arrays
        if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
            String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        // "[[I" or "[[Ljava.lang.String;" style arrays
        if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
            String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
            Class<?> elementClass = forName(elementName, classLoader);
            return Array.newInstance(elementClass, 0).getClass();
        }

        ClassLoader classLoaderToUse = classLoader;
        if (classLoaderToUse == null) {
            classLoaderToUse = getDefaultClassLoader();
        }
        try {
            return classLoaderToUse.loadClass(name);
        } catch (ClassNotFoundException ex) {
            int lastDotIndex = name.lastIndexOf('.');
            if (lastDotIndex != -1) {
                String innerClassName = name.substring(0, lastDotIndex) + '$' + name.substring(lastDotIndex + 1);
                try {
                    return classLoaderToUse.loadClass(innerClassName);
                } catch (ClassNotFoundException ex2) {
                    // swallow - let original exception get through
                }
            }
            throw new IllegalStateException(ex);
        }
    }

    public static Class<?> resolveClassName(String className, ClassLoader classLoader) throws IllegalArgumentException {
        try {
            return forName(className, classLoader);
        }
//		catch (ClassNotFoundException ex) {
//			throw new IllegalArgumentException("Cannot find class [" + className + "]", ex);
//		}
        catch (LinkageError ex) {
            throw new IllegalArgumentException(
                    "Error loading class [" + className + "]: problem with class file or dependent class.", ex);
        }
    }

    public static Class<?> resolvePrimitiveClassName(String name) {
        Class<?> result = null;
        if (name != null && name.length() <= 8) {
            // Could be a primitive - likely.
            result = primitiveTypeNameMap.get(name);
        }
        return result;
    }


    public static Class<?> getClass(String str) {
        try {
            return CUtils.forName(str, CUtils.getDefaultClassLoader());
        } catch (Exception e) {
//			logger.error("not found class["+e.getMessage()+"]",e);
            throw new IllegalArgumentException(e);
        }
    }


    public static Object getInstance(String s) {
        if (defaultClassLoader == null) {
            return getInstance(s, CUtils.getDefaultClassLoader());
        }
        return getInstance(s, defaultClassLoader);
    }

    public static Object getInstance(Class<?> s) {
        return getInstance(s, defaultClassLoader);
    }


    public static boolean isInnerClass(Class<?> cls) {
        if (cls == null) {
            return false;
        }
        return cls.getName().indexOf(INNER_CLASS_SEPARATOR_CHAR) >= 0;
    }

    public static Object getInstance(String s, ClassLoader cl) {
        try {
            Class<?> c = CUtils.forName(s, cl);
            if (isInnerClass(c)) {
                if (!Modifier.isStatic(c.getModifiers())) {
                    return getInnerInstance(c, cl);
                }
            }
            return c.newInstance();
        } catch (Exception e) {
            //		logger.error("not found class["+e.getMessage()+"]",e);
            throw new IllegalArgumentException(e);
        }
    }

    public static Object getInstance(Class<?> clazz, ClassLoader classLoader) {
        try {
            if (isInnerClass(clazz)) {
                if (!Modifier.isStatic(clazz.getModifiers())) {
                    return getInnerInstance(clazz, classLoader);
                }
            }
            return clazz.newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Object getInnerInstance(Class<?> s, ClassLoader cl) {
        try {
            String name = s.getName();
            int index = name.indexOf(INNER_CLASS_SEPARATOR_CHAR);
            String pName = name.substring(0, index);
            Class<?> pclss = getClass(pName);
            Constructor<?> clazz = s.getDeclaredConstructor(pclss);
            CUtils.makeAccessible(clazz);
            Object outer = pclss.newInstance();
            return clazz.newInstance(outer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T instantiateClass(final Class<T> clazz, final Object... args) {
        Validate.notNull(clazz, "Class must not be null");
        try {
            Constructor<?>[] cols = clazz.getConstructors();
            Constructor<T> tcol = null;
            if (args == null || args.length < 1) {
                tcol = clazz.getConstructor();
            } else {
                for (Constructor<?> co : cols) {
                    Class<?>[] cTypes = co.getParameterTypes();
                    if (parameterTypeMatched(cTypes, args)) {
                        tcol = clazz.getConstructor(cTypes);
                        break;
                    }
                }
            }
            if (tcol == null) {
                throw new IllegalArgumentException("Can not found Class Constructor for[" + args + "] ");
            }
            return instantiateClass(tcol, args);
        } catch (Exception e) {
//			e.printStackTrace();
//			System.out.println(clazz.getName()+"]");
            throw new IllegalArgumentException(e);
        }
    }


    public static boolean parameterTypeMatched(Class<?>[] parameterTypes,
                                               Object[] args) {
        if (parameterTypes.length == args.length) {
            int count = 0;
            for (Class<?> pType : parameterTypes) {
                Class<?> ppType = pType;
                Class<?> tarType = primitiveTypeToWrapperMap.get(pType);
                if (tarType != null) {
                    ppType = tarType;
                }
                for (Object obs : args) {
                    if (ppType.getName().equalsIgnoreCase(obs.getClass().getName())) {
                        count = count + 1;
                        break;
                    }
                }
            }
            if (count > 0) {
                if (count == args.length) {
                    return true;
                }
            }
        }
        return false;
    }

    public static <T> T instantiateClass(Constructor<T> ctor, Object... args) {
        Validate.notNull(ctor, "Constructor must not be null");
        try {
            CUtils.makeAccessible(ctor);
            return ctor.newInstance(args);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }


// Method and Filed =========================================================================================================================================>>

    public static Field findField(Class<?> clazz, String name) {
        return findField(clazz, name, null);
    }

    public static Field findField(Class<?> clazz, String name, Class<?> type) {
        Validate.notNull(clazz, "Class must not be null");
        Validate.isTrue(name != null || type != null, "Either name or type of the field must be specified");
        Class<?> searchType = clazz;
        while (!Object.class.equals(searchType) && searchType != null) {
            Field[] fields = searchType.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                Field field = fields[i];
                if ((name == null || name.equals(field.getName()))
                        && (type == null || type.equals(field.getType()))) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    public static void setField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException ex) {
            handleReflectionException(ex);
            throw new IllegalStateException(
                    "Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    /**
     * Get the field represented by the supplied {@link Field field object} on
     * the specified {@link Object target object}. In accordance with
     * {@link Field#get(Object)} semantics, the returned value is
     * automatically wrapped if the underlying field has a primitive type.
     * <p>Thrown exceptions are handled via a call to
     * {@link #handleReflectionException(Exception)}.
     *
     * @param field  the field to get
     * @param target the target object from which to get the field
     * @return the field's current value
     */
    public static Object getField(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException ex) {
            handleReflectionException(ex);
            throw new IllegalStateException(
                    "Unexpected reflection exception - " + ex.getClass().getName() + ": " + ex.getMessage());
        }
    }

    /**
     * Attempt to find a {@link Method} on the supplied class with the supplied name
     * and no parameters. Searches all superclasses up to <code>Object</code>.
     * <p>Returns <code>null</code> if no {@link Method} can be found.
     *
     * @param clazz the class to introspect
     * @param name  the name of the method
     * @return the Method object, or <code>null</code> if none found
     */
    public static Method findMethod(Class<?> clazz, String name) {
        return findMethod(clazz, name, new Class[0]);
    }

    /**
     * Attempt to find a {@link Method} on the supplied class with the supplied name
     * and parameter types. Searches all superclasses up to <code>Object</code>.
     * <p>Returns <code>null</code> if no {@link Method} can be found.
     *
     * @param clazz      the class to introspect
     * @param name       the name of the method
     * @param paramTypes the parameter types of the method
     *                   (may be <code>null</code> to indicate any signature)
     * @return the Method object, or <code>null</code> if none found
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>[] paramTypes) {
        Validate.notNull(clazz, "Class must not be null");
        Validate.notNull(name, "Method name must not be null");
        Class<?> searchType = clazz;
        while (!Object.class.equals(searchType) && searchType != null) {
            Method[] methods = (searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods());
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (name.equals(method.getName()) &&
                        (paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    /**
     * Invoke the specified {@link Method} against the supplied target object
     * with no arguments. The target object can be <code>null</code> when
     * invoking a static {@link Method}.
     * <p>Thrown exceptions are handled via a call to {@link #handleReflectionException}.
     *
     * @param method the method to invoke
     * @param target the target object to invoke the method on
     * @return the invocation result, if any
     * @see #invokeMethod(Method, Object, Object[])
     */
    public static Object invokeMethod(Method method, Object target) {
        return invokeMethod(method, target, null);
    }

    /**
     * Invoke the specified {@link Method} against the supplied target object
     * with the supplied arguments. The target object can be <code>null</code>
     * when invoking a static {@link Method}.
     * <p>Thrown exceptions are handled via a call to {@link #handleReflectionException}.
     *
     * @param method the method to invoke
     * @param target the target object to invoke the method on
     * @param args   the invocation arguments (may be <code>null</code>)
     * @return the invocation result, if any
     */
    public static Object invokeMethod(Method method, Object target, Object[] args) {
        try {
            return method.invoke(target, args);
        } catch (Exception ex) {
            ex.printStackTrace();
            handleReflectionException(ex);
        }
        throw new IllegalStateException("Should never get here");
    }

    /**
     * Invoke the specified JDBC API {@link Method} against the supplied
     * target object with no arguments.
     *
     * @param method the method to invoke
     * @param target the target object to invoke the method on
     * @return the invocation result, if any
     * @throws SQLException the JDBC API SQLException to rethrow (if any)
     * @see #invokeJdbcMethod(Method, Object, Object[])
     */
    public static Object invokeJdbcMethod(Method method, Object target) throws SQLException {
        return invokeJdbcMethod(method, target, null);
    }

    /**
     * Invoke the specified JDBC API {@link Method} against the supplied
     * target object with the supplied arguments.
     *
     * @param method the method to invoke
     * @param target the target object to invoke the method on
     * @param args   the invocation arguments (may be <code>null</code>)
     * @return the invocation result, if any
     * @throws SQLException the JDBC API SQLException to rethrow (if any)
     * @see #invokeMethod(Method, Object, Object[])
     */
    public static Object invokeJdbcMethod(Method method, Object target, Object[] args) throws SQLException {
        try {
            return method.invoke(target, args);
        } catch (IllegalAccessException ex) {
            handleReflectionException(ex);
        } catch (InvocationTargetException ex) {
            if (ex.getTargetException() instanceof SQLException) {
                throw (SQLException) ex.getTargetException();
            }
            handleInvocationTargetException(ex);
        }
        throw new IllegalStateException("Should never get here");
    }

    /**
     * Handle the given reflection exception. Should only be called if
     * no checked exception is expected to be thrown by the target method.
     * <p>Throws the underlying RuntimeException or Error in case of an
     * InvocationTargetException with such a root cause. Throws an
     * IllegalStateException with an appropriate message else.
     *
     * @param ex the reflection exception to handle
     */
    public static void handleReflectionException(Exception ex) {
        if (ex instanceof NoSuchMethodException) {
            throw new IllegalStateException("Method not found: " + ex.getMessage());
        }
        if (ex instanceof IllegalAccessException) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage());
        }
        if (ex instanceof InvocationTargetException) {
            handleInvocationTargetException((InvocationTargetException) ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        handleUnexpectedException(ex);
    }

    /**
     * Handle the given invocation target exception. Should only be called if
     * no checked exception is expected to be thrown by the target method.
     * <p>Throws the underlying RuntimeException or Error in case of such
     * a root cause. Throws an IllegalStateException else.
     *
     * @param ex the invocation target exception to handle
     */
    public static void handleInvocationTargetException(InvocationTargetException ex) {
        rethrowRuntimeException(ex.getTargetException());
    }

    /**
     * Rethrow the given {@link Throwable exception}, which is presumably the
     * <em>target exception</em> of an {@link InvocationTargetException}.
     * Should only be called if no checked exception is expected to be thrown by
     * the target method.
     * <p>Rethrows the underlying exception cast to an {@link RuntimeException}
     * or {@link Error} if appropriate; otherwise, throws an
     * {@link IllegalStateException}.
     *
     * @param ex the exception to rethrow
     * @throws RuntimeException the rethrown exception
     */
    public static void rethrowRuntimeException(Throwable ex) {
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        handleUnexpectedException(ex);
    }

    /**
     * Rethrow the given {@link Throwable exception}, which is presumably the
     * <em>target exception</em> of an {@link InvocationTargetException}.
     * Should only be called if no checked exception is expected to be thrown by
     * the target method.
     * <p>Rethrows the underlying exception cast to an {@link Exception} or
     * {@link Error} if appropriate; otherwise, throws an
     * {@link IllegalStateException}.
     *
     * @param ex the exception to rethrow
     * @throws Exception the rethrown exception (in case of a checked exception)
     */
    public static void rethrowException(Throwable ex) throws Exception {
        if (ex instanceof Exception) {
            throw (Exception) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        handleUnexpectedException(ex);
    }

    /**
     * Throws an IllegalStateException with the given exception as root cause.
     *
     * @param ex the unexpected exception
     */
    private static void handleUnexpectedException(Throwable ex) {
        // Needs to avoid the chained constructor for JDK 1.4 compatibility.
        IllegalStateException isex = new IllegalStateException("Unexpected exception thrown");
        isex.initCause(ex);
        throw isex;
    }

    /**
     * Determine whether the given method explicitly declares the given exception
     * or one of its superclasses, which means that an exception of that type
     * can be propagated as-is within a reflective invocation.
     *
     * @param method        the declaring method
     * @param exceptionType the exception to throw
     * @return <code>true</code> if the exception can be thrown as-is;
     * <code>false</code> if it needs to be wrapped
     */
    public static boolean declaresException(Method method, Class<?> exceptionType) {
        Validate.notNull(method, "Method must not be null");
        Class<?>[] declaredExceptions = method.getExceptionTypes();
        for (int i = 0; i < declaredExceptions.length; i++) {
            Class<?> declaredException = declaredExceptions[i];
            if (declaredException.isAssignableFrom(exceptionType)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Determine whether the given field is a "public static final" constant.
     *
     * @param field the field to check
     */
    public static boolean isPublicStaticFinal(Field field) {
        int modifiers = field.getModifiers();
        return (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
    }

    /**
     * Determine whether the given method is an "equals" method.
     *
     * @see Object#equals
     */
    public static boolean isEqualsMethod(Method method) {
        if (method == null || !method.getName().equals("equals")) {
            return false;
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        return (paramTypes.length == 1 && paramTypes[0] == Object.class);
    }

    /**
     * Determine whether the given method is a "hashCode" method.
     *
     * @see Object#hashCode
     */
    public static boolean isHashCodeMethod(Method method) {
        return (method != null && method.getName().equals("hashCode") &&
                method.getParameterTypes().length == 0);
    }

    /**
     * Determine whether the given method is a "toString" method.
     *
     * @see Object#toString()
     */
    public static boolean isToStringMethod(Method method) {
        return (method != null && method.getName().equals("toString") &&
                method.getParameterTypes().length == 0);
    }


    /**
     * Make the given field accessible, explicitly setting it accessible if necessary.
     * The <code>setAccessible(true)</code> method is only called when actually necessary,
     * to avoid unnecessary conflicts with a JVM SecurityManager (if active).
     *
     * @param field the field to make accessible
     * @see Field#setAccessible
     */
    public static void makeAccessible(Field field) {
        if (!Modifier.isPublic(field.getModifiers()) ||
                !Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
            field.setAccessible(true);
        }
    }

    /**
     * Make the given method accessible, explicitly setting it accessible if necessary.
     * The <code>setAccessible(true)</code> method is only called when actually necessary,
     * to avoid unnecessary conflicts with a JVM SecurityManager (if active).
     *
     * @param method the method to make accessible
     * @see Method#setAccessible
     */
    public static void makeAccessible(Method method) {
        if (!Modifier.isPublic(method.getModifiers()) ||
                !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            method.setAccessible(true);
        }
    }


    public static String convertResourcePathToClassName(String resourcePath) {
        Validate.notNull(resourcePath, "Resource path must not be null");
        return resourcePath.replace('/', '.');
    }

    /**
     * Make the given constructor accessible, explicitly setting it accessible if necessary.
     * The <code>setAccessible(true)</code> method is only called when actually necessary,
     * to avoid unnecessary conflicts with a JVM SecurityManager (if active).
     *
     * @param ctor the constructor to make accessible
     * @see Constructor#setAccessible
     */
    public static void makeAccessible(Constructor<?> ctor) {
        if (!Modifier.isPublic(ctor.getModifiers()) ||
                !Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) {
            ctor.setAccessible(true);
        }
    }


    /**
     * Perform the given callback operation on all matching methods of the
     * given class and superclasses.
     * <p>The same named method occurring on subclass and superclass will
     * appear twice, unless excluded by a {@link MethodFilter}.
     *
     * @param targetClass class to start looking at
     * @param mc          the callback to invoke for each method
     * @see #doWithMethods(Class, MethodCallback, MethodFilter)
     */
    public static void doWithMethods(Class<?> targetClass, MethodCallback mc) throws IllegalArgumentException {
        doWithMethods(targetClass, mc, null);
    }

    /**
     * Perform the given callback operation on all matching methods of the
     * given class and superclasses.
     * <p>The same named method occurring on subclass and superclass will
     * appear twice, unless excluded by the specified {@link MethodFilter}.
     *
     * @param targetClass class to start looking at
     * @param mc          the callback to invoke for each method
     * @param mf          the filter that determines the methods to apply the callback to
     */
    public static void doWithMethods(Class<?> targetClass, MethodCallback mc, MethodFilter mf)
            throws IllegalArgumentException {

        // Keep backing up the inheritance hierarchy.
        do {
            Method[] methods = targetClass.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                if (mf != null && !mf.matches(methods[i])) {
                    continue;
                }
                try {
                    mc.doWith(methods[i]);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(
                            "Shouldn't be illegal to access method '" + methods[i].getName() + "': " + ex);
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != null);
    }

    /**
     * Get all declared methods on the leaf class and all superclasses.
     * Leaf class methods are included first.
     */
    public static Method[] getAllDeclaredMethods(Class<?> leafClass) throws IllegalArgumentException {
        final List<Method> list = new ArrayList<Method>(32);
        doWithMethods(leafClass, new MethodCallback() {
            public void doWith(Method method) {
                list.add(method);
            }
        });
        return (Method[]) list.toArray(new Method[list.size()]);
    }


    /**
     * Invoke the given callback on all fields in the target class,
     * going up the class hierarchy to get all declared fields.
     *
     * @param targetClass the target class to analyze
     * @param fc          the callback to invoke for each field
     */
    public static void doWithFields(Class<?> targetClass, FieldCallback fc) throws IllegalArgumentException {
        doWithFields(targetClass, fc, null);
    }

    /**
     * Invoke the given callback on all fields in the target class,
     * going up the class hierarchy to get all declared fields.
     *
     * @param targetClass the target class to analyze
     * @param fc          the callback to invoke for each field
     * @param ff          the filter that determines the fields to apply the callback to
     */
    public static void doWithFields(Class<?> targetClass, FieldCallback fc, FieldFilter ff)
            throws IllegalArgumentException {

        // Keep backing up the inheritance hierarchy.
        do {
            // Copy each field declared on this class unless it's static or file.
            Field[] fields = targetClass.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                // Skip static and final fields.
                if (ff != null && !ff.matches(fields[i])) {
                    continue;
                }
                try {
                    fc.doWith(fields[i]);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(
                            "Shouldn't be illegal to access field '" + fields[i].getName() + "': " + ex);
                }
            }
            targetClass = targetClass.getSuperclass();
        }
        while (targetClass != null && targetClass != Object.class);
    }

    /**
     * Given the source object and the destination, which must be the same class
     * or a subclass, copy all fields, including inherited fields. Designed to
     * work on objects with public no-arg constructors.
     *
     * @throws IllegalArgumentException if the arguments are incompatible
     */
    public static void shallowCopyFieldState(final Object src, final Object dest) throws IllegalArgumentException {
        if (src == null) {
            throw new IllegalArgumentException("Source for field copy cannot be null");
        }
        if (dest == null) {
            throw new IllegalArgumentException("Destination for field copy cannot be null");
        }
        if (!src.getClass().isAssignableFrom(dest.getClass())) {
            throw new IllegalArgumentException("Destination class [" + dest.getClass().getName() +
                    "] must be same or subclass as source class [" + src.getClass().getName() + "]");
        }
        doWithFields(src.getClass(), new FieldCallback() {
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                makeAccessible(field);
                Object srcValue = field.get(src);
                field.set(dest, srcValue);
            }
        }, COPYABLE_FIELDS);
    }


    public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
        Validate.notNull(lhsType, "Left-hand side type must not be null");
        Validate.notNull(rhsType, "Right-hand side type must not be null");
        if (lhsType.isAssignableFrom(rhsType)) {
            return true;
        }
        if (lhsType.isPrimitive()) {
            Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
            if (resolvedPrimitive != null && lhsType.equals(resolvedPrimitive)) {
                return true;
            }
        } else {
            Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
            if (resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if the given type is assignable from the given value,
     * assuming setting by reflection. Considers primitive wrapper classes
     * as assignable to the corresponding primitive types.
     *
     * @param type  the target type
     * @param value the value that should be assigned to the type
     * @return if the type is assignable from the value
     */
    public static boolean isAssignableValue(Class<?> type, Object value) {
        Validate.notNull(type, "Type must not be null");
        return (value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive());
    }


    /**
     * Action to take on each method.
     */
    public static interface MethodCallback {

        /**
         * Perform an operation using the given method.
         *
         * @param method the method to operate on
         */
        void doWith(Method method) throws IllegalArgumentException, IllegalAccessException;
    }


    /**
     * Callback optionally used to method fields to be operated on by a method callback.
     */
    public static interface MethodFilter {

        /**
         * Determine whether the given method matches.
         *
         * @param method the method to check
         */
        boolean matches(Method method);
    }


    /**
     * Callback interface invoked on each field in the hierarchy.
     */
    public static interface FieldCallback {

        /**
         * Perform an operation using the given field.
         *
         * @param field the field to operate on
         */
        void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
    }


    /**
     * Callback optionally used to filter fields to be operated on by a field callback.
     */
    public static interface FieldFilter {

        /**
         * Determine whether the given field matches.
         *
         * @param field the field to check
         */
        boolean matches(Field field);
    }


    /**
     * Pre-built FieldFilter that matches all non-static, non-final fields.
     */
    public static FieldFilter COPYABLE_FIELDS = new FieldFilter() {
        public boolean matches(Field field) {
            return !(Modifier.isStatic(field.getModifiers()) ||
                    Modifier.isFinal(field.getModifiers()));
        }
    };


    public static String getQualifiedName(Class<?> clazz) {

        if (clazz == null) {
            throw new NullPointerException("Class must not be null");
        }

        if (clazz.isArray()) {
            return getQualifiedNameForArray(clazz);
        } else {
            return clazz.getName();
        }
    }

    public static String getShortName(Class<?> clazz) {
        return getShortName(getQualifiedName(clazz));
    }

    public static String getShortName(String className) {
        if (StrUtils.isEmpty(className)) {
            throw new NullPointerException("ClassName must not be null");
        }

        int lastDotIndex = className.lastIndexOf(PACKAGE_SEPARATOR);
        int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
        if (nameEndIndex == -1) {
            nameEndIndex = className.length();
        }
        String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
        shortName = shortName.replace(INNER_CLASS_SEPARATOR, PACKAGE_SEPARATOR);
        return shortName;
    }


    private static String getQualifiedNameForArray(Class<?> clazz) {
        StringBuilder result = new StringBuilder();
        while (clazz.isArray()) {
            clazz = clazz.getComponentType();
            result.append(CUtils.ARRAY_SUFFIX);
        }
        result.insert(0, clazz.getName());
        return result.toString();
    }


    public static final class Parameter {

        private String name;
        private int index;
        private Class<?> type;


        Parameter() {
        }

        public Parameter(String name2, int index, Class<?> type2) {
            this.name = name2;
            this.index = index;
            this.type = type2;
        }

        public String getName() {
            return name;
        }

        public int getIndex() {
            return index;
        }

        public Class<?> getType() {
            return type;
        }


        public static Parameter newObj(int index, String name, Class<?> type) {
            return new Parameter(name, index, type);
        }


    }


    public final static class MethodMeta {

        private Method method;
        private Object target;

        public MethodMeta(Method m, Object obs) {
            this.method = m;
            this.target = obs;
        }

        public Method getMethod() {
            return method;
        }

        public Object getTarget() {
            return target;
        }

    }


//end method and field =====================================================================================================================================>>


    // add class path url


    public static URL getClassPath(Class<?> theClass) {
        if (theClass == null) {
            throw new NullPointerException("Class");
        }

        return theClass.getProtectionDomain().getCodeSource().getLocation();


    }


}