package thread;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Minimum structure for thread creation in the following scenarios:
 * <ul>
 * <li><em>Timeout</em></li>
 * <p>
 * The thread will be active only until the timeout be fired.
 * </p>
 * <li><em>Delay</em></li>
 * <p>
 * The thread will be active only after the time delay be completed.
 * </p>
 * <li><em>Interval</em></li>
 * <p>
 * The thread will be repeated after the time interval be completed.
 * </p>
 * <li><em>Exception handling</em></li>
 * <p>
 * Handles of uncaught exceptions can be thrown and handled within threads.
 * </p>
 * </ul>
 * <b>Note:</b><br>
 * <em>The thread will wait a minimum delay
 * ({@link thread.ThreadBuilder.MINIMAL_REQUIRED_DELAY}) if and only if a
 * ({@link thread.ThreadBuilder#setAfterExecuteConsumer(BiConsumer)}) 
 * or a ({@link thread.ThreadBuilder#setUncaughtExceptionConsumer(Consumer)}) 
 * is present.</em>
 * 
 * <pre>
 * <b>Example:</b>
 * 
 * final ExecutorService thread = ThreadBuilder
 *          .newBuilder() //New object to build a new thread.
 *          .setDelay(1000) //The thread will wait one second before start.
 *          .setTimeout(4000) //The thread will be canceled after four seconds.
 *          .setInterval(1000) //The thread will be repeated every second. 
 *          .setAfterExecuteConsumer(afterExecuteConsumer) //A consumer will be called after thread execution.
 *          .setUncaughtExceptionConsumer(throwableConsumer) //A consumer will be called after any exception thrown.
 *          .setMayInterruptIfRunning(true) //The thread interruption/cancellation will not wait execution.
 *          .setSilentInterruption(true) //Interruption and Cancellation exceptions will not be thrown.
 *          .setExecution(anyRunnable) //The thread execution.
 *          .start();
 * </pre>
 * 
 * @author Diego Armange Costa
 * @since 2019-11-18 V1.0.0 (JDK 1.8)
 * @see thread.ScheduledCaughtExecutorService
 */
public class ThreadBuilder {
    /**
     * 1000 milliseconds as a minimal delay.
     */
    public static final long MINIMAL_REQUIRED_DELAY = 1000;
    private Optional<Duration> timeout = Optional.empty();
    private Optional<Duration> delay = Optional.empty();
    private Optional<Duration> interval = Optional.empty();
    private Optional<BiConsumer<Runnable, Throwable>> afterExecuteConsumer = Optional.empty();
    private Optional<Consumer<Throwable>> uncaughtExceptionConsumer = Optional.empty();
    private Runnable execution;
    private ScheduledCaughtExecutorService executor;
    private boolean mayInterruptIfRunning;
    private boolean silentInterruption;

    private ThreadBuilder() {}
    
    private ThreadBuilder(final ScheduledCaughtExecutorService executor) {
        this.executor = executor;
    }

    /**
     * @return a new object to perform a thread creation.
     */
    public static ThreadBuilder newBuilder() {
        return new ThreadBuilder();
    }
    
    /**
     * @param executor the {@link ScheduledCaughtExecutorService} from which the threads will be created.
     * @return a new object to perform a thread creation.
     */
    public static ThreadBuilder newBuilder(final ScheduledCaughtExecutorService executor) {
        return new ThreadBuilder(executor);
    }

    /**
     * Sets the timeout value.
     * @param milliseconds the timeout value in milliseconds. 
     * @return the current thread builder.
     */
    public ThreadBuilder setTimeout(final long milliseconds) {
        timeout = Optional.of(Duration.ofMillis(milliseconds));

        return this;
    }

    /**
     * Sets the delay value.
     * @param milliseconds the delay value in milliseconds.
     * @return the current thread builder.
     */
    public ThreadBuilder setDelay(final long milliseconds) {
        delay = Optional.of(Duration.ofMillis(milliseconds));

        return this;
    }

    /**
     * Sets the repeating interval value.
     * @param milliseconds the repeating interval value in milliseconds.
     * @return the current thread builder.
     */
    public ThreadBuilder setInterval(final long milliseconds) {
        interval = Optional.of(Duration.ofMillis(milliseconds));

        return this;
    }

    /**
     * Sets the consumer to be called after thread execution.
     * @param afterExecuteConsumer the consumer to be called after thread execution.
     * @return the current thread builder.
     * @see thread.ScheduledCaughtExecutorService#afterExecute(Runnable, Throwable)
     */
    public ThreadBuilder setAfterExecuteConsumer(final BiConsumer<Runnable, Throwable> afterExecuteConsumer) {
        this.afterExecuteConsumer = Optional.ofNullable(afterExecuteConsumer);

        return this;
    }

    /**
     * Sets the consumer to be called after exception throwing. This consumer will be called as a first after-executes 
     * consumer.
     * @param uncaughtExceptionConsumer the consumer to be called after exception throwing.
     * @return the current thread builder.
     */
    public ThreadBuilder setUncaughtExceptionConsumer(final Consumer<Throwable> uncaughtExceptionConsumer) {
        this.uncaughtExceptionConsumer = Optional.ofNullable(uncaughtExceptionConsumer);

        return this;
    }

    /**
     * Sets the thread execution.
     * @param execution the thread execution({@link java.lang.Runnable})
     * @return the current thread builder.
     */
    public ThreadBuilder setExecution(final Runnable execution) {
        this.execution = execution;

        requireExecutionNonNull();

        return this;
    }

    /**
     * Sets the thread-interrupting-flag.
     * @param flag true if the thread executing this task should be interrupted; 
     * otherwise, in-progress tasks are allowed to complete.
     * @return the current thread builder.
     * @see java.util.concurrent.Future#cancel(boolean)
     */
    public ThreadBuilder setMayInterruptIfRunning(final boolean flag) {
        mayInterruptIfRunning = flag;

        return this;
    }

    /**
     * Sets the thread-silent-interrupting-flag.
     * @param flag true if the Interruption/Cancellation exceptions should be ignored.
     * @return the current thread builder.
     * @see java.util.concurrent.Future#cancel(boolean)
     * @see java.util.concurrent.CancellationException
     * @see java.lang.InterruptedException
     */
    public ThreadBuilder setSilentInterruption(final boolean flag) {
        silentInterruption = flag;

        return this;
    }

    /**
     * Starts the thread.
     * @return the executor service after starting thread.
     */
    public ExecutorService start() {
        requireExecutionNonNull();

        executor = new ScheduledCaughtExecutorService(2);

        if (noSchedule()) {
            runWithNoSchedule();
        } else if (onlyDelay()) {
            runWithDelay();
        } else if (onlyTimeout()) {
            runWithTimeout();
        } else if (onlyInterval()) {
            repeatWithInterval();
        } else if (delayAndTimeout()) {
            runWithDelayAndTimeout();
        } else if (delayAndInterval()) {
            runWithDelayAndInterval();
        } else if (timeoutAndInterval()) {
            runWithTimeoutAndInterval();
        } else /* All */ {
            runWithAllTimesControls();
        }

        afterExecuteConsumer.ifPresent(executor::addAfterExecuteConsumer);

        return executor;
    }

    private void requireExecutionNonNull() {
        Objects.requireNonNull(execution, "The {execution} parameter is required");
    }

    private boolean noSchedule() {
        return !delay.isPresent() && !timeout.isPresent() && !interval.isPresent();
    }

    private boolean onlyDelay() {
        return delay.isPresent() && !timeout.isPresent() && !interval.isPresent();
    }

    private boolean onlyTimeout() {
        return !delay.isPresent() && timeout.isPresent() && !interval.isPresent();
    }

    private boolean onlyInterval() {
        return !delay.isPresent() && !timeout.isPresent() && interval.isPresent();
    }

    private boolean delayAndTimeout() {
        return delay.isPresent() && timeout.isPresent() && !interval.isPresent();
    }

    private boolean delayAndInterval() {
        return delay.isPresent() && !timeout.isPresent() && interval.isPresent();
    }

    private boolean timeoutAndInterval() {
        return !delay.isPresent() && timeout.isPresent() && interval.isPresent();
    }

    private void runWithNoSchedule() {
        final Future<?> future = executor.schedule(execution, handleDelay(), TimeUnit.MILLISECONDS);

        executor.addAfterExecuteConsumer(handleException(future));
    }

    private void runWithDelay() {
        final ScheduledFuture<?> future = executor.schedule(execution, handleDelay(), TimeUnit.MILLISECONDS);

        executor.addAfterExecuteConsumer(handleException(future));
    }

    private void runWithTimeout() {
        final ScheduledFuture<?> future = executor.schedule(execution, handleDelay(), TimeUnit.MILLISECONDS);

        executor.addAfterExecuteConsumer(handleException(future));

        handleInterruption(future);
    }

    private void repeatWithInterval() {
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(execution, handleDelay(),
                interval.get().toMillis(), TimeUnit.MILLISECONDS);

        executor.addAfterExecuteConsumer(handleException(future));
    }

    private void runWithDelayAndTimeout() {
        final ScheduledFuture<?> future = executor.schedule(execution, handleDelay(), TimeUnit.MILLISECONDS);

        executor.addAfterExecuteConsumer(handleException(future));

        handleInterruption(future);
    }

    private void runWithDelayAndInterval() {
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(execution, handleDelay(),
                interval.get().toMillis(), TimeUnit.MILLISECONDS);

        executor.addAfterExecuteConsumer(handleException(future));
    }

    private void runWithTimeoutAndInterval() {
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(execution, handleDelay(),
                interval.get().toMillis(), TimeUnit.MILLISECONDS);

        executor.addAfterExecuteConsumer(handleException(future));

        handleInterruption(future);
    }

    private void runWithAllTimesControls() {
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(execution, handleDelay(),
                interval.get().toMillis(), TimeUnit.MILLISECONDS);

        executor.addAfterExecuteConsumer(handleException(future));

        handleInterruption(future);
    }

    private long handleDelay() {
        if (uncaughtExceptionConsumer.isPresent() || afterExecuteConsumer.isPresent()) {
            final long localDelay = delay.orElse(Duration.ofMillis(0)).toMillis();

            return localDelay >= MINIMAL_REQUIRED_DELAY ? localDelay : localDelay + MINIMAL_REQUIRED_DELAY;
        } else {
            return delay.orElse(Duration.ofMillis(0)).toMillis();
        }
    }

    private BiConsumer<Runnable, Throwable> handleException(final Future<?> future) {
        return (a, b) -> {
            try {
                future.get();
            } catch (final InterruptedException | ExecutionException | CancellationException e) {
                if (isNotSilentOrIsExecutionException(e)) {
                    uncaughtExceptionConsumer.ifPresent(consumer -> consumer.accept(e));
                }
            }
        };
    }

    private boolean isNotSilentOrIsExecutionException(final Exception e) {
        return silentInterruption == false
                || !(e instanceof CancellationException) && !(e instanceof InterruptedException);
    }

    private void handleInterruption(final ScheduledFuture<?> future) {
        executor.schedule(() -> future.cancel(mayInterruptIfRunning), timeout.get().toMillis(), TimeUnit.MILLISECONDS);
    }
}
