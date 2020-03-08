# AQS 同步器、reentrantlock、countdownlatch

## countdownlatch闭锁
* 所有子线程共用一个 countdownlatch对象，
* 每个子线程执行完了调用countdownlatch的countDown方法，
* 主线程调用await方法，如果子线程没有执行完，则调用countdownlatch对象的await方法是阻塞的，如果所有子线程都执行完了，执行countdownlatch对象的await方法是顺畅的，接下来就是处理主线程的业务逻辑。

# reentrantlock
* reentrantlock是通过内部内sync来管理锁，而sync是aqs类的一个子类
* reentrantlock里面包含2种锁，分别是公平锁和非公平锁，NonfairSync和FairSync是Sync的子类
* reentrantlock默认采用非公平锁，通过构造函数来初始化
  
  ```
    public ReentrantLock() {
    // 默认非公平锁
    sync = new NonfairSync();
    }
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

  ```
* 在reentrantlock中，阻塞队列是一个链表，把每个抢占锁的线程封装成一个Node，数据结构如下
  ```
  static final class Node {
    // 标识节点当前在共享模式下
    static final Node SHARED = new Node();
    // 标识节点当前在独占模式下
    static final Node EXCLUSIVE = null;

    // ======== 下面的几个int常量是给waitStatus用的 ===========
    /** waitStatus value to indicate thread has cancelled */
    // 代码此线程取消了争抢这个锁
    static final int CANCELLED =  1;
    /** waitStatus value to indicate successor's thread needs unparking */
    // 官方的描述是，其表示当前node的后继节点对应的线程需要被唤醒（也就是说waitStatus并不是表示当前节点的状态，而是表明后续节点需要被唤醒）
    static final int SIGNAL    = -1;
    /** waitStatus value to indicate thread is waiting on condition */
    static final int CONDITION = -2;
    /**
     * waitStatus value to indicate the next acquireShared should
     * unconditionally propagate
     */
    static final int PROPAGATE = -3;
    // =====================================================


    // 取值为上面的1、-1、-2、-3，或者0
    // 这么理解，暂时只需要知道如果这个值 大于0 代表此线程取消了等待，
    //    ps: 半天抢不到锁，不抢了，ReentrantLock是可以指定timeouot的。。。
    volatile int waitStatus;
    // 前驱节点的引用
    volatile Node prev;
    // 后继节点的引用
    volatile Node next;
    // 这个就是线程本尊
    volatile Thread thread;

    }
  ```    
* 非公平锁，在调用（acquire）cas进行一次抢锁后，如果这个时候恰好锁没有被占用，那就直接获取锁。如果cas后被锁占用之后，就去调用tryAcquire(AQS)去获取锁，在tryAcquire中又先通过通过cas去抢锁，如果此时正好，如果还是没有抢到，就会把当前线程挂起，放到阻塞队列中等待被唤醒
* 公平锁，直接去调用tryAcquire(AQS)去获取锁，先判断堵塞队列中是否有等待的，没有等待则直接去用cas抢锁，如果没有抢到锁，则进入acquireQueued(AQS)
  ```
  在调用次方法前获取调用addwaiter方法，将当前线程节点添加了阻塞队列链表的末尾，addwaiter的时候也需要通过cas进行竞争入队列，如果发现正有线程在入队列，则通过自旋（一直循环去入队列，直到入队尾成功） --以下几个方法都是CQS的方法
  final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = node.predecessor();
                if (p == head && tryAcquire(arg)) {
                    //如果当前节点node的前一个节点p是head节点，head节点初始化时是个空对象，head节点一般是占有锁的线程，
                    //如果head节点是初始化进来的，则他是不占锁的，并且是个空对象，这个时候可以再去tryAcquire获取一下锁
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                //一直遍历前区节点（从后到前）第一个遍历到前区的waitstatus要挂起，这返回true,如果都不要挂起则返回false
                if (shouldParkAfterFailedAcquire(p, node) &&
                //挂起线程
                    parkAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node);
        }
    }


    // 此方法的作用是把线程包装成node，同时进入到队列中
    // 参数mode此时是Node.EXCLUSIVE，代表独占模式
    private Node addWaiter(Node mode) {
        Node node = new Node(Thread.currentThread(), mode);
        // Try the fast path of enq; backup to full enq on failure
        // 以下几行代码想把当前node加到链表的最后面去，也就是进到阻塞队列的最后
        Node pred = tail;

        // tail!=null => 队列不为空(tail==head的时候，其实队列是空的，不过不管这个吧)
        if (pred != null) { 
            // 将当前的队尾节点，设置为自己的前驱 
            node.prev = pred; 
            // 用CAS把自己设置为队尾, 如果成功后，tail == node 了，这个节点成为阻塞队列新的尾巴
            if (compareAndSetTail(pred, node)) { 
                // 进到这里说明设置成功，当前node==tail, 将自己与之前的队尾相连，
                // 上面已经有 node.prev = pred，加上下面这句，也就实现了和之前的尾节点双向连接了
                pred.next = node;
                // 线程入队了，可以返回了
                return node;
            }
        }
        // 如果会到这里，
        // 说明 pred==null(队列是空的) 或者 CAS失败(有线程在竞争入队)
        enq(node);
        return node;
    }


    // 采用自旋的方式入队
    // 之前说过，到这个方法只有两种可能：等待队列为空，或者有线程竞争入队，
    // 自旋在这边的语义是：CAS设置tail过程中，竞争一次竞争不到，我就多次竞争，总会排到的
    private Node enq(final Node node) {
        for (;;) {
            Node t = tail;
            // 之前说过，队列为空也会进来这里
            if (t == null) { // Must initialize
                // 初始化head节点
                // 原来 head 和 tail 初始化的时候都是 null 的
                // 还是一步CAS，你懂的，现在可能是很多线程同时进来呢
                if (compareAndSetHead(new Node()))
                    // 给后面用：这个时候head节点的waitStatus==0, 看new Node()构造方法就知道了

                    // 这个时候有了head，但是tail还是null，设置一下，
                    // 把tail指向head，放心，马上就有线程要来了，到时候tail就要被抢了
                    // 注意：这里只是设置了tail=head，这里可没return哦，没有return，没有return
                    // 所以，设置完了以后，继续for循环，下次就到下面的else分支了
                    tail = head;
            } else {
                // 下面几行，和上一个方法 addWaiter 是一样的，
                // 只是这个套在无限循环里，反正就是将当前线程排到队尾，有线程竞争的话排不上重复排
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }
  ```
* 对于在阻塞队列中的线程，什么时候被唤醒呢，就是在对调用reentrantlock调用unlock，紧接着sync调用release，也就是说在释放锁的时候唤醒
    ```
    public void unlock() {
        sync.release(1);
    }

    public final boolean release(int arg) {
        if (tryRelease(arg)) {
            Node h = head;
            if (h != null && h.waitStatus != 0)
                //这里就是唤醒阻塞队列里的线程
                unparkSuccessor(h);
            return true;
        }
        return false;
    }

    protected final boolean tryRelease(int releases) {
    int c = getState() - releases;
    if (Thread.currentThread() != getExclusiveOwnerThread())
        throw new IllegalMonitorStateException();
    // 是否完全释放锁
    boolean free = false;
    // 其实就是重入的问题，如果c==0，也就是说没有嵌套锁了，可以释放了，否则还不能释放掉
    if (c == 0) {
        free = true;
        setExclusiveOwnerThread(null);
    }
    setState(c);
    return free;
    }

    CQS的方法unparkSuccessor

    private void unparkSuccessor(Node node) {
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        int ws = node.waitStatus;
        //如果head节点当前waitStatus<0, 将其修改为0
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
        Node s = node.next;
        if (s == null || s.waitStatus > 0) {
            //说明head节点的next节点是空或者取消了等待，
            //这种情况则是从队尾开始寻找，排在最前面的waitstatus小于等于0的节点
            //为什么要从后往前找，因为
            s = null;
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }
        if (s != null)
            //唤醒next节点的线程
            LockSupport.unpark(s.thread);
    }

    ```
* AQS中，取消占用锁排队：包括三步node不关联任何线程，node的waitstatus为取消状态，node出阻塞队列。
* reentranklock 加锁解锁的需要三部分的协作
  * 1、锁状态，state=0 代表没有线程没有占有锁，可以通过cas去占用锁，占用成功后state设置为1，如果是同一个线程再次进来占锁，则state继续+1，解锁则state-1,这个state是volatile来修饰的，知道变成0，才可以继续被别的线程占用
  * 2、线程的阻塞和唤醒，阻塞线程LockSupport.park(this);，唤醒线程LockSupport.unpark(node.thread);
  * 3、阻塞队列，AQS通过一个fifo队列，也就是一个链表，每一个线程节点node,都有后续节点和前置节点的引用。

* 线程1获取锁后，还没是否锁，此时线程2调用lock来占用锁，阻塞队列的情况是，第一个节点是Head节点，Head节点的所属线程是null，Head节点的waitstatus=-1，而这个节点的下一个节点是线程2创建的node1,node1的所属线程是线程2，node1节点的waitstatus=0,node1的前置节点是Head节点。

* reentrantlock的lock，trylock、lockInterruptibly区别
   * lock是去获取锁，如果获取失败则进入阻塞队列，知道获取成功
   * trylock 一般是说获取一段时间，入参可以设置，在定义时间内获取成功返回true,获取失败返回false
   * lockInterruptibly（如果当前线程在获取锁过程中被中断，则抛异常，不再获取锁，阻塞队列中也不会有该线程，因为在获取锁抛异常之后，会将当前线程对队列cancelAcquire）， 
  ```
  //该方法是
  public final void acquireInterruptibly(int arg)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        if (!tryAcquire(arg))
        //获取锁失败，直接将当期线程中断
            doAcquireInterruptibly(arg);
    }

    //该方法是特定时间内获取锁
    public final boolean tryAcquireNanos(int arg, long nanosTimeout)
            throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();
        return tryAcquire(arg) ||
            doAcquireNanos(arg, nanosTimeout);
    }
  ```
  ```
      static class Resource{
        ReentrantLock lock = new ReentrantLock();
    public static void main(String[] args){
        final Resource resource = new Resource();
        Thread t1 = new Thread(()->{
            resource.test();
        },"t1");
        t1.start();
        try{
            Thread.sleep(1000);
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("a2 "+System.currentTimeMillis());
        Thread t2 = new Thread(()->{
            resource.test1();
        },"t2");
        t2.start();

    }

        public void test(){
            lock.lock();
            try{
                System.out.println("a "+System.currentTimeMillis());
            }catch (Exception e){
                e.printStackTrace();
            }
            //故意不释放锁
            //lock.unlock();
        }

        public void test1(){
            try{
                System.out.println("b "+System.currentTimeMillis());
                final Thread t = Thread.currentThread();
                new Thread(()->{
                    try{
                        System.out.println("e "+System.currentTimeMillis());
                        Thread.sleep(1000);
                        //中断线程，
                        t.interrupt();
                        System.out.println("f "+System.currentTimeMillis());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                },"t3").start();
                System.out.println("c "+System.currentTimeMillis());
                //因为上面t3线程中断了外面的线程，所以这里再获取中断锁的过程中会抛出线程中断异常。
                lock.lockInterruptibly();
                System.out.println(lock.tryLock());
            }catch (Exception e){
                e.printStackTrace();
                //获取中断锁的线程不会在阻塞队列中
                System.out.println("aaa"+lock.hasQueuedThread(Thread.currentThread()));
            }
        }

    }
  ```

## 参考博客
https://www.jianshu.com/p/01f2046aab64

https://www.jianshu.com/p/89132109d49d


## Condition

* 使用场景：生产者和消费者模型
* condition是依赖reentrantlock产生，reentrantlock.newCondition();
* 在使用 condition 时，必须先持有相应的锁。这个和 Object 类中的方法有相似的语义，需要先持有某个对象的监视器锁才可以执行 wait(), notify() 或 notifyAll() 方法
* 我们常用 obj.wait()，obj.notify() 或 obj.notifyAll() 来实现相似的功能，但是，它们是基于对象的监视器锁的. 这里说的 Condition 是基于 ReentrantLock 实现的，而 ReentrantLock 是依赖于 AbstractQueuedSynchronizer 实现的。
* 上面介绍AQS的时候，有一个阻塞队列（也叫同步队列sync queue）存放等待锁线程节点用的，而ConditionObject(Condition的子类)有两个属性
  ```
  public class ConditionObject implements Condition, java.io.Serializable {
        private static final long serialVersionUID = 1173984872572414699L;
        // 条件队列的第一个节点 transient 是不需要系列化的意思
        /** First node of condition queue. */
        private transient Node firstWaiter;
        // 条件队列的最后一个节点
        /** Last node of condition queue. */
        private transient Node lastWaiter;

        //这个Node 跟AQS是一个类，也就是说结构是一样的
        但是Condition的node没有pre和next值，但是有nextWaiter，所以condition是个单向链表
  ```
* 如线程 1 调用 condition1.await() 方法即可将当前线程 1 包装成 Node 后加入到条件队列中，然后阻塞在这里，不继续往下执行，条件队列是一个单向链表；并且此时也会释放锁。
  ```
    // 首先，这个方法是可被中断的，不可被中断的是另一个方法 awaitUninterruptibly()
    // 这个方法会阻塞，直到调用 signal 方法（指 signal() 和 signalAll()，下同），或被中断
    public final void await() throws InterruptedException {
        // 老规矩，既然该方法要响应中断，那么在最开始就判断中断状态
        if (Thread.interrupted())
            throw new InterruptedException();

        // 添加到 condition 的条件队列中
        Node node = addConditionWaiter();

        // 释放锁，返回值是释放锁之前的 state 值
        // await() 之前，当前线程是必须持有锁的，这里肯定要释放掉
        int savedState = fullyRelease(node);

        int interruptMode = 0;
        // 这里退出循环有两种情况，之后再仔细分析
        // 1. isOnSyncQueue(node) 返回 true，即当前 node 已经转移到阻塞队列了
        // 2. checkInterruptWhileWaiting(node) != 0 会到 break，然后退出循环，代表的是线程中断
        // 如果不在阻塞队列中，注意了，是阻塞队列 
        while (!isOnSyncQueue(node)) {
            LockSupport.park(this);// 线程挂起
            if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                break;
        }
        // 被唤醒后，将进入阻塞队列，等待获取锁
        if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
            interruptMode = REINTERRUPT;
        if (node.nextWaiter != null) // clean up if cancelled
            unlinkCancelledWaiters();
        if (interruptMode != 0)
            reportInterruptAfterWait(interruptMode);
    }

  ```
* 如果一个线程在不持有 lock 的基础上，就去调用 condition1.await() 方法，它能进入条件队列，但是在上面的这个方法中，由于它不持有锁，release(savedState) 这个方法肯定要返回 false，进入到异常分支，然后进入 finally 块设置 node.waitStatus = Node.CANCELLED，这个已经入队的节点之后会被后继的节点"请出去"。
* 调用condition1.signal() 触发一次唤醒，此时唤醒的是队头，会将condition1 对应的条件队列的 firstWaiter（队头） 移到阻塞队列的队尾，等待获取锁，获取锁后 await 方法才能返回，继续往下执行。
```
    // 唤醒等待了最久的线程
    // 其实就是，将这个线程对应的 node 从条件队列转移到阻塞队列
    public final void signal() {
        // 调用 signal 方法的线程必须持有当前的独占锁
        if (!isHeldExclusively())
            throw new IllegalMonitorStateException();
        Node first = firstWaiter;
        if (first != null)
            doSignal(first);
    }

    // 从条件队列队头往后遍历，找出第一个需要转移的 node
    // 因为前面我们说过，有些线程会取消排队，但是可能还在队列中
    private void doSignal(Node first) {
        do {
            // 将 firstWaiter 指向 first 节点后面的第一个，因为 first 节点马上要离开了
            // 如果将 first 移除后，后面没有节点在等待了，那么需要将 lastWaiter 置为 null
            if ( (firstWaiter = first.nextWaiter) == null)
                lastWaiter = null;
            // 因为 first 马上要被移到阻塞队列了，和条件队列的链接关系在这里断掉
            first.nextWaiter = null;
        } while (!transferForSignal(first) &&
                (first = firstWaiter) != null);
        // 这里 while 循环，如果 first 转移不成功，那么选择 first 后面的第一个节点进行转移，依此类推
    }

    // 将节点从条件队列转移到阻塞队列
    // true 代表成功转移
    // false 代表在 signal 之前，节点已经取消了
    final boolean transferForSignal(Node node) {

        // CAS 如果失败，说明此 node 的 waitStatus 已不是 Node.CONDITION，说明节点已经取消，
        // 既然已经取消，也就不需要转移了，方法返回，转移后面一个节点
        // 否则，将 waitStatus 置为 0
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        // enq(node): 自旋进入阻塞队列的队尾
        // 注意，这里的返回值 p 是 node 在阻塞队列的前驱节点
        Node p = enq(node);
        int ws = p.waitStatus;
        // ws > 0 说明 node 在阻塞队列中的前驱节点取消了等待锁，直接唤醒 node 对应的线程。唤醒之后会怎么样，后面再解释
        // 如果 ws <= 0, 那么 compareAndSetWaitStatus 将会被调用，节点入队后，需要把前驱节点的状态设为 Node.SIGNAL(-1)
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            // 如果前驱节点取消或者 CAS 失败，会进到这里唤醒线程，之后的操作看下一节
            LockSupport.unpark(node.thread);
        return true;
    }
```
* 用aqs的condition实现生产者消费者模型，当然阻塞队列（block queue）是天生的自带生产者消费者模型的队列
```
   static class Resource{
        LinkedList<String> list = new LinkedList<>();
        int MAX_SIZE = 10;
        Lock lock = new ReentrantLock();
        Condition inCondition = lock.newCondition();
        Condition outCondition = lock.newCondition();

        public synchronized void produce(String element) throws InterruptedException {
            lock.lockInterruptibly();
            try{
                while(list.size() >= MAX_SIZE){
                    //将当前线程封装成node加入到inCondition,并且释放当前锁。
                    inCondition.await();
                }
                list.addLast(element);
                System.out.println(Arrays.toString(new LinkedList[]{list}));
                //唤醒outCondition队列的队头，将outCondition的队头加入到lock的阻塞队列,让consume
                outCondition.signalAll();
                //这里的inCondition、outCondition使用的锁都是try上面获取的那个锁
            }  finally {
                lock.unlock();
            }
        }

        public String consume() throws InterruptedException {
            lock.lockInterruptibly();
            try{
                while(list.size() == 0){
                    outCondition.await();
                }
                String ele = list.removeFirst();
                System.out.println(Arrays.toString(new LinkedList[]{list}));
                inCondition.signalAll();
                return ele;
            }finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args){
        final Resource resource = new Resource();
        for(int i =0; i< 5; i++){
            new Thread(()->{
                try {
                    while(true) {
                        Thread.sleep(1000);
                        resource.produce(new Random().nextInt(100) + "");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        for(int i =0; i< 5; i++){
            new Thread(()->{
                try {
                    while(true) {
                        Thread.sleep(2000);
                        resource.consume();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
```
