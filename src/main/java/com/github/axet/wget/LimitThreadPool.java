package com.github.axet.wget;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.github.axet.wget.info.ex.DownloadInterrupted;

public class LimitThreadPool extends ThreadPoolExecutor {
    Object lock = new Object();

    static class BlockUntilFree implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                // Access to the task queue is intended primarily for
                // debugging and monitoring. This queue may be in active
                // use.
                //
                // So we are a little bit off road here :) But since we have
                // full control over executor we are safe.
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                throw new DownloadInterrupted(e);
            }
        }
    }

    public LimitThreadPool(int maxThreadCount) {
        super(0, maxThreadCount, 0, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new BlockUntilFree());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);

        synchronized (lock) {
            lock.notifyAll();
        }
    }

    /**
     * downloader working if here any getTasks() > 0
     */
    public boolean active() {
        return getActiveCount() > 0;
    }

    /**
     * Wait until current task ends. if here is no tasks exit immidiatly.
     * 
     */
    public void waitUntilNextTaskEnds() {
        synchronized (lock) {
            if (active()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new DownloadInterrupted(e);
                }
            }
        }
    }

    /**
     * Wait until thread pool execute its last task. Waits forever unti end.
     * 
     */
    public void waitUntilTermination() {
        synchronized (lock) {
            while (active())
                waitUntilNextTaskEnds();
        }
    }
}