package thread;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.function.BiConsumer;

public class ScheduledCaughtExecutorService extends ScheduledThreadPoolExecutor {
    private List<BiConsumer<Runnable, Throwable>> afterExecuteConsumers = new LinkedList<>();
    
    public ScheduledCaughtExecutorService(int corePoolSize) {
        super(corePoolSize);
    }

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
