package edu.stanford.smi.protege.query.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/*
 * I have had trouble with generics in a context very similar to this one
 * (see the comment at the top of Protege job).  I think that they should be
 * ok but I don't want to take the risk.
 */

/**
 * I should probaby use the java.util.concurrent.FutureTask here but
 * it is not well documented so it is not clear what to do.  I will
 * try to document this class from the point of view of the developer
 * of the Executor, the developer of a task, and a developer who wants
 * to interact with a queued task (e.g. cancel it or wait for
 * results).
 * 
 * @author tredmond
 *
 */
public abstract class FutureTask implements Future, Runnable {
    
    private enum State {
        IDLE, RUNNING, CANCELLED_RUNNING, CANCELLED, ERROR, SUCCESS
    }
    private State state = State.IDLE;
    
    private Object result;
    private Throwable t;

    /**
     * Cancels the task if possible.  If the task hasn't started
     * running yet, the task will be successfully cancelled.  The
     * interruptRunningTask indicates whether this routine should
     * attempt to interrupt the task if it is already running.
     * 
     * @param interruptRunningTask indicates whether this method
     * should attempt to interrupt the task if it is running.
     * Depending on how it is implemented a running task may ignore
     * the cancelled status.
     * 
     *  @return Whether the cancel operation was successful.  If the
     *  task is running, the cancel operation is viewed as succesful
     *  if it is allowed to change the state.
     */
    public synchronized boolean cancel(boolean interruptRunningTask) {
        switch (state) {
        case IDLE:
            state = State.CANCELLED;
            this.notifyAll();
            return true;
        case RUNNING:
            if (interruptRunningTask) {
                state = State.CANCELLED_RUNNING;
                return true;
            }
            else return false;
        case CANCELLED_RUNNING:
        case CANCELLED:
            return true;
        default:
            return false;
        }
    }
    
    /**
     *  This routine waits for this task to successfully return a
     *  result and then returns the result found.
     *  
     *  @return the result calculated by the task
     * 
     * @throws InterruptedException if the thread waiting for the
     *                                exception is interrupted or if
     *                                the task was interrupted.
     * 
     * @throws ExecutionException if the task threw an exception when
     * it executed.
     */
    public Object get() throws InterruptedException, ExecutionException {
        try {
            return get(0, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            throw new RuntimeException("shouldn't reach this point"); 
        }
    }

    /**
     *  This routine waits a bounded period of time for this task to
     *  successfully return a result and then returns the result
     *  found.
     *  
     *  @param waitInterval a bound on how long this routine will wait.  
     *         If it is zero then this routine behaves exactly like get().
     *
     *  @param tu The unit in which the waitInterval is measured.
     *
     *  @return the result calculated by the task
     * 
     * @throws InterruptedException if the thread waiting for the
     *                                exception is interrupted or if
     *                                the task was interrupted.
     * 
     * @throws ExecutionException if the task threw an exception when
     *                 it executed.
     *
     * @throws TimeoutException if the task does not complete in time.
     */
    public synchronized Object get(long waitInterval, TimeUnit tu) 
                            throws InterruptedException, ExecutionException, 
                                   TimeoutException {
        if (waitInterval == 0) {
            while (!isDone()) {
                this.wait();
            }
        }
        else {
            long remaining = TimeUnit.MILLISECONDS.convert(waitInterval, tu);
            // round off errors make the following questionable...
            while (!isDone() && remaining > 0) { 
                long start = System.currentTimeMillis();
                this.wait(remaining);
                long end = System.currentTimeMillis();
                if (!isDone() && remaining < (end - start)) {
                    throw new TimeoutException("Wait for results timed out");
                }
                remaining = remaining - (end - start);
            }
        }
        switch (state) {
        case CANCELLED:
            throw new InterruptedException("Execution was cancelled before execution started");
        case ERROR:
            if (t instanceof InterruptedException) {
                throw (InterruptedException) t;
            }
            throw new ExecutionException(t);
        case SUCCESS:
            return result;
        default:
            throw new RuntimeException("Shouldn't get here");
        }
    }

    /**
     * Returns true if the task is cancelled.  One use for this method is to allow the implementation
     * of the task check periodically while it runs to see if the task has been cancelled.
     */
    public synchronized boolean isCancelled() {
        return state == State.CANCELLED || state == State.CANCELLED_RUNNING;
    }
    
    /**
     * Returns true if the task has completed either normally or with an error.
     */
    public synchronized boolean isDone() {
        return state == State.CANCELLED 
          || state == State.ERROR || state == State.SUCCESS;
    }
    
    /**
     * Sets a successful completion value for the task.  This is intended to be used
     * by the implementation of the future task to set the result.
     * 
     * @param result the result of the computation.
     */
    public synchronized void set(Object result) {
        this.result = result;
        state = State.SUCCESS;
        this.notifyAll();
    }

    /**
     * Sets an exception for the task in the case of an unsuccessful execution of the task.
     * This is intended to be used by the implementation of the future task to set an unsuccessful 
     * result.
     * 
     * @param t the exception that the future task should thorw.
     */
    public synchronized void setException(Throwable t) {
        this.t = t;
        state = State.ERROR;
        this.notifyAll();
    }
    
    /**
     * This method puts the task in the running mode.  It is used by the Executor that 
     * calls the FutureTask.run() method as follows:
     * 
     *    if (task.preRun()) {
     *        task.run();
     *    }
     * 
     * @return true if the task has succesfully transitioned into the RUNNING state.
     */
    public synchronized boolean preRun() {
        if (state == State.CANCELLED) {
            return false;
        }
        state = State.RUNNING;
        return true;
    }
}
