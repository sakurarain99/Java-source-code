/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * An object that executes submitted {@link Runnable} tasks. This
 * interface provides a way of decoupling task submission from the
 * mechanics of how each task will be run, including details of thread
 * use, scheduling, etc.  An {@code Executor} is normally used
 * instead of explicitly creating threads. For example, rather than
 * invoking {@code new Thread(new(RunnableTask())).start()} for each
 * of a set of tasks, you might use:
 *
 * <pre>
 * Executor executor = <em>anExecutor</em>;
 * executor.execute(new RunnableTask1());
 * executor.execute(new RunnableTask2());
 * ...
 * </pre>
 * <p>
 * However, the {@code Executor} interface does not strictly
 * require that execution be asynchronous. In the simplest case, an
 * executor can run the submitted task immediately in the caller's
 * thread:
 *
 * <pre> {@code
 * class DirectExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     r.run();
 *   }
 * }}</pre>
 * <p>
 * More typically, tasks are executed in some thread other
 * than the caller's thread.  The executor below spawns a new thread
 * for each task.
 *
 * <pre> {@code
 * class ThreadPerTaskExecutor implements Executor {
 *   public void execute(Runnable r) {
 *     new Thread(r).start();
 *   }
 * }}</pre>
 * <p>
 * Many {@code Executor} implementations impose some sort of
 * limitation on how and when tasks are scheduled.  The executor below
 * serializes the submission of tasks to a second executor,
 * illustrating a composite executor.
 *
 * <pre> {@code
 * class SerialExecutor implements Executor {
 *   final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
 *   final Executor executor;
 *   Runnable active;
 *
 *   SerialExecutor(Executor executor) {
 *     this.executor = executor;
 *   }
 *
 *   public synchronized void execute(final Runnable r) {
 *     tasks.offer(new Runnable() {
 *       public void run() {
 *         try {
 *           r.run();
 *         } finally {
 *           scheduleNext();
 *         }
 *       }
 *     });
 *     if (active == null) {
 *       scheduleNext();
 *     }
 *   }
 *
 *   protected synchronized void scheduleNext() {
 *     if ((active = tasks.poll()) != null) {
 *       executor.execute(active);
 *     }
 *   }
 * }}</pre>
 * <p>
 * The {@code Executor} implementations provided in this package
 * implement {@link ExecutorService}, which is a more extensive
 * interface.  The {@link ThreadPoolExecutor} class provides an
 * extensible thread pool implementation. The {@link Executors} class
 * provides convenient factory methods for these Executors.
 *
 * <p>Memory consistency effects: Actions in a thread prior to
 * submitting a {@code Runnable} object to an {@code Executor}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * its execution begins, perhaps in another thread.
 *
 * @author Doug Lea
 * @since 1.5
 */
/*
    它本身是一个对象，对象本身会执行提交过来的Runnable的任务，这个接口提供了一种方式，将任务的提交和任务的运行进行了解耦，
    这包括线程使用细节以及线程调度的信息，我们通常会使用Executor，而没必要显示的去new Thread()去创建线程了，
    对于每一个任务集合里面的每个任务来说我们不会new Thread(new(RunnableTask())).start()做，
    而是：
         Executor executor = <em>anExecutor</em>;   //创建一个Executor执行器
         executor.execute(new RunnableTask1());     //然后将待执行的任务作为Executor的execute()方法的参数传进来
         executor.execute(new RunnableTask2());


    然而Executor这个接口并不会严格的要求执行一定要是异步的，在最简单的一种情况一个执行器可以在调用者的线程当中立刻去运行所提交的任务
        (一般来说我们定义好一个任务，然后将任务交给执行器，执行器本身是在一个线程中执行的，将任务本身交给执行器是在用户线程中执行的，这两个是独立的线程
        ，但是文档说有可能在用户线程里执行执行器里所提交的任务。实现方法如下：)
        class DirectExecutor implements Executor {
            //这个execute()方法是在caller(调用者)递交任务的线程当中执行的
            public void execute(Runnable r) {
                r.run();    //直接执行了并没有起新的线程，起新的线程需要调用start()
            }
        }
        这样写线程只有一个，并不会新起一个线程去执行run方法

    更为典型的，任务是在除了调用者线程的其它线程当中去执行的。下面这个Executor(执行器)会针对每一个任务创建一个新的线程
        class ThreadPerTaskExecutor implements Executor {
            public void execute(Runnable r) {
                new Thread(r).start();  //这样调用，调用者的线程与执行任务的线程就是两个不同的独立的线程了，谁先执行谁后执行就不一定了
            }
         }

    很多的Executor的实现会对任务如何以及何时被调度，施加了某些限制。下面这些执行器会将任务提交串行化给第二个执行器(Executor)，
    阐述了组合Executor(两个执行器，我先接到了任务执行完之后我再交给第二个执行器去执行)。
       class SerialExecutor implements Executor {
           final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
           final Executor executor;
           Runnable active;

           SerialExecutor(Executor executor) {
             this.executor = executor;
           }

           public synchronized void execute(final Runnable r) {
             tasks.offer(new Runnable() {
               public void run() {
                 try {
                   r.run();
                 } finally {
                   scheduleNext();
                 }
               }
             });
             if (active == null) {
               scheduleNext();
             }
           }

           protected synchronized void scheduleNext() {
             if ((active = tasks.poll()) != null) {
               executor.execute(active);
             }
           }
      }
    当前这个包里的Executor实现会实现ExecutorService这是一个更广泛/扩展的接口。ThreadPoolExecutor类提供了可扩展的线程池实现。
    Executors(辅助类/工厂类)类为这些Executor提供了方便的工厂方法。

    内存一致性的影响：在将Runnable对象提交给一个Executor之前的线程当中的动作，它是happen-before(发生-之前，
    比如：A happen-before B，A这件事情一定是在B之前发生的)。在将Runnable对象提交给Executor去执行的时候，
    提交之前线程当中的动作一定是在它的执行之前就发生了，也许是在另一个线程发生的。

 */
public interface Executor {

    /**
     * Executes the given command at some time in the future.  The command
     * may execute in a new thread, in a pooled thread, or in the calling
     * thread, at the discretion of the {@code Executor} implementation.
     *
     * @param command the runnable task
     * @throws RejectedExecutionException if this task cannot be
     *                                    accepted for execution
     * @throws NullPointerException       if command is null
     */
    /*
        在未来的某个时间去执行给定的命令，这个命令可能在新的线程中执行这是大多数情况，也可能在一个线程池里执行
        或者在调用者线程里执行这是很少见的情况，这是由Executor实现决定的。
     */
    void execute(Runnable command);
}
