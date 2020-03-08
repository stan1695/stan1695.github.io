* hotspot中的栈 每启用一个线程，jvm为线程分配一个java栈，栈是以帧为单位，每当线程调用一个方法，jvm就向栈里面压入一帧，这个方法就成为了当前帧，方法执行完则将对应帧从栈中弹出，并把返回结果存储在调用方法的帧的操作数栈中。
* 栈帧中存储了方法的局部变量表、操作数栈、动态链接和方法返回地址信息。


## 类加载器

* 启动类加载器：Bootstrap ClassLoader 没有子类加载器，也没有父类加载器
* 加载范围是：专门加载<JAVA_HOME>\lib下或者-Xbootclasspath参数指定的路径下的类库，加载到jvm的内存中去  
* 该类加载器使用c++语言实现，是虚拟机自身实现的一部分  
* String bootClassPath = System.getProperty("sun.boot.class.path"); //负责加载路径
  
```
-Xbootclasspath/a:D:\yyfax\CODE\SOLID\GIT_bash\SOLID-Repay\SOLID-Repay-core\src\main\java\java1\lang
-Xbootclasspath/b:D:\yyfax\CODE\SOLID\GIT_bash\SOLID-Repay\SOLID-Repay-core\src\main\java\java1\lang

打jar包：jar cfm test.jar h.mf System1.class
要先创建一个头文件h.mf
```

* 扩展类加载器 ExtClassLoader 子类是运用程序加载类AppClassLoader
* 加载范围：专门加载<JAVA_HOME>\lib\ext或者java.ext.dirs系统变量指定路径的类库
* String var0 = System.getProperty("java.ext.dirs");//负责加载的路径


* 运用程序加载类AppClassLoader 父类是扩展类加载器 ExtClassLoader,子类是开发者可继承实现
* 加载范围：专门加载类路径Classpath上指定的类库。（就是项目bin目录下的class文件）  
* String var1 = System.getProperty("java.class.path");//负责加载的路径

* 配置 -XX:+TraceClassLoading 查看类加载属性
* jinfo可以查看启动类加载器加载了那些jar包(也可以通过URL[] urls = Launcher.getBootstrapClassPath().getURLs(); 来查看)
    sun.boot.class.path = /usr/java/jdk1.8.0_171/jre/lib/resources.jar
    :/usr/java/jdk1.8.0_171/jre/lib/rt.jar
    :/usr/java/jdk1.8.0_171/jre/lib/sunrsasign.jar
    :/usr/java/jdk1.8.0_171/jre/lib/jsse.jar
    :/usr/java/jdk1.8.0_171/jre/lib/jce.jar
    :/usr/java/jdk1.8.0_171/jre/lib/charsets.jar
    :/usr/java/jdk1.8.0_171/jre/lib/jfr.jar
    :/usr/java/jdk1.8.0_171/jre/classes

    java.ext.dirs = /usr/java/jdk1.8.0_171/jre/lib/ext:/usr/java/packages/lib/ext
  
  ```

    public static void main(String[] args) {
        System.out.println(ClassLoader.getSystemClassLoader());
        System.out.println(ClassLoader.getSystemClassLoader().getParent());
        System.out.println(ClassLoader.getSystemClassLoader().getParent().getParent());
        URL[] urls = Launcher.getBootstrapClassPath().getURLs();
        String str = System.getProperty("java.ext.dirs");
        System.getProperty("java.class.path")
        System.out.println(str);
    }


    [Opened C:\Program Files\java8\jdk1.8.0_45\jre\lib\rt.jar]
    [Loaded java.lang.Object from C:\Program Files\java8\jdk1.8.0_45\jre\lib\rt.jar]
    [Loaded java.io.Serializable from C:\Program Files\java8\jdk1.8.0_45\jre\lib\rt.jar]
    [Loaded java.lang.Comparable from C:\Program Files\java8\jdk1.8.0_45\jre\lib\rt.jar]
    [Loaded java.lang.CharSequence from C:\Program Files\java8\jdk1.8.0_45\jre\lib\rt.jar]
    ...
    [Loaded ch.qos.logback.core.spi.ContextAwareBase from file:/D:/maven/repository/ch/qos/logback/logback-core/1.1.11/logback-core-1.1.11.jar]
    [Loaded ch.qos.logback.classic.BasicConfigurator from file:/D:/maven/repository/ch/qos/logback/logback-classic/1.1.11/logback-classic-1.1.11.jar]
    [Loaded ch.qos.logback.core.Layout from file:/D:/maven/repository/ch/qos/logback/logback-core/1.1.11/logback-core-1.1.11.jar]
    [Loaded ch.qos.logback.core.encoder.Encoder from file:/D:/maven/repository/ch/qos/logback/logback-core/1.1.11/logback-core-1.1.11.jar]
    [Loaded ch.qos.logback.core.spi.FilterAttachable from file:/D:/maven/repository/ch/qos/logback/logback-core/1.1.11/logback-core-1.1.11.jar]

    sun.misc.Launcher$AppClassLoader@14dad5dc
    sun.misc.Launcher$ExtClassLoader@182decdb
    null
    C:\Program Files\java8\jdk1.8.0_45\jre\lib\ext;C:\Windows\Sun\Java\lib\ext

  ```

* 为什么要委派双亲制
    1、安全，因为jvm里面的类一般都是服务正常启动的必要条件，我们不要随意去改动，如果用户自己的类与jvm里面的类一样，没有双亲委派机制的话，就很容易改变jvm里面类的机能。
    2、同一类只会加载一次，在加载过程中发现已经加载了相同的类，就不会再加载。

* 如果要打破双亲委托规则 则需要重写ClassLoader的loadClass方法。可以设计成在加载某些类时打破双亲委托规则。
* Launcher源码里定义了static的扩展类加载器ExtClassLoader， static的系统类加载器AppClassLoader。
* 看看Launcher的构造方法。先实例化ExtClassL
* oader，从java.ext.dirs系统变量里获得URL。用这个ExtClassLoader作为parent去实例化AppClassLoader，从java.class.path系统变量里获得URL。Launcher getClassLoader()就是返回的这个AppClassLoader。设置AppClassLoader为ContextClassLoader。
* class.forName()和classloader.loadClass()区别。class.forname不仅仅可以加载类，还会对类进行连接和初始化，但是Classloader.loadClass得到的class是还没有连接的。

## 类的卸载  
* jvm中自带的类加载器（根加载器、扩展类加载器、运用类加载器），在虚拟机的生命中期中是不会被卸载的，因为jvm本身始终引用了这些类加载器，而这些类加载器又会始终引用他们所加载类的class对象，所以这些class对象始终是可不触及的。
* 但是用户自定义的类加载器加载的类是可以被卸载的。只有当类对象都为空，类的class对象也为空，类加载求对象也是空，支持类可能被垃圾回收进行卸载。
* 
## 参考阅读
* https://www.jianshu.com/p/d98324f5ad23
* https://blog.csdn.net/m0_37284598/article/details/82950779
* https://www.cnblogs.com/joemsu/p/9310226.html