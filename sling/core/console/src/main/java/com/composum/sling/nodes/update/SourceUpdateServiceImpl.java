package com.composum.sling.nodes.update;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.io.Importer;
import org.apache.jackrabbit.vault.fs.io.MemoryArchive;
import org.apache.sling.api.resource.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.composum.sling.core.util.ResourceUtil.*;


@Component(
        label = "Composum Source Update Service",
        description = "service to update content trees from XML"
)
@Service(SourceUpdateService.class)
public class SourceUpdateServiceImpl implements SourceUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(SourceUpdateServiceImpl.class);

    /**
     * Attributes that are not modified on the target.
     * See result of JCR query <code>/jcr:system/jcr:nodeTypes/*[jcr:isMixin=true]/rep:namedPropertyDefinitions//*[jcr:protected=true]</code>.
     */
    private static final Collection<String> ignoredMetadataAttributes = new HashSet<>(Arrays.asList("jcr:uuid", "jcr:lastModified",
            "jcr:lastModifiedBy", "jcr:created", "jcr:createdBy", "jcr:isCheckedOut", "jcr:baseVersion",
            "jcr:versionHistory", "jcr:predecessors", "jcr:mergeFailed", "jcr:mergeFailed", "jcr:configuration",
            "jcr:activity", "jcr:etag", "rep:hold", "rep:retentionPolicy", "rep:versions",
            JcrConstants.JCR_MIXINTYPES // mixinTypes is treated separately
    ));

    /**
     * Nodes that should not be removed from the target.
     * See result of JCR query <code>/jcr:system/jcr:nodeTypes//element(*,nt:childNodeDefinition)[jcr:name]</code>
     * and <code>/jcr:system/jcr:nodeTypes//rep:namedChildNodeDefinitions</code>
     */
    private static final Collection<String> noRemoveNodeNames = new HashSet<>(Arrays.asList("rep:policy", "oak:index", "rep:repoPolicy"));

    /**
     * Mixins that should not be removed from the target.
     * See result of JCR query <code>/jcr:system/jcr:nodeTypes/*[jcr:isMixin=true]</code>
     */
    private static final Collection<String> noRemoveMixins = new HashSet<>(Arrays.asList(
            // various internal Jackrabbit stuff - we rather not touch that.
            "rep:AccessControllable", "rep:RepoAccessControllable", "rep:Impersonatable", "rep:VersionablePaths", "rep:VersionReference", "rep:RetentionManageable", "mix:indexable"
    ));

    /**
     * {@inheritDoc}
     * <p>
     * Make subtree equivalent to a ZIP in vault format. General strategy: we update the attributes of all nodes according to the XML documents,
     * creating nonexistent nodes along the way, and make node which nodes were present, and which were changed.
     * In a second pass, we recurse through the JCR tree again, delete nodes that were not present and update the lastModified
     * properties of nodes, below which there were changes.
     */
    @Override
    public void updateFromZip(@Nonnull ResourceResolver resolver, @Nonnull InputStream rawZipInputStream, @Nonnull String nodePath)
            throws IOException, RepositoryException {
        Session session = resolver.adaptTo(Session.class);
        Resource tmpdir = makeTempdir(resolver);
        final String tmpPath = tmpdir.getPath();

        Importer importer;
        ImportErrorListener errorListener = new ImportErrorListener();
        try {
            importer = new Importer();
            // TODO(hps,11.02.20) Use ZipStreamArchive once that's public (in later vault versions)
            MemoryArchive archive = new MemoryArchive(false);
            archive.run(rawZipInputStream);
            importer.getOptions().setStrict(true);
            importer.getOptions().setListener(errorListener);
            importer.run(archive, tmpdir.adaptTo(Node.class));
        } catch (IOException | RepositoryException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            rawZipInputStream.close();
        }
        if (importer.hasErrors()) {
            StringBuilder buf = new StringBuilder("Errors during import: ");
            errorListener.errors.forEach(e ->
                    buf.append(e.getLeft()).append(" : ").append(String.valueOf(e.getRight())).append("\n"));
            throw new RepositoryException(buf.toString());
        }

        try {
            Resource topnode = tmpdir.getChild(nodePath.replaceFirst("^/+", ""));
            if (topnode == null) {
                throw new IllegalArgumentException("Archive does not contain given root path " + nodePath);
            }
            if (StringUtils.countMatches(nodePath, "/") < 3) {
                throw new IllegalArgumentException("Suspicious / short root path: " + nodePath);
            }
            Resource resource = resolver.getResource(nodePath);
            if (resource == null) {
                throw new IllegalArgumentException("Node does not exist, so we cannot update it: " + nodePath);
            }
            equalize(topnode, resource, session);

            LOG.info("Have changes: {}", session.hasPendingChanges());
            session.save();
        } finally {
            session.refresh(false); // discard - if it went OK it's already saved.
            if (resolver.getResource(tmpPath) != null) {
                session.removeItem(tmpdir.getPath());
            }
            session.save();
        }
    }

    protected Resource makeTempdir(ResourceResolver resolver) throws RepositoryException {
        String path = "/tmp/composum/nodes/SourceUpdateService/" + UUID.randomUUID().toString();
        return ResourceUtil.getOrCreateResource(resolver, path, TYPE_SLING_FOLDER);
    }

    private void equalize(@Nonnull Resource templateresource, @Nonnull Resource resource, Session session)
            throws PersistenceException, RepositoryException {
        boolean thisNodeChanged = false;
        ValueMap templatevalues = ResourceUtil.getValueMap(templateresource);
        ModifiableValueMap newvalues = resource.adaptTo(ModifiableValueMap.class);
        if (newvalues == null) {
            throw new IllegalArgumentException("Node not modifiable: " + resource.getPath());
        }

        try {
            // first copy type information since this changes attributes
            newvalues.put(PROP_PRIMARY_TYPE, templatevalues.get(PROP_PRIMARY_TYPE));
            List<String> newMixins = new ArrayList<>(Arrays.asList(templatevalues.get(PROP_MIXINTYPES, new String[0])));
            for (String mixin : newvalues.get(PROP_MIXINTYPES, new String[0])) {
                if (noRemoveMixins.contains(mixin) && !newMixins.contains(mixin)) {
                    newMixins.add(mixin);
                }
            }
            if (!newMixins.isEmpty() || newvalues.containsKey(PROP_MIXINTYPES)) {
                newvalues.put(PROP_MIXINTYPES, newMixins.toArray(new String[newMixins.size()]));
            }

            Node node = resource.adaptTo(Node.class);
            NodeDefinition definition = node.getDefinition();
            if (definition.allowsSameNameSiblings()) {
                checkForSamenameSiblings(templateresource, resource);
            }

            for (Map.Entry<String, Object> entry : templatevalues.entrySet()) {
                if (!ignoredMetadataAttributes.contains(entry.getKey()) &&
                        ObjectUtils.notEqual(entry.getValue(), newvalues.get(entry.getKey()))) {
                    thisNodeChanged = true;
                    newvalues.put(entry.getKey(), entry.getValue());
                }
            }

            for (String key : new HashSet<>(newvalues.keySet())) {
                if (!ignoredMetadataAttributes.contains(key) && !templatevalues.containsKey(key)) {
                    thisNodeChanged = true;
                    newvalues.remove(key);
                }
            }

            for (Resource child : resource.getChildren()) {
                Resource templateChild = templateresource.getChild(child.getName());
                if (templateChild == null) {
                    if (!noRemoveNodeNames.contains(child.getName())) {
                        thisNodeChanged = true;
                        try {
                            resource.getResourceResolver().delete(child);
                        } catch (PersistenceException | RuntimeException e) {
                            LOG.error("Can't delete {}", child.getPath(), e);
                            throw e;
                        }
                    }
                } else {
                    equalize(templateChild, child, session);
                }
            }

            // save reference order here, since we might move some nodes
            List<Resource> templatechildren = IteratorUtils.toList(templateresource.listChildren());

            for (Resource templateChild : templateresource.getChildren()) {
                if (null == resource.getChild(templateChild.getName())) {
                    session.move(templateChild.getPath(), resource.getPath() + "/" + templateChild.getName());
                    thisNodeChanged = true;
                }
            }

            if (node.getPrimaryNodeType().hasOrderableChildNodes()) {
                ensureSameOrdering(templatechildren, resource);
            }

            if (thisNodeChanged) {
                Resource modifcandidate = resource;
                while (modifcandidate != null && !ResourceUtil.isNodeType(modifcandidate, TYPE_LAST_MODIFIED)) {
                    modifcandidate = modifcandidate.getParent();
                }
                if (modifcandidate != null) {
                    ResourceHandle.use(modifcandidate).setProperty(PROP_LAST_MODIFIED, Calendar.getInstance());
                }
            }
        } catch (PersistenceException | RepositoryException | RuntimeException e) {
            LOG.error("Error at {} : {}", resource.getPath(), e.toString());
            throw e;
        }
    }

    private void ensureSameOrdering(List<Resource> templatechildren, Resource resource) throws RepositoryException {
        Node node = Objects.requireNonNull(resource.adaptTo(Node.class));
        List<Resource> resourcechildren = IteratorUtils.toList(resource.listChildren());
        if (templatechildren.size() != resourcechildren.size()) {
            throw new IllegalStateException("Bug: template and resource of " + resource.getPath() +
                    " should have same size now but have " + templatechildren.size() + " and " + resourcechildren.size());
        }
        if (resourcechildren.size() < 2) {
            return;
        }
        Map<String, Resource> nameToNode = new HashMap<>();
        for (Resource child : resourcechildren) {
            nameToNode.put(child.getName(), child);
        }
        for (int i = 0; i < resourcechildren.size(); ++i) {
            if (!StringUtils.equals(resourcechildren.get(i).getName(), templatechildren.get(i).getName())) {
                node.orderBefore(templatechildren.get(i).getName(), resourcechildren.get(i).getName());
                resourcechildren = IteratorUtils.toList(resource.listChildren());
            }
        }
    }

    private void checkForSamenameSiblings(Resource templateresource, @Nonnull Resource resource) throws IllegalArgumentException {
        Set<String> nodenames = new HashSet<>();
        for (Resource child : templateresource.getChildren()) {
            if (nodenames.contains(child.getName())) {
                throw new IllegalArgumentException("Equally named children not supported yet: existing resource " + templateresource.getPath() + " has two " + child.getName());
            }
            nodenames.add(child.getName());
        }
        nodenames.clear();
        for (Resource child : resource.getChildren()) {
            if (nodenames.contains(child.getName())) {
                throw new IllegalArgumentException("Equally named children not supported yet: imported resource " + resource.getPath() + " has two " + child.getName());
            }
            nodenames.add(child.getName());
        }
    }

    protected static class ImportErrorListener implements ProgressTrackerListener {

        public final List<Pair<String, Exception>> errors = new ArrayList<>();

        @Override
        public void onMessage(Mode mode, String action, String path) {
            // ignore
        }

        @Override
        public void onError(Mode mode, String path, Exception e) {
            errors.add(Pair.of(path, e));
        }
    }
}
