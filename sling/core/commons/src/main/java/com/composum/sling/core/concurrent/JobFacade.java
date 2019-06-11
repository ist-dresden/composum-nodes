package com.composum.sling.core.concurrent;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.jobs.Job;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.Objects;
import java.util.Set;

/**
 * this facade encapsulates the Job objects to prevent from the use of deprecated features
 * which are removed in the next major release (4.0.0) of the job events framework
 */
public interface JobFacade extends Comparable<JobFacade> {

    String getTopic();

    String getId();

    Object getProperty(String name);

    Set<String> getPropertyNames();

    <T> T getProperty(String name, Class<T> type);

    <T> T getProperty(String name, T defaultValue);

    int getRetryCount();

    int getNumberOfRetries();

    String getQueueName();

    String getTargetInstance();

    Calendar getProcessingStarted();

    Calendar getCreated();

    String getCreatedInstance();

    Job.JobState getJobState();

    Calendar getFinishedDate();

    String getResultMessage();

    String[] getProgressLog();

    int getProgressStepCount();

    int getFinishedProgressStep();

    Calendar getProgressETA();

    public abstract class AbstractJobFacade implements JobFacade {

        @Override
        public int compareTo(JobFacade o2) {
            CompareToBuilder builder = new CompareToBuilder();
            builder.append(getProcessingStarted(), o2.getProcessingStarted());
            builder.append(getId(), o2.getId());
            return builder.toComparison();
        }
    }

    public class EventJob extends AbstractJobFacade {

        @Nonnull
        public final Job job;

        public EventJob(@Nonnull Job job) {
            this.job = Objects.requireNonNull(job);
        }

        @Override
        public String getTopic() {
            return job.getTopic();
        }

        @Override
        public String getId() {
            return job.getId();
        }

        @Override
        public Object getProperty(String name) {
            return job.getProperty(name);
        }

        @Override
        public Set<String> getPropertyNames() {
            return job.getPropertyNames();
        }

        @Override
        public <T> T getProperty(String name, Class<T> type) {
            return job.getProperty(name, type);
        }

        @Override
        public <T> T getProperty(String name, T defaultValue) {
            return job.getProperty(name, defaultValue);
        }

        @Override
        public int getRetryCount() {
            return job.getRetryCount();
        }

        @Override
        public int getNumberOfRetries() {
            return job.getNumberOfRetries();
        }

        @Override
        public String getQueueName() {
            return job.getQueueName();
        }

        @Override
        public String getTargetInstance() {
            return job.getTargetInstance();
        }

        @Override
        public Calendar getProcessingStarted() {
            return job.getProcessingStarted();
        }

        @Override
        public Calendar getCreated() {
            return (Calendar) job.getCreated();
        }

        @Override
        public String getCreatedInstance() {
            return job.getCreatedInstance();
        }

        @Override
        public Job.JobState getJobState() {
            return job.getJobState();
        }

        @Override
        public Calendar getFinishedDate() {
            return job.getFinishedDate();
        }

        @Override
        public String getResultMessage() {
            return job.getResultMessage();
        }

        @Override
        public String[] getProgressLog() {
            return job.getProgressLog();
        }

        @Override
        public int getProgressStepCount() {
            return job.getProgressStepCount();
        }

        @Override
        public int getFinishedProgressStep() {
            return job.getFinishedProgressStep();
        }

        @Override
        public Calendar getProgressETA() {
            return job.getProgressETA();
        }
    }

    public class AuditJob extends AbstractJobFacade {

        @Nonnull
        public final Resource resource;

        public AuditJob(@Nonnull Resource resource) {
            this.resource = resource;
        }

        protected String getStringProperty(String name) {
            final ValueMap valueMap = resource.adaptTo(ValueMap.class);
            return valueMap.get(name, "");
        }

        @Override
        public String getTopic() {
            return getStringProperty("event.job.topic");
        }

        @Override
        public String getId() {
            return getStringProperty("slingevent:eventId");
        }

        @Override
        public Object getProperty(String name) {
            final ValueMap valueMap = resource.adaptTo(ValueMap.class);
            return valueMap.get(name);
        }

        @Override
        public Set<String> getPropertyNames() {
            final ValueMap valueMap = resource.adaptTo(ValueMap.class);
            return valueMap.keySet();
        }

        @Override
        public <T> T getProperty(String name, Class<T> type) {
            final ValueMap valueMap = resource.adaptTo(ValueMap.class);
            return valueMap.get(name, type);
        }

        @Override
        public <T> T getProperty(String name, T defaultValue) {
            final ValueMap valueMap = resource.adaptTo(ValueMap.class);
            return valueMap.get(name, defaultValue);
        }

        @Override
        public int getRetryCount() {
            return (Integer) this.getProperty(Job.PROPERTY_JOB_RETRY_COUNT);
        }

        @Override
        public int getNumberOfRetries() {
            return (Integer) this.getProperty(Job.PROPERTY_JOB_RETRIES);
        }

        @Override
        public String getQueueName() {
            return (String) this.getProperty(Job.PROPERTY_JOB_QUEUE_NAME);
        }

        @Override
        public String getTargetInstance() {
            return (String) this.getProperty(Job.PROPERTY_JOB_TARGET_INSTANCE);
        }

        @Override
        public Calendar getProcessingStarted() {
            return (Calendar) this.getProperty(Job.PROPERTY_JOB_STARTED_TIME);
        }

        @Override
        public Calendar getCreated() {
            return (Calendar) this.getProperty(Job.PROPERTY_JOB_CREATED);
        }

        @Override
        public String getCreatedInstance() {
            return (String) this.getProperty(Job.PROPERTY_JOB_CREATED_INSTANCE);
        }

        @Override
        public Job.JobState getJobState() {
            final String enumValue = this.getProperty(JobImpl.PROPERTY_FINISHED_STATE, String.class);
            if (enumValue == null) {
                if (this.getProcessingStarted() != null) {
                    return Job.JobState.ACTIVE;
                }
                return Job.JobState.QUEUED;
            }
            return Job.JobState.valueOf(enumValue);
        }

        /**
         * @see Job#getFinishedDate()
         */
        @Override
        public Calendar getFinishedDate() {
            return this.getProperty(Job.PROPERTY_FINISHED_DATE, Calendar.class);
        }

        /**
         * @see Job#getResultMessage()
         */
        @Override
        public String getResultMessage() {
            return this.getProperty(Job.PROPERTY_RESULT_MESSAGE, String.class);
        }

        /**
         * @see Job#getProgressLog()
         */
        @Override
        public String[] getProgressLog() {
            return this.getProperty(Job.PROPERTY_JOB_PROGRESS_LOG, String[].class);
        }

        /**
         * @see Job#getProgressStepCount()
         */
        @Override
        public int getProgressStepCount() {
            return this.getProperty(Job.PROPERTY_JOB_PROGRESS_STEPS, -1);
        }

        /**
         * @see Job#getFinishedProgressStep()
         */
        @Override
        public int getFinishedProgressStep() {
            return this.getProperty(Job.PROPERTY_JOB_PROGRESS_STEP, 0);
        }

        /**
         * @see Job#getProgressETA()
         */
        @Override
        public Calendar getProgressETA() {
            return this.getProperty(Job.PROPERTY_JOB_PROGRESS_ETA, Calendar.class);
        }
    }
}
