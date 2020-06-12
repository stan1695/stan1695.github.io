---
title: 为什么String 类要被修饰成final
categories:
 - jdk
tags: 
 - String
 - 值传递
---


## java中一切参数传递都是值传递

> 基础数据类型不用讲，调用方法时传递的参数，都是从外面的方法栈中赋值了一份数值传递放到调用方法栈中。
> 而对象数据类型(引用数据类型)，调用方法时传递的参数，也是赋值了一个对象的引用传递到调用方法栈中。也就是说堆内存中的对象多了一个引用方。
> 所以在调用方法中对传递的对象重新赋值，也就是说重新换一个引用的话，此时外面的对象跟重新换引用的对象就不是同一个对象了。
```
    public static void main(String[] args) {
        Date d = new Date();
        String s = new String("s1");
        System.out.println("1.d:"+d.hashCode());
        System.out.println("1.s:"+s);
        test(d,s);
        System.out.println("3.d:"+d.hashCode());
        System.out.println("3.s:"+s);
    }
    private static void test(Date d,String s){
        try {
            Thread.sleep(1000);
            d = new Date();
            s = "s2";
            System.out.println("2.d:"+d.hashCode());
            System.out.println("2.s:"+s);
        }catch (Exception e){

        }

    }

    //输出：
    1.d:-1484995211
    1.s:s1
    2.d:-1484994915
    2.s:s2
    3.d:-1484995211
    3.s:s1

    可以看到，在test方法中对d换引用，并没有影响到main方法中d的引用（1和2处的对象是同一个对象）
    同样在test中s = "s2",这里其实也是给s赋值了一个新的对象引用，但是这个对象是常量池里的对象，所以这里的s和main函数的s是同一个对象。这是因为jvm中将String设置成了常量池。
    但是如果将s = new Sting("s1"),经过断点看以分析出这里的s与main函数里的s不是一个对象。
```


## 参考资料
 > https://blog.csdn.net/z1790424577/article/details/83788791