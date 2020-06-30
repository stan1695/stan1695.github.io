---
title: AtomicInteger
categories:
 - jdk
tags: 
 - Unsafe
 - compareAndSwap
 - cas 
---

## 普通的基础数据类型，线程不安全

```
package com;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerTest {

    private static int threadCount = 10;
    private static CountDownLatch countDown = new CountDownLatch(threadCount);
    private static int count = new AtomicInteger(0);

    public static void main(String[] args) {
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new Counter());
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].start();
        }
        try {
            countDown.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("count=" + count);
    }

    private static class Counter implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < 1000; i++) {
                count++;
            }
            countDown.countDown();
        }
    }
}

```
> 输出的结果，每次跑都不一样。


## AtomicInteger原子操作类  

```
package com;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerTest {

    private static int threadCount = 10;
    private static CountDownLatch countDown = new CountDownLatch(threadCount);
    private static AtomicInteger count = new AtomicInteger(0);

    public static void main(String[] args) {
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(new Counter());
        }
        for (int i = 0; i < threadCount; i++) {
            threads[i].start();
        }
        try {
            countDown.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("count=" + count);
    }

    private static class Counter implements Runnable {

        @Override
        public void run() {
            for (int i = 0; i < 1000; i++) {
                count.getAndIncrement();
            }
            countDown.countDown();
        }
    }
}

```

> 线程安全，每次跑的结果都是10000  

## AtomicInteger中的一些变量

```

//unsafe实例，用来获取并操作内存的数据。
private static final Unsafe unsafe = Unsafe.getUnsafe();

//用来记录偏移量，这是一个final变量
private static final long valueOffset;

static {
    try {
        //valueOffset默认值是0
        valueOffset = unsafe.objectFieldOffset
            (AtomicInteger.class.getDeclaredField("value"));
    } catch (Exception ex) { throw new Error(ex); }
}

//value，存储AtomicInteger的int值，该属性需要借助volatile关键字保证其在线程间是可见的。
private volatile int value;
```
