package com.composum.sling.core.util;

import com.composum.sling.core.ResourceHandle;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Binary;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * some helpers for content mime types and resource file names
 */
public class MimeTypeUtil {

    private static final Logger LOG = LoggerFactory.getLogger(MimeTypeUtil.class);

    /** name pattern to check existence of a file extension at the end of the name */
    public static final Pattern HAS_FILE_EXT_PATTERN = Pattern.compile("^.*\\.([a-zA-Z]+[0-9]?)$");

    /** file extension from mime type value pattern fallback */
    public static final Pattern FILE_EXT_FROM_MIME = Pattern.compile("^.*/([a-zA-Z]+[0-9]?)$");

    //
    // mime type check
    //

    public static boolean isMimeType(Resource resource, String pattern) {
        return isMimeType(resource, Pattern.compile(pattern));
    }

    public static boolean isMimeType(Resource resource, Pattern pattern) {
        String mimeType = getMimeType(resource, "");
        return pattern.matcher(mimeType).matches();
    }

    //
    // mime types
    //

    /**
     * Detects the mime type of a binary resource (nt:file, nt:resource or another asset type).
     *
     * @param resource     the binary resource
     * @param defaultValue the default value if the detection has no useful result
     * @return he detected mime type or the default value given
     */
    public static String getMimeType(Resource resource, String defaultValue) {
        MimeType mimeType = getMimeType(resource);
        return mimeType != null ? mimeType.toString() : defaultValue;
    }

    /**
     * Detects the mime type of a binary property in the context of the nodes name.
     *
     * @param name         the name of the node which defines the binary resource (probably a file name)
     * @param property     the binary property (for stream parsing)
     * @param defaultValue the default value if the detection has no useful result
     * @return the detected mime type or the default value given
     */
    public static String getMimeType(String name, Property property, String defaultValue) {
        MimeType mimeType = getMimeType(name, property);
        return mimeType != null ? mimeType.toString() : defaultValue;
    }

    /**
     * Detects the mime type of a binary resource (nt:file, nt:resource or another asset type).
     *
     * @param resource the binary resource
     * @return he detected mime type or 'null' if the detection was not successful
     */
    public static MimeType getMimeType(Resource resource) {
        MimeType result = null;
        if (resource != null) {
            ResourceHandle handle = ResourceHandle.use(resource);
            result = getMimeType(handle.getProperty(ResourceUtil.PROP_MIME_TYPE, ""));
            if (result == null) {
                String name = resource.getName();
                if (ResourceUtil.CONTENT_NODE.equals(name)) {
                    result = getParentMimeType(resource);
                } else {
                    result = getContentMimeType(resource);
                    if (result == null) {
                        String filename = getResourceName(resource);
                        result = getMimeType(filename);
                    }
                }
            }
        }
        return result;
    }

    /**
     * a helper if the resource is a content node an should use its parent as a fallback
     */
    private static MimeType getParentMimeType(Resource resource) {
        MimeType result = null;
        if (resource != null && (resource = resource.getParent()) != null) {
            ResourceHandle handle = ResourceHandle.use(resource);
            result = getMimeType(handle.getProperty(ResourceUtil.PROP_MIME_TYPE, ""));
            if (result == null) {
                String filename = getResourceName(resource);
                result = getMimeType(filename);
            }
        }
        return result;
    }

    /**
     * a helper if the resource has a content node an should use this content as a fallback
     */
    private static MimeType getContentMimeType(Resource resource) {
        MimeType result = null;
        if (resource != null && (resource = resource.getChild(ResourceUtil.CONTENT_NODE)) != null) {
            ResourceHandle handle = ResourceHandle.use(resource);
            result = getMimeType(handle.getProperty(ResourceUtil.PROP_MIME_TYPE, ""));
        }
        return result;
    }

    /**
     * Detects the mime type of a binary property in the context of the nodes name.
     *
     * @param name     the name of the node which defines the binary resource (probably a file name)
     * @param property the binary property (for stream parsing)
     * @return he detected mime type or 'null' if the detection was not successful
     */
    public static MimeType getMimeType(String name, Property property) {
        MimeType result = null;
        try {
            Binary binary = property != null ? property.getBinary() : null;
            if (binary != null) {
                try {
                    InputStream input = binary.getStream();
                    try {
                        MediaType mediaType = getMediaType(input, name);
                        if (mediaType != null) {
                            try {
                                result = getMimeTypes().forName(mediaType.toString());
                            } catch (MimeTypeException e1) {
                                // detection not successful
                            }
                        }
                    } finally {
                        try {
                            input.close();
                        } catch (IOException ioex) {
                            LOG.error(ioex.toString());
                        }
                    }
                } finally {
                    binary.dispose();
                }
            }
        } catch (RepositoryException rex) {
            LOG.error(rex.toString());
        }
        return result;
    }

    //
    // file names
    //

    /**
     * Determines the filename for a resource using their name and the mime type of the resource
     * (useful as filename hint in the HTTP headers - Content-Disposition - of a download operation).
     *
     * @param resource    the (binary) resource
     * @param extFallback a default filename extension if no extension hint can be determined
     *                    from the mime type
     * @return the filename with an appropriate file extension
     */
    public static String getFilename(Resource resource, String extFallback) {
        String filename = getResourceName(resource);
        if (!HAS_FILE_EXT_PATTERN.matcher(filename).matches()) {
            String filenameEnd = null;
            MimeType mimeType = getMimeType(resource);
            if (mimeType != null) {
                String ext = mimeType.getExtension();
                if (StringUtils.isNotBlank(ext)) {
                    filenameEnd = ext;
                } else {
                    Matcher matcher = FILE_EXT_FROM_MIME.matcher(mimeType.toString());
                    if (matcher.matches()) {
                        filenameEnd = "." + matcher.group(1);
                    }
                }
            }
            if (StringUtils.isBlank(filenameEnd) && StringUtils.isNotBlank(extFallback)) {
                filenameEnd = "." + extFallback;
            }
            if (StringUtils.isNotBlank(filenameEnd) &&
                    !filename.toLowerCase().endsWith(filenameEnd.toLowerCase())) {
                filename += filenameEnd;
            }
        }
        return filename;
    }

    /**
     * Retrieves the name of a resource as base for a filename;
     * uses the parent of a 'jcr:content' resource automatically.
     */
    public static String getResourceName(Resource resource) {
        String result = null;
        if (resource != null) {
            String name = resource.getName();
            if (ResourceUtil.CONTENT_NODE.equals(name)) {
                result = getResourceName(resource.getParent());
            } else {
                result = name;
            }
        }
        return result;
    }

    //
    // Apache Tika framework...
    //

    private static MimeTypes mimeTypes;

    /**
     * Retrieves the MimeType object according to a name value
     *
     * @param value the name hint; can be a mime type value or a filename
     */
    public static MimeType getMimeType(String value) {
        if (StringUtils.isNotBlank(value)) {
            try {
                return getMimeTypes().forName(value);
            } catch (MimeTypeException e) {
                MediaType mediaType = getMediaType(null, value);
                if (mediaType != null) {
                    try {
                        return getMimeTypes().forName(mediaType.toString());
                    } catch (MimeTypeException e1) {
                        // detection not successful
                    }
                }
            }
        }
        return null;
    }

    public static MediaType getMediaType(InputStream input, String filename) {
        MediaType result = null;
        Metadata metaData = new Metadata();
        if (StringUtils.isNotBlank(filename)) {
            metaData.set(Metadata.RESOURCE_NAME_KEY, filename);
        }
        MimeTypes mimeTypes = getMimeTypes();
        try {
            // FIXME (rw,2015-06-27): use input stream, currently the use of the stream doesn't work ? right us of tika ??
            result = mimeTypes.detect(null, metaData);
        } catch (IOException ioex) {
            // ignore this in the detection context
        }
        return result;
    }

    public static MimeTypes getMimeTypes() {
        if (mimeTypes == null) {
            mimeTypes = MimeTypes.getDefaultMimeTypes();
        }
        return mimeTypes;
    }
}
