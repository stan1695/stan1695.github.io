---
title: spring加载bean顺序
categories: spring
tags: 
 - spring
 - spring boot
 - 注解
---

#### bean注册以及初始化

spring boot启动时 有一步是刷新上下文，入口就是 `org.springframework.context.support.AbstractApplicationContext`的`refresh`  

* 该方法存在一个对象同步锁（startupShutdownMonitor），在销毁上下文的时也会使用该对象。为了保证刷新上下文和销毁上下文不同时进行。一下12步就是初始化刷新beanFactory的  

* 1、`prepareRefresh();`准备环境，给context添加环境变量数据，检测一下必要的参数是否存在。  

* 2、`ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();` 获创建beanFactory，这个对象作为applicationContext的成员变量，可以被applicationContext拿来用,并且解析资源（例如xml文件），取得bean的定义，放在beanFactory中  

```
    protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
        //是否允许同名的类覆盖原有的类
        if (this.allowBeanDefinitionOverriding != null) {
            beanFactory.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
        }
        //是否允许循环依赖注入
        if (this.allowCircularReferences != null) {
            beanFactory.setAllowCircularReferences(this.allowCircularReferences);
        }
    }
    ...
    在DefaultListableBeanFactory类中
    /** Whether to allow re-registration of a different definition with the same name */
    private boolean allowBeanDefinitionOverriding = true;
    在AbstractAutowireCapableBeanFactory类中
    /** Whether to automatically try to resolve circular references between beans */
    private boolean allowCircularReferences = true;

```
> `AbstractRefreshableApplicationContext`的`refreshBeanFactory`方法`loadBeanDefinitions(beanFactory);` 去加载bean，并存入DefaultListableBeanFactory的`Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>`中,bean的名字则放在`List<String> beanDefinitionNames = new ArrayList<String>`  

* 3、`prepareBeanFactory(beanFactory);`  

> 设置beanFactory类加载器  
> 设置bean中el表达是的解析器  
> 添加一个bean的后置处理器，用于将spring中的环境参数注入到实例化的bean中  
> 设置一些类型的属性，不进行依赖注入，比如`ResourceLoaderAware`,`ApplicationEventPublisherAware`,`MessageSourceAware`,`ApplicationContextAware`,`EnvironmentAware`
> 添加一个bean的后置处理器，用于用于AOP静态代理相关的处理  

* 4、`postProcessBeanFactory(beanFactory);`  添加bean的后置处理去，添加到beanFactory的`List<BeanPostProcessor> beanPostProcessors`属性中去，在类进行初始化之后，会执行这里的后置处理器的前置和后置方法  

* 5、`invokeBeanFactoryPostProcessors(beanFactory);`调用后置处理器，通过处理bean的后置处理器，来对bean做适当的修改  

* 6、`registerBeanPostProcessors(beanFactory);`主要是后置处理器的归置排序的操作  
  
* 7、`initMessageSource();`处理实现了MessageSource接口的类，并实例化bean  

* 8、`initApplicationEventMulticaster();` 如果beanFactory中有ApplicationEventMulticaster实现类，则实例化该类，如果没有则添加一个SimpleApplicationEventMulticaster并实例化它  

* 9、`onRefresh();`该操作，可以进行tomcat容器启动等  

* 10、`registerListeners();`将ApplicationListener添加到ApplicationEventMulticaster中去，此时并没有开始实例化监听器

* 11、`finishBeanFactoryInitialization(beanFactory);`实例化bean

```
    protected void finishBeanFactoryInitialization(ConfigurableListableBeanFactory beanFactory) {
        // Initialize conversion service for this context.
        //初始实例化 类型转换服务(conversionService) bean
        if (beanFactory.containsBean(CONVERSION_SERVICE_BEAN_NAME) &&
                beanFactory.isTypeMatch(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class)) {
            beanFactory.setConversionService(
                    beanFactory.getBean(CONVERSION_SERVICE_BEAN_NAME, ConversionService.class));
        }

        // Register a default embedded value resolver if no bean post-processor
        // (such as a PropertyPlaceholderConfigurer bean) registered any before:
        // at this point, primarily for resolution in annotation attribute values.
        //如果beanFactory没有嵌入数值后置处理器，则添加一个默认的，主要用于解析properties、xml等配置文件的属性值
        if (!beanFactory.hasEmbeddedValueResolver()) {
            beanFactory.addEmbeddedValueResolver(new StringValueResolver() {
                @Override
                public String resolveStringValue(String strVal) {
                    return getEnvironment().resolvePlaceholders(strVal);
                }
            });
        }

        // Initialize LoadTimeWeaverAware beans early to allow for registering their transformers early.
        //尽早实例化LoadTimeWeaverAware接口的bean，用于ApsectJ的类加载期织入的处理
        String[] weaverAwareNames = beanFactory.getBeanNamesForType(LoadTimeWeaverAware.class, false, false);
        for (String weaverAwareName : weaverAwareNames) {
            getBean(weaverAwareName);
        }

        // Stop using the temporary ClassLoader for type matching.
        // 确保临时的classLoader为空，临时classLoader一般被用来做类型匹配的
        beanFactory.setTempClassLoader(null);

        // Allow for caching all bean definition metadata, not expecting further changes.
        // 将一个标志设置为true，表示applicationContext已经缓存了所有bean的定义，这些bean的name都被保存在applicationContext的frozenBeanDefinitionNames成员变量中，相当于一个快照，记录了当前那些bean的定义已经拿到了
        beanFactory.freezeConfiguration();

        // Instantiate all remaining (non-lazy-init) singletons.
        // 实例化所有还未实例化的单例bean
        beanFactory.preInstantiateSingletons();

        //实例化bean的方法实现

        public void preInstantiateSingletons() throws BeansException {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Pre-instantiating singletons in " + this);
        }

        List<String> beanNames = new ArrayList<String>(this.beanDefinitionNames);

        // Trigger initialization of all non-lazy singleton beans...
        //初始化所有非懒加载的单利模式的bean,可以看出他们的顺序是遍历this.beanDefinitionNames
        for (String beanName : beanNames) {
            RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
            // 非抽象类、是单例、非懒加载
            if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
                //FactoryBean的处理
                if (isFactoryBean(beanName)) {
                    final FactoryBean<?> factory = (FactoryBean<?>) getBean(FACTORY_BEAN_PREFIX + beanName);
                    boolean isEagerInit;
                    if (System.getSecurityManager() != null && factory instanceof SmartFactoryBean) {
                        isEagerInit = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                            @Override
                            public Boolean run() {
                                return ((SmartFactoryBean<?>) factory).isEagerInit();
                            }
                        }, getAccessControlContext());
                    }
                    else {
                        isEagerInit = (factory instanceof SmartFactoryBean &&
                                ((SmartFactoryBean<?>) factory).isEagerInit());
                    }
                    if (isEagerInit) {
                        getBean(beanName);
                    }
                }
                else {
                    //非FactoryBean的实例化、初始化
                    getBean(beanName);
                }
            }
        }

        // Trigger post-initialization callback for all applicable beans...
        // 单例实例化完成后，如果实现了SmartInitializingSingleton接口，afterSingletonsInstantiated就会被调用，此处用到了特权控制逻辑AccessController.doPrivileged
        for (String beanName : beanNames) {
            Object singletonInstance = getSingleton(beanName);
            if (singletonInstance instanceof SmartInitializingSingleton) {
                final SmartInitializingSingleton smartSingleton = (SmartInitializingSingleton) singletonInstance;
                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged(new PrivilegedAction<Object>() {
                        @Override
                        public Object run() {
                            smartSingleton.afterSingletonsInstantiated();
                            return null;
                        }
                    }, getAccessControlContext());
                }
                else {
                    smartSingleton.afterSingletonsInstantiated();
                }
            }
        }
    }
    }

    getbean(),的逻辑可以依次沿着一下逻辑取找：
    AbstractBeanFactory.getBean(String name)
    ->
    AbstractBeanFactory.doGetBean(String name, Class<T> requiredType, final Object[] args, boolean typeCheckOnly)
    ->
    DefaultSingletonBeanRegistry.getSingleton(String beanName, ObjectFactory<?> singletonFactory)
    ->
    AbstractBeanFactory.doGetBean中的匿名类的getObject方法
    ->
    AbstractAutowireCapableBeanFactory.createBean(String beanName, RootBeanDefinition mbd, Object[] args)
    ->
    AbstractAutowireCapableBeanFactory.doCreateBean(final String beanName, final RootBeanDefinition mbd, Object[] args)
    ->
    AbstractAutowireCapableBeanFactory.createBeanInstance(String beanName, RootBeanDefinition mbd, Object[] args)
    ->
    instantiateBean(final String beanName, final RootBeanDefinition mbd)
    ->
    SimpleInstantiationStrategy.instantiate(RootBeanDefinition bd, String beanName, BeanFactory owner)
    ->
    BeanUtils.instantiateClass(Constructor<T> ctor, Object... args)
    ->
    Constructor.newInstance(Object ... initargs)

    在AbstractAutowireCapableBeanFactory.doCreateBean时populateBean方法可以看到，是在这时候进行bean注入的

```

* 12、`finishRefresh();`刷新appliactionContext,查看是否设置生命周期等


#### 参考  

https://blog.csdn.net/boling_cavalry/article/details/81045637  
https://www.jianshu.com/p/19e01388ccc5  
https://www.jianshu.com/p/210115d5a4aa

