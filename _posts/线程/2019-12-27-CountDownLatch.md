# CountDownLatch  

* 计数器，java.util.concurrent.CountDownLatch基于AQS实现，关键的方法wait,countDown  
* 初始化CountDownLatch(int count),执行完一个线程就调用countDown的方法，wait:阻塞当前线程，直到latch减到0，也就是说阻塞到所有的子任务都执行完了，再往下走。
* Sync是一个AQS同步器，实现了 tryAcquireShared()和tryReleaseShared()，这是一个共享模式下的同步器。
* CountDownLatch在初始化的时候传入一个同步线程数量count表示AQS的state。然后执行await()的时候，AQS会先判断state是否为0，state不为0的话就会阻塞当前线程，并让当前线程进入AQS的CLH队列中排队。
* 当有线程执行countDown的时候,Sync会通过cas+自旋的方式将state减一，然后判断state是否等于0。等于0的时候返回true，AQS发现tryReleaseShared()返回true，就会去唤醒正在CLH队列中排队等待的线程，先唤醒排在最前面的那个线程。由于是共享模式，那个线程被唤醒后，检查state=0了，就结束阻塞，并且会通知下一个排队线程，下一个线程醒来后，一样判断state是否等于0了，然后结束阻塞，通知下一个，一直循环下去，直到所有阻塞中的线程全部被唤醒

* 自己实现一个CountDownLatch，不是基于AQS  
  
```
    public class MyCountDownLatch {

        private final int total;

        private int counter = 0;

        public MyCountDownLatch(int total) {
            this.total = total;
        }

        public void countDown() {
            synchronized (this) {
                this.counter++;
                this.notifyAll();
            }
        }

        public void await() throws InterruptedException {
            synchronized (this) {
                while (counter != total) {
                    this.wait();
                }
            }
        }
    }

```