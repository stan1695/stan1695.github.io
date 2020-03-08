## InnoDB事务

* 为了解决并发与隔离的矛盾，sql92 定义了4个隔离级别：  

    读未提交：会存在脏读。  
    读已提交：解决了脏读，但是仍然会有不可重复读。  
    可重复的：解决了脏读，不可重复的，但是仍有幻读问题。  
    串行执行：解决了脏读，不可重复读，幻读的问题。

* InnoDB中的日志文件  
  MySQL Innodb中存在多种日志，除了错误日志、查询日志外，还有很多和数据持久性、一致性有关的日志。  

  binlog，是mysql服务层产生的日志，常用来进行数据恢复、数据库复制，常见的mysql主从架构，就是采用slave同步master的binlog实现的, 另外通过解析binlog能够实现mysql到其他数据源（如ElasticSearch)的数据复制。redo log 跟binary log 的区别，redo log 是存储引擎层产生的，而binary log是数据库层产生的。假设一个大事务，对tba做10万行的记录插入，在这个过程中，一直不断的往redo log顺序记录，而binary log不会记录，直到这个事务提交，才会一次写入到binary log文件中。  

  redo log，是innodb引擎层产生的日志，当数据库对数据做修改的时候，需要把数据页从磁盘读到buffer pool中，然后在buffer pool中进行修改，那么这个时候buffer pool中的数据页就与磁盘上的数据页内容不一致，称buffer pool的数据页为dirty page 脏数据，如果这个时候发生非正常的DB服务重启，那么这些数据还在内存，并没有同步到磁盘文件中（注意，同步到磁盘文件是个随机IO），也就是会发生数据丢失，如果这个时候，能够在有一个文件，当buffer pool 中的data page变更结束后，把相应修改记录记录到这个文件（注意，记录日志是顺序IO），那么当DB服务发生crash的情况，恢复DB的时候，也可以根据这个文件的记录内容，重新应用到磁盘文件，数据保持一致。这个文件就是redo log ，用于记录 数据修改后的记录，顺序记录。

  undo Log（回滚日志）: 是innodb引擎层产生的日志，除了记录redo log外，当进行cluster index数据修改时还会记录undo log，undo log用于数据的撤回操作，它记录了修改的反向操作，比如，插入对应删除，修改对应修改为原来的数据，通过undo log可以实现事务回滚，并且可以根据undo log回溯到某个特定的版本的数据，实现MVCC。  
  
* undo record:undo记录的主要类型如下，其中TRX_UNDO_INSERT_REC为insert的undo，其他为update和delete的undo。  
  
    ```  
    #define TRX_UNDO_INSERT_REC 11 /* fresh insert into clustered index */
    #define TRX_UNDO_UPD_EXIST_REC 12 /* update of a non-delete-marked record */
    #define TRX_UNDO_UPD_DEL_REC 13 
    /* update of a delete marked record to a not delete 
    marked record; also the fields of the record can change */
    #define TRX_UNDO_DEL_MARK_REC 14 
    /* delete marking of a record; fields do not change */
    ```  

    对于insert和delete，undo中会记录键值，delete操作只是标记删除(delete mark)记录。
    对于update，如果是原地更新，undo中会记录键值和老值。update如果是通过delete+insert方式进行的，则undo中记录键值，不需记录老值。其中delete也是标记删除记录。二级索引的更新总是delete+insert方式进行。
  
* 如果当前行的事务不可见，则根据DB_ROLL_PTR去undo log信息中寻找，再判断下这个版本的数据是否可见，以此类推。
  
* insert：当事务1插入一条数据时，回滚指针是null，生成一条Insert undo log。  
  
  update:生成一条update undo log,记录之前的旧值；DB_ROLL_PTR指向生成的update undo log  
  
  delete:生成一条update undo log,记录之前的旧值；将索引中的值标记为删除，DB_ROLL_PTR指向生成的update undo log，此时并没有真正的删除数据，而是需要purge线程来工作（补充：purge线程两个主要作用是：清理undo页和清除page里面带有Delete_Bit标识的数据行。在InnoDB中，事务中的Delete操作实际上并不是真正的删除掉数据行，而是一种Delete Mark操作，在记录上标识Delete_Bit，而不删除记录。是一种"假删除",只是做了个标记，真正的删除工作需要后台purge线程去完成。

* insert undo log是指在insert 操作中产生的undo log，因为insert操作的记录，只对事务本身可见，对其他事务不可见。故该undo log可以在事务提交后直接删除，不需要进行purge操作。

* 而update undo log记录的是对delete 和update操作产生的undo log，该undo log可能需要提供MVCC机制，因此不能再事务提交时就进行删除。提交时放入undo log链表，等待purge线程进行最后的删除。

* 当事务2更新一条数据时，1、用排它锁锁定该行，2、把该行修改前的值copy到undo log中，3、修改当前值，填写事务编号，使回滚指针指向undo log修改前那条记录，

* InnoDB中怎么解决可重复读的问题呢：借助undo log实现版本隔离  
  mvcc(multiversion concurrency controll)多版本并发控制引擎。但是mvcc并不能解决幻读的问题。  
  Mysql Innodb cluster index中的行记录的存储格式，除了最基本的行信息外,还有2个字段：DB_TRX_ID,DB_ROLL_PTR  
  DB_TRX_ID（事务编号）:用来标示，最后一次对该行记录进行修改（update|insert）的事务id。  
  DB_ROLL_PTR（回滚指针）:需要回滚的那条记录的指针，指向undo log record(撤销日志记录)。如果一条记录被更新，则需要往undo log record 中插入一条被更新之前的记录。  
  此外，删除在内部被视为更新，其中行中的特殊位被设置为将其标记为已删除

## InnoDB 快照 read view

* read view:数据快照，在读数据时（只有在select情况下才会创建视图），我们得到的是那个时间点的快照数据，而不用管同时其他事务对数据的修改，查询过程中，若其他事务修改了数据，那么就需要从 undo log中获取旧版本的数据。这么做可以有效避免因为需要加锁（来阻止其他事务同时对这些数据的修改）而导致事务并行度下降的问题。同时也保证了数据的一致性。  
  
* read view的数据结构，当前事务标识creator_trx_id，当前活跃事务数量n_trx_ids，当前活跃事务数组trx_ids，视图链表view_list，能看见最大版本low_limit_id（大于等于low_limit_id的版本都看不见），能看见的最小版本up_limit_id（小于up_limit_id的版本都看见）；比如：当前事务creator_trx_id=5，活跃事务n_trx_ids=4，活跃事务数组trx_ids=[8,4,3](不包括当前事务),low_limit_id=8，up_limit_id=3，视图链表view_list=trx8->trx4->trx3;在活跃数组中的事务，通过二分法查找，如果查找出来的事务与当前事务相等则可见，否则就不可见。也就是说最好可见的事务版本是当前版本和小于up_limit_id的版本。  
  
* 何时创建快照：在RC隔离级别下，是每个SELECT都会获取最新的read view；而在RR隔离级别下，则是当事务中的第一个SELECT请求才创建read view。
  
* select ... for update和select ... lock in share mode（8.0是select ... for share）会重新生成read view;  

    ```
    Mysql 官方给出的幻读解释是：只要在一个事务中，第二次 select 多出了 row 就算幻读。
    a 事务先 select，b 事务 insert 确实会加一个 gap 锁，但是如果 b 事务 commit，这个 gap 锁就会释放（释放后 a 事务可以随意 dml 操作），a 事务再 select 出来的结果在 MVCC 下还和第一次 select 一样，接着 a 事务不加条件地 update，这个 update 会作用在所有行上（包括 b 事务新加的），a 事务再次 select 就会出现 b 事务中的新行，并且这个新行已经被 update 修改了，实测在 RR 级别下确实如此。
    如果这样理解的话，Mysql 的 RR 级别确实防不住幻读

    在快照读读情况下，mysql 通过 mvcc 来避免幻读。
    在当前读读情况下，mysql 通过 next-key 来避免幻读。
    select * from t where a=1; 属于快照读
    select * from t where a=1 lock in share mode; 属于当前读

    不能把快照读和当前读得到的结果不一样这种情况认为是幻读，这是两种不同的使用。所以我认为 mysql 的 rr 级别是解决了幻读的。  
    如引用一问题所说，T1 select 之后 update，会将 T2 中 insert 的数据一起更新，那么认为多出来一行，所以防不住幻读。看着说法无懈可击，但是其实是错误的，InnoDB 中设置了 快照读 和 当前读 两种模式，如果只有快照读，那么自然没有幻读问题，但是如果将语句提升到当前读，那么 T1 在 select 的时候需要用如下语法： select * from t for update (lock in share mode) 进入当前读，那么自然没有 T2 可以插入数据这一回事儿了。

    ```

### 参考  
https://liuzhengyang.github.io/2017/04/18/innodb-mvcc/  
https://my.oschina.net/alchemystar/blog/1927425  
https://my.oschina.net/xinxingegeya/blog/505675  