package com.kangaroo.component;

import com.kangaroo.util.ObjectUtil;
import com.kangaroo.util.Validate;
import io.reactivex.Observable;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by woonill on 2/24/16.
 */
public interface AutoComponentConfigurable {

    ComponentConfigurable init(Observable<Class<?>> classes);

    AutoComponentConfigurable NONE = new AutoComponentConfigurable() {
        @Override
        public ComponentConfigurable init(Observable<Class<?>> classes) {
            return ComponentConfigurable.NONE;
        }
    };


    public final class Composite implements AutoComponentConfigurable {

        private AutoComponentConfigurable[] accs;

        private Composite(AutoComponentConfigurable... acc) {
            this.accs = acc;
        }


        @Override
        public ComponentConfigurable init(Observable<Class<?>> classes) {

            return new ComponentConfigurable() {

                @Override
                public ComponentDefinition[] getDefinitions() {
                    List<ComponentDefinition> cdArray = new LinkedList<ComponentDefinition>();
                    for (AutoComponentConfigurable ac : accs) {
                        final ComponentConfigurable init = ac.init(classes);
                        final ComponentDefinition[] definitions = init.getDefinitions();
                        if (!ObjectUtil.isEmpty(definitions)) {
                            cdArray.addAll(Arrays.asList(definitions));
                        }
                    }
                    return cdArray.toArray(new ComponentDefinition[cdArray.size()]);
                }
            };
        }

        public static AutoComponentConfigurable newObj(AutoComponentConfigurable... acc) {
            Validate.notEmpty(acc, "AutoComponentConfigurable is required");
            return new Composite(acc);

        }
    }
}
