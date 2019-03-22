package com.composum.sling.core.concurrent;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.composum.sling.core.concurrent.AbstractJobExecutor.JOB_OUTFILE_PROPERTY;

/**
 * Created by rw on 20.04.16.
 */
public class JobUtil {

    @Nullable
    public static JobFacade getJobById(JobManager jobManager, ResourceResolver resolver, String jobId) {
        Job job = jobManager.getJobById(jobId);
        if (job == null) {
            //fallback: use audit
            final Iterator<Resource> resources = resolver.findResources("/jcr:root/var/audit/jobs//*[slingevent:eventId='" + jobId + "']", "xpath");
            if (resources.hasNext()) {
                final Resource audit = resources.next();
                return new JobFacade.AuditJob(audit);
            }
            return null;
        }
        return new JobFacade.EventJob(job);
    }

    public static Collection<JobFacade> getAuditJobs(JobManager.QueryType state, final ResourceResolver resolver) {
        final List<JobFacade> result = new ArrayList<>();
        final Iterator<Resource> resources = resolver.findResources("/jcr:root/var/audit/jobs//*[@slingevent:eventId]", "xpath");
        while (resources.hasNext()) {
            final JobFacade job = new JobFacade.AuditJob(resources.next());
            final Job.JobState jobState = job.getJobState();
            if (state == JobManager.QueryType.ALL || state == JobManager.QueryType.HISTORY || state.name().equals(jobState.name())) {
                result.add(job);
            }
        }
        return result;
    }

    public static class JobComparator implements Comparator<JobFacade> {
        @Override
        public int compare(JobFacade o1, JobFacade o2) {
            final Calendar j1s = o1.getProcessingStarted();
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
