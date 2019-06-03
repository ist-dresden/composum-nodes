package com.composum.sling.core.concurrent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component(immediate = true, metatype = false)
@Service
public class SemaphoreSequencer implements SequencerService<SemaphoreSequencer.Token> {

    private static final Logger LOG = LoggerFactory.getLogger(SemaphoreSequencer.class);

    public static final class Token implements SequencerService.Token {

        protected final String key;
        protected final Semaphore semaphore;

        protected Token(String key, Semaphore semaphore) {
            this.key = key;
            this.semaphore = semaphore;
        }
    }

    /**
     * We use here a synchronized {@link WeakHashMap} so that the semaphores can be reclaimed once they are not used anymore.
     * As long as the semaphore is in the token, it's not thrown away; afterwards GC can remove it at leisure.
     */
    volatile WeakHashMap<String, Semaphore> semaphores;

    @Override
    public Token acquire(String key) {

        Semaphore semaphore;

        synchronized (this) {
            semaphore = semaphores.get(key);
            if (semaphore == null) {
                semaphore = new Semaphore(1);
                semaphores.put(key, semaphore);
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            // tryAcquire would fail, and it doesn't make sense to continue here.
            throw new java.lang.IllegalStateException("Can't acquire lock since our thread is interrupted.");
        }

        try {
            LOG.debug("acquiring (" + key + ")");
            semaphore.tryAcquire(1, TimeUnit.HOURS);
            LOG.debug("acquired (" + key + ")");
        } catch (InterruptedException ex) {
            LOG.error(ex.getMessage(), ex);
            if (!Thread.currentThread().isInterrupted()) { // must be a timeout
                LOG.error("Unlocking semaphore for {} since we've been waiting for an hour. There must be something broken.", key);
                // Hard to tell what to do here. We try having everything waiting for this continue and hopefully crash.
                semaphore.release();
                while (semaphore.hasQueuedThreads()) semaphore.release();
            }
            throw new java.lang.IllegalStateException("Could not acquive lock for a loong time, or we have been interrupted.", ex);
        }

        return new Token(key, semaphore);
    }

    @Override
    public void release(Token token) {
        if (token != null) {
            LOG.debug("release (" + token.key + ")");
            token.semaphore.release();
        } else {
            LOG.warn("release called with a null token");
        }
    }

    @Activate
    protected void activate(@SuppressWarnings("UnusedParameters") ComponentContext context) {
        Map<String, Semaphore> oldSemaphores;
        synchronized (this) {
            oldSemaphores = semaphores;
            semaphores = new WeakHashMap<String, Semaphore>();
        }
        cleanOldSemaphores(oldSemaphores);
    }

    /**
     * We release all locks currently held to unblock threads waiting for something. That can break our users, but
     * what can you do when the service is deactivated? That's probably better than having blocked threads lying around.
     */
    @Deactivate
    protected void deactivate(@SuppressWarnings("UnusedParameters") ComponentContext context) {
        Map<String, Semaphore> semaphoresToClear = null;
        synchronized (this) {
            if (semaphores != null) {
                semaphoresToClear = semaphores;
                semaphores = null;
            }
        }
        cleanOldSemaphores(semaphoresToClear);
    }

    protected void cleanOldSemaphores(Map<String, Semaphore> semaphoresToClear) {
        if (semaphoresToClear == null || semaphoresToClear.isEmpty())
            return;
        for (int i = 0; i < 10; ++i) { // if our users try to reacquire the semaphore.
            for (Semaphore semaphore : semaphoresToClear.values()) {
                while (semaphore.hasQueuedThreads()) semaphore.release();
            }
        }
    }
}
