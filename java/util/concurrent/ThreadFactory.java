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
 * An object that creates new threads on demand.  Using thread factories
 * removes hardwiring of calls to {@link Thread#Thread(Runnable) new Thread},
 * enabling applications to use special thread subclasses, priorities, etc.
 *
 * <p>
 * The simplest implementation of this interface is just:
 * <pre> {@code
 * class SimpleThreadFactory implements ThreadFactory {
 *   public Thread newThread(Runnable r) {
 *     return new Thread(r);
 *   }
 * }}</pre>
 * <p>
 * The {@link Executors#defaultThreadFactory} method provides a more
 * useful simple implementation, that sets the created thread context
 * to known values before returning it.
 *
 * @author Doug Lea
 * @since 1.5
 */
/*
    这是一个对象会根据需要来去创建新的Thread(线程)，使用线程工厂就不必每次都去调用new Thread()构造方法来去创建了
    这使得应用可以去使用特殊的线程子类，优先级等等。

    对于这个接口最简单的实现方式：
    class SimpleThreadFactory implements ThreadFactory {
        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }

    可以被认为是函数式接口(只包含一个抽象方法的接口)

    Executors的defaultThreadFactory()方法会提供一个更加有用简单的实现，它会设置所创建线程的上下文
    在将所创建的线程返回之前，会将所创建线程的上下文设定成一个已知的一些值
 */
public interface ThreadFactory {

    /**
     * Constructs a new {@code Thread}.  Implementations may also initialize
     * priority, name, daemon status, {@code ThreadGroup}, etc.
     *
     * @param r a runnable to be executed by new thread instance
     * @return constructed thread, or {@code null} if the request to
     * create a thread is rejected
     */
    /*
        构建一个新的线程，这个接口的实现还可以初始化线程的优先级，名字，是否是后台线程，线程组等等。

        param：由新线程实例执行的可运行对象
        return：所创建的线程
     */
    Thread newThread(Runnable r);
}

