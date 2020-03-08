# forkjoin 分而治之

* ForkJoinPool在jdk1.7引入，在jdk1.8进行了优化，这个框架是通过（递归）把问题划分成子任务，然后并行执行这些子任务，等所有这些子任务都执行完成时，在将这些子任务合并成最终结果。
* ForkJoinPool，与ThreadPoolExecutor一样，是ExecutorService的实现。
* ThreadPoolExecutor 中每个任务都是由单个线程独立处理的，如果出现一个非常耗时的大任务（比如大数组排序），就可能出现线程池中只有一个线程在处理这个大任务，而其他线程却空闲着，这会导致 CPU 负载不均衡，空闲的处理器无法帮助工作繁忙的处理器。
* ForkJoinPool 可以用来解决这种问题，将一个大任务拆分成多个小任务后，使用 Fork 可以将小任务分发给其他线程同时处理，使用 Join 可以将多个线程处理的结果进行汇总。
* ForkJoinPool 的两大核心是，分而治之和工作窃取
* 通常情况下不需要集成ForkJoinTask,ForkJoinTask衍生了2个子类，RecursiveAction用于没有返回结果的任务。RecursiveTask用于有返回结果的任务。
* forkJoin 思路：
  ```
    if(任务很小）{
        直接计算得到结果
    }else{
        分拆成N个子任务
        调用子任务的fork()进行计算
        调用子任务的join()合并计算结果
    }
  ```

* ExecutorService 实现
  ```
    public class ExecutorServiceCalculator implements Calculator {
        private int parallism;
        private ExecutorService pool;

        public ExecutorServiceCalculator() {
            parallism = Runtime.getRuntime().availableProcessors(); // CPU的核心数
            pool = Executors.newFixedThreadPool(parallism);
        }

        private static class SumTask implements Callable<Long> {
            private long[] numbers;
            private int from;
            private int to;

            public SumTask(long[] numbers, int from, int to) {
                this.numbers = numbers;
                this.from = from;
                this.to = to;
            }

            @Override
            public Long call() throws Exception {
                long total = 0;
                for (int i = from; i <= to; i++) {
                    total += numbers[i];
                }
                return total;
            }
        }

        @Override
        public long sumUp(long[] numbers) {
            List<Future<Long>> results = new ArrayList<>();

            // 把任务分解为 n 份，交给 n 个线程处理
            int part = numbers.length / parallism;
            for (int i = 0; i < parallism; i++) {
                int from = i * part;
                int to = (i == parallism - 1) ? numbers.length - 1 : (i + 1) * part - 1;
                results.add(pool.submit(new SumTask(numbers, from, to)));
            }

            // 把每个线程的结果相加，得到最终结果
            long total = 0L;
            for (Future<Long> f : results) {
                try {
                    total += f.get();
                } catch (Exception ignore) {}
            }
            return total;
        }
    }
  ```
* ForkJoinPool实现
  ```
    public class ForkJoinCalculator implements Calculator {
        private ForkJoinPool pool;

        private static class SumTask extends RecursiveTask<Long> {
            private long[] numbers;
            private int from;
            private int to;

            public SumTask(long[] numbers, int from, int to) {
                this.numbers = numbers;
                this.from = from;
                this.to = to;
            }

            @Override
            protected Long compute() {
                // 当需要计算的数字小于6时，直接计算结果
                if (to - from < 6) {
                    long total = 0;
                    for (int i = from; i <= to; i++) {
                        total += numbers[i];
                    }
                    return total;
                // 否则，把任务一分为二，递归计算
                } else {
                    int middle = (from + to) / 2;
                    SumTask taskLeft = new SumTask(numbers, from, middle);
                    SumTask taskRight = new SumTask(numbers, middle+1, to);
                    taskLeft.fork();
                    taskRight.fork();
                    return taskLeft.join() + taskRight.join();
                }
            }
        }

        public ForkJoinCalculator() {
            // 也可以使用公用的 ForkJoinPool：
            // pool = ForkJoinPool.commonPool()
            pool = new ForkJoinPool();
        }

        @Override
        public long sumUp(long[] numbers) {
            return pool.invoke(new SumTask(numbers, 0, numbers.length-1));
        }
    }
  ```
