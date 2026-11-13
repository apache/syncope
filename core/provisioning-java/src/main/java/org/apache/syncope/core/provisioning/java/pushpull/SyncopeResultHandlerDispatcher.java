/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.provisioning.java.pushpull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import org.apache.syncope.core.persistence.api.entity.task.ProvisioningTask;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningActions;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopeResultHandler;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SyncopeAuthenticationDetails;
import org.apache.syncope.core.spring.security.SyncopeGrantedAuthority;
import org.apache.syncope.core.spring.task.VirtualThreadPoolTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public abstract class SyncopeResultHandlerDispatcher<
        T extends ProvisioningTask<?>, A extends ProvisioningActions, RA extends SyncopeResultHandler<T, A>> {

    protected static final Logger LOG = LoggerFactory.getLogger(SyncopeResultHandlerDispatcher.class);

    private static final String PLACEHOLDER_PWD = "PLACEHOLDER_PWD";

    /**
     * Default value for {@link #submitTimeoutSeconds}.
     */
    public static final int DEFAULT_SUBMIT_TIMEOUT_SECONDS = 300;

    /**
     * Default value for {@link #completionTimeoutSeconds}.
     */
    public static final int DEFAULT_COMPLETION_TIMEOUT_SECONDS = 600;

    protected Optional<VirtualThreadPoolTaskExecutor> tpte;

    /**
     * Only set in concurrent mode: one permit per configured pool size, so that the caller of
     * {@link #submit(Runnable)} is blocked while no handler task can be started - but never for longer than
     * {@link #submitTimeoutSeconds}.
     */
    protected Semaphore permits;

    /**
     * Maximum time to wait for a free slot before rejecting a new handler task.
     */
    protected int submitTimeoutSeconds = DEFAULT_SUBMIT_TIMEOUT_SECONDS;

    /**
     * Maximum overall time to wait, on {@link #shutdown()}, for the submitted handler tasks to complete.
     */
    protected int completionTimeoutSeconds = DEFAULT_COMPLETION_TIMEOUT_SECONDS;

    protected final Map<String, Supplier<RA>> suppliers = new ConcurrentHashMap<>();

    protected final Map<String, RA> handlers = new ConcurrentHashMap<>();

    protected final List<Future<?>> futures = new ArrayList<>();

    /**
     * Number of handler tasks rejected because no free slot became available within {@link #submitTimeoutSeconds}.
     */
    protected int rejectedSubmissions;

    protected void init(final ProvisioningProfile<T, A> profile) {
        if (profile.getTask().getConcurrentSettings() == null) {
            tpte = Optional.empty();
        } else {
            int poolSize = profile.getTask().getConcurrentSettings().getPoolSize();

            VirtualThreadPoolTaskExecutor t = new VirtualThreadPoolTaskExecutor();
            t.setPoolSize(poolSize);
            t.setWaitForTasksToCompleteOnShutdown(true);
            t.setThreadNamePrefix("provisioningTask-" + profile.getTask().getKey() + "-");
            t.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

            String domain = AuthContextUtils.getDomain();
            String delegatedBy = AuthContextUtils.getDelegatedBy().orElse(null);
            Set<SyncopeGrantedAuthority> authorities = AuthContextUtils.getAuthorities();
            t.setTaskDecorator(d -> () -> {
                // set placeholder authentication object by creating fresh and copying data from caller's
                UsernamePasswordAuthenticationToken placeHolderAuth = new UsernamePasswordAuthenticationToken(
                        new User(profile.getExecutor(), PLACEHOLDER_PWD, authorities), PLACEHOLDER_PWD, authorities);
                placeHolderAuth.setDetails(new SyncopeAuthenticationDetails(domain, delegatedBy));
                SecurityContextHolder.getContext().setAuthentication(placeHolderAuth);

                d.run();
            });

            t.initialize();

            permits = new Semaphore(poolSize);

            tpte = Optional.of(t);
        }
    }

    public void addHandlerSupplier(final String key, final Supplier<RA> supplier) {
        suppliers.put(key, supplier);
    }

    public RA nonConcurrentHandler(final String key) {
        return handlers.computeIfAbsent(key, k -> suppliers.get(k).get());
    }

    public void setSubmitTimeoutSeconds(final int submitTimeoutSeconds) {
        this.submitTimeoutSeconds = submitTimeoutSeconds;
    }

    public void setCompletionTimeoutSeconds(final int completionTimeoutSeconds) {
        this.completionTimeoutSeconds = completionTimeoutSeconds;
    }

    protected void submit(final Runnable runnable) {
        if (tpte.isEmpty()) {
            return;
        }

        if (!acquirePermit()) {
            rejectedSubmissions++;

            throw new RejectedExecutionException("No free slot for handler task after " + submitTimeoutSeconds + 's');
        }

        try {
            futures.add(tpte.get().submit(() -> {
                try {
                    runnable.run();
                } finally {
                    releasePermit();
                }
            }));
        } catch (RuntimeException e) {
            releasePermit();

            throw e;
        }
    }

    private boolean acquirePermit() {
        if (permits == null) {
            return true;
        }

        try {
            if (permits.tryAcquire(submitTimeoutSeconds, TimeUnit.SECONDS)) {
                return true;
            }

            LOG.error("Timeout after {}s while waiting for a free slot to submit a new handler task",
                    submitTimeoutSeconds);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            LOG.error("Interrupted while waiting for a free slot to submit a new handler task", e);
            return false;
        }
    }

    private void releasePermit() {
        Optional.ofNullable(permits).ifPresent(Semaphore::release);
    }

    public void stop() {
        handlers.values().forEach(SyncopeResultHandler::stop);
    }

    /**
     * Waits for the submitted handler tasks to complete, for no longer than {@link #completionTimeoutSeconds} overall;
     * any task still running at that point is cancelled.
     *
     * @throws JobExecutionException if not all handler tasks could be submitted and completed
     */
    protected void shutdown() throws JobExecutionException {
        awaitCompletion();

        List<Future<?>> notCompleted = futures.stream().filter(future -> !future.isDone()).toList();
        notCompleted.forEach(future -> future.cancel(true));

        tpte.ifPresent(VirtualThreadPoolTaskExecutor::shutdown);

        if (!notCompleted.isEmpty() || rejectedSubmissions > 0) {
            throw new JobExecutionException("Concurrent handling did not complete: "
                    + notCompleted.size() + " out of " + futures.size() + " handler task(s) still running after "
                    + completionTimeoutSeconds + 's'
                    + (rejectedSubmissions == 0
                            ? ""
                            : ", " + rejectedSubmissions + " handler task(s) not submitted within "
                            + submitTimeoutSeconds + 's'));
        }
    }

    private void awaitCompletion() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(completionTimeoutSeconds);

        for (Future<?> future : futures) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                LOG.error("Timeout after {}s while waiting for handler tasks to complete", completionTimeoutSeconds);
                return;
            }

            try {
                future.get(remaining, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                LOG.error("Timeout after {}s while waiting for handler tasks to complete", completionTimeoutSeconds, e);
                return;
            } catch (CancellationException e) {
                LOG.debug("Handler task was cancelled", e);
            } catch (ExecutionException e) {
                LOG.error("Unexpected error when waiting for completion", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                LOG.error("Interrupted while waiting for completion", e);
                return;
            }
        }
    }
}
