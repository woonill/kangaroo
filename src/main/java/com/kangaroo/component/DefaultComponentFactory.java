package com.kangaroo.component;

import com.kangaroo.ComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import javax.inject.Inject;

public class DefaultComponentFactory implements ComponentFactory {



    private DefaultListableBeanFactory cBeanFactory;
    private AutowiredAnnotationBeanPostProcessor autowireBeanPoster = null;
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    private DefaultComponentFactory parent = null;


    public DefaultComponentFactory(ComponentDefinition... cds) {
        this.initPostProcessor(null, cds);
    }

    public DefaultComponentFactory(
            DefaultComponentFactory parentSbi,
            ComponentDefinition... cds) {
        initPostProcessor(parentSbi, cds);
    }

    protected final Logger logger() {
        return this.logger;
    }

    private void initPostProcessor(
            DefaultComponentFactory parent,
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

    @Override
    public <T> T get(T instance) {
        autowireBeanPoster.processInjection(instance);
        return instance;
    }

    @Override
    public <T> T toInstance(Class<T> type) {
        T components = cBeanFactory.createBean(type);
        return components;
    }
}
