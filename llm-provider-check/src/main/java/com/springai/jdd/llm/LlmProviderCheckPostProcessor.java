package com.springai.jdd.llm;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.Environment;

class LlmProviderCheckPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware, PriorityOrdered {

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        try (LlmProviderCheck check = LlmProviderCheck.withDefaults()) {
            check.verify(environment);
        }
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
