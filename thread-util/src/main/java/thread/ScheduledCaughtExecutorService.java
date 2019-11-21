package thread;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;

/**
 * @author Diego Armange Costa
 * @since 2019-11-18 V1.0.0 (JDK 1.8)
 * @see java.util.concurrent.ScheduledThreadPoolExecutor
 */
public class ScheduledCaughtExecutorService extends ScheduledThreadPoolExecutor {
    private List<BiConsumer<Runnable, Throwable>> afterExecuteConsumers = new LinkedList<>();
    
    /**
     * @see java.util.concurrent.ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int)
     */
    public ScheduledCaughtExecutorService(final int corePoolSize) {
        super(corePoolSize);
    }
    
    /**
     * @see java.util.concurrent.ScheduledThreadPoolExecutor#ScheduledThreadPoolExecutor(int, ThreadFactory)
     */
    public ScheduledCaughtExecutorService(final int corePoolSize, final ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    /**
     * Method invoked upon completion of execution of the given Runnable.
     * This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException}
     * or {@code Error} that caused execution to terminate abruptly.
     *
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     *
     * <p><b>Note:</b> When actions are enclosed in tasks (such as
     * {@link FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain
     * computational exceptions, and so they do not cause abrupt
     * termination, and the internal exceptions are <em>not</em>
     * passed to this method. If you would like to trap both kinds of
     * failures in this method, you can further probe for such cases,
     * as in this sample subclass that prints either the direct cause
     * or the underlying exception if a task has been aborted:
     *
     *  <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null && r instanceof Future<?>) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // ignore/reset
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * @param r the runnable that has completed
     * @param t the exception that caused termination, or null if
     * execution completed normally
     */
    @Override
    public void afterExecute(final Runnable runnable, final Throwable throwable) {
        super.afterExecute(runnable, throwable);
        afterExecuteConsumers.forEach(consumer -> consumer.accept(runnable, throwable));
    }

    public List<BiConsumer<Runnable, Throwable>> getAfterExecuteConsumers() {
        return afterExecuteConsumers;
    }

    public void addAfterExecuteConsumer(final BiConsumer<Runnable, Throwable> afterExecuteBiConsumer) {
        this.afterExecuteConsumers.add(afterExecuteBiConsumer);
    }
}
