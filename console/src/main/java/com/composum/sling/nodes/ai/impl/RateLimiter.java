package com.composum.sling.nodes.ai.impl;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to limit the rate of requests to the chat service.
 * The idea is that we set a certain limit of the number of requests in a given time period,
 * do not limit the first half of the request count in that time period, but then delay requests if it looks like the limit
 * would be exceeded at the end of that time period, beginning freshly after it has passed.
 * We can make a chain of RateLimiters - e.g. one for the current minute, one for the current hour, one for the current day.
 * Also it can be chained to one for the whole service (that could be split similarly).
 * <p>
 * The rate limiting is introduced for several reasons: first, OpenAI poses some limits by itself, then we want to
 * limit DOS attacks, and, last but not least, each request has a price, even if it's much less than a cent.
 */
public class RateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiter.class);

    @Nullable
    private final RateLimiter parent;
    @Nonnegative
    private final int limit;
    @Nonnegative
    private final int period;
    @Nonnull
    private final TimeUnit timeUnit;
    private final long periodDurationMillis;

    private int requestCount;
    private long nextResetTime;

    /**
     * Constructs a rate limiter with a parent.
     *
     * @param parent   if set, this is also observed
     * @param limit    the number of requests allowed in the given time period
     *                 (the first half of the requests are not limited, the second half might be limited)
     *                 (if the parent is set, the limit is applied to the sum of requests from this and the parent)
     *                 (if the parent is not set, the limit is applied to the requests from this only)
     * @param period   the time period in the given time unit
     * @param timeUnit the time unit of the time period
     */
    public RateLimiter(@Nullable RateLimiter parent, @Nonnegative int limit, @Nonnegative int period, @Nonnull TimeUnit timeUnit) {
        this.parent = parent;
        this.limit = limit;
        this.period = period;
        this.timeUnit = timeUnit;
        this.periodDurationMillis = timeUnit.toMillis(period);
    }

    /**
     * For a synchronous call: {@link #waitForLimit()} has to be called before starting the request; it'll
     * make sure the rate limit is not exceeded.
     * We are using a soft limit here, i.e. we allow the first half of the requests in the time period to go through
     * without any delay, but then we delay requests if it looks like the limit would be exceeded at the end of the time period.
     * That way the user is served promptly if he makes only few requests, but if he makes more requests he has not
     * to wait for the whole time period to pass, but only a minimum time so that the requests are spaced out evenly
     * over the rest of the period.
     * <p>
     * Specifically, we make sure that the user never has to wait more than 2 * periodDurationMillis / limit for the next request.
     */
    public synchronized void waitForLimit() {
        if (parent != null) {
            parent.waitForLimit();
        }
        long now = getCurrentTimeMillis();
        if (now >= nextResetTime) {
            requestCount = 1;
            nextResetTime = now + periodDurationMillis;
        } else {
            requestCount++;
            if (requestCount > limit / 2) {
                long safetyTime = (limit - requestCount + 1) * 2 * periodDurationMillis / limit;
                long earliestRequestTime = nextResetTime - safetyTime;
                long delay = 0;
                if (now < earliestRequestTime) {
                    delay = earliestRequestTime - now;
                }
                try {
                    sleep(delay);
                } catch (InterruptedException e) {
                    // should not happen, but we can only give up here if it does
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                now = getCurrentTimeMillis();
                if (now >= nextResetTime) { // we've reached the next round during the sleep, so reset
                    requestCount = 1;
                    nextResetTime = now + periodDurationMillis;
                }
            }
        }
    }

    /**
     * Provides the possibility to fake time, for easy unittests.
     */
    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Provides the possibility to fake time, for easy unittests.
     */
    protected void sleep(long delay) throws InterruptedException {
        LOG.info("Sleeping for {} ms because of {} requests limit {} in {}", delay, requestCount, limit, period);
        Thread.sleep(delay);
    }

    protected static final Pattern PATTERN_LIMIT_ERROR = Pattern.compile("Limit: (\\d+) / min.");

    /**
     * Tries to find something like "Limit: 3 / min." in errorbody and returns a RateLimiter for that.
     */
    @Nullable
    public static RateLimiter of(String errorbody) {
        Matcher matcher = PATTERN_LIMIT_ERROR.matcher(errorbody);
        if (matcher.find()) {
            String limitString = matcher.group(1);
            int limit = Integer.parseInt(limitString);
            LOG.info("Found limit {} per minute in errorbody", limit);
            return new RateLimiter(null, limit, 1, TimeUnit.MINUTES);
        } else {
            return null;
        }
    }

}
