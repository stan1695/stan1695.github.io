## @Import

* 在@Import注解的参数中可以填写类名，例如@Import(Abc.class)，根据类Abc的不同类型，spring容器有以下四种处理方式：  
  * 1. 如果Abc类实现了ImportSelector接口，spring容器就会实例化Abc类，并且调用其selectImports方法；  
  * 2. DeferredImportSelector是ImportSelector的子类，如果Abc类实现了DeferredImportSelector接口，spring容器就会实例化Abc类，并且调用其selectImports方法，和ImportSelector的实例不同的是，DeferredImportSelector的实例的selectImports方法调用时机晚于ImportSelector的实例，要等到@Configuration注解中相关的业务全部都处理完了才会调用（具体逻辑在ConfigurationClassParser.processDeferredImportSelectors方法中），想了解更多DeferredImportSelector和ImportSelector的区别，请参考《ImportSelector与DeferredImportSelector的区别（spring4）》；  
  * 3. 如果Abc类实现了ImportBeanDefinitionRegistrar接口，spring容器就会实例化Abc类，并且调用其registerBeanDefinitions方法；  
  * 4. 如果Abc没有实现ImportSelector、DeferredImportSelector、ImportBeanDefinitionRegistrar等其中的任何一个，spring容器就会实例化Abc类  

  * 5. 普通类（即没有实现ImportBeanDefinitionRegistrar、ImportSelector、DeferredImportSelector等接口的类）会通过  ConfigurationClassBeanDefinitionReader.loadBeanDefinitionsFromImportedResources方法将bean定义注册到spring容器；  
  * 6. ImportSelector实现类，其selectImports方法返回的bean的名称，通过ConfigurationClassParser类的asSourceClass方法转成SourceClass对象，然后被当作普通类处理；  
  * 7. ImportSelector与DeferredImportSelector的区别，就是selectImports方法执行时机有差别，这个差别期间，spring容器对此Configguration类做了些其他的逻辑：包括对@ImportResource、@Bean这些注解的处理（注意，这里只是对@Bean修饰的方法的处理，并不是立即调用@Bean修饰的方法，这个区别很重要！）；  
  * 8. ImportBeanDefinitionRegistrar实现类的registerBeanDefinitions方法会被调用，里面可以注册业务所需的bean定义；  

