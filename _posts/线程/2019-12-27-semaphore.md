# Semaphore
* 信号量，设计思路如下: 一个阻塞队列，一个信号量，3个信号量方法。改变信号量的方法应该是原子性的，也就是下面的down up方法应原子性的。
* Init()计数器的初始值
* up()计数器的值加1。如果此时计数器值的<=0,则唤醒等待队里的一个线程，并从等待队列中移除
* down()计数器的值减1。如果此时的计数器值<0,则当前的线程被阻塞。否则，线程继续执行。

```
  class Semaphore{
    // 计数器
    int count;
    // 等待队列
    Queue queue;
    // 初始化操作
    Semaphore(int c){
        this.count=c;
    }
    //
    void down(){
        this.count--;
        if(this.count<0){
            // 将当前线程插入等待队列
            // 阻塞当前线程
        }
    }
    void up(){
        this.count++;
        if(this.count<=0) {
            // 移除等待队列中的某个线程 T
            // 唤醒线程 T
        }
    }
  }


    static int count;

    static final Semaphore s = new Semaphore(1);
    static void addOne() {
        s.down();
        try {
            count+=1;
        } finally {
            s.up();
        }
    }
```

* java.util.concurrent.Semaphore就是基于AQS实现的(与entrantlock类似)，acquire()方法实现信号量模型的down()方法。relase()方法实现信号量模型的up()方法。
