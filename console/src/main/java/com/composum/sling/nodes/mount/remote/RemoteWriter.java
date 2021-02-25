package com.composum.sling.nodes.mount.remote;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpPost;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.PropertyType;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RemoteWriter extends RemoteClient {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteWriter.class);

    enum ChangeType {modify, create, upload, copy, move, delete}

    public static abstract class ResourceChange {

        public final RemoteResource resource;

        protected ResourceChange(RemoteResource resource) {
            this.resource = resource;
        }

        @Nonnull
        public abstract ChangeType getChangeType();

        protected abstract boolean commit(RemoteWriter writer) throws IOException;
    }

    public static class ChangeSet extends LinkedHashMap<String, ResourceChange> {

        public void addModify(@Nonnull final RemoteResource resource) {
            String path = resource.getPath();
            ResourceChange change = get(path);
            if (change == null) {
                put(path, new ResourceModify(resource));
            } else if (change.resource != resource) {
                resource.modifiedValues = change.resource.modifiedValues;
            }
        }

        public void addCreate(@Nonnull final RemoteResource resource) {
            put(resource.getPath(), new ResourceCreate(resource));
        }

        public void addUpload(@Nonnull final RemoteResource resource, @Nonnull final InputStream content,
                              @Nullable final String filename, @Nullable final String contentType,
                              @Nullable final String charset) {
            put(resource.getPath(), new ResourceUpload(resource, content, filename, contentType, charset));
        }

        public void addDelete(@Nonnull final RemoteResource resource) {
            put(resource.getPath(), new ResourceDelete(resource));
        }

        public void addCopy(@Nonnull final RemoteResource designated, @Nonnull final Resource source) {
            put(designated.getPath(), new ResourceCopy(designated, source));
        }

        public void addMove(@Nonnull final RemoteResource designated, @Nonnull final Resource source,
                            @Nullable final String order) {
            put(designated.getPath(), new ResourceMove(designated, source, order));
        }

        public boolean commit(@Nonnull final RemoteWriter writer) throws IOException {
            boolean changesMade = false;
            if (LOG.isDebugEnabled()) {
                LOG.debug("commit({})...", size());
            }
            try {
                for (ResourceChange change : values()) {
                    changesMade = change.commit(writer) || changesMade;
                }
            } finally {
                clear();
            }
            return changesMade;
        }
    }

    public RemoteWriter(@Nonnull final RemoteProvider provider, @Nonnull final String httpUrl,
                        @Nonnull final String username, @Nonnull final String password) {
        super(provider, httpUrl, username, password);
    }

    public boolean commitChanges(@Nonnull final ChangeSet changeSet) throws IOException {
        return changeSet.commit(this);
    }

    //
    // resource operations via remote Sling POST servlet
    //

    public static class ResourceCopy extends ResourceChange {

        protected final Resource source;

        public ResourceCopy(@Nonnull final RemoteResource resource, @Nonnull final Resource source) {
            super(resource);
            this.source = source;
            if (LOG.isDebugEnabled()) {
                LOG.debug("copy({},{})...", source.getPath(), resource.getPath());
            }
        }

        @Nonnull
        public ChangeType getChangeType() {
            return ChangeType.copy;
        }

        protected boolean commit(RemoteWriter writer) {
            Parameters parameters = new Parameters();
            parameters.add(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_COPY);
            parameters.add(SlingPostConstants.RP_DEST, writer.provider.remotePath(resource.getPath()));
            if (LOG.isInfoEnabled()) {
                LOG.info("copy({},{})", source.getPath(), resource.getPath());
            }
            return writer.postForm(this, source.getPath(), parameters);
        }
    }

    public static class ResourceMove extends ResourceChange {

        protected final Resource source;
        protected final String order;

        public ResourceMove(@Nonnull final RemoteResource resource, @Nonnull final Resource source,
                            @Nullable final String order) {
            super(resource);
            this.source = source;
            this.order = order;
            if (LOG.isDebugEnabled()) {
                LOG.debug("move({},{})...", source.getPath(), resource.getPath());
            }
        }

        @Nonnull
        public ChangeType getChangeType() {
            return ChangeType.copy;
        }

        protected boolean commit(RemoteWriter writer) {
            Parameters parameters = new Parameters();
            if (StringUtils.substringBeforeLast(source.getPath(), "/")
                    .equals(StringUtils.substringBeforeLast(resource.getPath(), "/"))) {
                if (StringUtils.isBlank(order)) {
                    return false;
                }
            } else {
                parameters.add(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_MOVE);
                parameters.add(SlingPostConstants.RP_DEST, writer.provider.remotePath(resource.getPath()));
            }
            if (StringUtils.isNotBlank(order)) {
                parameters.add(SlingPostConstants.RP_ORDER, order);
            }
            if (LOG.isInfoEnabled()) {
                LOG.info("move({},{})", source.getPath(), resource.getPath());
            }
            return writer.postForm(this, source.getPath(), parameters);
        }
    }

    public static class ResourceDelete extends ResourceChange {

        public ResourceDelete(@Nonnull final RemoteResource resource) {
            super(resource);
            if (LOG.isDebugEnabled()) {
                LOG.debug("delete({})...", resource.getPath());
            }
        }

        @Nonnull
        public ChangeType getChangeType() {
            return ChangeType.delete;
        }

        protected boolean commit(RemoteWriter writer) {
            Parameters parameters = new Parameters();
            parameters.add(SlingPostConstants.RP_OPERATION, SlingPostConstants.OPERATION_DELETE);
            if (LOG.isInfoEnabled()) {
                LOG.info("delete({})", resource.getPath());
            }
            return writer.postForm(this, null, parameters);
        }
    }

    public static class ResourceUpload extends ResourceChange {

        public static final String JCR_DATA_PROP = JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_DATA;
        public static final String FILENAME_PROP = JcrConstants.JCR_CONTENT + "/filename";
        public static final String MIME_TYPE_PROP = JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_MIMETYPE;
        public static final String LAST_MOD_PROP = JcrConstants.JCR_CONTENT + "/" + JcrConstants.JCR_LASTMODIFIED;

        protected final InputStream content;
        protected final String filename;
        protected final String contentType;
        protected final String charset;

        public ResourceUpload(@Nonnull final RemoteResource resource, @Nonnull final InputStream content,
                              @Nullable final String filename, @Nullable final String contentType,
                              @Nullable final String charset) {
            super(resource);
            this.content = content;
            this.filename = filename;
            this.contentType = contentType;
            this.charset = charset;
            if (LOG.isDebugEnabled()) {
                LOG.debug("upload({},{})...", resource.getPath(), filename);
            }
        }

        @Override
        @Nonnull
        public ChangeType getChangeType() {
            return ChangeType.upload;
        }

        @Override
        protected boolean commit(@Nonnull final RemoteWriter writer) throws IOException {
            String parentPath = StringUtils.substringBeforeLast(resource.getPath(), "/");
            List<Part> parts = new ArrayList<>();
            Parameters parameters = new Parameters();
            parts.add(new FilePart(resource.getName(),
                    new ByteArrayPartSource(filename != null ? filename : resource.getName(),
                            IOUtils.toByteArray(content)), contentType,
                    StringUtils.isNotBlank(charset) ? charset : StandardCharsets.UTF_8.name()));
            if (LOG.isInfoEnabled()) {
                LOG.info("upload({})", resource.getPath());
            }
            return writer.postMultipart(this, parentPath, parts, parameters);
        }
    }

    public static class ResourceCreate extends ResourceModify {

        public ResourceCreate(@Nonnull final RemoteResource resource) {
            super(resource);
            if (LOG.isDebugEnabled()) {
                LOG.debug("create({})...", resource.getPath());
            }
        }

        @Override
        @Nonnull
        public ChangeType getChangeType() {
            return ChangeType.create;
        }
    }

    public static class ResourceModify extends ResourceChange {

        public ResourceModify(@Nonnull final RemoteResource resource) {
            super(resource);
            if (LOG.isDebugEnabled()) {
                LOG.debug("modify({})...", resource.getPath());
            }
        }

        @Override
        @Nonnull
        public ChangeType getChangeType() {
            return ChangeType.modify;
        }

        @Override
        protected boolean commit(@Nonnull final RemoteWriter writer) throws IOException {
            return commit(writer, new Parameters());
        }

        protected boolean commit(RemoteWriter writer, Parameters parameters)
                throws IOException {
            List<Part> parts = new ArrayList<>();
            writer.buildForm(resource, parts, parameters);
            if (LOG.isDebugEnabled()) {
                LOG.debug(getChangeType() + "({}): parts:{}, parameters:{}", resource.getPath(), parts.size(), parameters);
            } else if (LOG.isInfoEnabled()) {
                LOG.info(getChangeType() + "({}): parts:{}, parameters:{}", resource.getPath(), parts.size(), parameters.size());
            }
            boolean changesMade;
            if (parts.size() > 0) {
                changesMade = writer.postMultipart(this, null, parts, parameters);
            } else {
                changesMade = writer.postForm(this, null, parameters);
            }
            return changesMade;
        }
    }

    //
    // HTTP transfer...
    //

    public boolean postForm(@Nonnull final ResourceChange change, @Nullable final String path,
                            @Nonnull final Parameters parameters) {
        boolean changesMade = false;
        if (parameters.size() > 0) {
            EntityBuilder entityBuilder = EntityBuilder.create();
            entityBuilder.setContentEncoding("UTF-8");
            entityBuilder.setParameters(parameters);
            postEntity(change, path, entityBuilder.build());
            changesMade = true;
        }
        return changesMade;
    }

    public boolean postMultipart(@Nonnull final ResourceChange change, @Nullable final String path,
                                 @Nonnull final List<Part> parts, @Nonnull final Parameters parameters) {
        boolean changesMade = false;
        if (parts.size() > 0 || parameters.size() > 0) {
            for (NameValuePair param : parameters) {
                parts.add(new StringPart(param.getName(), param.getValue(), "UTF-8"));
            }
            HttpMethodParams params = new HttpMethodParams();
            MultipartRequestEntity multipart = new MultipartRequestEntity(parts.toArray(new Part[0]), params);
            org.apache.commons.httpclient.HttpClient httpClient = buildCommonsClient();
            PostMethod postMethod = buildPostMethod(getHttpUrl(path != null ? path : change.resource.getPath()));
            try {
                postMethod.setRequestEntity(multipart);
                int statusCode = httpClient.executeMethod(postMethod);
                LOG.info("multipart({}): {}", postMethod.getURI(), statusCode);
                changesMade = true;
            } catch (IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return changesMade;
    }

    public void postEntity(@Nonnull final ResourceChange change, @Nullable final String path,
                           @Nonnull final HttpEntity httpEntity) {
        try {
            String url = getHttpUrl(path != null ? path : change.resource.path);
            HttpClient httpClient = buildClient();
            HttpPost httpPost = buildHttpPost(url);
            httpPost.setEntity(httpEntity);
            HttpResponse response = httpClient.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                LOG.debug(change.getChangeType() + ".POST({}): {}", httpPost.getURI(), statusCode);
            } else {
                LOG.warn(change.getChangeType() + ".POST({}): {}", httpPost.getURI(), statusCode);
            }
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    //
    // modify via remote Sling POST servlet...
    //

    public static final String POST_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final Set<String> VALUE_TYPES = new HashSet<>(Arrays.asList(
            PropertyType.TYPENAME_BOOLEAN,
            PropertyType.TYPENAME_DATE,
            PropertyType.TYPENAME_DECIMAL,
            PropertyType.TYPENAME_DOUBLE,
            PropertyType.TYPENAME_LONG
    ));

    protected void buildForm(@Nonnull final RemoteResource resource,
                             @Nonnull final List<Part> parts, @Nonnull final Parameters parameters)
            throws IOException {
        ModifiableValueMap modified = resource.modifiedValues;
        if (modified != null) {
            Set<String> scanned = new HashSet<>();
            ValueMap origin = resource.getValueMap();
            for (Map.Entry<String, Object> entry : modified.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Object read = origin.get(key);
                if (value != null) {
                    if (!value.equals(read)) {
                        addFormValue(parts, parameters, key, value);
                    }
                } else {
                    if (read != null) {
                        parameters.add(key + SlingPostConstants.SUFFIX_DELETE, "null");
                    }
                }
                scanned.add(key);
            }
            for (Map.Entry<String, Object> entry : origin.entrySet()) {
                String key = entry.getKey();
                if (!scanned.contains(key)) {
                    parameters.add(key + SlingPostConstants.SUFFIX_DELETE, "removed");
                }
            }
        }
    }

    protected void addFormValue(@Nonnull final List<Part> parts, @Nonnull final Parameters parameters,
                                @Nonnull final String name, @Nonnull Object value)
            throws IOException {
        if (value instanceof InputStream) {
            if (!(value instanceof RemoteReader.RemoteBinary)) {
                parts.add(new FilePart(name, new ByteArrayPartSource(name, IOUtils.toByteArray((InputStream) value))));
            }
        } else if (value instanceof Object[]) {
            String propertyType = null;
            for (Object item : (Object[]) value) {
                ParameterValue itemValue = new ParameterValue(item);
                if (propertyType == null) {
                    propertyType = itemValue.propertyType;
                } else if (itemValue.propertyType != null && !propertyType.equals(itemValue.propertyType)) {
                    throw new IllegalArgumentException("various multi value entry types in '"
                            + name + "' (" + propertyType + " / " + itemValue.propertyType + ")");
                }
                parameters.add(name, itemValue.value);
            }
            if (propertyType != null) {
                parameters.add(
                        name + SlingPostConstants.TYPE_HINT_SUFFIX, propertyType + "[]");
            }
        } else {
            ParameterValue parameterValue = new ParameterValue(value);
            parameters.add(name, parameterValue.value);
            if (VALUE_TYPES.contains(parameterValue.propertyType)) {
                parameters.add(
                        name + SlingPostConstants.TYPE_HINT_SUFFIX, parameterValue.propertyType);
            }
        }
    }

    protected class ParameterValue {

        @Nullable
        public final String propertyType;
        @Nullable
        public final String value;

        public ParameterValue(@Nullable Object value) {
            Integer type = null;
            if (value != null) {
                String simpleName = value.getClass().getSimpleName();
                if (value instanceof String) {
                    type = PropertyType.STRING;
                } else if (value instanceof Long || value instanceof Integer) {
                    type = PropertyType.LONG;
                } else if (value instanceof Calendar || value instanceof Date) {
                    type = PropertyType.DATE;
                    Date date;
                    if (value instanceof Calendar) {
                        date = ((Calendar) value).getTime();
                    } else {
                        date = (Date) value;
                    }
                    value = new SimpleDateFormat(POST_DATE_FORMAT).format(date);
                } else if (value instanceof Double || value instanceof Float) {
                    type = PropertyType.DOUBLE;
                } else if (value instanceof BigDecimal) {
                    type = PropertyType.DECIMAL;
                } else {
                    type = PropertyType.STRING;
                }
            }
            this.propertyType = type != null ? PropertyType.nameFromValue(type) : null;
            this.value = value != null ? value.toString() : null;
        }
    }
}
