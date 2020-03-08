* ThreadLocal是一个线程本地变量。
* ThreadLocal里面有个内部类，ThreadLocalMap,用来存储线程副本。ThreadLocalMap里面的数据结构是一个Entry<k，v>一个弱引用。
* 比如有一个共享变量，private static final ThreadLocal t = new ThreadLocal();
* 线程1进来，存放一个变量t.set(object1);线程2也是用这个共享变量t，存放一个变量t.set(object2);线程1在获取t的变量时t.get()会返回obejct1,线程2在获取t的变量时t.get()会返回obejct2。
* ThreadLocal除了可以通过set(object1)来存放线程副本，也可以通过实现initialValue方法，将initialValue方法的返回值存入ThreadLocalMap里面。例如：
  ```
  Java7中的SimpleDateFormat不是线程安全的，可以用ThreadLocal来解决这个问题：
    public class DateUtil {
        private static ThreadLocal<SimpleDateFormat> format1 = new ThreadLocal<SimpleDateFormat>() {
            @Override
            protected SimpleDateFormat initialValue() {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            }
        };

        public static String formatDate(Date date) {
            return format1.get().format(date);
        }
    }

  ```

* ThreadLocal的结构
  ```
  ThreadLocal threadLocal,
  Thread t1,Thread t2,Thread t3,
  Object value1,Object value2,Object value3
    t1--->ThreadLocalMap(threadLocal,value1)
    t2--->ThreadLocalMap(threadLocal,value2)
    t3--->ThreadLocalMap(threadLocal,value3)
  ```
  * 所以在当threadLocal为null时，但是线程t1/t2/t3还存在时，threadLocal因为是弱引用会被回收，但是value1/value2/value3不会被回收，这样就可能存在内存泄露。

* 强引用、弱引用、软引用、虚引用
   * 强引用：使用最普遍的引用，一般情况下，垃圾回收器绝对不会回收它。内存不足时，抛出OOM。
    ```
    String s = new String("hello");
    s = null; //不加该行，会输出hello
    System.gc(); //垃圾回收
    System.out.println(s);

    输出结果：null

    对象只有在创建它的方法执行结束才会被回收，或者主动设置obj = null。
    ```
   * 软引用：内存空间足够，垃圾回收器不会回收它。反之，则回收。适用于缓存，而且不会OOM。
    ```
    SoftReference<Object[]> reference = new SoftReference<>(new Object[300000000]);
    System.out.println(reference.get());
    Object[] objects = new Object[100000000];// 3
    System.out.println(reference.get());

    输出结果：
    [Ljava.lang.Object;@4554617c
    null

    结果说明执行代码3时，内存不够，垃圾回收器主动回收了软引用指向的对象。
    PS：Object数组长度根据JVM配置不同而不同。
    ```
   * 弱引用：只有当垃圾回收器扫描到弱引用指向的对象时，才会回收它。生命周期比软引用更短。
   ```
    WeakReference<String> reference = new WeakReference<>(new String("hello"));
    System.out.println(reference.get());
    System.gc(); //垃圾回收
    System.out.println(reference.get());

    输出结果：
    hello
    null
   ```
   * 虚引用：在任何时候都可能被垃圾回收器回收，必须与引用队列关联使用。
   ```
    ReferenceQueue<String> queue = new ReferenceQueue<>();
    PhantomReference<String> reference = new PhantomReference<>(new String("hello"), queue);
    System.out.println(reference.get());

    输出结果：
    null
   ```
  https://www.jianshu.com/p/94de80aee1bf