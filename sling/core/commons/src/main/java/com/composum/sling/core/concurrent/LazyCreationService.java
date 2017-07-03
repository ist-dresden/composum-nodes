package com.composum.sling.core.concurrent;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * Service that encapsulates a 'get or create' pattern for resources (incl. possibly a path several parent
 * directories), that adresses the following problems:
 * <ul>
 * <li>The creation is cluster-safe. If requests in several cluster nodes need the same resource and try to
 * create it at the same time, only one of them will succeed. This should, however, not cause the other
 * requests to fail.</li>
 * <li>The user needs the right to read the resource, but shouldn't neccesarily have the right to create or modify
 * the resource. Thus, the creation is done with an admin resolver, but the result is returned with the user's
 * resolver.</li>
 * </ul>
 * An admin resolver is only created when needed.
 */
public interface LazyCreationService {

    /**
     * Retrieves a resource or applies a creation strategy to be carried out with an admin resolver to create it.
     * <p>
     * It is an error if getter still returns null after creator is executed.
     * </p>
     *
     * @param <T>              the type the {@link RetrievalStrategy} returns.
     * @param resolver         the users resolver. If the resource needed to be created, that is getter returns null, we
     *                         create it and call a {@link javax.jcr.Session#refresh(boolean)}(true) on this session
     *                         before we call getter again. Passed as a parameter to getter.
     * @param path             the absolute path at which the creator creates the resource. Passed as a parameter to
     *                         getter and creator.
     * @param getter           side effect free function to retrieve the resource. This can be executed several times in
     *                         this process.
     * @param creator          a strategy to create the resource. Only called when the resource doesn't exist. Is only
     *                         called after the parent of path is created.
     * @param parentProperties properties with which non-existing parents of path are created ({@link
     *                         ResourceResolver#create(Resource, String, Map)}).
     * @return the object.
     */
    <T> T getOrCreate(ResourceResolver resolver, String path, RetrievalStrategy<T> getter,
                      CreationStrategy creator, Map<String, Object> parentProperties)
            throws RepositoryException;

    interface RetrievalStrategy<T> {
        /**
         * Side effect free function to retrieve whatever we want to retrieve.
         * Important is that is no sideeffects whenever it returns null, since that might be called
         * several times.
         *
         * @param resolver the users resolver, if needed here.
         * @param path     path at which we expect it, if needed here.
         * @return null if it doesn't exist yet.
         */
        T get(ResourceResolver resolver, String path) throws RepositoryException;
    }

    interface CreationStrategy {
        /**
         * Create the resource with the given administrative resolver. A {@link ResourceResolver#commit()} is
         * not necessary - that's performed in the LazyCreationService.
         *
         * @param resolver an administrative resolver
         * @param path     the path at which the resource is created.
         */
        void create(ResourceResolver resolver, String path) throws RepositoryException, PersistenceException;
    }

}
