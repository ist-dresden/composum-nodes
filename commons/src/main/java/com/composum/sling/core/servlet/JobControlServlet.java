package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.concurrent.JobFacade;
import com.composum.sling.core.concurrent.JobUtil;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResourceUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.composum.sling.core.util.XSS;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Job Control Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + JobControlServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_PUT,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_DELETE,
                "sling.auth.requirements=" + JobControlServlet.SERVLET_PATH
        }
)
public class JobControlServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(JobControlServlet.class);

    public static final String SERVLET_PATH = "/bin/cpm/core/jobcontrol";

    public enum Extension {txt, json}

    public enum Operation {job, jobs, outfile, cleanup}

    protected ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Reference
    private CoreConfiguration coreConfig;

    @Reference
    private JobManager jobManager;

    @Override
    protected boolean isEnabled() {
        return coreConfig.isEnabled(this);
    }

    @Override
    protected ServletOperationSet getOperations() {
        return operations;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        // GET
        // curl -X GET http://localhost:9090/bin/cpm/core/jobcontrol.jobs.HISTORY.json/?topic=com/composum/sling/core/script/GroovyJobExecutor
        // [ALL, ACTIVE, QUEUED, HISTORY, CANCELLED, SUCCEEDED, STOPPED, GIVEN_UP, ERROR, DROPPED]
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.jobs, new GetAllJobs());

        // curl http://localhost:9090/bin/cpm/core/jobcontrol.job.json/2016/4/11/13/48/3d51ae17-ce12-4fa3-a87a-5dbfdd739093_0
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.job, new GetJob());

        // curl -r 100-125 -X GET http://localhost:9090/bin/cpm/core/jobcontrol.outfile.txt/2016/4/8/15/21/3d51ae17-ce12-4fa3-a87a-5dbfdd739093_81
        operations.setOperation(ServletOperationSet.Method.GET, Extension.txt, Operation.outfile, new GetOutfile());

        // POST
        // curl -v -Fevent.job.topic=com/composum/sling/core/script/GroovyJobExecutor -Freference=/hello.groovy -Foutfileprefix=groovyjob -X POST http://localhost:9090/bin/cpm/core/jobcontrol.job.json
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.job, new CreateJob());
        // curl -v -u admin:admin -Fevent.job.topic=com/composum/sling/core/script/GroovyJobExecutor -Freference=/libs/hello.groovy -Fkeep=2 -X POST http://localhost:9090/bin/cpm/core/jobcontrol.cleanup.json
        // curl -v -u admin:admin -Fevent.job.topic=com/composum/sling/core/script/GroovyJobExecutor -Fkeep=2 -X POST http://localhost:9090/bin/cpm/core/jobcontrol.cleanup.json
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.cleanup, new PurgeAudit());

        // DELETE
        // curl -v -X DELETE http://localhost:9090/bin/cpm/core/jobcontrol.job.json/2016/4/8/15/21/3d51ae17-ce12-4fa3-a87a-5dbfdd739093_81
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json, Operation.job, new CancelJob());
        // curl -u admin:admin -X DELETE http://localhost:9090/bin/cpm/core/jobcontrol.cleanup.json/2016/4/12/9/50/3d51ae17-ce12-4fa3-a87a-5dbfdd739093_0
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json, Operation.cleanup, new CleanupJob());

    }

    /**
     * Gets a part of the named temp. outputfile.
     */
    private class GetOutfile implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) throws IOException {
            final String jobId = AbstractServiceServlet.getPath(request).substring(1);
            final JobFacade job = JobUtil.getJobById(jobManager, request.getResourceResolver(), jobId);
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
                        final Resource outfileResource = resolver.getResource(audit, path.substring(path.lastIndexOf(File.separator) + 1));
                        try (final ServletOutputStream outputStream = response.getOutputStream();
                             final InputStream inputStream = outfileResource.adaptTo(InputStream.class)) {
                            writeStream(ranges, outputStream, inputStream);
                        }
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
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
            int read;
            while ((read = inputStream.read()) >= 0 && !(pos > end)) {
                outputStream.write(read);
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
            final RequestParameter topic = request.getRequestParameter("topic");
            JobManager.QueryType selector = RequestUtil.getSelector(request, JobManager.QueryType.ALL);
            boolean useAudit = (selector == JobManager.QueryType.ALL || selector == JobManager.QueryType.HISTORY || selector == JobManager.QueryType.SUCCEEDED);
            Collection<Job> jobs = jobManager.findJobs(selector, topic.getString(), 0);
            List<JobFacade> allJobs = new ArrayList<>();
            for (Job job : jobs) {
                allJobs.add(new JobFacade.EventJob(job));
            }
            if (useAudit) {
                final Collection<JobFacade> auditJobs = JobUtil.getAuditJobs(selector, request.getResourceResolver());
                for (JobFacade auditJob : auditJobs) {
                    if (!containsJob(jobs, auditJob)) {
                        allJobs.add(auditJob);
                    }
                }
            }
            try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                Collections.sort(allJobs, new Comparator<JobFacade>() {
                    @Override
                    public int compare(JobFacade o1, JobFacade o2) {
                        return o1.getCreated().compareTo(o2.getCreated());
                    }
                });
                jsonWriter.beginArray();
                for (JobFacade job : allJobs) {
                    if (path.length() > 1) {
                        final String script = job.getProperty("reference", String.class);
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

        private boolean containsJob(Collection<Job> jobs, JobFacade jobToFind) {
            for (Job job : jobs) {
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
            JobFacade job = JobUtil.getJobById(jobManager, request.getResourceResolver(), jobId);
            if (job != null) {
                try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                    job2json(jsonWriter, job);
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

    private class PurgeAudit implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) throws RepositoryException, IOException, ServletException {
            try {
                final ResourceResolver resolver = request.getResourceResolver();
                final int keep = Integer.parseInt(request.getRequestParameter("keep").getString());
                final RequestParameter reference = request.getRequestParameter("reference");
                final RequestParameter topic = request.getRequestParameter("event.job.topic");
                if (reference != null) {
                    final String referenceString = reference.getString();
                    final String query = "/jcr:root/var/audit/jobs/" + topic.getString().replaceAll("/", ".") + referenceString + "/*[@slingevent:eventId]";
                    final Iterator<Resource> auditResources = resolver.findResources(query, "xpath");
                    removeAudits(resolver, keep, auditResources);
                } else {
                    final String allAuditsQuery = "/jcr:root/var/audit/jobs/" + topic.getString().replaceAll("/", ".") + "//*[@slingevent:eventId]";
                    final Iterator<Resource> allAuditResources = resolver.findResources(allAuditsQuery, "xpath");
                    final Set<String> referencePaths = new HashSet<>();
                    while (allAuditResources.hasNext()) {
                        final Resource auditResource = allAuditResources.next();
                        final String referencePath = auditResource.getParent().getPath();
                        referencePaths.add(referencePath);
                    }
                    for (String path : referencePaths) {
                        final Resource referenceResource = resolver.getResource(path);
                        final Iterable<Resource> auditResources = referenceResource.getChildren();
                        removeAudits(resolver, keep, auditResources.iterator());
                    }

                }
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
        }

        private void removeAudits(ResourceResolver resolver, int keep, Iterator<Resource> auditResources) throws PersistenceException {
            final List<JobFacade.AuditJob> allAuditJobs = new ArrayList<>();
            while (auditResources.hasNext()) {
                final Resource auditResource = auditResources.next();
                final JobFacade.AuditJob auditJob = new JobFacade.AuditJob(auditResource);
                allAuditJobs.add(auditJob);
            }
            final Comparator<JobFacade> comparator = new JobUtil.JobComparator();
            Collections.sort(allAuditJobs, comparator);
            int size = allAuditJobs.size();
            for (int i = 0; i < size - keep; i++) {
                final JobFacade.AuditJob x = allAuditJobs.get(i);
                resolver.delete(x.resource);
            }
            resolver.commit();
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
                            .name("outfile").value(b)
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
     * <p>
     * used parameters:
     * <ul>
     * <li>outfileprefix</li>
     * <li>event.job.topic</li>
     * <li>reference</li>
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
            Map<String, String[]> parameters = request.getParameterMap();
            for (Map.Entry<String, String[]> parameter : parameters.entrySet()) {
                if (parameter.getKey().equals("event.job.topic")) {
                    topic = XSS.filter(parameter.getValue()[0]);
                } else {
                    String[] value = XSS.filter(parameter.getValue());
                    if (value.length == 1) {
                        properties.put(parameter.getKey(), value[0]);
                    } else {
                        properties.put(parameter.getKey(), value);
                    }
                }
            }
            properties.put("userid", session.getUserID());
            JobUtil.buildOutfileName(properties);
            Job job = jobManager.addJob(topic, properties);
            try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                job2json(jsonWriter, new JobFacade.EventJob(job));
            }
        }
    }

    private void job2json(JsonWriter jsonWriter, JobFacade job) throws IOException {
        jsonWriter.beginObject();
        Set<String> propertyNames = Collections.unmodifiableSet(job.getPropertyNames());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (String propertyName : propertyNames) {
            Object property = job.getProperty(propertyName);
            if (property instanceof Calendar) {
                dateFormat.setTimeZone(((Calendar) property).getTimeZone());
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
                    jsonWriter.name(propertyName).value(s.substring(s.lastIndexOf(File.separator) + 1));
                } else {
                    jsonWriter.name(propertyName).value(s);
                }
            } else if (property instanceof Object[]) {
                jsonWriter.name(propertyName);
                jsonWriter.beginArray();
                for (Object o : (Object[]) property) {
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
}
