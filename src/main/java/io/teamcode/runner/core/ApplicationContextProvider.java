package io.teamcode.runner.core;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Created by chiang on 2017. 5. 8..
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    private ApplicationContextProvider(){}

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    public  static <T> T getBean(String name,Class<T> aClass){
        return context.getBean(name,aClass);
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        context = ctx;
    }
}
