package com.composum.sling.core.concurrent;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

@Component(immediate = true, metatype = false)
@Service
public class SemaphoreSequencer implements SequencerService<SemaphoreSequencer.Token> {

    private static final Logger LOG = LoggerFactory.getLogger(SemaphoreSequencer.class);

    public static final class Token {

        protected final String key;
        protected final Semaphore semaphore;

        protected Token(String key, Semaphore semaphore) {
            this.key = key;
            this.semaphore = semaphore;
        }
    }

    HashMap<String, Semaphore> semaphores;

    public Token acquire(String key) {

        Semaphore semaphore;

        synchronized (this) {
            semaphore = semaphores.get(key);
            if (semaphore == null) {
                semaphore = new Semaphore(1);
                semaphores.put(key, semaphore);
            }
        }

        try {
            LOG.debug("aquire (" + key + ")");
            semaphore.acquire();

        } catch (InterruptedException ex) {
            LOG.error(ex.getMessage(), ex);
            semaphore.release();
        }

        return new Token(key, semaphore);
    }

    public void release(Token token) {

        if (token != null) {

            LOG.debug("release (" + token.key + ")");
            token.semaphore.release();

            synchronized (this) {
                if (!token.semaphore.hasQueuedThreads()) {
                    semaphores.remove(token.key);
                }
            }
        }
    }

    @Activate
    protected void activate(ComponentContext context) {
        semaphores = new HashMap<>();
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        synchronized (this) {
            if (semaphores != null) {
                for (Semaphore semaphore : semaphores.values()) {
                    while (semaphore.hasQueuedThreads()) {
                        semaphore.release();
                    }
                }
                semaphores.clear();
                semaphores = null;
            }
        }
    }
}
