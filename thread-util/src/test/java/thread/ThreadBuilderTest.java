package thread;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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
            t.printStackTrace();
        }
    }
    
    private LocalRunnable localRunnable = Mockito.spy(new LocalRunnable());
    private LocalRunnableWithException localRunnableWithException = Mockito.spy(new LocalRunnableWithException());
    private LazyRunnableWithException lazyRunnableWithException = Mockito.spy(new LazyRunnableWithException());
    private ThrowableConsumer throwableConsumer = Mockito.spy(new ThrowableConsumer());
    
    @Before
    public void beforeTests() {
        Mockito.reset(localRunnable);
        Mockito.reset(localRunnableWithException);
        Mockito.reset(throwableConsumer);
        Mockito.reset(lazyRunnableWithException);
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
        
        ThreadUtil.sleepUnchecked(2000);
        
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
            .setUncaughtExceptionConsumer(throwableConsumer)
            .start();
        
        ThreadUtil.sleepUnchecked(2000);
        
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
    public void useIntervalCaughtException() {
        ThreadBuilder
            .newBuilder()
            .setInterval(1000)
            .setUncaughtExceptionConsumer(throwableConsumer)
            .setExecution(lazyRunnableWithException)
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
            .start();
        
        ThreadUtil.sleepUnchecked(2500);
        
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
            .setExecution(localRunnableWithException)
            .start();
        
        ThreadUtil.sleepUnchecked(1500);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        Mockito.verify(throwableConsumer, Mockito.times(1)).accept(Mockito.any());
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
            .setExecution(localRunnableWithException)
            .start();
        
        ThreadUtil.sleepUnchecked(2500);
        
        Mockito.verify(localRunnableWithException, Mockito.times(1)).run();
        
        ThreadUtil.sleepUnchecked(1000);
        
        Mockito.verify(localRunnableWithException, Mockito.times(2)).run();
        
        thread.shutdownNow();
        
        ThreadUtil.sleepUnchecked(1000);
        Mockito.verify(localRunnableWithException, Mockito.times(2)).run();
    }
}
