---
title: jdk集合
categories:
 - jdk
tags: 
 - Collection
 - queue 
---

## set、list、queue、map

* List、Set、Queue 都继承自 Collection 接口，而 Map 则不是（继承自 Object），所以容器类有两个根接口，分别是 Collection 和 Map，Collection 表示单个元素的集合，Map 表示键值对的集合。  
  
## List

> LinkedList
> ArrayList
> Vector
> CopyOnWriteArrayList
> Stack

## set  

* HashSet
* TreeSet
* LinkedHashSet
* SortedSet

## queue

* BlockingQueue
  
> ArrayBlockingQueue  
> LinkedBlockingQueue
> PriorityBlockingQueue
> DelayQueue
> SynchronousQueue
 
> Deque
> SynchronizedQueue
> ConcurrentLinkedQueue
> PriorityQueue

## map  

> * HashMap线程不安全方面，主要体现在put的时候导致数据不一致。线程A和B,A希望插入一对key-value,分配到hash槽中，但是B线程同时也插入一个key-value到A线程相同的hash槽中，如果AB两个线程同时进行的话，很有可能会出现被覆盖的情况。  
> HashMap 允许key和value为空
> * ConcurrentHashMap>ConcurrentMap，不允许key和value为空，`if (key == null || value == null) throw new NullPointerException();`
> * 利用cas（一种无锁操作）+synchronized来保证并发更新的安全性，其他的跟HashMap相通。  
> * LinkedHashMap
> * Hashtable
> * SortedMap
