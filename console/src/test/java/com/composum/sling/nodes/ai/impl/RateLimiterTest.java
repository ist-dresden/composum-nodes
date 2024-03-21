package com.composum.sling.nodes.ai.impl;

import static org.hamcrest.CoreMatchers.is;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import com.composum.sling.nodes.ai.impl.RateLimiter;

/**
 * Tests {@link RateLimiter}.
 */
public class RateLimiterTest {

    @Rule
    public final ErrorCollector ec = new ErrorCollector();

    protected long time = 0; // System.currentTimeMillis();
    protected long startTime = time;
    protected int requestNumber = 0;

    protected RateLimiter limiter;

    protected class RateLimiterWithTestSetup extends RateLimiter {
        public RateLimiterWithTestSetup(RateLimiter parent, int limit, int period, TimeUnit timeUnit) {
            super(parent, limit, period, timeUnit);
        }

        @Override
        protected long getCurrentTimeMillis() {
            return time;
        }

        @Override
        protected void sleep(long delay) {
            time += delay;
        }

        @Override
        public synchronized void waitForLimit() {
            super.waitForLimit();
            requestNumber += 1;
        }
    }


    @Before
    public void setUp() {
        limiter = new RateLimiterWithTestSetup(null, 10, 100, TimeUnit.SECONDS);
    }

    protected void waitFor(int seconds) {
        time += TimeUnit.SECONDS.toMillis(seconds);
    }

    @Test
    public void testRateLimiting() {
        ec.checkThat(time, is(startTime));
        // the first five requests should not be limited
        for (int i = 0; i < 6; i++) {
            limiter.waitForLimit();
            ec.checkThat("On request " + i, time, is(startTime));
        }
        // the rest should spread out over the remaining two minutes.
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 20000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 40000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 60000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 80000L));
        // request 11 happens at 100 seconds and leads to reset.
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 100000L));
        for (int i = 0; i < 5; i++) {
            limiter.waitForLimit();
            ec.checkThat("On request " + i, time, is(startTime + 100000L));
        }
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 120000L));
    }

    @Test
    public void testRateLimitingWithWait() {
        ec.checkThat(time, is(startTime));
        // the first five requests should not be limited
        for (int i = 0; i < 5; i++) {
            limiter.waitForLimit();
            ec.checkThat("On request " + i, time, is(startTime));
        }
        waitFor(90);
        // since we've waited a while, we can do a spike without being limited now.
        ec.checkThat(time, is(startTime + 90000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 90000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 90000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 90000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 90000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 90000L));
        // but now the limit is reached, and we have to wait until the next reset.
        for (int i = 0; i < 6; i++) {
            limiter.waitForLimit();
            ec.checkThat("On request " + i, time, is(startTime + 100000L));
        }
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 120000L));
    }


    @Test
    public void testWithParent() {
        limiter = new RateLimiterWithTestSetup(limiter, 20, 1, TimeUnit.HOURS);
        for (int i = 0; i < 6; i++) {
            limiter.waitForLimit();
            ec.checkThat("On request " + i, time, is(startTime));
        }
        // the rest should spread out over the remaining two minutes, as the parent limiter says
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 20000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 40000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 60000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 80000L));
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 100000L));
        // now our limiter kicks in.
        limiter.waitForLimit();
        ec.checkThat(time, is(startTime + 360000L));
    }

    /**
     * Test that if we have a limit of 10 requests in 100 seconds, and we get 10 requests evenly spaced out over one minute, that we do never have a waiting time.
     */
    @Test
    public void testEvenlySpacedRequestsDontWait() {
        limiter = new RateLimiterWithTestSetup(null, 10, 100, TimeUnit.SECONDS);
        for (int i = 1; i <= 10; i++) {
            long currentTime = time;
            limiter.waitForLimit();
            ec.checkThat("On request " + i, time, is(currentTime));
            waitFor(10);
        }
    }

    @Test
    public void testRandomRequestsOverLongTimeDontExceedRate() {
        limiter = new RateLimiterWithTestSetup(null, 10, 100, TimeUnit.SECONDS);
        long startTime = time;
        int requestcount = 10000;
        for (int i = 0; i <= requestcount; i++) {
            limiter.waitForLimit();
            waitFor((int) (Math.random() * 5));
        }
        long duration = time - startTime;
        double realrate = requestcount * 1.0 / TimeUnit.MILLISECONDS.toSeconds(duration); // requests per second
        double expectedRate = 10.0 / 100.0;
        // System.out.printf("Real rate: %.6f, expected rate: %.6f%n", realrate, expectedRate);
        long minimumDuration = TimeUnit.SECONDS.toMillis(1000 / 10 * 100);
        ec.checkThat("Actually taken " + duration, duration > minimumDuration, is(true));
        // the real rate should be below the expected rate, but above 95% of it (statistical fluctuations)
        ec.checkThat("Real rate " + realrate, realrate <= expectedRate, is(true));
        ec.checkThat("Real rate " + realrate, realrate > expectedRate * 0.95, is(true));
    }

}
