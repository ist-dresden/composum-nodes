package com.composum.sling.core.concurrent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component(immediate = true)
@Service
public class SemaphoreSequencer implements SequencerService<SemaphoreSequencer.Token> {

    private static final Logger LOG = LoggerFactory.getLogger(SemaphoreSequencer.class);

    public static final class Token implements SequencerService.Token {
        @Nonnull
        protected final String key;
        protected final Semaphore semaphore;

        protected Token(@Nonnull String key, Semaphore semaphore) {
            this.key = key;
            this.semaphore = semaphore;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null) return false;
            Token token = (Token) o;
            return key.equals(token.key);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }

    /**
     * Memorizes the tokens we have passed out, so that a new acquire with the same key goes to exactly the same semaphore.
     * The weakreference goes to the token itself.
     * We use here a synchronized {@link WeakHashMap} so that the semaphores can be reclaimed once they are not used anymore.
     * As long as the semaphore is in the token, it's not thrown away; afterwards GC can remove it at leisure.
     */
    volatile WeakHashMap<Token, WeakReference<Token>> activeTokens;

    @Override
    @Nonnull
    public Token acquire(String key) {

        Token token;
        Semaphore semaphore;

        synchronized (this) {
            WeakReference<Token> ref = activeTokens.get(new Token(key, null));
            token = ref != null ? ref.get() : null;
            if (token == null) {
                semaphore = new Semaphore(1);
                token = new Token(key, semaphore);
                activeTokens.put(token, new WeakReference<>(token));
            } else {
                semaphore = token.semaphore;
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
                while (semaphore.hasQueuedThreads()) {
                    Thread.yield();
                    semaphore.release();
                }
            }
            throw new java.lang.IllegalStateException("Could not acquive lock for a loong time, or we have been interrupted.", ex);
        }

        // We create a fresh token so that each user gets it's own token, which might make debugging easier at times.
        synchronized (this) {
            token = new Token(key, semaphore);
            activeTokens.put(token, new WeakReference<>(token));
        }
        return token;
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
        WeakHashMap<Token, WeakReference<Token>> oldTokens;
        synchronized (this) {
            oldTokens = activeTokens;
            activeTokens = new WeakHashMap<>();
        }
        cleanOldSemaphores(oldTokens);
    }

    /**
     * We release all locks currently held to unblock threads waiting for something. That can break our users, but
     * what can you do when the service is deactivated? That's probably better than having blocked threads lying around.
     */
    @Deactivate
    protected void deactivate(@SuppressWarnings("UnusedParameters") ComponentContext context) {
        Map<Token, WeakReference<Token>> semaphoresToClear = null;
        synchronized (this) {
            if (activeTokens != null) {
                semaphoresToClear = activeTokens;
                activeTokens = null;
            }
        }
        cleanOldSemaphores(semaphoresToClear);
    }

    protected void cleanOldSemaphores(Map<Token, WeakReference<Token>> tokensToClear) {
        if (tokensToClear == null || tokensToClear.isEmpty())
            return;
        for (int i = 0; i < 10; ++i) { // if our users try to reacquire the semaphore.
            for (Token token : tokensToClear.keySet()) {
                while (token.semaphore.hasQueuedThreads()) {
                    token.semaphore.release();
                    Thread.yield();
                }
            }
        }
    }
}
