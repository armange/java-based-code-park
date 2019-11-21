package thread;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;

/**
 * @author Diego Armange Costa
 * @since 2019-11-18 V1.0.0 (JDK 1.8)
 * @see java.util.concurrent.ThreadFactory
 */
public class CaughtExecutorThreadFactory implements ThreadFactory {
    private final UncaughtExceptionHandler uncaughtExceptionHandler;
    
    public CaughtExecutorThreadFactory(final UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    /**
     * @see java.util.concurrent.ThreadFactory#newThread(Runnable)
     */
    @Override
    public Thread newThread(final Runnable runnable) {
        final Thread thread = new Thread(runnable);
        
        Optional.ofNullable(uncaughtExceptionHandler).ifPresent(thread::setUncaughtExceptionHandler);
        
        return thread;
    }
}
