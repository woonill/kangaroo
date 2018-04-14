package com.kangaroo.component;

import java.util.List;


public interface ComponentConfigurable {


    NonComponentConfigurable NONE = new NonComponentConfigurable();

    ComponentDefinition[] getDefinitions();

    public static ComponentConfigurable create(ComponentDefinition cd) {
        return new ComponentConfigurable() {
            @Override
            public ComponentDefinition[] getDefinitions() {
                return new ComponentDefinition[]{cd};
            }
        };

    }

    ;

    static ComponentConfigurable[] toArray(ComponentConfigurable cc) {
        return new ComponentConfigurable[]{cc};
    }

    static ComponentConfigurable toIntance(ComponentDefinition... compsDefinitions) {
//		return new ComponentConfigurable[]{
//				new ComponentConfigurable(){
//					@Override
//					public ComponentDefinition[] getDefinitions(Environment cenv) {
//						return compsDefinitions;
//					}
//				}
//		};
        return new ComponentConfigurable() {
            @Override
            public ComponentDefinition[] getDefinitions() {
                return compsDefinitions;
            }
        };

    }


    public class NonComponentConfigurable implements ComponentConfigurable {

        @Override
        public ComponentDefinition[] getDefinitions() {
            return null;
        }
    }


    public class DefaultComponentConfigurable implements ComponentConfigurable {

        private final ComponentDefinition[] cDefinitions;

        public DefaultComponentConfigurable(ComponentDefinition[] cDefinitions) {
            this.cDefinitions = cDefinitions;
        }

        public DefaultComponentConfigurable(List<ComponentDefinition> cDefinitions) {
            this.cDefinitions = cDefinitions.toArray(new ComponentDefinition[cDefinitions.size()]);
        }

        @Override
        public ComponentDefinition[] getDefinitions() {
            return cDefinitions;
        }
    }
}