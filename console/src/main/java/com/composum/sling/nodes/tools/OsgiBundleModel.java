package com.composum.sling.nodes.tools;

import com.composum.sling.core.AbstractSlingBean;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.osgi.framework.wiring.BundleRevision.TYPE_FRAGMENT;

public class OsgiBundleModel extends AbstractSlingBean {

    public static final Pattern BUNDLE_ID_SUFFIX = Pattern.compile("/(?<id>[0-9]+)$");

    public static final String HD_ARCHIVER_VERSION = "Archiver Version";
    public static final String HD_BUNDLE_CATEGORY = "Category";
    public static final String HD_BUNDLE_LICENSE = "License";
    public static final String HD_BUNDLE_NAME = "Name";
    public static final String HD_BUNDLE_VENDOR = "Vendor";
    public static final String HD_CREATED_BY = "Created By";
    public static final String HD_DESCRIPTION = "Description";
    public static final String HD_DYNAMIC_IMPORT_PACKAGE = "Dynamic Import";
    public static final String HD_EMBED_DEPENDENCY = "Embed Dependency";
    public static final String HD_EXPORT_PACKAGE = "Export Package";
    public static final String HD_IMPLEMENTATION_BUILD = "Implementation Build";
    public static final String HD_IMPLEMENTATION_TITLE = "Implementation Title";
    public static final String HD_IMPLEMENTATION_VENDOR = "Implementation Vendor";
    public static final String HD_IMPLEMENTATION_VENDOR_ID = "Impl. Vendor-Id";
    public static final String HD_IMPLEMENTATION_VERSION = "Implementation Version";
    public static final String HD_IMPORT_PACKAGE = "Import Package";
    public static final String HD_INCLUDE_RESOURCE = "Include Resource";
    public static final String HD_PRIVATE_PACKAGE = "Private Package";
    public static final String HD_PROVIDE_CAPABILITY = "Provide Capability";
    public static final String HD_REQUIRE_CAPABILITY = "Require Capability";
    public static final String HD_REQ_EXEC_ENVIRONMENT = "Required Environment";
    public static final String HD_SERVICE_COMPONENT = "Service Component";
    public static final String HD_SPECIFICATION_TITLE = "Specification Title";
    public static final String HD_SPECIFICATION_VENDOR = "Specification Vendor";
    public static final String HD_SPECIFICATION_VERSION = "Specification Version";

    public static final Map<String, String> HEADER_NAME_MAP;

    static {
        HEADER_NAME_MAP = new TreeMap<>();
        HEADER_NAME_MAP.put("Bundle-Category", HD_BUNDLE_CATEGORY);
        HEADER_NAME_MAP.put("Bundle-License", HD_BUNDLE_LICENSE);
        HEADER_NAME_MAP.put("Bundle-Name", HD_BUNDLE_NAME);
        HEADER_NAME_MAP.put("Bundle-Vendor", HD_BUNDLE_VENDOR);
        HEADER_NAME_MAP.put("Bundle-Description", HD_DESCRIPTION);
        HEADER_NAME_MAP.put("DynamicImport-Package", HD_DYNAMIC_IMPORT_PACKAGE);
        HEADER_NAME_MAP.put("Implementation-Build", HD_IMPLEMENTATION_BUILD);
        HEADER_NAME_MAP.put("Implementation-Title", HD_IMPLEMENTATION_TITLE);
        HEADER_NAME_MAP.put("Implementation-Vendor", HD_IMPLEMENTATION_VENDOR);
        HEADER_NAME_MAP.put("Implementation-Vendor-Id", HD_IMPLEMENTATION_VENDOR_ID);
        HEADER_NAME_MAP.put("Implementation-Version", HD_IMPLEMENTATION_VERSION);
        HEADER_NAME_MAP.put("Private-Package", HD_PRIVATE_PACKAGE);
        HEADER_NAME_MAP.put("Bundle-RequiredExecutionEnvironment", HD_REQ_EXEC_ENVIRONMENT);
        HEADER_NAME_MAP.put("Specification-Title", HD_SPECIFICATION_TITLE);
        HEADER_NAME_MAP.put("Specification-Vendor", HD_SPECIFICATION_VENDOR);
        HEADER_NAME_MAP.put("Specification-Version", HD_SPECIFICATION_VERSION);
    }

    public static final List<String> REFERENCE_NAMES = Arrays.asList(
            HD_DYNAMIC_IMPORT_PACKAGE,
            HD_EXPORT_PACKAGE,
            HD_IMPORT_PACKAGE,
            HD_PRIVATE_PACKAGE);

    public enum State {
        uninstalled(Bundle.UNINSTALLED),
        installed(Bundle.INSTALLED),
        resolved(Bundle.RESOLVED),
        starting(Bundle.STARTING),
        stopping(Bundle.STOPPING),
        active(Bundle.ACTIVE),
        fragment(0x00000040);

        public final int value;

        static final Map<Integer, State> MAP = new HashMap<>();

        static {
            for (State state : values()) {
                MAP.put(state.value, state);
            }
        }

        State(int value) {
            this.value = value;
        }

        public static State valueOf(int value) {
            return MAP.get(value);
        }
    }

    public class Exported {

        protected final String symbolicName;
        protected final Version version;

        public Exported(@Nonnull final String symbolicName, @Nullable final Version version) {
            this.symbolicName = symbolicName;
            this.version = version;
        }
    }

    public class Imported {

        protected final String symbolicName;
        protected final VersionRange version;
        protected final boolean optional;
        protected Resolved resolved;

        public Imported(@Nonnull final String symbolicName, @Nullable final VersionRange version,
                        boolean optional) {
            this.symbolicName = symbolicName;
            this.version = version;
            this.optional = optional;
        }
    }

    public class Resolved {

        protected final Bundle bundle;
        protected final String symbolicName;
        protected final Version version;
        protected final boolean active;

        public Resolved(@Nonnull final Bundle bundle, @Nonnull final String symbolicName,
                        @Nullable final Version version) {
            this.bundle = bundle;
            this.symbolicName = symbolicName;
            this.version = version;
            active = isActive(bundle);
        }
    }

    public class ServiceModel {

        protected final ServiceReference<?> service;
        protected final Map<String, Object> properties = new TreeMap<>();

        public List<String> objectClass = new ArrayList<>();
        public int componentId;
        public String componentName;
        public int serviceId;
        public String servicePid;
        public int bundleId;
        public String description;

        public ServiceModel(@Nonnull final ServiceReference<?> service) {
            this.service = service;
            for (final String key : service.getPropertyKeys()) {
                final Object value = service.getProperty(key);
                if (value != null) {
                    switch (key) {
                        case "objectClass":
                            objectClass.addAll(Arrays.asList((String[]) value));
                            break;
                        case "component.id":
                            componentId = Integer.parseInt(value.toString());
                            break;
                        case "component.name":
                            componentName = value.toString();
                            break;
                        case "service.id":
                            serviceId = Integer.parseInt(value.toString());
                            break;
                        case "service.pid":
                            servicePid = value.toString();
                            break;
                        case "service.bundleid":
                            bundleId = Integer.parseInt(value.toString());
                            break;
                        case "service.description":
                            description = value.toString();
                            break;
                        case "service.scope":
                            break;
                        default:
                            properties.put(key, value);
                            break;
                    }
                }
            }
        }

        public Map<String, Object> getProperties() {
            return properties;
        }
    }

    protected Bundle bundle;
    protected Map<String, String> headers;
    protected String name;
    protected String category;

    private transient State state;
    private transient Boolean fragment;

    private transient Map<String, Imported> importedSet;
    private transient Map<String, Exported> exportedSet;
    private transient Map<String, Resolved> resolvedSet;

    private transient BundleContext bundleContext;

    @Override
    public void initialize(BeanContext context, Resource resource) {
        super.initialize(context, resource);
        final RequestPathInfo pathInfo = context.getRequest().getRequestPathInfo();
        final String suffix = pathInfo.getSuffix();
        final Matcher matcher;
        if (suffix != null && (matcher = BUNDLE_ID_SUFFIX.matcher(suffix)).matches()) {
            final Bundle bundle = getBundleContext().getBundle(Long.parseLong(matcher.group("id")));
            if (bundle != null) {
                initialize(bundle);
            }
        }
    }

    protected void initialize(@Nonnull final Bundle bundle) {
        this.bundle = bundle;
        headers = new TreeMap<>();
        final Dictionary<String, String> bundleHeaders = bundle.getHeaders();
        for (final Enumeration<String> keys = bundleHeaders.keys(); keys.hasMoreElements(); ) {
            final String key = keys.nextElement();
            final String name = HEADER_NAME_MAP.get(key);
            if (name != null) {
                final String value = bundleHeaders.get(key);
                switch (name) {
                    case HD_BUNDLE_NAME:
                        this.name = value;
                        break;
                    case HD_BUNDLE_CATEGORY:
                        category = value;
                        break;
                    default:
                        headers.put(name, bundleHeaders.get(key));
                        break;
                }
            }
        }
    }

    public boolean isValid() {
        return bundle != null;
    }

    public boolean isFragment() {
        if (fragment == null) {
            fragment = isFragment(bundle);
        }
        return fragment;
    }

    protected static boolean isFragment(@Nonnull final Bundle bundle) {
        BundleRevision revision = bundle.adapt(BundleRevision.class);
        return revision != null && (revision.getTypes() & TYPE_FRAGMENT) != 0;
    }

    @Nonnull
    public Long getBundleId() {
        return bundle.getBundleId();
    }

    @Nonnull
    public Set<Map.Entry<String, String>> getHeaders() {
        return headers.entrySet();
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getSymbolicName() {
        return bundle.getSymbolicName();
    }

    @Nonnull
    public String getCategory() {
        return category;
    }

    @Nonnull
    public State getState() {
        if (state == null) {
            state = getState(bundle);
        }
        return state;
    }

    protected static State getState(@Nonnull final Bundle bundle) {
        State state = State.valueOf(bundle.getState());
        if (isFragment(bundle) && state == State.resolved) {
            state = State.fragment;
        }
        return state;
    }

    public boolean isActive() {
        State state = getState();
        return state == State.active || state == State.fragment;
    }

    protected static boolean isActive(Bundle bundle) {
        State state = getState(bundle);
        return state == State.active || state == State.fragment;
    }

    @Nonnull
    public String getLastModified() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(bundle.getLastModified()));
    }

    @Nonnull
    public String getVersion() {
        return bundle.getVersion().toString();
    }

    @Nonnull
    public String getLocation() {
        return bundle.getLocation();
    }

    @Nonnull
    public Iterator<ServiceModel> getProvidedServices() {
        ServiceReference<?>[] registered = bundle.getRegisteredServices();
        final Iterator<ServiceReference<?>> services = Stream.of(
                registered != null ? registered : new ServiceReference<?>[0]).iterator();
        return new Iterator<ServiceModel>() {

            @Override
            public boolean hasNext() {
                return services.hasNext();
            }

            @Override
            public ServiceModel next() {
                return new ServiceModel(services.next());
            }
        };
    }

    @Nonnull
    public Iterator<ServiceModel> getUsedServices() {
        ServiceReference<?>[] registered = bundle.getServicesInUse();
        final Iterator<ServiceReference<?>> services = Stream.of(
                registered != null ? registered : new ServiceReference<?>[0]).iterator();
        return new Iterator<ServiceModel>() {

            @Override
            public boolean hasNext() {
                return services.hasNext();
            }

            @Override
            public ServiceModel next() {
                return new ServiceModel(services.next());
            }
        };
    }

    public Collection<Exported> getExported() {
        return getExportedSet().values();
    }

    @Nullable
    public Exported getExported(@Nonnull String symbolicName) {
        while (symbolicName.indexOf('.') > 0) {
            final Exported exported = getExportedSet().get(symbolicName);
            if (exported != null) {
                return exported;
            }
            symbolicName = symbolicName.substring(0, symbolicName.lastIndexOf('.'));
        }
        return null;
    }

    public Map<String, Exported> getExportedSet() {
        if (exportedSet == null) {
            exportedSet = new TreeMap<>();
            final PackageSet packageSet = scanPackgeReferences(bundle.getHeaders().get("Export-Package"));
            for (PackageReference pckgRef : packageSet.getPackages()) {
                final String symbolicName = pckgRef.getName();
                final String version = pckgRef.getVersion();
                exportedSet.put(symbolicName, new Exported(pckgRef.getName(),
                        StringUtils.isNotBlank(version) ? new Version(version) : null));
            }
        }
        return exportedSet;
    }

    public Collection<Imported> getImported() {
        return getImportedSet().values();
    }

    @Nullable
    public Imported getImported(@Nonnull final String symbolicName) {
        return getImportedSet().get(symbolicName);
    }

    public Map<String, Imported> getImportedSet() {
        if (importedSet == null) {
            importedSet = new TreeMap<>();
            final PackageSet packageSet = scanPackgeReferences(bundle.getHeaders().get("Import-Package"));
            for (PackageReference pckgRef : packageSet.getPackages()) {
                final String symbolicName = pckgRef.getName();
                final String version = pckgRef.getVersion();
                if (getExported(symbolicName) == null) {
                    importedSet.put(symbolicName, new Imported(pckgRef.getName(),
                            StringUtils.isNotBlank(version) ? new VersionRange(version) : null,
                            "optional".equals(pckgRef.getResolution())));
                }
            }
            for (Imported imported : importedSet.values()) {
                imported.resolved = getResolved(imported.symbolicName);
            }
        }
        return importedSet;
    }

    public Collection<Resolved> getResolved() {
        return getResolvedSet().values();
    }

    @Nullable
    public Resolved getResolved(@Nonnull String symbolicName) {
        while (symbolicName.indexOf('.') > 0) {
            final Resolved resolved = getResolvedSet().get(symbolicName);
            if (resolved != null) {
                return resolved;
            }
            symbolicName = symbolicName.substring(0, symbolicName.lastIndexOf('.'));
        }
        return null;
    }

    public Map<String, Resolved> getResolvedSet() {
        if (resolvedSet == null) {
            resolvedSet = new TreeMap<>();
            final BundleWiring wiring = bundle.adapt(BundleWiring.class);
            if (wiring != null) {
                final List<BundleWire> wires = wiring.getRequiredWires(null);
                for (final BundleWire wire : wires) {
                    final BundleCapability capability = wire.getCapability();
                    final String symbolicName = (String) capability.getAttributes().get("osgi.wiring.package");
                    final BundleRevision provider = wire.getProvider();
                    if (provider != null && StringUtils.isNotBlank(symbolicName)) {
                        final Version version = (Version) capability.getAttributes().get("version");
                        resolvedSet.put(symbolicName, new Resolved(provider.getBundle(), symbolicName, version));
                    }
                }
            } else if (importedSet != null) {
                final OsgiBundlesModel bundles = new OsgiBundlesModel(context);
                for (final OsgiBundleModel model : bundles.getBundles()) {
                    final Bundle bundle = model.bundle;
                    final List<BundleCapability> capabilities = bundle.adapt(BundleRevision.class)
                            .getDeclaredCapabilities(null);
                    for (BundleCapability capability : capabilities) {
                        final String symbolicName = (String) capability.getAttributes().get("osgi.wiring.package");
                        for (final Imported imported : importedSet.values()) {
                            if (imported.symbolicName.equals(symbolicName)) {
                                final Object value = capability.getAttributes().get("version");
                                if (value instanceof List) {
                                    for (Version version : ((List<Version>) value)) {
                                        if (imported.version == null || imported.version.includes(version)) {
                                            resolvedSet.put(symbolicName, new Resolved(bundle, symbolicName, version));
                                            break;
                                        }
                                    }
                                } else {
                                    final Version version = (Version) value;
                                    if (imported.version == null || version == null || imported.version.includes(version)) {
                                        resolvedSet.put(symbolicName, new Resolved(bundle, symbolicName, version));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return resolvedSet;
    }

    // reference resolving

    public class PackageReference {

        private final String name;
        private final Map<String, List<String>> options = new HashMap<>();

        public PackageReference(@Nonnull final String name) {
            this.name = name;
        }

        @Nonnull
        public String getName() {
            return name;
        }

        @Nullable
        public String getOption(@Nonnull final String key) {
            final List<String> values = getOptionValues(key);
            return values != null ? StringUtils.join(values, ",") : null;
        }

        @Nullable
        public List<String> getOptionValues(@Nonnull final String key) {
            return options.get(key);
        }

        protected void addOption(@Nonnull final String key, @Nonnull final String value) {
            List<String> values = options.computeIfAbsent(key, k -> new ArrayList<>());
            values.add(value);
        }

        @Nullable
        public String getVersion() {
            return getOption("version");
        }

        @Nullable
        public String getResolution() {
            return getOption("resolution");
        }

        @Nonnull
        public String toString() {
            final String version = getVersion();
            return name + (StringUtils.isNotBlank(version) ? (";" + version) : "");
        }
    }

    public class PackageSet {

        protected final Map<String, PackageReference> packages = new TreeMap<>();

        public boolean isEmpty() {
            return packages.isEmpty();
        }

        @Nonnull
        public Set<String> getKeys() {
            return packages.keySet();
        }

        public void add(@Nonnull final PackageReference packageRef) {
            packages.put(packageRef.getName(), packageRef);
        }

        public void remove(@Nonnull final String key) {
            packages.remove(key);
        }

        @Nonnull
        public Collection<PackageReference> getPackages() {
            return packages.values();
        }
    }

    public static final Pattern PACKAGE_REF_START = Pattern.compile(
            "( *,)? *(?<name>[^ ;,]+)");
    public static final Pattern PACKAGE_REF_OPTION = Pattern.compile(
            " *; *(?<key>[^:= \"']+) *:?= *(([\"'](?<values>[^\"']+)[\"'])|(?<value>[^;,]+))");

    @Nonnull
    protected PackageSet scanPackgeReferences(@Nonnull final String references) {
        final PackageSet result = new PackageSet();
        if (StringUtils.isNotBlank(references)) {
            final Matcher matcher = PACKAGE_REF_START.matcher(references);
            int pos = 0;
            while (matcher.find(pos) && matcher.start() == pos) {
                final PackageReference packageRef = new PackageReference(matcher.group("name"));
                pos = matcher.end();
                final String tail = references.substring(pos);
                final Matcher options = PACKAGE_REF_OPTION.matcher(tail);
                int optPos = 0;
                while (options.find(optPos) && options.start() == optPos) {
                    final String values = options.group("values");
                    packageRef.addOption(options.group("key"),
                            StringUtils.isNotBlank(values) ? values : options.group("value"));
                    optPos = options.end();
                }
                pos += optPos;
                result.add(packageRef);
            }
        }
        return result;
    }

    // initializing

    protected BundleContext getBundleContext() {
        if (bundleContext == null) {
            bundleContext = FrameworkUtil.getBundle(BeanContext.class).getBundleContext();
        }
        return bundleContext;
    }

    public OsgiBundleModel(@Nonnull final BeanContext context, @Nonnull final Bundle bundle) {
        this.context = context;
        this.resource = ResourceHandle.use(context.getResource());
        initialize(bundle);
    }

    public OsgiBundleModel(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public OsgiBundleModel(BeanContext context) {
        super(context);
    }

    public OsgiBundleModel() {
        super();
    }
}
