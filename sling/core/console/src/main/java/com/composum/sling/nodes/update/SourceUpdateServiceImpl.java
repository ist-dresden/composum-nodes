package com.composum.sling.nodes.update;

import com.composum.sling.core.ResourceHandle;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.annotation.Nonnull;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.composum.sling.core.util.ResourceUtil.PROP_LAST_MODIFIED;
import static com.composum.sling.core.util.ResourceUtil.TYPE_LAST_MODIFIED;

@Component(
        label = "Composum Source Update Service",
        description = "service to update content trees from XML"
)
@Service(SourceUpdateService.class)
public class SourceUpdateServiceImpl implements SourceUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(SourceUpdateServiceImpl.class);

    private SAXParserFactory saxParserFactory;
    private TransformerFactory transformerFactory;

    @Activate
    private void activate(final BundleContext bundleContext) {
        saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        transformerFactory = TransformerFactory.newInstance();
    }

    private static final Collection<String> ignoredMetadataAttributes = new HashSet<>(Arrays.asList("jcr:uuid", "jcr:lastModified",
            "jcr:lastModifiedBy", "jcr:created", "jcr:createdBy", "jcr:isCheckedOut", "jcr:baseVersion",
            "jcr:versionHistory", "jcr:predecessors", "jcr:mergeFailed", "jcr:mergeFailed", "jcr:configuration"));

    /**
     * Make node equivalent to an XML document. General strategy: we update the attributes of all nodes according to the XML document,
     * creating nonexistent nodes along the way, and make node which nodes were present, and which were changed.
     * In a second pass, we recurse through the JCR tree again, delete nodes that were not present and update the lastModified
     * properties of nodes, below which there were changes.
     */
    @Override
    public void updateFromXml(@Nonnull Resource resource, @Nonnull InputStream inputStream) throws RepositoryException, IOException, ParserConfigurationException, SAXException, TransformerException {
        ResourceResolver resolver = resource.getResourceResolver();
        Node node = resource.adaptTo(Node.class);
        Session session = node.getSession();
        try {
            Resource vartmp = resolver.getResource("/var/tmp");
            if (null == vartmp)
                vartmp = resolver.create(resolver.getResource("/var"), "tmp", Collections.<String, Object>singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED));
            Resource tmpdir = resolver.create(vartmp, UUID.randomUUID().toString(), Collections.<String, Object>singletonMap(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED));

            InputStream xslt = getClass().getResourceAsStream("/bundled/sourceupdate/removemetadata.xslt");
            Transformer transformer = transformerFactory.newTransformer(new StreamSource(xslt));
            Source source = new StreamSource(inputStream);

            Result result = new SAXResult(session.getImportContentHandler(tmpdir.getPath(), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING));
            transformer.transform(source, result);

            Resource newroot = tmpdir.listChildren().next();

            equalize(newroot, resource, session, resolver);

            session.removeItem(newroot.getPath());
            session.save();
        } finally {
            LOG.info("Have changes (2): {}", session.hasPendingChanges());
            session.refresh(false); // XXX or save.
        }
    }

    // XXX unclear: what about changed resource types / mixins?
    // XXX resource ordering...
    private void equalize(@Nonnull Resource templateresource, @Nonnull Resource resource, Session session, ResourceResolver resolver)
            throws PersistenceException, RepositoryException {
        boolean thisNodeChanged = false;
        ValueMap templatevalues = ResourceUtil.getValueMap(templateresource);
        ModifiableValueMap newvalues = resource.adaptTo(ModifiableValueMap.class);
        if (newvalues == null) throw new IllegalArgumentException("Node not modifiable: " + resource.getPath());

        for (Map.Entry<String, Object> entry : templatevalues.entrySet()) {
            if (!ignoredMetadataAttributes.contains(entry.getKey()) &&
                    ObjectUtils.notEqual(entry.getValue(), newvalues.get(entry.getKey()))) {
                thisNodeChanged = true;
                newvalues.put(entry.getKey(), entry.getValue());
            }
        }

        for (String key : newvalues.keySet()) {
            if (!ignoredMetadataAttributes.contains(key) && !templatevalues.containsKey(key)) {
                thisNodeChanged = true;
                newvalues.remove(key);
            }
        }

        for (Resource child : resource.getChildren()) { // XXX are there equally named children somewhere?
            Resource templateChild = templateresource.getChild(child.getName());
            if (templateChild == null) {
                thisNodeChanged = true;
                resource.getResourceResolver().delete(child);
            } else {
                equalize(templateChild, child, session, resolver);
            }
        }

        for (Resource templateChild : templateresource.getChildren()) {
            if (null == resource.getChild(templateChild.getName())) {
                session.move(templateChild.getPath(), resource.getPath() + "/" + templateChild.getName());
                thisNodeChanged = true;
            }
        }

        NodeDefinition definition = resource.adaptTo(Node.class).getDefinition();
        if (definition.allowsSameNameSiblings()) LOG.warn("Same name siblings in " + resource.getPath());
        if (definition.getDeclaringNodeType().hasOrderableChildNodes())
            LOG.warn("Orderable subnodes not supported yet: " + resource.getResourceType() + " at " + resource.getPath());

        if (thisNodeChanged) {
            Resource modifcandidate = resource;
            while (modifcandidate != null && !modifcandidate.isResourceType(TYPE_LAST_MODIFIED))
                modifcandidate = modifcandidate.getParent();
            if (modifcandidate != null) {
                newvalues.put(PROP_LAST_MODIFIED, Calendar.getInstance());
            }
        }
    }

}
