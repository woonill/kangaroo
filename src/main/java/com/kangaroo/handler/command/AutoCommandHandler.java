package com.kangaroo.handler.command;

import com.kangaroo.component.ComponentDefinition;
import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AutoCommandHandler {

    String value();


    public static final Predicate<Class<?>> annoFilter = new Predicate<Class<?>>() {
        @Override
        public boolean test(Class<?> in) {
            return in.isAnnotationPresent(AutoCommandHandler.class);
        }
    };

    public final static Function<Observable<Class<?>>, Observable<ComponentDefinition>> repFunc = new Function<Observable<Class<?>>, Observable<ComponentDefinition>>() {
        @Override
        public Observable<ComponentDefinition> apply(Observable<Class<?>> in) {
            return in.map(new Function<Class<?>, ComponentDefinition>() {
                @Override
                public ComponentDefinition apply(Class<?> cin) {
                    AutoCommandHandler rp = cin.getAnnotation(AutoCommandHandler.class);
                    if (rp != null) {
                        return new ComponentDefinition(rp.value(), cin);
                    }
                    return null;
                }
            }).filter(new Predicate<ComponentDefinition>() {
                @Override
                public boolean test(ComponentDefinition in) {
                    return in != null;
                }
            });
        }
    };
}