package com.composum.sling.core.concurrent;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;

import javax.annotation.Nullable;
import java.util.concurrent.Callable;

/**
 * Created by rw on 20.04.16.
 */
public abstract class JobMonitor implements Callable<Boolean> {

    public static class IsStarted extends JobMonitor {

        public IsStarted(JobManager jobManager, ResourceResolver resolver,
                         String jobId, long timeout) {
            super(jobManager, resolver, jobId, timeout);
        }

        @Override
        protected boolean goalReached() {
            return started();
        }
    }

    public static class IsDone extends JobMonitor {

        public IsDone(JobManager jobManager, ResourceResolver resolver,
                      String jobId, long timeout) {
            super(jobManager, resolver, jobId, timeout);
        }

        @Override
        protected boolean goalReached() {
            return done();
        }
    }

    public boolean started() {
        return wasActive;
    }

    public boolean succeeded() {
        return Job.JobState.SUCCEEDED == finalState;
    }

    public boolean stopped() {
        return Job.JobState.STOPPED == finalState;
    }

    public boolean error() {
        return finalState != null && !succeeded() && !stopped();
    }

    public boolean done() {
        return finalState != null;
    }

    protected abstract boolean goalReached();

    public static final long DELAY = 100L;

    public final JobManager jobManager;
    public final ResourceResolver resolver;
    public final String jobId;

    protected Boolean isActive = false;
    protected Boolean wasActive = false;
    protected Job.JobState currentState;
    protected Job.JobState finalState;

    protected boolean running = false;
    protected boolean done = false;
    protected long timeout;

    protected JobMonitor(JobManager jobManager, ResourceResolver resolver,
                         String jobId, Long timeout) {
        this.jobManager = jobManager;
        this.resolver = resolver;
        this.jobId = jobId;
        if (timeout != null) {
            this.timeout = timeout;
        }
    }

    @Override
    public Boolean call() {
        running = true;
        while (!goalReached() && !done() && timeout > 0) {
            checkJobState();
            try {
                if (!goalReached() && !done()) {
                    Thread.sleep(DELAY);
                    timeout -= DELAY;
                }
            } catch (InterruptedException iex) {
                break;
            }
        }
        checkJobState();
        running = false;
        done = true;
        return goalReached();
    }

    @Nullable
    public JobFacade getJob() {
        return JobUtil.getJobById(jobManager, resolver, jobId);
    }

    protected void checkJobState() {
        JobFacade job = JobUtil.getJobById(jobManager, resolver, jobId);
        if (job != null) {
            currentState = job.getJobState();
        }
        switch (currentState) {
            case ACTIVE:
                isActive = true;
                wasActive = true;
                break;
            case SUCCEEDED:
            case GIVEN_UP:
            case STOPPED:
                wasActive = true;
            case ERROR:
            case DROPPED:
                finalState = currentState;
                isActive = false;
                break;
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("jobId", jobId)
                .append("isActive", isActive)
                .append("wasActive", wasActive)
                .append("currentState", currentState)
                .append("finalState", finalState)
                .append("running", running)
                .append("done", done)
                .append("timeout", timeout)
                .toString();
    }
}
