package com.kids.configuration;


import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    // دالة استاتيكية للوصول لأي خدمة من سبرينج من أي مكان في مشروعك
    public static ApplicationContext getContext() {
        return context;
    }
}
