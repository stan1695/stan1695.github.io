## mysql锁概述  
* 相对于其他数据库的锁，mysql的锁机制比较简单；不同的存储引擎，锁机制不同。  
* MyISAM和MEMORY存储引擎采用的是表级锁（table-level locking）。  
* BerkeleyDB(BDB) 存储引擎采用的是页面锁（page-level locking）,也支持表级锁。  
* InnoDB存储引擎既支持行级锁（row-level locking），也支持表级锁。  
  
* 表级锁：开销小，加锁快；不会出现死锁；锁定粒度大，发生锁冲突的概率最高，并发度最低。  
* 行级锁：开销大，加锁慢；会出现死锁；锁定粒度最小，发生锁冲突的概率最低，并发度也最高。  
* 页面锁：开销和加锁时间界于表锁和行锁之间；会出现死锁；锁定粒度界于表锁和行锁之间，并发度一般  
  
## MyISAM
### MyISAM表锁
* mysql的表锁有两种模式：1、表共享读锁（Table Read Lock）,表独占锁（Table Write Lock）
* 当一个session获得一个表的写锁后，只有获得写锁的session可以对表进行读写操作，其他session的读、写都会等待，直到锁被释放为止。  
* 当一个session获得一个表的读锁后，这个session可以查询锁定表中的记录，但是当前session不能查询没有锁定的表，当前session插入和更新锁定表也会报错；其他session可以查询锁定表中的记录，其他session也可以查询和更新其他表未锁定的表，其他session更新锁定表会等待。

### MyISAM如何加锁
* select 给涉及的所有表加读锁  
* UPDATE、DELETE、INSERT 自动给涉及的表加写锁  
* 显示加锁 LOCK TABLES，但是在显示加锁之后 只能访问加锁了的表，不能访问其他表了。所以显示加锁，只能一次性把所需要的表都锁上，如果存在别名，也要加上别名一起锁上。比如:lock table actor as a read,actor as b read;
### MyISAM并发插入（Concurrent Inserts）
MyISAM存储引擎有一个系统变量concurrent_insert，专门用以控制其并发插入的行为，其值分别可以为0、1或2。
* 当concurrent_insert设置为0时，不允许并发插入。
* 当concurrent_insert设置为1时，如果MyISAM表中没有空洞（即表的中间没有被删除的行），MyISAM允许在一个进程读表的同时，另一个进程从表尾插入记录。这也是MySQL的默认设置。
* 当concurrent_insert设置为2时，无论MyISAM表中有没有空洞，都允许在表尾并发插入记录。

## InnoDB
### InnoDB行锁  
InnoDB实现了以下两种类型的行锁。
* 共享锁（s）：又称读锁。若事务T对数据对象A加上S锁，则事务T可以读A但不能修改A，其他事务只能再对A加S锁，而不能加X锁，直到T释放A上的S锁。这保证了其他事务可以读A，但在T释放A上的S锁之前不能对A做任何修改。
* 排他锁（x）：又称写锁。若事务T对数据对象A加上X锁，事务T可以读A也可以修改A，其他事务不能再对A加任何锁，直到T释放A上的锁。

### InnoDB如何加行锁
* select语句默认不会加任何锁类型
* update,delete,insert都会自动给涉及到的数据加上排他锁
* select …for update 加排他锁
* 显性加共享锁 select … lock in share mode  

所以加过排他锁的数据行在其他事务种是不能修改数据的，也不能通过for update和lock in share mode锁的方式查询数据，但可以直接通过select …from…查询数据，因为普通查询没有任何锁机制。

### InnoDB行锁实现方式
* InnoDB行锁是通过给索引上的索引项加锁来实现的，这一点MySQL与Oracle不同，后者是通过在数据块中对相应数据行加锁来实现的。
* 只有通过索引条件检索数据，InnoDB才使用行级锁，否则，InnoDB将使用表锁！
* 由于MySQL的行锁是针对索引加的锁，不是针对记录加的锁，所以虽然是访问不同行的记录，但是如果是使用相同的索引键，是会出现锁冲突的。
* 使用不同的索引，如果访问的记录被其他session锁定，也是需要等待锁才能访问。
* 即便在条件中使用了索引字段，但是否使用索引来检索数据是由MySQL通过判断不同执行计划的代价来决 定的，如果MySQL认为全表扫描效率更高，比如对一些很小的表，它就不会使用索引，这种情况下InnoDB将使用表锁，而不是行锁。因此，在分析锁冲突 时，别忘了检查SQL的执行计划，以确认是否真正使用了索引。 
* 检索值的数据类型与索引字段不同，虽然MySQL能够进行数据类型转换，但却不会使用索引，从而导致InnoDB使用表锁。通过用explain检查两条SQL的执行计划，我们可以清楚地看到了这一点。  
  
### 间隙锁（Next-Key锁）
当我们用范围条件而不是相等条件检索数据，并请求共享或排他锁时，InnoDB会给符合条件的已有数据记录的 索引项加锁；对于键值在条件范围内但并不存在的记录，叫做“间隙（GAP)”，InnoDB也会对这个“间隙”加锁，这种锁机制就是所谓的间隙锁 （Next-Key锁）。  

举例来说，假如emp表中只有101条记录，其empid的值分别是 1,2,…,100,101，下面的SQL：Select * from  emp where empid > 100 for update;是一个范围条件的检索，InnoDB不仅会对符合条件的empid值为101的记录加锁，也会对empid大于101（这些记录并不存在）的“间隙”加锁。  

InnoDB使用间隙锁的目的，一方面是为了防止幻读，以满足相关隔离级别的要求，对于上面的例子，要是不使 用间隙锁，如果其他事务插入了empid大于100的任何记录，那么本事务如果再次执行上述语句，就会发生幻读；

## 锁分类  
* 锁粒度：行锁，表锁，叶锁  
* 锁模式：记录锁，gap锁，next-key锁，意向所，插入意向所
* 加锁机制：乐观锁，悲观锁  
* 锁兼容性：共享受，排它锁  


### 参考
* https://blog.csdn.net/and1kaney/article/details/51214001 

