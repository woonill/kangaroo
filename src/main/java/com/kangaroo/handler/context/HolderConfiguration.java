/*
package com.kangaroo.handler.configure;

import com.kangaroo.component.AutoComponentConfigurable;
import com.kangaroo.component.ComponentConfigurable;
import com.kangaroo.handler.*;
import com.kangaroo.util.ObjectUtil;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.kangaroo.handler.configure.AbstractRequestHandlerHolderContext.RequestHandlerHolderConfigurable;

*/
/**
 * Created by woonill on 2/25/16.
 *//*

public abstract class HolderConfiguration {


    private HolderConfiguration() {
    }

    protected abstract RequestHandlerHolder build(RequestHandlerHolderConfigurable configer);


    public static final Builder newBuilder(String name) {
        return new Builder(name);
    }

    public final static class Builder {

        private String name;
        private List<HolderConfiguration> children = new LinkedList<>();
        private List<String> mappings = new LinkedList<>();
        private AutoComponentConfigurable componentConfigurable = null;

        Builder(String name) {
            this.name = name;
        }

        public Builder addChild(HolderConfiguration... child) {

            if (!ObjectUtil.isEmpty(child)) {
                this.children.addAll(Arrays.asList(child));
            }
            return this;
        }

        public Builder addMappingUri(String... uri) {
            if (!ObjectUtil.isEmpty(uri)) {
                this.mappings.addAll(Arrays.asList(uri));
            }
            return this;
        }

        public Builder componentProvider(AutoComponentConfigurable cc) {
            this.componentConfigurable = cc;
            return this;
        }

        public HolderConfiguration build(RequestHandler handler) {
            return this.build(new RequestHandlerInitializer() {
                @Override
                public RequestHandler init(RequestHandlerConfigurable configer) {
                    return handler;
                }
            });
        }

        public HolderConfiguration build(RequestHandlerInitializer initializer) {

            this.componentConfigurable = this.componentConfigurable == null ? AutoComponentConfigurable.NONE : this.componentConfigurable;
            final String[] uriMaps = this.mappings.toArray(new String[mappings.size()]);

            return new HolderConfiguration() {
                @Override
                protected RequestHandlerHolder build(RequestHandlerHolderConfigurable configer) {

                    RequestHandlerHolder[] childrenHolders = null;
                    if (!children.isEmpty()) {
                        childrenHolders = new RequestHandlerHolder[children.size()];
                        int count = 0;
                        for (HolderConfiguration subInit : children) {
                            childrenHolders[count++] = subInit.build(configer);
                        }
                    }
                    ComponentConfigurable cc = configer.newComponentConfigurable(componentConfigurable);
                    return configer.newHolder(name, initializer, cc, uriMaps, childrenHolders);
                }
            };
        }
    }
}*/
