package com.composum.sling.core.servlet;

import com.composum.sling.core.CoreConfiguration;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.RequestUtil;
import com.composum.sling.core.util.ResponseUtil;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
    public enum Operation {job, jobs, outfile}

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

        // curl -r 100-125 -X GET http://localhost:9090/bin/core/jobcontrol.outfile.txt/
        operations.setOperation(ServletOperationSet.Method.GET, Extension.txt, Operation.outfile, new GetOutfile());

        // POST
        // curl -v -Fevent.job.topic=com/composum/sling/core/script/GroovyJobExecutor -Fscript=/hello.groovy -Foutfileprefix=groovyjob -X POST http://localhost:9090/bin/core/jobcontrol.job.json
        operations.setOperation(ServletOperationSet.Method.POST, Extension.json, Operation.job, new CreateJob());

        // DELETE
        // curl -v -X DELETE http://localhost:9090/bin/core/jobcontrol.job.json/2016/4/8/15/21/3d51ae17-ce12-4fa3-a87a-5dbfdd739093_81
        operations.setOperation(ServletOperationSet.Method.DELETE, Extension.json, Operation.job, new CancelJob());

    }

    /**
     * Gets a part of the named temp. outputfile.
     */
    private class GetOutfile implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) throws IOException {
            final String path = AbstractServiceServlet.getPath(request);
            final String range = request.getHeader("Range");
            final List<Range> ranges = decodeRange(range);
            final String filename = System.getProperty("java.io.tmpdir") + path.substring(1);
            final File file = new File(filename);
            try (final ServletOutputStream outputStream = response.getOutputStream();
                 final FileInputStream inputStream = new FileInputStream(file)) {
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
            } catch (FileNotFoundException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, path.substring(1));
            }
        }
    }

    /**
     * Gets a list of all jobs matching the state given in the extra selector.
     */
    private class GetAllJobs implements ServletOperation {

        @Override
        public void doIt(SlingHttpServletRequest request, SlingHttpServletResponse response, ResourceHandle resource) throws IOException {
            JobManager.QueryType selector = RequestUtil.getSelector(request, JobManager.QueryType.ALL);
            Collection<Job> jobs = jobManager.findJobs(selector, null, 0);
            try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                jsonWriter.beginArray();
                for (Job job: jobs) {
                    job2json2(jsonWriter, job);
                }
                jsonWriter.endArray();
            }
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
            try (final JsonWriter jsonWriter = ResponseUtil.getJsonWriter(response)) {
                job2json2(jsonWriter, job);
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
                job2json2(jsonWriter, job);
            }
        }
    }

    private void job2json2(JsonWriter jsonWriter, Job job) throws IOException {
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
        jsonWriter.endObject();
    }

    class Range {
        Integer start;
        Integer end;
        Integer suffixLength;
    }

    private List<Range> decodeRange(String rangeHeader) {
        List<Range> ranges = new ArrayList<>();
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
