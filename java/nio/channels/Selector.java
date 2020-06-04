/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.spi.SelectorProvider;
import java.util.Set;


/**
 * A multiplexor of {@link SelectableChannel} objects.
 *
 * <p> A selector may be created by invoking the {@link #open open} method of
 * this class, which will use the system's default {@link
 * java.nio.channels.spi.SelectorProvider selector provider} to
 * create a new selector.  A selector may also be created by invoking the
 * {@link java.nio.channels.spi.SelectorProvider#openSelector openSelector}
 * method of a custom selector provider.  A selector remains open until it is
 * closed via its {@link #close close} method.
 *
 * <a name="ks"></a>
 *
 * <p> A selectable channel's registration with a selector is represented by a
 * {@link SelectionKey} object.  A selector maintains three sets of selection
 * keys:
 *
 * <ul>
 *
 *   <li><p> The <i>key set</i> contains the keys representing the current
 *   channel registrations of this selector.  This set is returned by the
 *   {@link #keys() keys} method. </p></li>
 *
 *   <li><p> The <i>selected-key set</i> is the set of keys such that each
 *   key's channel was detected to be ready for at least one of the operations
 *   identified in the key's interest set during a prior selection operation.
 *   This set is returned by the {@link #selectedKeys() selectedKeys} method.
 *   The selected-key set is always a subset of the key set. </p></li>
 *
 *   <li><p> The <i>cancelled-key</i> set is the set of keys that have been
 *   cancelled but whose channels have not yet been deregistered.  This set is
 *   not directly accessible.  The cancelled-key set is always a subset of the
 *   key set. </p></li>
 *
 * </ul>
 *
 * <p> All three sets are empty in a newly-created selector.
 *
 * <p> A key is added to a selector's key set as a side effect of registering a
 * channel via the channel's {@link SelectableChannel#register(Selector,int)
 * register} method.  Cancelled keys are removed from the key set during
 * selection operations.  The key set itself is not directly modifiable.
 *
 * <p> A key is added to its selector's cancelled-key set when it is cancelled,
 * whether by closing its channel or by invoking its {@link SelectionKey#cancel
 * cancel} method.  Cancelling a key will cause its channel to be deregistered
 * during the next selection operation, at which time the key will removed from
 * all of the selector's key sets.
 *
 * <a name="sks"></a><p> Keys are added to the selected-key set by selection
 * operations.  A key may be removed directly from the selected-key set by
 * invoking the set's {@link java.util.Set#remove(java.lang.Object) remove}
 * method or by invoking the {@link java.util.Iterator#remove() remove} method
 * of an {@link java.util.Iterator iterator} obtained from the
 * set.  Keys are never removed from the selected-key set in any other way;
 * they are not, in particular, removed as a side effect of selection
 * operations.  Keys may not be added directly to the selected-key set. </p>
 *
 *
 * <a name="selop"></a>
 * <h2>Selection</h2>
 *
 * <p> During each selection operation, keys may be added to and removed from a
 * selector's selected-key set and may be removed from its key and
 * cancelled-key sets.  Selection is performed by the {@link #select()}, {@link
 * #select(long)}, and {@link #selectNow()} methods, and involves three steps:
 * </p>
 *
 * <ol>
 *
 *   <li><p> Each key in the cancelled-key set is removed from each key set of
 *   which it is a member, and its channel is deregistered.  This step leaves
 *   the cancelled-key set empty. </p></li>
 *
 *   <li><p> The underlying operating system is queried for an update as to the
 *   readiness of each remaining channel to perform any of the operations
 *   identified by its key's interest set as of the moment that the selection
 *   operation began.  For a channel that is ready for at least one such
 *   operation, one of the following two actions is performed: </p>
 *
 *   <ol>
 *
 *     <li><p> If the channel's key is not already in the selected-key set then
 *     it is added to that set and its ready-operation set is modified to
 *     identify exactly those operations for which the channel is now reported
 *     to be ready.  Any readiness information previously recorded in the ready
 *     set is discarded.  </p></li>
 *
 *     <li><p> Otherwise the channel's key is already in the selected-key set,
 *     so its ready-operation set is modified to identify any new operations
 *     for which the channel is reported to be ready.  Any readiness
 *     information previously recorded in the ready set is preserved; in other
 *     words, the ready set returned by the underlying system is
 *     bitwise-disjoined into the key's current ready set. </p></li>
 *
 *   </ol>
 *
 *   If all of the keys in the key set at the start of this step have empty
 *   interest sets then neither the selected-key set nor any of the keys'
 *   ready-operation sets will be updated.
 *
 *   <li><p> If any keys were added to the cancelled-key set while step (2) was
 *   in progress then they are processed as in step (1). </p></li>
 *
 * </ol>
 *
 * <p> Whether or not a selection operation blocks to wait for one or more
 * channels to become ready, and if so for how long, is the only essential
 * difference between the three selection methods. </p>
 *
 *
 * <h2>Concurrency</h2>
 *
 * <p> Selectors are themselves safe for use by multiple concurrent threads;
 * their key sets, however, are not.
 *
 * <p> The selection operations synchronize on the selector itself, on the key
 * set, and on the selected-key set, in that order.  They also synchronize on
 * the cancelled-key set during steps (1) and (3) above.
 *
 * <p> Changes made to the interest sets of a selector's keys while a
 * selection operation is in progress have no effect upon that operation; they
 * will be seen by the next selection operation.
 *
 * <p> Keys may be cancelled and channels may be closed at any time.  Hence the
 * presence of a key in one or more of a selector's key sets does not imply
 * that the key is valid or that its channel is open.  Application code should
 * be careful to synchronize and check these conditions as necessary if there
 * is any possibility that another thread will cancel a key or close a channel.
 *
 * <p> A thread blocked in one of the {@link #select()} or {@link
 * #select(long)} methods may be interrupted by some other thread in one of
 * three ways:
 *
 * <ul>
 *
 *   <li><p> By invoking the selector's {@link #wakeup wakeup} method,
 *   </p></li>
 *
 *   <li><p> By invoking the selector's {@link #close close} method, or
 *   </p></li>
 *
 *   <li><p> By invoking the blocked thread's {@link
 *   java.lang.Thread#interrupt() interrupt} method, in which case its
 *   interrupt status will be set and the selector's {@link #wakeup wakeup}
 *   method will be invoked. </p></li>
 *
 * </ul>
 *
 * <p> The {@link #close close} method synchronizes on the selector and all
 * three key sets in the same order as in a selection operation.
 *
 * <a name="ksc"></a>
 *
 * <p> A selector's key and selected-key sets are not, in general, safe for use
 * by multiple concurrent threads.  If such a thread might modify one of these
 * sets directly then access should be controlled by synchronizing on the set
 * itself.  The iterators returned by these sets' {@link
 * java.util.Set#iterator() iterator} methods are <i>fail-fast:</i> If the set
 * is modified after the iterator is created, in any way except by invoking the
 * iterator's own {@link java.util.Iterator#remove() remove} method, then a
 * {@link java.util.ConcurrentModificationException} will be thrown. </p>
 *
 *
 * @author Mark Reinhold
 * @author JSR-51 Expert Group
 * @since 1.4
 *
 * @see SelectableChannel
 * @see SelectionKey
 */
/*
    它是一个双工多路传输的 SelectableChannel 对象
    Selector是可以调用这个类的open()方法来创建，而这个open()本身又会使用系统默认的
    选择器提供者(java.nio.channels.spi.SelectorProvider)来去创建一个新的选择器(selector)
    选择器还可以通过调用java.nio.channels.spi.SelectorProvider的openSelector()方法并且提供了一个
    自定义的选择器提供者来去创建，一个选择器(Selector)会持续的保持打开状态，直到调用它的close()方法它才会真正的关闭。

    一个可选择的通道(selectable channel's)向选择器(Selector)的注册结果由一个SelectionKey对象表示
    一个selector会维护三种SelectionKey的集合：
        key set(键集(全集)) 包含代表此选择器当前通道注册的键。该集合由keys()方法返回。
        selected-key set(选定的键集) 是这样的一组key检测到key的通道已为至少一个操作做好准备，在先前的选择操作中在密钥的兴趣集中标识。
            此集合由link#selectedKeys()方法返回。它是key set的子集(例如key set有四种连接事件，已连接、连接断开、可读、可写，比如我们只对读和写感兴趣，那么selected-key set中就包含可读、可写的事件并不包含已连接、连接断开)
        cancelled-key(取消的键集)    是一组已取消(之前关注但是现在取消了)，但其通道可能尚未注销，
            不如你不再关注这个动作了这个Channel本身是下一次再去Selector时进行关闭 它是key set的子集

    所有这三个集合在新创建的selector中都是空的

    我们可以通过Channel的register()方法将一个selector注册到Channel上面，同时也会将一个键(key)添加到选择器的键(key)集中，
    selection选择操作期间，已取消的key将从key set中删除。key set 本身不能直接修改。

    当它被取消的时候，一个key会被添加到cancelled-key这个集合当中，取消方式有 关闭了这个Channel或者是调用SelectionKey的cancel()方法
    取消一个key会导致这个key所关联的Channel在下一次选择操作(selection operations指的就是select()方法)当中被取消注册，
    在这个时候这个key将会从cancelled-key集合中被移除掉

    keys这个键会被添加到selected-key set这个集合 通过选择操作(selection operations)，一个key可能直接从selected-key set中被移除了
    通过调用set(selected-key set,选定的键集)的remove()方法或者是通过这个集合的Iterator(迭代器)的remove()方法，这个Iterator是通过这个集合所获取的
    除了上面两种方法keys是绝对不会以其它方式被移除的，特别的它们并不会作为选择操作(selection operations)的副作用，
    keys是不可能直接被添加到selected-key set当中的


Selection
    在每一个选择操作过程中，可以将key添加到selector选择器的selected-key set集合中或从中删除，也可以从其key和cancelled-key(取消键集)中删除，
    选择操作是通过select()、select(long)、selectNow()方法来去执行的，select()方法本身是一个阻塞的方法。它涉及到了一下三个步骤：
        1.在cancelled-key(取消键集)当中每一个键都会从这个集合当中被移除掉，然后他的Channel会被取消注册，这一步会使得cancelled-key(取消键集)
        变为空集合。
        2.程序会询问底层操作系统是否进行更新，以便每个剩余Channel准备执行任何操作，这些操作由选择操作开始时由其key的selected-key set(兴趣集)确定。
        对于一个Channel准备至少一个此类操作，将执行以下两个操作之一：
            1.如果一个Channel的key并没有在selected-key set当中，那么他会被添加到这个集合当中，那么它这个准备操作的集合会被修改以精确标示这个动作
            然后之任何先前记录的就绪信息会被丢弃掉
            2.

 */
public abstract class Selector implements Closeable {

    /**
     * Initializes a new instance of this class.
     */
    protected Selector() { }

    /**
     * Opens a selector.
     *
     * <p> The new selector is created by invoking the {@link
     * java.nio.channels.spi.SelectorProvider#openSelector openSelector} method
     * of the system-wide default {@link
     * java.nio.channels.spi.SelectorProvider} object.  </p>
     *
     * @return  A new selector
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public static Selector open() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    /**
     * Tells whether or not this selector is open.
     *
     * @return <tt>true</tt> if, and only if, this selector is open
     */
    public abstract boolean isOpen();

    /**
     * Returns the provider that created this channel.
     *
     * @return  The provider that created this channel
     */
    public abstract SelectorProvider provider();

    /**
     * Returns this selector's key set.
     *
     * <p> The key set is not directly modifiable.  A key is removed only after
     * it has been cancelled and its channel has been deregistered.  Any
     * attempt to modify the key set will cause an {@link
     * UnsupportedOperationException} to be thrown.
     *
     * <p> The key set is <a href="#ksc">not thread-safe</a>. </p>
     *
     * @return  This selector's key set
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract Set<SelectionKey> keys();

    /**
     * Returns this selector's selected-key set.
     *
     * <p> Keys may be removed from, but not directly added to, the
     * selected-key set.  Any attempt to add an object to the key set will
     * cause an {@link UnsupportedOperationException} to be thrown.
     *
     * <p> The selected-key set is <a href="#ksc">not thread-safe</a>. </p>
     *
     * @return  This selector's selected-key set
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract Set<SelectionKey> selectedKeys();

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O
     * operations.
     *
     * <p> This method performs a non-blocking <a href="#selop">selection
     * operation</a>.  If no channels have become selectable since the previous
     * selection operation then this method immediately returns zero.
     *
     * <p> Invoking this method clears the effect of any previous invocations
     * of the {@link #wakeup wakeup} method.  </p>
     *
     * @return  The number of keys, possibly zero, whose ready-operation sets
     *          were updated by the selection operation
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    public abstract int selectNow() throws IOException;

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O
     * operations.
     *
     * <p> This method performs a blocking <a href="#selop">selection
     * operation</a>.  It returns only after at least one channel is selected,
     * this selector's {@link #wakeup wakeup} method is invoked, the current
     * thread is interrupted, or the given timeout period expires, whichever
     * comes first.
     *
     * <p> This method does not offer real-time guarantees: It schedules the
     * timeout as if by invoking the {@link Object#wait(long)} method. </p>
     *
     * @param  timeout  If positive, block for up to <tt>timeout</tt>
     *                  milliseconds, more or less, while waiting for a
     *                  channel to become ready; if zero, block indefinitely;
     *                  must not be negative
     *
     * @return  The number of keys, possibly zero,
     *          whose ready-operation sets were updated
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     *
     * @throws  IllegalArgumentException
     *          If the value of the timeout argument is negative
     */
    public abstract int select(long timeout)
        throws IOException;

    /**
     * Selects a set of keys whose corresponding channels are ready for I/O
     * operations.
     *
     * <p> This method performs a blocking <a href="#selop">selection
     * operation</a>.  It returns only after at least one channel is selected,
     * this selector's {@link #wakeup wakeup} method is invoked, or the current
     * thread is interrupted, whichever comes first.  </p>
     *
     * @return  The number of keys, possibly zero,
     *          whose ready-operation sets were updated
     *
     * @throws  IOException
     *          If an I/O error occurs
     *
     * @throws  ClosedSelectorException
     *          If this selector is closed
     */
    /*
        返回的是一个key的数量，可能是0s
     */
    public abstract int select() throws IOException;

    /**
     * Causes the first selection operation that has not yet returned to return
     * immediately.
     *
     * <p> If another thread is currently blocked in an invocation of the
     * {@link #select()} or {@link #select(long)} methods then that invocation
     * will return immediately.  If no selection operation is currently in
     * progress then the next invocation of one of these methods will return
     * immediately unless the {@link #selectNow()} method is invoked in the
     * meantime.  In any case the value returned by that invocation may be
     * non-zero.  Subsequent invocations of the {@link #select()} or {@link
     * #select(long)} methods will block as usual unless this method is invoked
     * again in the meantime.
     *
     * <p> Invoking this method more than once between two successive selection
     * operations has the same effect as invoking it just once.  </p>
     *
     * @return  This selector
     */
    public abstract Selector wakeup();

    /**
     * Closes this selector.
     *
     * <p> If a thread is currently blocked in one of this selector's selection
     * methods then it is interrupted as if by invoking the selector's {@link
     * #wakeup wakeup} method.
     *
     * <p> Any uncancelled keys still associated with this selector are
     * invalidated, their channels are deregistered, and any other resources
     * associated with this selector are released.
     *
     * <p> If this selector is already closed then invoking this method has no
     * effect.
     *
     * <p> After a selector is closed, any further attempt to use it, except by
     * invoking this method or the {@link #wakeup wakeup} method, will cause a
     * {@link ClosedSelectorException} to be thrown. </p>
     *
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract void close() throws IOException;

}
