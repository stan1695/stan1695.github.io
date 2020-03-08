* CyclicBarrier:字面意思“循环栅栏”，可循环屏障。
* 作用就是让所有的线程等待执行完，再进行下一步。
* 怎么使用：
    1、初始化构造方法
    ```
    public CyclicBarrier(int parties) //参与线程的个数
    public CyclicBarrier(int parties, Runnable barrierAction)//构造方法有一个 Runnable 参数，这个参数的意思是最后一个到达线程要做的任务
    ```
    2、wait方法，线程调用 await() 表示自己已经到达栅栏。
    ```
    public int await() throws InterruptedException, BrokenBarrierException
    public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException
    //BrokenBarrierException 表示栅栏已经被破坏，破坏的原因可能是其中一个线程 await() 时被中断或者超时
    ```
* 运用例子
  ```
    public class CyclicBarrierDemo {

        static class TaskThread extends Thread {
            
            CyclicBarrier barrier;
            
            public TaskThread(CyclicBarrier barrier) {
                this.barrier = barrier;
            }
            
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                    System.out.println(getName() + " 到达栅栏 A");
                    barrier.await();
                    System.out.println(getName() + " 冲破栅栏 A");
                    
                    Thread.sleep(2000);
                    System.out.println(getName() + " 到达栅栏 B");
                    barrier.await();
                    System.out.println(getName() + " 冲破栅栏 B");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        
        public static void main(String[] args) {
            int threadNum = 5;
            CyclicBarrier barrier = new CyclicBarrier(threadNum, new Runnable() {
                
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName() + " 完成最后任务");
                }
            });
            
            for(int i = 0; i < threadNum; i++) {
                new TaskThread(barrier).start();
            }
        }
        
    }
  ```
打印结果
```
Thread-1 到达栅栏 A
Thread-3 到达栅栏 A
Thread-0 到达栅栏 A
Thread-4 到达栅栏 A
Thread-2 到达栅栏 A
Thread-2 完成最后任务
Thread-2 冲破栅栏 A
Thread-1 冲破栅栏 A
Thread-3 冲破栅栏 A
Thread-4 冲破栅栏 A
Thread-0 冲破栅栏 A
Thread-4 到达栅栏 B
Thread-0 到达栅栏 B
Thread-3 到达栅栏 B
Thread-2 到达栅栏 B
Thread-1 到达栅栏 B
Thread-1 完成最后任务
Thread-1 冲破栅栏 B
Thread-0 冲破栅栏 B
Thread-4 冲破栅栏 B
Thread-2 冲破栅栏 B
Thread-3 冲破栅栏 B
```