/*
package com.kangaroo.component;

import com.kangaroo.*;
import com.kangaroo.component.ComponentConfigurable.NonComponentConfigurable;
import com.kangaroo.component.scan.ConfigurableComponentContext;
import com.kangaroo.handler.configure.ComponentContextFactory;
import com.kangaroo.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import io.reactivex.Observable;

import javax.inject.Inject;
import java.util.*;

public final class DefaultComponentContext implements ConfigurableComponentContext {

    private DefaultListableBeanFactory cBeanFactory;
    private AutowiredAnnotationBeanPostProcessor autowireBeanPoster = null;
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private DefaultComponentContext parent = null;


    public DefaultComponentContext(ComponentDefinition... cds) {
        this.initPostProcessor(null, cds);
    }

    public DefaultComponentContext(
            DefaultComponentContext parentSbi,
            ComponentDefinition... cds) {
        initPostProcessor(parentSbi, cds);
    }

    protected final Logger logger() {
        return this.logger;
    }

    private void initPostProcessor(
            DefaultComponentContext parent,
            ComponentDefinition... cds) {

        this.parent = parent;
        this.cBeanFactory = new DefaultListableBeanFactory();
        if (parent != null) {
            logger.info("Set parent Injector now-------------------------------------->>");
            this.cBeanFactory.setParentBeanFactory(parent.cBeanFactory);
        }

        this.autowireBeanPoster = new AutowiredAnnotationBeanPostProcessor();
        this.autowireBeanPoster.setBeanFactory(cBeanFactory);
        this.autowireBeanPoster.setAutowiredAnnotationType(Inject.class);
        this.cBeanFactory.addBeanPostProcessor(this.autowireBeanPoster);
        this.cBeanFactory.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());
        this.cBeanFactory.addBeanPostProcessor(new RequiredAnnotationBeanPostProcessor());

        String[] pps = cBeanFactory.getBeanNamesForType(PropertyPlaceholderConfigurer.class);
        if (pps != null && pps.length > 0) {
            PropertyPlaceholderConfigurer ppc = (PropertyPlaceholderConfigurer) cBeanFactory.getBean(pps[0]);
            ppc.postProcessBeanFactory(cBeanFactory);
        }
        if (cds != null && cds.length > 0) {
            registAllComponent(cds);
        }
    }


    private void registAllComponent(ComponentDefinition... cms) {

        StringBuilder sb = new StringBuilder("Registed Component:" + cms.length);
        sb.append("[ \n");
        for (ComponentDefinition cd : cms) {
            if (cd.instance() != null) {
                this.autowireBeanPoster.processInjection(cd.instance());
                this.cBeanFactory.registerSingleton(cd.name(), cd.instance());
            } else {
                RootBeanDefinition rbd = new RootBeanDefinition(cd.type());
                rbd.setScope(RootBeanDefinition.SCOPE_SINGLETON);
                this.cBeanFactory.registerBeanDefinition(cd.name(), rbd);
            }
            sb.append("\t" + cd.name() + " type:" + cd.type().getName() + " \n");
        }
//		sb.append("count of Components:"+this.cBeanFactory.getBeanDefinitionCount());
        sb.append("]");
        logger.info(sb.toString());
    }


    private final ComponentInjector injecotr = new ComponentInjector() {

        @Override
        public Object inject(Object bean) {
            Validate.notNull(bean, "Can not Inject null object");
            autowireBeanPoster.processInjection(bean);
            return bean;
        }

        @Override
        public <T> T toComponent(Class<T> coms) {
            T components = cBeanFactory.createBean(coms);
            return components;
        }

    };

    @Override
    public ComponentInjector injector() {
        return injecotr;
    }


    @Override
    public <T> T getComponent(String name) {
        final Object bean = cBeanFactory.getBean(name);
        if (bean == null && this.parent != null) {
            return this.parent.getComponent(name);
        }
        return (T) bean;
    }

    @Override
    public <T> T getComponent(Class<T> requiredType) {
        final T bean = cBeanFactory.getBean(requiredType);
        if (bean == null && this.parent != null) {
            return this.parent.getComponent(requiredType);
        }
        return (T) bean;
    }

    @Override
    public Observable<Object> components() {

        List<Object> components = new LinkedList<Object>();
        Iterator<String> names = this.cBeanFactory.getBeanNamesIterator();
        while (names.hasNext()) {
            String bname = names.next();
            Object com = this.cBeanFactory.getBean(bname);
            components.add(com);
        }
        return Observable.fromIterable(components);
    }

    static final ComponentContext create(ComponentConfigurable cc2) {
        return create(null, cc2);
    }

    public static ComponentContext create(
            ComponentContext components,
            ComponentConfigurable cc2) {
        ComponentConfigurable cc = cc2 == null ? new NonComponentConfigurable() : cc2;
        ComponentDefinition[] cdf = cc.getDefinitions();
        return new DefaultComponentContext((DefaultComponentContext) components, cdf);
    }


    private final Set<PathComponentContext> children = new HashSet<PathComponentContext>();


    @Override
    public ComponentContext creatSub(String uri, ComponentDefinition... cc) {
        // TODO Auto-generated method stub
        DefaultComponentContext comps = new DefaultComponentContext(this, cc);
//		comps.cBeanFactory.setParentBeanFactory(this.cBeanFactory);//set Parent Spring BeanFactory
        if (!children.add(new PathComponentContext(uri, comps))) {
            throw new IllegalArgumentException("ComponentContext :" + uri + " found");
        }
        return comps;
    }


    @Override
    public ComponentContext getComponents(String name) {
        for (PathComponentContext pcc : children) {
            if (pcc.name.equalsIgnoreCase(name)) {
                return pcc;
            }
        }
        return NONE;
    }


    class PathComponentContext implements ComponentContext {

        private final String name;
        private final ComponentContext componentContext;

        public PathComponentContext(String name, ComponentContext cc) {
            this.name = name;
            this.componentContext = cc;
        }

        @Override
        public <T> Observable<T> components() {
            return this.componentContext.components();
        }

        @Override
        public ComponentInjector injector() {
            return this.componentContext.injector();
        }


        @Override
        public <T> T getComponent(String name) {
            return componentContext.getComponent(name);
        }

        @Override
        public <T> T getComponent(Class<T> requiredType) {
            return componentContext.getComponent(requiredType);
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            PathComponentContext pcc = (PathComponentContext) that;
            return this.name.equalsIgnoreCase(pcc.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }


    public static final class DefaultComponentContextFactory implements ComponentContextFactory {


        @Override
        public ConfigurableComponentContext get(ComponentDefinition... initConfigure) {
            return new DefaultComponentContext(initConfigure);
        }
    }

}
*/
