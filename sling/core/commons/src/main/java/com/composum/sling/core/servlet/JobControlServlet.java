package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.impl.jobs.JobImpl;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SlingServlet(
        paths = "/bin/core/jobcontrol",
        methods = {"GET", "PUT", "POST", "DELETE"}
)
public class JobControlServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(JobControlServlet.class);

    public enum Extension {txt, json}
    public enum Operation {job, jobs, outfile, cleanup}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Reference
    private CoreConfiguration coreConfig;

    @Reference
    private JobManager jobManager;

    @Override protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    @Override protected ServletOperationSet getOperations() {
        return operations;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        // curl -X GET http://localhost:9090/bin/core/jobcontrol.jobs.HISTORY.json/
        // [ALL, ACTIVE, QUEUED, HISTORY, CANCELLED, SUCCEEDED, STOPPED, GIVEN_UP, ERROR, DROPPED]
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.jobs, new GetAllJobs());

        // curl http://localhost:9090/bin/core/jobcontrol.job.json/2016/4/11/13/48/3d51ae17-ce12-4fa3-a87a-5dbfdd739093_0
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.job, new GetJob());

        // curl -r 100-125 -X GET http://localhost:9090/bin/core/jobcontrol.outfile.txt/2016/4/8/15/21/3d51ae17-ce12-4fa3-a87a-5dbfdd739093_81
        operations.setOperation(ServletOperationSet.Method.GET, Extension.txt, Operation.outfile, new GetOutfile());

        // POST
        // curl -v -Fevent.job.topic=com/composum/sling/core/script/GroovyJobExecutor -Fscript=/hello.groovy -Foutfileprefix=groovyjob -X POST http://localhost:9090/bin/core/jobcontrol.job.json
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.job, new CreateJob());

        // DELETE
        // curl -v -X DELETE http://localhost:9090/bin/core/jobcontrol.job.json/2016/4/8/15/21/3d51ae17-ce12-4fa3-a87a-5dbfdd739093_81
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json, Operation.job, new CancelJob());
        // curl -u admin:admin -X DELETE http://localhost:9090/bin/core/jobcontrol.cleanup.json/2016/4/12/9/50/3d51ae17-ce12-4fa3-a87a-5dbfdd739093_0
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json, Operation.cleanup, new CleanupJob());

    }

    /**
     * Gets a part of the named temp. outputfile.
     */
    private class GetOutfile implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) throws IOException {
            final String jobId = AbstractServiceServlet.getPath(request).substring(1);
            final Job job = getJobById(jobId, request);
            if (job != null) {
                final String path = job.getProperty("outfile", String.class);
                final String range = request.getHeader("Range");
                final List<Range> ranges = decodeRange(range);
                final File file = new File(path);
                response.setCharacterEncoding("UTF-8");
                response.setContentType("text/plain;charset=utf-8");
                if (file.exists()) {
                    try (final ServletOutputStream outputStream = response.getOutputStream();
                         final InputStream inputStream = new FileInputStream(file)) {
                        writeStream(ranges, outputStream, inputStream);
                    } catch (FileNotFoundException e) {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, path);
                    }
                } else {
                    final ResourceResolver resolver = request.getResourceResolver();
                    final Iterator<Resource> resources = resolver.findResources("/jcr:root/var/audit/jobs//*[outfile='" + path + "']", "xpath");
                    if (resources.hasNext()) {
                        final Resource audit = resources.next();
                        final Resource outfileResource = resolver.getResource(audit, path.substring(path.lastIndexOf('/')+1));
                        try (final ServletOutputStream outputStream = response.getOutputStream();
                             final InputStream inputStream = outfileResource.adaptTo(InputStream.class)) {
                            writeStream(ranges, outputStream, inputStream);
                        }
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND, path);
                    }
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND, path);
            }
        }

        private void writeStream(List<Range> ranges, ServletOutputStream outputStream, InputStream inputStream) throws IOException {
            long end = Long.MAX_VALUE;
            long pos = 0L;
            if (!ranges.isEmpty()) {
                final Range range1 = ranges.get(0);
                if (range1.start != null) {
                    inputStream.skip(range1.start);
                    pos = range1.start;
                }
                if (range1.end != null) {
                    end = range1.end;
                }
            }
            while (inputStream.available() > 0 && !(pos > end)) {
                outputStream.write(inputStream.read());
                pos++;
            }
        }

        class Range {
            Integer start;
            Integer end;
            Integer suffixLength;
        }

        private List<Range> decodeRange(String rangeHeader) {
            List<Range> ranges = new ArrayList<>();
            if (StringUtils.isEmpty(rangeHeader)) {
                ranges.add(new Range());
                return ranges;
            }
            String byteRangeSetRegex = "(((?<byteRangeSpec>(?<firstBytePos>\\d+)-(?<lastBytePos>\\d+)?)|(?<suffixByteRangeSpec>-(?<suffixLength>\\d+)))(,|$))";
            String byteRangesSpecifierRegex = "bytes=(?<byteRangeSet>" + byteRangeSetRegex + "{1,})";
            Pattern byteRangeSetPattern = Pattern.compile(byteRangeSetRegex);
            Pattern byteRangesSpecifierPattern = Pattern.compile(byteRangesSpecifierRegex);
            Matcher byteRangesSpecifierMatcher = byteRangesSpecifierPattern.matcher(rangeHeader);
            if (byteRangesSpecifierMatcher.matches()) {
                String byteRangeSet = byteRangesSpecifierMatcher.group("byteRangeSet");
                Matcher byteRangeSetMatcher = byteRangeSetPattern.matcher(byteRangeSet);
                while (byteRangeSetMatcher.find()) {
                    Range range = new Range();
                    if (byteRangeSetMatcher.group("byteRangeSpec") != null) {
                        String start = byteRangeSetMatcher.group("firstBytePos");
                        String end = byteRangeSetMatcher.group("lastBytePos");
                        range.start = Integer.valueOf(start);
                        range.end = end == null ? null : Integer.valueOf(end);
                    } else if (byteRangeSetMatcher.group("suffixByteRangeSpec") != null) {
                        range.suffixLength = Integer.valueOf(byteRangeSetMatcher.group("suffixLength"));
                    } else {
                        return Collections.emptyList();
                    }
                    ranges.add(range);
                }
            } else {
                return Collections.emptyList();
            }
            return ranges;
        }
    }

    /**
     * Gets a list of all jobs matching the state given in the extra selector.
     */
    private class GetAllJobs implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) throws IOException {
            final String path = AbstractServiceServlet.getPath(request);
            JobManager.QueryType selector = RequestUtil.getSelector(request, JobManager.QueryType.ALL);
            boolean useAudit = (selector == JobManager.QueryType.ALL || selector == JobManager.QueryType.HISTORY || selector == JobManager.QueryType.SUCCEEDED);
            Collection<Job> jobs = jobManager.findJobs(selector, null, 0);
            Collection<Job> allJobs = new ArrayList<>(jobs);
            if (useAudit) {
                final Collection<Job> auditJobs = getAuditJobs(selector, request.getResourceResolver());
                for (Job auditJob: auditJobs) {
                    if (!containsJob(jobs, auditJob)) {
                        allJobs.add(auditJob);
                    }
                }
            }
            try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                jsonWriter.beginArray();
                for (Job job: allJobs) {
                    if (path.length() > 1) {
                        final String script = job.getProperty("script", String.class);
                        if (script != null && script.equals(path)) {
                            job2json(jsonWriter, job);
                        }
                    } else {
                        job2json(jsonWriter, job);
                    }
                }
                jsonWriter.endArray();
            }
        }

        private boolean containsJob(Collection<Job> jobs, Job jobToFind) {
            for (Job job: jobs) {
                if (job.getId().equals(jobToFind.getId())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Retrieves a singe job.
     */
    private class GetJob implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) throws IOException {
            final String path = AbstractServiceServlet.getPath(request);
            String jobId = path.substring(1);
            Job job = jobManager.getJobById(jobId);
            if (job != null) {
                try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                    job2json(jsonWriter, job);
                }
            } else {
                //fallback: use audit
                final ResourceResolver resolver = request.getResourceResolver();
                final Iterator<Resource> resources = resolver.findResources("/jcr:root/var/audit/jobs//*[slingevent:eventId='" + jobId + "']", "xpath");
                if (resources.hasNext()) {
                    final Resource audit = resources.next();
                    final AuditJob auditJob = new AuditJob(audit);
                    try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                        job2json(jsonWriter, auditJob);
                    }
                }
            }
        }
    }

    /**
     * Cancel a single job by its id.
     */
    private class CancelJob implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) {
            final String path = AbstractServiceServlet.getPath(request);
            String jobId = path.substring(1);
            jobManager.stopJobById(jobId);
        }
    }

    /**
     * Cleans up audit and tempfile.
     */
    private class CleanupJob implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) throws RepositoryException, IOException, ServletException {
            try {
                final String path = AbstractServiceServlet.getPath(request);
                final String jobId = path.substring(1);
                final Job job = jobManager.getJobById(jobId);
                final String outfile = job.getProperty("outfile", String.class);
                final String topic = job.getProperty("event.job.topic", String.class);
                final ResourceResolver resolver = request.getResourceResolver();
                final Iterator<Resource> resources = resolver.findResources("/jcr:root/var/audit/jobs/" + topic + "//*[slingevent:eventId='" + jobId + "']", "xpath");
                boolean auditResourceDeleted = false;
                if (resources.hasNext()) {
                    final Resource audit = resources.next();
                    if (audit != null && !ResourceUtil.isNonExistingResource(audit)) {
                        resolver.delete(audit);
                        resolver.commit();
                        auditResourceDeleted = true;
                    }
                }
                final boolean b = new File(outfile).delete();
                try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                    jsonWriter
                        .beginObject()
                            .name("audit").value(auditResourceDeleted)
                            .name("outfilefile").value(b)
                        .endObject();
                }
            } catch (final Exception ex) {
                LOG.error(ex.getMessage(), ex);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());
            }
        }
    }

    /**
     * Creates a new job.
     *
     * used parameters:
     * <ul>
     *     <li>outfileprefix</li>
     *     <li>event.job.topic</li>
     *     <li>script - only for groovy jobs</li>
     * </ul>
     */
    private class CreateJob implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) throws IOException {
            final ResourceResolver resolver = request.getResourceResolver();
            final JackrabbitSession session = (JackrabbitSession) resolver.adaptTo(Session.class);
            String topic = "";
            Map<String, Object> properties = new HashMap<>();
            @SuppressWarnings("unchecked")
            Map<String, String []> parameters = request.getParameterMap();
            for (Map.Entry<String, String []> parameter: parameters.entrySet()) {
                if (parameter.getKey().equals("event.job.topic")) {
                    topic = parameter.getValue()[0];
                } else {
                    String[] value = parameter.getValue();
                    if (value.length == 1) {
                        properties.put(parameter.getKey(), value[0]);
                    } else {
                        properties.put(parameter.getKey(), value);
                    }
                }
            }
            properties.put("userid", session.getUserID());
            String outfilePrefix = (String) properties.get("outfileprefix");
            String outfile = System.getProperty("java.io.tmpdir") + (StringUtils.isBlank(outfilePrefix) ? "slingjob" : outfilePrefix) + "_" + System.currentTimeMillis() + ".out";
            properties.put("outfile", outfile);
            Job job = jobManager.addJob(topic, properties);
            try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                job2json(jsonWriter, job);
            }
        }
    }

    private void job2json(JsonWriter jsonWriter, Job job) throws IOException {
        jsonWriter.beginObject();
        Set<String> propertyNames = job.getPropertyNames();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (String propertyName: propertyNames) {
            Object property = job.getProperty(propertyName);
            if (property instanceof Calendar) {
                dateFormat.setTimeZone(((Calendar)property).getTimeZone());
                jsonWriter.name(propertyName).value(dateFormat.format(((Calendar) property).getTime()));
            } else if (property instanceof Boolean) {
                jsonWriter.name(propertyName).value((Boolean) property);
            } else if (property instanceof Long) {
                jsonWriter.name(propertyName).value((Long) property);
            } else if (property instanceof Number) {
                jsonWriter.name(propertyName).value((Number) property);
            } else if (property instanceof String) {
                final String s = (String) property;
                if (propertyName.equals("outfile")) {
                    jsonWriter.name(propertyName).value(s.substring(s.lastIndexOf('/') + 1));
                } else {
                    jsonWriter.name(propertyName).value(s);
                }
            } else if (property instanceof Object[]) {
                jsonWriter.name(propertyName);
                jsonWriter.beginArray();
                for (Object o: (Object[])property) {
                    jsonWriter.value(String.valueOf(o));
                }
                jsonWriter.endArray();
            } else {
                jsonWriter.name(propertyName).value(String.valueOf(property));
            }
        }
        jsonWriter.name("jobState").value(job.getJobState().name());
        jsonWriter.endObject();
    }

    private Job getJobById(String jobId, SlingHttpServletRequest request) {
        Job job = jobManager.getJobById(jobId);
        if (job != null) {
            return job;
        } else {
            //fallback: use audit
            final ResourceResolver resolver = request.getResourceResolver();
            final Iterator<Resource> resources = resolver.findResources("/jcr:root/var/audit/jobs//*[slingevent:eventId='" + jobId + "']", "xpath");
            if (resources.hasNext()) {
                final Resource audit = resources.next();
                final AuditJob auditJob = new AuditJob(audit);
                return auditJob;
            } else {
                return null;
            }
        }
    }

    private Collection<Job> getAuditJobs(JobManager.QueryType state, final ResourceResolver resolver) {
        final List<Job> result = new ArrayList<>();
        final Iterator<Resource> resources = resolver.findResources("/jcr:root/var/audit/jobs//*[@slingevent:eventId]", "xpath");
        while (resources.hasNext()) {
            final AuditJob job = new AuditJob(resources.next());
            final Job.JobState jobState = job.getJobState();
            if (state == JobManager.QueryType.ALL || state == JobManager.QueryType.HISTORY || state.name().equals(jobState.name())) {
                result.add(job);
            }
        }
        return result;
    }

    private static class AuditJob implements Job {

        private Resource resource;

        AuditJob(Resource resource) {
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
        public JobUtil.JobPriority getJobPriority() {
            return JobUtil.JobPriority.NORM;
        }

        @Override
        public int getRetryCount() {
            return (Integer)this.getProperty(Job.PROPERTY_JOB_RETRY_COUNT);
        }

        @Override
        public int getNumberOfRetries() {
            return (Integer)this.getProperty(Job.PROPERTY_JOB_RETRIES);
        }

        @Override
        public String getQueueName() {
            return (String)this.getProperty(Job.PROPERTY_JOB_QUEUE_NAME);
        }

        @Override
        public String getTargetInstance() {
            return (String)this.getProperty(Job.PROPERTY_JOB_TARGET_INSTANCE);
        }

        @Override
        public Calendar getProcessingStarted() {
            return (Calendar)this.getProperty(Job.PROPERTY_JOB_STARTED_TIME);
        }

        @Override
        public Calendar getCreated() {
            return (Calendar)this.getProperty(Job.PROPERTY_JOB_CREATED);
        }

        @Override
        public String getCreatedInstance() {
            return (String)this.getProperty(Job.PROPERTY_JOB_CREATED_INSTANCE);
        }

        @Override
        public JobState getJobState() {
            final String enumValue = this.getProperty(JobImpl.PROPERTY_FINISHED_STATE, String.class);
            if ( enumValue == null ) {
                if ( this.getProcessingStarted() != null ) {
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
    }
}
