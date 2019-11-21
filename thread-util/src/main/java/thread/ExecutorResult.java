package thread;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class ExecutorResult {

    private final ExecutorService executorService;
    private final List<Future<?>> futures = new LinkedList<Future<?>>();
    private final List<ExecutorResult> timeoutExecutorResults = new LinkedList<>();
    
    public ExecutorResult(final ExecutorService executorService) {
        this.executorService = executorService;
    }
    
    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    public List<Future<?>> getFutures() {
        return futures;
    }
    
    public List<ExecutorResult> getTimeoutExecutorResults() {
        return timeoutExecutorResults;
    }
}
