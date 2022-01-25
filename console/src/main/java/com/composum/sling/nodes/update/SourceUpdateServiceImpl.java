package com.composum.sling.nodes.update;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.ResourceUtil;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter;
import org.apache.jackrabbit.vault.fs.io.Importer;
import org.apache.jackrabbit.vault.fs.io.ZipStreamArchive;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.composum.sling.core.util.ResourceUtil.PROP_LAST_MODIFIED;
import static com.composum.sling.core.util.ResourceUtil.PROP_MIXINTYPES;
import static com.composum.sling.core.util.ResourceUtil.PROP_PRIMARY_TYPE;
import static com.composum.sling.core.util.ResourceUtil.TYPE_LAST_MODIFIED;
import static com.composum.sling.core.util.ResourceUtil.TYPE_SLING_FOLDER;


@Component(
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum Nodes Source Update Service : service to update content trees from XML"
        }
)
public class SourceUpdateServiceImpl implements SourceUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(SourceUpdateServiceImpl.class);

    /**
     * Attributes that are not modified on the target.
     * See result of JCR query {@code /jcr:system/jcr:nodeTypes/*[jcr:isMixin=true]/rep:namedPropertyDefinitions//*[jcr:protected=true]}.
     */
    private static final Collection<String> ignoredMetadataAttributes = new HashSet<>(Arrays.asList("jcr:uuid", "jcr:lastModified",
            "jcr:lastModifiedBy", "jcr:created", "jcr:createdBy", "jcr:isCheckedOut", "jcr:baseVersion",
            "jcr:versionHistory", "jcr:predecessors", "jcr:mergeFailed", "jcr:mergeFailed", "jcr:configuration",
            "jcr:activity", "jcr:etag", "rep:hold", "rep:retentionPolicy", "rep:versions",
            JcrConstants.JCR_MIXINTYPES // mixinTypes is treated separately
    ));

    /**
     * Nodes that should not be removed from the target.
     * See result of JCR query {@code /jcr:system/jcr:nodeTypes//element(*,nt:childNodeDefinition)[jcr:name]}
     * and {@code /jcr:system/jcr:nodeTypes//rep:namedChildNodeDefinitions}
     */
    private static final Collection<String> noRemoveNodeNames = new HashSet<>(Arrays.asList("rep:policy", "oak:index", "rep:repoPolicy"));

    /**
     * Mixins that should not be removed from the target.
     * See result of JCR query {@code /jcr:system/jcr:nodeTypes/*[jcr:isMixin=true]}
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
    public void updateFromZip(@NotNull ResourceResolver resolver, @NotNull InputStream rawZipInputStream, @NotNull String nodePath)
            throws IOException, RepositoryException {
        Session session = Objects.requireNonNull(resolver.adaptTo(Session.class));
        Resource tmpdir = makeTempdir(resolver);
        final String tmpPath = tmpdir.getPath();

        Importer importer;
        ImportErrorListener errorListener = new ImportErrorListener();
        ZipStreamArchive archive = new ZipStreamArchive(rawZipInputStream);
        try {
            importer = new Importer();
            importer.getOptions().setFilter(new DefaultWorkspaceFilter());
            importer.getOptions().setListener(errorListener);
            archive.open(true);
            LOG.info("Importing {}", archive.getMetaInf().getProperties());
            importer.run(archive, session, tmpdir.getPath());
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
                    buf.append(e.getLeft()).append(" : ").append(e.getRight()).append("\n"));
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

    private void equalize(@NotNull Resource templateresource, @NotNull Resource resource, Session session)
            throws PersistenceException, RepositoryException {
        boolean thisNodeChanged = false;
        ValueMap templatevalues = ResourceUtil.getValueMap(templateresource);
        ModifiableValueMap newvalues = resource.adaptTo(ModifiableValueMap.class);
        if (newvalues == null) {
            throw new IllegalArgumentException("Node not modifiable: " + resource.getPath());
        }

        try {
            copyTypeInformation(templatevalues, newvalues); // first this since it might change attributes.
            checkForSamenameSiblings(templateresource, resource);

            for (Map.Entry<String, Object> entry : templatevalues.entrySet()) {
                if (!ignoredMetadataAttributes.contains(entry.getKey()) &&
                        !Objects.deepEquals(entry.getValue(), newvalues.get(entry.getKey()))) {
                    thisNodeChanged = true;
                    if (isArray(entry.getValue()) != isArray(newvalues.get(entry.getKey()))) {
                        newvalues.remove(entry.getKey()); // strangely, we cannot change multi-value-ness with a put
                    }
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
                    if (!noRemoveNodeNames.contains(child.getName()) && !ResourceUtil.isSyntheticResource(child)) {
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

            ensureSameOrdering(templatechildren, resource);

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

    protected boolean isArray(Object value) {
        return value != null && value.getClass().isArray();
    }

    private void copyTypeInformation(ValueMap templatevalues, ModifiableValueMap newvalues) {
        newvalues.put(PROP_PRIMARY_TYPE, templatevalues.get(PROP_PRIMARY_TYPE));
        List<String> newMixins = new ArrayList<>(Arrays.asList(templatevalues.get(PROP_MIXINTYPES, new String[0])));
        for (String mixin : newvalues.get(PROP_MIXINTYPES, new String[0])) {
            if (noRemoveMixins.contains(mixin) && !newMixins.contains(mixin)) {
                newMixins.add(mixin);
            }
        }
        if (!newMixins.isEmpty() || newvalues.containsKey(PROP_MIXINTYPES)) {
            newvalues.put(PROP_MIXINTYPES, newMixins.toArray(new String[0]));
        }
    }

    private void ensureSameOrdering(List<Resource> templatechildren, Resource resource) throws RepositoryException {
        if (resource.adaptTo(Node.class).getPrimaryNodeType().hasOrderableChildNodes()) {
            templatechildren = filterNoRemoveNodes(templatechildren);
            Node node = Objects.requireNonNull(resource.adaptTo(Node.class));
            List<Resource> resourcechildren = filterNoRemoveNodes(IteratorUtils.toList(resource.listChildren()));
            if (templatechildren.size() != resourcechildren.size()) {
                throw new IllegalStateException("Bug: template and resource of " + resource.getPath() +
                        " should have same size now but have " + templatechildren.size() + " and " + resourcechildren.size());
            }
            if (resourcechildren.size() < 2) {
                return;
            }
            for (int i = 0; i < resourcechildren.size(); ++i) {
                if (!StringUtils.equals(resourcechildren.get(i).getName(), templatechildren.get(i).getName())) {
                    node.orderBefore(templatechildren.get(i).getName(), resourcechildren.get(i).getName());
                    resourcechildren = filterNoRemoveNodes(IteratorUtils.toList(resource.listChildren()));
                }
            }
        }
    }

    private List<Resource> filterNoRemoveNodes(List<Resource> children) {
        return children.stream().filter(r -> !noRemoveNodeNames.contains(r.getName())).collect(Collectors.toList());
    }

    private void checkForSamenameSiblings(Resource templateresource, @NotNull Resource resource)
            throws IllegalArgumentException, RepositoryException {
        Node node = resource.adaptTo(Node.class);
        NodeDefinition definition = node.getDefinition();
        if (definition.allowsSameNameSiblings()) {
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
    }

    protected static class ImportErrorListener implements ProgressTrackerListener {

        public final List<Pair<String, Exception>> errors = new ArrayList<>();

        @Override
        public void onMessage(Mode mode, String action, String path) {
            LOG.debug("Import message {} : {} : {}", mode, action, path);
        }

        @Override
        public void onError(Mode mode, String path, Exception e) {
            errors.add(Pair.of(path, e));
            LOG.debug("Import error {} : {} : {}", mode, path, String.valueOf(e));
        }
    }
}
