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

    /**
     * Retrieves a resource or applies a creation strategy to be carried out with an admin resolver to create it.
     * <p>
     * It is an error if getter still returns null after creator is executed.
     * </p>
     *
     * @param <T>                    the type the {@link RetrievalStrategy} returns.
     * @param resolver               the users resolver. If the resource needed to be created, that is getter returns
     *                               null, we create it and call a {@link javax.jcr.Session#refresh(boolean)}(true) on
     *                               this session before we call getter again. Passed as a parameter to getter.
     * @param path                   the absolute path at which the creator creates the resource. Passed as a parameter
     *                               to getter and creator.
     * @param getter                 side effect free function to retrieve the resource. This can be executed several
     *                               times in this process.
     * @param creator                a strategy to create the resource. Only called when the resource doesn't exist. Is
     *                               only called after the parent of path is created.
     * @param parentCreationStrategy strategy with which non-existing parents of path are created.
     * @return the object.
     */
    <T> T getOrCreate(ResourceResolver resolver, String path, RetrievalStrategy<T> getter,
                      CreationStrategy creator, ParentCreationStrategy parentCreationStrategy)
            throws RepositoryException;

    /**
     * Retrieves a resource or applies a creation and initialization strategy to be carried out with an admin resolver
     * to create it, for resource intensive initialization processes that should not performed twice in the cluster. The
     * resource is locked for the cluster with {@link javax.jcr.lock.LockManager} during that time. <p> It is an error
     * if getter still returns null after creator is executed. </p>
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
     *                         called after the parent of path is created. This should not perform any resource
     *                         intensive actions - these should be done in initializer.
     * @param initializer      the resource intensive part of the resource creation
     * @param parentProperties properties with which non-existing parents of path are created ({@link
     *                         ResourceResolver#create(Resource, String, Map)}).
     * @return the object.
     */
    <T> T getOrCreate(ResourceResolver resolver, String path, RetrievalStrategy<T> getter,
                      CreationStrategy creator, InitializationStrategy initializer,
                      Map<String, Object> parentProperties) throws RepositoryException, PersistenceException;

    /**
     * Retrieves a resource or applies a creation and initialization strategy to be carried out with an admin resolver
     * to create it, for resource intensive initialization processes that should not performed twice in the cluster. The
     * resource is locked for the cluster with {@link javax.jcr.lock.LockManager} during that time. <p> It is an error
     * if getter still returns null after creator is executed. </p>
     * <p>
     * <p>
     * It is an error if getter still returns null after creator is executed.
     * </p>
     *
     * @param <T>                    the type the {@link RetrievalStrategy} returns.
     * @param resolver               the users resolver. If the resource needed to be created, that is getter returns
     *                               null, we create it and call a {@link javax.jcr.Session#refresh(boolean)}(true) on
     *                               this session before we call getter again. Passed as a parameter to getter.
     * @param path                   the absolute path at which the creator creates the resource. Passed as a parameter
     *                               to getter and creator.
     * @param getter                 side effect free function to retrieve the resource. This can be executed several
     *                               times in this process.
     * @param creator                a strategy to create the resource. Only called when the resource doesn't exist. Is
     *                               only called after the parent of path is created.
     * @param initializer            the resource intensive part of the resource creation
     * @param parentCreationStrategy strategy with which non-existing parents of path are created.
     * @return the object.
     */
    <T> T getOrCreate(ResourceResolver resolver, String path, RetrievalStrategy<T> getter,
                      CreationStrategy creator, InitializationStrategy initializer,
                      ParentCreationStrategy parentCreationStrategy) throws RepositoryException, PersistenceException;

    /**
     * For resources created by {@link #getOrCreate(ResourceResolver, String, RetrievalStrategy, CreationStrategy,
     * InitializationStrategy, ParentCreationStrategy)} or {@link #getOrCreate(ResourceResolver, String,
     * RetrievalStrategy, CreationStrategy, InitializationStrategy, Map)}, this returns true when the initialization
     * process is finished.
     */
    boolean isInitialized(Resource resource) throws RepositoryException;

    /**
     * For resources created by {@link #getOrCreate(ResourceResolver, String, RetrievalStrategy, CreationStrategy,
     * InitializationStrategy, ParentCreationStrategy)} or {@link #getOrCreate(ResourceResolver, String,
     * RetrievalStrategy, CreationStrategy, InitializationStrategy, Map)}, this returns the resource when the
     * initialization process is finished. If the resource is in creation, this waits a while.
     *
     * @return the initialized resource, or null if we couldn't find it or it took too long.
     */
    Resource waitForInitialization(ResourceResolver resolver, String path) throws RepositoryException;

    /** Strategy to retrieve the resources content. */
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

    /** Strategy to create the resource itself. */
    interface CreationStrategy {
        /**
         * Create the resource with the given administrative resolver. A {@link ResourceResolver#commit()} is
         * not necessary - that's performed in the LazyCreationService.
         *
         * @param resolver an administrative resolver
         * @param parent   the parent of the resource at which to attach the resource
         * @param name     the name of the resource to create.
         * @return the created resource, which must be the child with the given name of the given parent.
         */
        Resource create(ResourceResolver resolver, Resource parent, String name)
                throws RepositoryException, PersistenceException;
    }

    /**
     * Strategy to initialize the resource, if that's a resource intensive task that should be separated from
     * {@link CreationStrategy}.
     */
    interface InitializationStrategy {
        /**
         * Initializes the resource. A {@link ResourceResolver#commit()} should not be done - that's done by the {@link
         * LazyCreationService}.
         *
         * @param resolver the admin-resolver - can be used if child resources need to be created etc.
         * @param resource the resource to initialize
         */
        void initialize(ResourceResolver resolver, Resource resource) throws RepositoryException, PersistenceException;
    }

    /** Strategy to create the parents of the retrieved resource. */
    interface ParentCreationStrategy {
        /**
         * Create the resource with the given administrative resolver. A {@link ResourceResolver#commit()} is
         * not necessary - that's performed in the LazyCreationService.
         *
         * @param resolver      an administrative resolver
         * @param parentsParent the parent of the parent resource at which to attach the new parent
         * @param parentName    the name of the parent resource to create.
         * @param level         informative, the level of the parent: 1 is the immediate parent of the resource, 2 the
         *                      next level up, ...
         * @return the created parent resource
         */
        Resource createParent(ResourceResolver resolver, Resource parentsParent, String parentName, int level)
                throws RepositoryException, PersistenceException;
    }

    /**
     * Simplest {@link com.composum.sling.core.concurrent.LazyCreationService.RetrievalStrategy}: just returns the
     * resource.
     */
    final RetrievalStrategy<Resource> IDENTITY_RETRIEVER = new RetrievalStrategy<Resource>() {
        @Override
        public Resource get(ResourceResolver resolver, String path) throws RepositoryException {
            return resolver.getResource(path);
        }
    };
}
