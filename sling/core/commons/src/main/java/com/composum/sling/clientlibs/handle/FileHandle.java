package com.composum.sling.clientlibs.handle;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class FileHandle {

    private final Logger LOG = LoggerFactory.getLogger(FileHandle.class);

    public static final Map<String, Object> CRUD_FILE_PROPS;
    public static final Map<String, Object> CRUD_CONTENT_PROPS;

    static {
        CRUD_FILE_PROPS = new HashMap<>();
        CRUD_FILE_PROPS.put(ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_FILE);
        CRUD_CONTENT_PROPS = new HashMap<>();
        CRUD_CONTENT_PROPS.put(ResourceUtil.PROP_PRIMARY_TYPE, ResourceUtil.TYPE_RESOURCE);
    }

    protected final ResourceHandle resource;
    protected final ResourceHandle content;

    protected transient String name;
    protected transient String extension;
    protected transient String mimeType;
    protected transient String encoding;
    protected transient Calendar lastModified;

    public FileHandle(Resource resource) {
        this.resource = ResourceHandle.use(resource);
        this.content = retrieveContent();
    }

    /** Handle to the content node of the file; not null. */
    public ResourceHandle getContent() {
        return content;
    }

    /** Handle to the main node of the file; not null. */
    public ResourceHandle getResource() {
        return resource;
    }

    public boolean isValid() {
        return resource.isValid() && content.isValid();
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getName() {
        if (name == null) {
            retrieveNameAndExt();
        }
        return name;
    }

    public String getExtension() {
        if (extension == null) {
            retrieveNameAndExt();
        }
        return extension;
    }

    public String getMimeType() {
        if (mimeType == null) {
            if (content.isValid()) {
                mimeType = content.getProperty(ResourceUtil.PROP_MIME_TYPE, "");
            }
        }
        return mimeType;
    }

    public String getEncoding() {
        if (encoding == null) {
            if (content.isValid()) {
                encoding = content.getProperty(ResourceUtil.PROP_ENCODING, "");
            }
        }
        return encoding;
    }

    public Calendar getLastModified() {
        if (lastModified == null) {
            if (content.isValid()) {
                lastModified = content.getLastModified();
            }
            if (null == lastModified && resource.isValid()) {
                lastModified = resource.getLastModified();
            }
        }
        return lastModified;
    }

    public Long getSize() {
        Long size = null;
        Binary binary = ResourceUtil.getBinaryData(resource);
        if (binary != null) {
            try {
                size = binary.getSize();
            } catch (RepositoryException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        }
        return size;
    }

    public InputStream getStream() {
        InputStream stream = null;
        if (content.isValid()) {
            ValueMap values = content.adaptTo(ValueMap.class);
            stream = values.get(ResourceUtil.PROP_DATA, InputStream.class);
        }
        return stream;
    }

    public void storeContent(InputStream stream) {
        if (content.isValid()) {
            ModifiableValueMap values = content.adaptTo(ModifiableValueMap.class);
            values.put(ResourceUtil.PROP_DATA, stream);
        }
    }

    protected ResourceHandle retrieveContent() {
        Resource content = null;
        if (resource.isValid()) {
            if (resource.isResourceType(ResourceUtil.TYPE_FILE)) {
                content = resource.getChild(ResourceUtil.CONTENT_NODE);
            } else if (resource.isResourceType(ResourceUtil.TYPE_LINKED_FILE)) {
                String uuid = resource.getProperty(ResourceUtil.CONTENT_NODE, "");
                if (StringUtils.isNotBlank(uuid)) {
                    ResourceResolver resolver = resource.getResourceResolver();
                    Session session = resolver.adaptTo(Session.class);
                    try {
                        Node node = session.getNodeByIdentifier(uuid);
                        if (node != null) {
                            FileHandle file = new FileHandle(resolver.getResource(node.getPath()));
                            if (file.isValid()) {
                                content = file.getContent();
                            }
                        }
                    } catch (RepositoryException rex) {
                        LOG.error(rex.getMessage(), rex);
                    }
                }
            } else {
                String fileRef = resource.getProperty(ResourceUtil.PROP_FILE_REFERENCE, "");
                if (StringUtils.isNotBlank(fileRef)) {
                    ResourceResolver resolver = resource.getResourceResolver();
                    Resource referenced = resolver.getResource(fileRef);
                    FileHandle file = new FileHandle(referenced);
                    if (file.isValid()) {
                        content = file.getContent();
                    }
                }
            }
        }
        return ResourceHandle.use(content);
    }

    protected void retrieveNameAndExt() {
        if (resource != null) {
            name = resource.getName();
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                extension = name.substring(lastDot + 1);
                name = name.substring(0, lastDot);
            } else {
                extension = "";
            }
        }
    }

    @Override
    public String toString() {
        return "FileHandle, " + (isValid() ? resource.getPath() : ("<invalid: " + resource + ">"));
    }
}
