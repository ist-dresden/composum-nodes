package com.composum.sling.core.concurrent;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.composum.sling.core.concurrent.AbstractJobExecutor.JOB_OUTFILE_PROPERTY;

/**
 * Created by rw on 20.04.16.
 */
public class JobUtil {

    public static Job getJobById(JobManager jobManager, ResourceResolver resolver, String jobId) {
        Job job = jobManager.getJobById(jobId);
        if (job == null) {
            //fallback: use audit
            final Iterator<Resource> resources = resolver.findResources("/jcr:root/var/audit/jobs//*[slingevent:eventId='" + jobId + "']", "xpath");
            if (resources.hasNext()) {
                final Resource audit = resources.next();
                job = new AuditJob(audit);
            }
        }
        return job;
    }

    public static Collection<Job> getAuditJobs(JobManager.QueryType state, final ResourceResolver resolver) {
        final List<Job> result = new ArrayList<>();
        final Iterator<Resource> resources = resolver.findResources("/jcr:root/var/audit/jobs//*[@slingevent:eventId]", "xpath");
        while (resources.hasNext()) {
            final JobUtil.AuditJob job = new JobUtil.AuditJob(resources.next());
            final Job.JobState jobState = job.getJobState();
            if (state == JobManager.QueryType.ALL || state == JobManager.QueryType.HISTORY || state.name().equals(jobState.name())) {
                result.add(job);
            }
        }
        return result;
    }

    public static class JobComparator implements Comparator<Job> {
        @Override
        public int compare(Job o1, Job o2) {
            final Calendar j1s = o1.getProcessingStarted();
            final Calendar j2s = o2.getProcessingStarted();
            return j1s.compareTo(j2s);
        }
    }

    public static class AuditJob implements Job, Comparable<Job> {

        public final Resource resource;

        public AuditJob(Resource resource) {
            this.resource = resource;
        }

        private String getStringProperty(String name) {
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
        public String getName() {
            return getStringProperty(Job.PROPERTY_JOB_TITLE);
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
        @Deprecated
        public org.apache.sling.event.jobs.JobUtil.JobPriority getJobPriority() {
            return org.apache.sling.event.jobs.JobUtil.JobPriority.NORM;
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
        public JobState getJobState() {
            final String enumValue = this.getProperty(JobImpl.PROPERTY_FINISHED_STATE, String.class);
            if (enumValue == null) {
                if (this.getProcessingStarted() != null) {
                    return JobState.ACTIVE;
                }
                return JobState.QUEUED;
            }
            return JobState.valueOf(enumValue);
        }

        /**
         * @see org.apache.sling.event.jobs.Job#getFinishedDate()
         */
        @Override
        public Calendar getFinishedDate() {
            return this.getProperty(Job.PROPERTY_FINISHED_DATE, Calendar.class);
        }

        /**
         * @see org.apache.sling.event.jobs.Job#getResultMessage()
         */
        @Override
        public String getResultMessage() {
            return this.getProperty(Job.PROPERTY_RESULT_MESSAGE, String.class);
        }

        /**
         * @see org.apache.sling.event.jobs.Job#getProgressLog()
         */
        @Override
        public String[] getProgressLog() {
            return this.getProperty(Job.PROPERTY_JOB_PROGRESS_LOG, String[].class);
        }

        /**
         * @see org.apache.sling.event.jobs.Job#getProgressStepCount()
         */
        @Override
        public int getProgressStepCount() {
            return this.getProperty(Job.PROPERTY_JOB_PROGRESS_STEPS, -1);
        }

        /**
         * @see org.apache.sling.event.jobs.Job#getFinishedProgressStep()
         */
        @Override
        public int getFinishedProgressStep() {
            return this.getProperty(Job.PROPERTY_JOB_PROGRESS_STEP, 0);
        }

        /**
         * @see org.apache.sling.event.jobs.Job#getProgressETA()
         */
        @Override
        public Calendar getProgressETA() {
            return this.getProperty(Job.PROPERTY_JOB_PROGRESS_ETA, Calendar.class);
        }

        @Override
        public int compareTo(Job o2) {
            final Calendar j1s = getProcessingStarted();
            final Calendar j2s = o2.getProcessingStarted();
            return j1s.compareTo(j2s);
        }
    }

    public static String buildOutfileName(Map<String, Object> properties) {
        String outfile = (String) properties.get(JOB_OUTFILE_PROPERTY);
        if (StringUtils.isBlank(outfile)) {
            String outfilePrefix = (String) properties.get("outfileprefix");
            final String tmpdir = System.getProperty("java.io.tmpdir");
            final boolean endsWithSeparator = (tmpdir.charAt(tmpdir.length() - 1) == File.separatorChar);
            outfile = tmpdir + (endsWithSeparator ? "" : File.separator)
                    + (StringUtils.isBlank(outfilePrefix) ? "slingjob" : outfilePrefix)
                    + "_" + System.currentTimeMillis() + ".out";
            properties.put("outfile", outfile);
        }
        return outfile;
    }
}
