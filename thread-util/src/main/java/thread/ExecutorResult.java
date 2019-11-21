package thread;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ExecutorResult {

    private final ExecutorService executorService;
    private final List<Future<?>> futures = new LinkedList<Future<?>>();
    private final ExecutorResult timeoutExecutorResult;
    
    public ExecutorResult(final ExecutorService executorService) {
        this.executorService = executorService;
        timeoutExecutorResult = null;
    }
    
    public ExecutorResult(final ExecutorService executorService, final ExecutorResult timeoutExecutorResult) {
        this.executorService = executorService;
        this.timeoutExecutorResult = timeoutExecutorResult;
    }
    
    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    public List<Future<?>> getFutures() {
        return futures;
    }
    
    public ExecutorResult getTimeoutExecutorResult() {
        return timeoutExecutorResult;
    }
}
