package thread;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ThreadBuilder {
    private Optional<Duration> timeout = Optional.empty();
    private Optional<Duration> delay = Optional.empty();
    private Optional<Duration> interval = Optional.empty();
    private Optional<BiConsumer<Runnable, Throwable>> afterExecuteConsumer = Optional.empty();
    private Optional<Consumer<Throwable>> uncaughtExceptionConsumer = Optional.empty();
    private Runnable execution;
    private ScheduledCaughtExecutorService executor;
    private boolean mayInterruptIfRunning;
    
    private ThreadBuilder() {}
    
    public static ThreadBuilder newBuilder() {
        return new ThreadBuilder();
    }
    
    public ThreadBuilder setTimeout(final long milliseconds) {
        timeout = Optional.of(Duration.ofMillis(milliseconds));
        
        return this;
    }
    
    public ThreadBuilder setDelay(final long milliseconds) {
        delay = Optional.of(Duration.ofMillis(milliseconds));
        
        return this;
    }
    
    public ThreadBuilder setInterval(final long milliseconds) {
        interval = Optional.of(Duration.ofMillis(milliseconds));
        
        return this;
    }
    
    public ThreadBuilder setAfterExecuteConsumer(final BiConsumer<Runnable, Throwable> afterExecuteConsumer) {
        this.afterExecuteConsumer = Optional.ofNullable(afterExecuteConsumer);
        
        return this;
    } 
    
    public ThreadBuilder setUncaughtExceptionConsumer(final Consumer<Throwable> uncaughtExceptionConsumer) {
        this.uncaughtExceptionConsumer = Optional.ofNullable(uncaughtExceptionConsumer);
        
        return this;
    }
    
    public ThreadBuilder setExecution(final Runnable execution) {
        this.execution = execution;
        
        requireExecutionNonNull();
        
        return this;
    }

    public ThreadBuilder setMayInterruptIfRunning(final boolean flag) {
        mayInterruptIfRunning = flag;
        
        return this;
    } 
    
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
        } else /*All*/ {
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
        final Future<?> future = executor.schedule(execution, 1000, TimeUnit.MILLISECONDS);
        
        executor.addAfterExecuteConsumer(handleException(future));
    }

    private void runWithDelay() {
        final ScheduledFuture<?> future = executor.schedule(
                execution, 
                delay.get().toMillis() + 1000, 
                TimeUnit.MILLISECONDS);
        
        executor.addAfterExecuteConsumer(handleException(future));
    }
    
    private void runWithTimeout() {
        final Future<?> future = executor.schedule(execution, 1000, TimeUnit.MILLISECONDS);
        
        executor.addAfterExecuteConsumer(handleException(future));
        
        final Callable<Boolean> callable = () -> future.cancel(mayInterruptIfRunning);
        
        executor.schedule(
                callable, 
                timeout.get().toMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    private void repeatWithInterval() {
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                execution, 
                1000, 
                interval.get().toMillis(), 
                TimeUnit.MILLISECONDS);
        
        executor.addAfterExecuteConsumer(handleException(future));
    }
    
    private void runWithDelayAndTimeout() {
        final Future<?> future = executor
                .schedule(
                        execution, 
                        delay.get().toMillis() + 1000, 
                        TimeUnit.MILLISECONDS);
        
        executor.addAfterExecuteConsumer(handleException(future));
        
        final Callable<Boolean> callable = () -> future.cancel(mayInterruptIfRunning);
        
        executor.schedule(
                callable, 
                timeout.get().toMillis(), 
                TimeUnit.MILLISECONDS);
    }
    
    private void runWithDelayAndInterval() {
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                execution, 
                delay.get().toMillis() + 1000, 
                interval.get().toMillis(), 
                TimeUnit.MILLISECONDS);
        
        executor.addAfterExecuteConsumer(handleException(future));
    }
    
    private void runWithTimeoutAndInterval() {
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                execution, 
                1000, 
                interval.get().toMillis(), 
                TimeUnit.MILLISECONDS);
        
        executor.addAfterExecuteConsumer(handleException(future));
        
        executor.schedule(() -> future.cancel(mayInterruptIfRunning), timeout.get().toMillis(), TimeUnit.MILLISECONDS);
    }
    
    private void runWithAllTimesControls() {
        final ScheduledFuture<?> future = executor.scheduleAtFixedRate(
                execution, 
                delay.get().toMillis() + 1000, 
                interval.get().toMillis(), 
                TimeUnit.MILLISECONDS);
        
        executor.addAfterExecuteConsumer(handleException(future));
        
        executor.schedule(() -> future.cancel(mayInterruptIfRunning), timeout.get().toMillis(), TimeUnit.MILLISECONDS);
    }
    
    private BiConsumer<Runnable, Throwable> handleException(final Future<?> future) {
        return (a,b) -> {
            try {
                future.get();
            } catch (final InterruptedException | ExecutionException e) {
                uncaughtExceptionConsumer.ifPresent(consumer -> consumer.accept(e));
            }
        };
    }
}
