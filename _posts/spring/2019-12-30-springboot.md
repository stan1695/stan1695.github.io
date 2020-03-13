---
title: spring boot
categories: spring
tags: 
 - spring
 - spring boot
---

org.springframework.context.ApplicationContextInitializer
org.springframework.context.ApplicationListener

org.springframework.boot.SpringApplicationRunListener


# spring boot启动过程

* springboot 是否启动成web项目还是Standalone（独立的）的项目，取决于classpath中是否含有javax.servlet.Servlet, org.springframework.web.context.ConfigurableWebApplicationContext这两个类。
* ConfigurableWebApplicationContext这个类是在spring-web包中
* 第二步，初始化加载classpath下的所有的可用的ApplicationListener  
* 第三步，初始化加载main方法所在类  
* 第四步，运行run方法
* 第五步，根据第二步的结果判断当前运用是否是web服务来加载容器，如果是web服务则加载org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext类，如果不是web项目则加载org.springframework.context.annotation.AnnotationConfigApplicationContext类，然后在初始化创建一个容器类对象。  
* 创建上下文，刷新上下文

#  spring boot配置内部tomcat

* springboot2.0(2018年春发布) 创建tomcat的服务器和启动tomcat服务方式与2.0之前的版本不太一样。
* SpringBoot的启动主要是通过实例化SpringApplication来启动的，启动过程主要做了以下几件事情：配置属性、获取监听器，发布应用开始启动事件、初始化输入参数、配置环境，输出banner、创建上下文、预处理上下文、刷新上下文、再刷新上下文、发布应用已经启动事件、发布应用启动完成事件。在SpringBoot中启动tomcat的工作在刷新上下文这一步。
* 而tomcat的启动主要是实例化两个组件：Connector、Container，一个tomcat实例就是一个Server，一个Server包含多个Service，也就是多个应用程序，每个Service包含多个Connector和一个Container，而一个Container下又包含多个子容器。

# spring bean

* 动态注入bean分以下几步：获取ApplicationContext;通过ApplicationContext获取到BeanFacotory;通过BeanDefinitionBuilder构建BeanDefiniton;调用beanFactory的registerBeanDefinition注入beanDefinition；使用ApplicationContext.getBean获取bean进行测试；
* 多次注入同一个bean的，如果beanName不一样的话，那么会产生两个Bean；如果beanName一样的话，后面注入的会覆盖前面的。  

```

//获取context.  
ApplicationContext ctx =  (ApplicationContext) SpringApplication.run(App.class, args);  
        
//获取BeanFactory  
DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) ctx.getAutowireCapableBeanFactory();  
        
//创建bean信息.  
BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(TestService.class);  
beanDefinitionBuilder.addPropertyValue("name","张三");  
        
//动态注册bean.  
defaultListableBeanFactory.registerBeanDefinition("testService", beanDefinitionBuilder.getBeanDefinition());  
        
//获取动态注册的bean.  
TestService testService =ctx.getBean(TestService.class);

```

## springboot 启动 
初始化springApplication
> 1、加载初始化启动类(this.setInitializers)，通过方法this.getSpringFactoriesInstances（）从各个jar或项目中查找/META-INF/spring.factories文件中key为org.springframework.context.ApplicationContextInitializer的类，只是把这些类加载进来，还没有执行类里面的初始化方法，如下图  
> 2、加载监听器（this.setListeners），通过方法this.getSpringFactoriesInstances（）从各个jar或项目中查找/META-INF/spring.factories文件中key为org.springframework.context.ApplicationListener的类
> 以上两步是组装springApplication对象的准备工作


> 3、开始实例化run监听器（这个监听器跟随springboot整个启动过程）  
> SpringApplicationRunListeners listeners = this.getRunListeners(args);  
> this.getRunListeners > this.getSpringFactoriesInstances(org.springframework.boot.SpringApplicationRunListener)找/META-INF/spring.factories文件中key为org.springframework.boot.SpringApplicationRunListener的类  

```
    package org.springframework.boot;
    public interface SpringApplicationRunListener {

        // 在run()方法开始执行时，该方法就立即被调用，可用于在初始化最早期时做一些工作
        void starting();
        // 当environment构建完成，ApplicationContext创建之前，该方法被调用
        void environmentPrepared(ConfigurableEnvironment environment);
        // 当ApplicationContext构建完成时，该方法被调用
        void contextPrepared(ConfigurableApplicationContext context);
        // 在ApplicationContext完成加载，但没有被刷新前，该方法被调用
        void contextLoaded(ConfigurableApplicationContext context);
        // 在ApplicationContext刷新并启动后，CommandLineRunners和ApplicationRunner未被调用前，该方法被调用
        void started(ConfigurableApplicationContext context);
        // 在run()方法执行完成前该方法被调用
        void running(ConfigurableApplicationContext context);
        // 当应用运行出错时该方法被调用
        void failed(ConfigurableApplicationContext context, Throwable exception);
    }
```

> 我们可以自定义SpringApplicationRunListener来监听springboot启动过程。（实现该接口，然后在/META-INF/spring.factories配置上实现类，把自定义的监听器暴露出来）

> 3、执行监听器(this.prepareEnvironment)

![image1]()  ![image2]()


## 参考文献
* https://www.cnblogs.com/sword-successful/p/11383723.html（2.0版本启动内置tomcat）