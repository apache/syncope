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
package org.apache.syncope.common.keymaster.client.self;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.keymaster.rest.api.service.NetworkServiceService;
import org.apache.syncope.common.keymaster.rest.api.service.NetworkServiceService.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

public class SelfKeymasterServiceOps extends SelfKeymasterOps implements ServiceOps {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceOps.class);

    private final int maxRetries;

    private final String path;

    public SelfKeymasterServiceOps(final JAXRSClientFactoryBean clientFactory, final int maxRetries) {
        super(clientFactory);
        this.maxRetries = maxRetries;
        this.path = NetworkServiceService.class.getAnnotation(Path.class).value();
    }

    @Override
    public List<NetworkService> list(final NetworkService.Type serviceType) {
        try {
            return client(NetworkServiceService.class, Map.of()).list(serviceType);
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    @Override
    public NetworkService get(final NetworkService.Type serviceType) {
        try {
            return client(NetworkServiceService.class, Map.of()).get(serviceType);
        } catch (KeymasterException e) {
            throw e;
        } catch (Exception e) {
            throw new KeymasterException(e);
        }
    }

    private void handleRetry(
            final NetworkService service,
            final Action action,
            final int retries,
            final BackOffExecution backOffExecution) {

        try {
            if (action == Action.register && retries > 0) {
                long nextBackoff = backOffExecution.nextBackOff();

                LOG.debug("Still {} retries available for {}; waiting for {} ms", retries, service, nextBackoff);
                try {
                    Thread.sleep(nextBackoff);
                } catch (InterruptedException e) {
                    // ignore
                }

                retry(completionStage(action, service), service, action, retries - 1, backOffExecution);
            } else {
                LOG.debug("No more retries {} for {}", action, service);
            }
        } catch (Throwable t) {
            LOG.error("Could not continue {} for {}, aborting", action, service, t);
        }
    }

    private void retry(
            final CompletionStage<Response> completionStage,
            final NetworkService service,
            final Action action,
            final int retries,
            final BackOffExecution backOffExecution) {

        completionStage.whenComplete((response, throwable) -> {
            if (throwable == null && response.getStatus() < 300) {
                LOG.info("{} successfully {}ed", service, action);
            } else {
                LOG.error("Could not {} {}", action, service, throwable);

                handleRetry(service, action, retries, backOffExecution);
            }
        }).exceptionally(throwable -> {
            LOG.error("Could not {} {}", action, service, throwable);

            handleRetry(service, action, retries, backOffExecution);

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        });
    }

    private CompletionStage<Response> completionStage(final Action action, final NetworkService service) {
        return rx(path + "?action=" + action.name()).post(Entity.entity(service, MediaType.APPLICATION_JSON));
    }

    @Override
    public void register(final NetworkService service) {
        retry(completionStage(Action.register, service),
                service, Action.register, maxRetries, new ExponentialBackOff(5000L, 1.5).start());
    }

    @Override
    public void unregister(final NetworkService service) {
        retry(completionStage(Action.unregister, service),
                service, Action.unregister, maxRetries, new ExponentialBackOff(5000L, 1.5).start());
    }
}
