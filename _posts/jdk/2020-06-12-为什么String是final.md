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

## String final

> String、StringBuilder、StringBuffer都是final的，不允许外部继承和改变
> 为了线程安全，String作为一个对象，在进行参数传递是，传递的是对象的引用，如果我通过引用可以改变String对象，这样在多线程时，就非常不安全。一个线程正在用这个String对象，突然被另一个线程给改变了。如果设置成final。则这个对象是不可改变的。
> String是final的，char[] value也是final的。
> String可以理解为基础数据类型一样，不能被继承。
> 因为String 是final的，所以在可以将String字符串存入常量池中。

## 常量池

> String 作为大量频繁使用的对象，在创建的时候如果跟普通对象一样，不做优化处理，就可能极大影响程序的性能。
> jvm 为了提高性能，减少开销，在实例化字符串常量的时候进行了一下优化。

```
public static void main(String[] args) {

    public static void main(String[] args) {

        String str1 = "abc";
        String str2 = "abc";
        String str3 = "a"+ "bc";

        String str4 = new String("abc");
        String str5 = new String("abc");
        String str6 = new String("a");
        String str7 = str6+"bc";
        String str8 = new String("bc");
        String str9 = str6 + str8;
        String str10 = str1+ str2;

        System.out.println(str1 == str2);//T
        System.out.println(str1 == str3);//T
        System.out.println(str1 == str4);//F
        System.out.println(str5 == str4);//F
        System.out.println(str5 == str7);//F
        System.out.println(str1 == str7);//F
        System.out.println(str3 == str7.intern());//T
        System.out.println(str5 == str9);//F
        System.out.println(str1 == str9);//F
        System.out.println(str1 == str10.intern());//F
    }

    //再看一下String的equals方法
public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof String) {
            String anotherString = (String)anObject;
            int n = value.length;
            if (n == anotherString.value.length) {
                char v1[] = value;
                char v2[] = anotherString.value;
                int i = 0;
                while (n-- != 0) {
                    if (v1[i] != v2[i])
                        return false;
                    i++;
                }
                return true;
            }
        }
        return false;
    }

    //equals方法就是比较字符串的char[]中各下标的字符是否相等。


    public native String intern();

```
> 上述代码中Str1 Str2 Str3 都是从常量池中获取数据，也就是他们的引用都是指向了常量池。
> `String str3 = "a"+ "bc";`在编译的时候做了字符串拼接，发现常量池中存在'abc',就直接指向常量池了。
> str4、str5、str6、str7都是现在堆内存中new了一个新对象，在判断常量池中是否存在字符串'abc',如果不存在则在常量池中创建一个常量'abc',然后将堆中对象值指向常量池。
> str7.intern()是返回的常量池地址，所以`str3 == str7.intern()`返回true 
> `str1 == str10.intern()`Jvm编译时的优化如果是两个常量，Jvm会认定这已经是不可变的，就会直接在编译时和常量池进行判断比对等，但是如果是加上一个变量，说明最后运行得出的结果是可变的，Jvm无法在编译时就确定执行之后的结果是多少，所以不会把该结果和常量池比对。  

## intern
> 当调用这个方法时候，如果常量池包含了一个<调用 code equals(Object)>相等的常量，就把该 常量池的对象返回，否则，就把当前对象加入到常量池中并且返回当前对象的引用。
> 判断这个常量是否存在于常量池。如果存在，则直接返回地址值（只不过地址值分为两种情况，1是堆中的引用，2是本身常量池的地址）
> 如果是引用，返回引用地址指向的堆空间对象地址值 如果是常量，则直接返回常量池常量的地址值，如果不存在，将当前对象引用复制到常量池,并且返回的是当前对象的引用  

```
public static void main(String[] args){
        String s1 = new String("1")+new String("23");
        s1.intern();
        String s2 = "123";
        System.out.println( s1 == s2);
}

分析：1 首先看第一行是两个new String类型的字符串相加（详见上文第4点）可知道，这里创建了堆中有3个对象 一个是1，一个是23，还有一个是结果 123，由于程序刚启动常量池也没有 1，23 所以会在常量池创建2个对象 （1 ， 23）
    2 当s1执行intern()方法之后，首先去常量池判断有没有123，此时发现没有，所以会把对象加入到常量池，并且返回当前对象的引用（堆中的地址）
    3 当创建s2时候（详见上文第1点），并且找到常量池中123，并且把常量池的地址值返回给s2
    4 由于常量池的地址值就是s1调用intern（）方法之后得到的堆中的引用，所以此时s1和s2的地址值一样，输出true。
```

```
public static void main(String[] args){
        String s1 = new String("1")+new String("23");
        String s2 = "123";
        s1.intern();
        System.out.println( s1 == s2);
}

如果把中间两行换一个位置，那输出就是false了，下面在分析一下不同点，上面分析过的不再赘述。
   1.在执行到第二行的时候String s2 = "123"时，发现常量池没有123，所以会先创建一个常量
   2.在当s1调用intern()方法时，会发现常量池已经有了123对象，就会直接把123的常量给返回出去，但是由于返回值并没有接收，所以此时s1还是堆中地址，则输入false；如果代码换成  s1 = s1.intern();那s1就会重新指向常量池了，那输出就为true。
```

## StringBuilder和StringBuffer

```

//StringBuilder
@Override
public StringBuilder append(String str) {
    super.append(str);
    return this;
}

public AbstractStringBuilder append(String str) {
        if (str == null)
            return appendNull();
        int len = str.length();
        ensureCapacityInternal(count + len);
        str.getChars(0, len, value, count);
        count += len;
        return this;
}

// count + len 和 count += len不是原子操作，在多线程的时候，会出现线程不安全现象，比如A B两个线程同时进来，这是count是10，假设len是1，这是同时进行count+len可能两个线程都是11,但是准确的应该是一个是11，一个是12.，如果两个都是算成11的话，还会出现下标越界的情况。

//而StringBuffer确实一个线程安全的类
@Override
public synchronized StringBuffer append(String str) {
    toStringCache = null;
    super.append(str);
    return this;
}

```

## 参考资料
 > https://blog.csdn.net/z1790424577/article/details/83788791