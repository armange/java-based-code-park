package thread;

import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CaughtExecutorService extends ThreadPoolExecutor {
    private Optional<BiConsumer<Runnable, Throwable>> afterExecuteConsumer = Optional.empty();
    private Optional<Consumer<Throwable>> uncaughtExceptionConsumer = Optional.empty();

    public CaughtExecutorService() {
        super(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
    }
    
    @Override
    public void afterExecute(final Runnable runnable, final Throwable throwable) {
        super.afterExecute(runnable, throwable);
        uncaughtExceptionConsumer.ifPresent(consumer -> consumer.accept(throwable));
        afterExecuteConsumer.ifPresent(consumer -> consumer.accept(runnable, throwable));
    }

    public Optional<BiConsumer<Runnable, Throwable>> getAfterExecuteConsumerConsumer() {
        return afterExecuteConsumer;
    }

    public void setAfterExecuteConsumer(final BiConsumer<Runnable, Throwable> afterExecuteBiConsumer) {
        this.afterExecuteConsumer = Optional.ofNullable(afterExecuteBiConsumer);
    }
    
    public Optional<Consumer<Throwable>> getUncaughtExceptionConsumer() {
        return uncaughtExceptionConsumer;
    }
    
    public void setUncaughtExceptionConsumer(final Consumer<Throwable> uncaughtExceptionHandlerConsumer) {
        this.uncaughtExceptionConsumer = Optional.ofNullable(uncaughtExceptionHandlerConsumer);
    }
}
