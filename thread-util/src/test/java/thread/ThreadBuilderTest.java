package thread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Diego Armange Costa
 * @since 2019-11-18 V1.0.0 (JDK 1.8)
 */
public class ThreadBuilderTest {
    private static final RuntimeException RUNTIME_EXCEPTION = new RuntimeException();
    
    private static class LocalRunnable implements Runnable {
        @Override
        public void run() {}
    }
    
    private static class LocalRunnableWithException implements Runnable {
        @Override
        public void run() {
            throw RUNTIME_EXCEPTION;
        }
    }
    
    private static class LazyRunnableWithException implements Runnable {
        private boolean shouldFail;
        @Override
        public void run() {
            if (!shouldFail) {
                shouldFail = true;
            } else {
                throw RUNTIME_EXCEPTION;
            }
        }
    }
    
    private static class ThrowableConsumer implements Consumer<Throwable> {
        @Override
        public void accept(final Throwable t) {
            System.out.println("Exception tested successfull: " + t);
        }
    }
    
    private static class AfterExecuteConsumer implements BiConsumer<Runnable, Throwable> {
        @Override
        public void accept(final Runnable t, final Throwable u) {
            System.out.println("After-Execute consumer tested successfull.");
        }
    }
    
    private LocalRunnable localRunnable = Mockito.spy(new LocalRunnable());
    private LocalRunnableWithException localRunnableWithException = Mockito.spy(new LocalRunnableWithException());
    private LazyRunnableWithException lazyRunnableWithException = Mockito.spy(new LazyRunnableWithException());
    private ThrowableConsumer throwableConsumer = Mockito.spy(new ThrowableConsumer());
    private AfterExecuteConsumer afterExecuteConsumer = Mockito.spy(new AfterExecuteConsumer());
    
    @Before
    public void beforeTests() {
        Mockito.reset(localRunnable);
        Mockito.reset(localRunnableWithException);
        Mockito.reset(throwableConsumer);
        Mockito.reset(lazyRunnableWithException);
        Mockito.reset(afterExecuteConsumer);
    }
    
    @Test
    public void noSchedule() {
        ThreadBuilder
            .newBuilder()
            .setExecution(localRunnable)
            .start();
        
        ThreadUtil.sleepUnchecked(2000);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
    }
    
    @Test
    public void noScheduleCaughtException() {
        ThreadBuilder
            .newBuilder()
            .setExecution(localRunnableWithException)
            .setSilentInterruption(true)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .start();
        
        ThreadUtil.sleepUnchecked(2000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        Mockito.verify(throwableConsumer, Mockito.times(1)).accept(Mockito.any());
    }
    
    @Test
    public void useDelay() {
        ThreadBuilder
            .newBuilder()
            .setDelay(2000)
            .setExecution(localRunnable)
            .start();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnable, Mockito.times(0)).run();
        
        ThreadUtil.sleepUnchecked(3000);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
    }
    
    @Test
    public void useDelayCaughtException() {
        ThreadBuilder
            .newBuilder()
            .setDelay(2000)
            .setExecution(localRunnableWithException)
            .setSilentInterruption(true)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .start();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(0)).run();
        
        ThreadUtil.sleepUnchecked(3000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        Mockito.verify(throwableConsumer, Mockito.times(1)).accept(Mockito.any());
    }
    
    @Test
    public void cancelByTimeout() {
        ThreadBuilder
            .newBuilder()
            .setTimeout(1000)
            .setMayInterruptIfRunning(true)
            .setSilentInterruption(true)
            .setExecution(() -> {
                    ThreadUtil.sleepUnchecked(2000);
                    localRunnable.run();
                })
            .start();
        
        ThreadUtil.sleepUnchecked(2000);
        
        Mockito.verify(localRunnable, Mockito.times(0)).run();
        
        ThreadUtil.sleepUnchecked(3000);
        
        Mockito.verify(localRunnable, Mockito.times(0)).run();
    }
    
    @Test
    public void runBeforeTimeout() {
        ThreadBuilder
            .newBuilder()
            .setTimeout(3000)
            .setMayInterruptIfRunning(true)
            .setSilentInterruption(true)
            .setExecution(localRunnable)
            .start();
        
        ThreadUtil.sleepUnchecked(2000);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
    }
    
    @Test
    public void runBeforeTimeoutCaughtException() {
        ThreadBuilder
            .newBuilder()
            .setTimeout(3000)
            .setMayInterruptIfRunning(true)
            .setSilentInterruption(true)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .setExecution(localRunnableWithException)
            .start();
        
        ThreadUtil.sleepUnchecked(2000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        Mockito.verify(throwableConsumer, Mockito.times(1)).accept(Mockito.any());
    }
    
    @Test
    public void useInterval() {
        final ExecutorService thread = ThreadBuilder
            .newBuilder()
            .setInterval(1000)
            .setSilentInterruption(true)
            .setExecution(localRunnable)
            .start();
        
        ThreadUtil.sleepUnchecked(500);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnable, Mockito.times(2)).run();
        
        thread.shutdownNow();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnable, Mockito.times(2)).run();
    }
    
    @Test
    public void useIntervalCaughtException() {
        ThreadBuilder
            .newBuilder()
            .setInterval(1000)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .setExecution(lazyRunnableWithException)
            .setSilentInterruption(true)
            .start();
        
        ThreadUtil.sleepUnchecked(1500);
        
        Mockito.verify(lazyRunnableWithException, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(lazyRunnableWithException, Mockito.times(2)).run();
        Mockito.verify(throwableConsumer, Mockito.times(2)).accept(Mockito.any());
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(lazyRunnableWithException, Mockito.times(2)).run();
        Mockito.verify(throwableConsumer, Mockito.times(2)).accept(Mockito.any());
    }
    
    @Test
    public void useIntervalAndDelay() {
        final ExecutorService thread = ThreadBuilder
            .newBuilder()
            .setDelay(1000)
            .setInterval(1000)
            .setExecution(localRunnable)
            .setSilentInterruption(true)
            .start();
        
        ThreadUtil.sleepUnchecked(1500);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnable, Mockito.times(2)).run();
        
        thread.shutdownNow();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnable, Mockito.times(2)).run();
    }
    
    @Test
    public void useIntervalAndDelayAndCaughtException() {
        ThreadBuilder
            .newBuilder()
            .setDelay(1000)
            .setInterval(1000)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .setExecution(localRunnableWithException)
            .setSilentInterruption(true)
            .start();
        
        ThreadUtil.sleepUnchecked(2500);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        Mockito.verify(throwableConsumer, Mockito.times(1)).accept(Mockito.any());
    }
    
    @Test
    public void useTimeoutAndInterval() {
        final ExecutorService thread = ThreadBuilder
            .newBuilder()
            .setTimeout(3000)
            .setInterval(1000)
            .setMayInterruptIfRunning(true)
            .setSilentInterruption(true)
            .setExecution(localRunnable)
            .start();
        
        ThreadUtil.sleepUnchecked(500);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnable, Mockito.times(2)).run();
        
        thread.shutdownNow();
        
        ThreadUtil.sleepUnchecked(1000);
        Mockito.verify(localRunnable, Mockito.times(2)).run();
    }
    
    @Test
    public void cancelByTimeoutUsingInterval() {
        ThreadBuilder
            .newBuilder()
            .setTimeout(2000)
            .setInterval(100)
            .setMayInterruptIfRunning(true)
            .setExecution(() -> {
                    ThreadUtil.sleepUnchecked(4000);
                    localRunnable.run();
                })
            .setSilentInterruption(true)
            .start();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnable, Mockito.times(0)).run();
        
        ThreadUtil.sleepUnchecked(3000);
        
        Mockito.verify(localRunnable, Mockito.times(0)).run();
    }
    
    @Test
    public void useTimeoutAndIntervalCaughtException() {
        ThreadBuilder
            .newBuilder()
            .setTimeout(3000)
            .setInterval(1000)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .setMayInterruptIfRunning(true)
            .setSilentInterruption(true)
            .setExecution(localRunnableWithException)
            .start();
        
        ThreadUtil.sleepUnchecked(1500);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        Mockito.verify(throwableConsumer, Mockito.times(1)).accept(Mockito.any());
    }
    
    @Test
    public void useAllDelayAndTimeout() {
        ThreadBuilder
            .newBuilder()
            .setDelay(1000)
            .setTimeout(2000)
            .setMayInterruptIfRunning(true)
            .setExecution(localRunnable)
            .start();
        
        ThreadUtil.sleepUnchecked(500);
        
        Mockito.verify(localRunnable, Mockito.times(0)).run();
        
        ThreadUtil.sleepUnchecked(500);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1100);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
    }
    
    @Test
    public void useAllTimeControlsAndCaughtException() {
        ThreadBuilder
            .newBuilder()
            .setDelay(1000)
            .setTimeout(4000)
            .setInterval(1000)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .setMayInterruptIfRunning(true)
            .setSilentInterruption(true)
            .setExecution(localRunnableWithException)
            .start();
        
        ThreadUtil.sleepUnchecked(2500);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        Mockito.verify(throwableConsumer, Mockito.times(1)).accept(Mockito.any());
    }
    
    @Test
    public void useAllTimeControls() {
        final ExecutorService thread = ThreadBuilder
            .newBuilder()
            .setDelay(1000)
            .setTimeout(4000)
            .setInterval(1000)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .setMayInterruptIfRunning(true)
            .setSilentInterruption(true)
            .setExecution(localRunnable)
            .start();
        
        ThreadUtil.sleepUnchecked(1500);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnable, Mockito.times(2)).run();
        
        thread.shutdownNow();
        
        ThreadUtil.sleepUnchecked(1000);
        Mockito.verify(localRunnable, Mockito.times(2)).run();
    }
    
    @Test
    public void silentCancellationException() {
        final ExecutorService thread = ThreadBuilder
            .newBuilder(new ScheduledCaughtExecutorService(2))
            .setDelay(5000)
            .setExecution(localRunnableWithException)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .setSilentInterruption(true)
            .start();
        
        thread.shutdownNow();
        
        Mockito.verify(localRunnableWithException, Mockito.times(0)).run();
        Mockito.verify(throwableConsumer, Mockito.times(0)).accept(Mockito.any());
    }
    
    @Test
    public void throwingInterruptedException() {
        final ExecutorService thread = ThreadBuilder
            .newBuilder(new ScheduledCaughtExecutorService(2))
            .setExecution(() -> ThreadUtil.sleepUnchecked(5000))
            .setUncaughtExceptionConsumer(throwableConsumer)
            .start();
        
        ThreadUtil.sleepUnchecked(1100);
        
        thread.shutdownNow();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(0)).run();
        Mockito.verify(throwableConsumer, Mockito.times(1)).accept(Mockito.any());
    }
    
    @Test
    public void throwingCancellationException() {
        ThreadBuilder
            .newBuilder()
            .setExecution(() -> ThreadUtil.sleepUnchecked(5000))
            .setTimeout(1000)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .start();
        
        ThreadUtil.sleepUnchecked(1100);
        
        Mockito.verify(throwableConsumer, Mockito.times(1)).accept(Mockito.any());
    }
    
    @Test
    public void callingAfterExecutionConsumer() {
        ThreadBuilder
            .newBuilder()
            .setExecution(localRunnable)
            .setAfterExecuteConsumer(afterExecuteConsumer)
            .start();
        
        ThreadUtil.sleepUnchecked(1100);
        
        Mockito.verify(localRunnable, Mockito.times(1)).run();
        Mockito.verify(afterExecuteConsumer, Mockito.times(1)).accept(Mockito.any(), Mockito.any());
    }
}
