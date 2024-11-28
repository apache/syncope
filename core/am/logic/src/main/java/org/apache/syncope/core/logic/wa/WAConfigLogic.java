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
package org.apache.syncope.core.logic.wa;

import jakarta.ws.rs.core.HttpHeaders;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apache.syncope.core.logic.AbstractTransactionalLogic;
import org.apache.syncope.core.logic.UnresolvedReferenceException;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public class WAConfigLogic extends AbstractTransactionalLogic<EntityTO> {

    protected final ServiceOps serviceOps;

    protected final WAConfigDataBinder binder;

    protected final WAConfigDAO waConfigDAO;

    protected final SecurityProperties securityProperties;

    public WAConfigLogic(
            final ServiceOps serviceOps,
            final WAConfigDataBinder binder,
            final WAConfigDAO waConfigDAO,
            final SecurityProperties securityProperties) {

        this.serviceOps = serviceOps;
        this.binder = binder;
        this.waConfigDAO = waConfigDAO;
        this.securityProperties = securityProperties;
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_LIST + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<Attr> list() {
        return waConfigDAO.findAll().stream().map(binder::get).toList();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_GET + "')")
    @Transactional(readOnly = true)
    public Attr get(final String key) {
        return waConfigDAO.findById(key).
                map(binder::get).
                orElseThrow(() -> new NotFoundException("WAConfigEntry " + key));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_SET + "')")
    public void set(final Attr value) {
        waConfigDAO.save(binder.set(value));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_DELETE + "')")
    public void delete(final String key) {
        waConfigDAO.deleteById(key);
    }

    protected void registeredServices(final HttpClient client, final String serviceAddress) {
        String target = StringUtils.appendIfMissing(serviceAddress, "/") + "actuator/registeredServices";
        client.sendAsync(
                HttpRequest.newBuilder(URI.create(target)).
                        header(HttpHeaders.AUTHORIZATION, DefaultBasicAuthSupplier.getBasicAuthHeader(
                                securityProperties.getAnonymousUser(), securityProperties.getAnonymousKey())).
                        GET().build(),
                HttpResponse.BodyHandlers.discarding()).
                thenAcceptAsync(response -> LOG.info(
                "Pushed to {} with HTTP status: {}", target, response.statusCode()));
    }

    protected void refresh(final HttpClient client, final String serviceAddress) {
        String target = StringUtils.appendIfMissing(serviceAddress, "/") + "actuator/refresh";
        client.sendAsync(
                HttpRequest.newBuilder(URI.create(target)).
                        header(HttpHeaders.AUTHORIZATION, DefaultBasicAuthSupplier.getBasicAuthHeader(
                                securityProperties.getAnonymousUser(), securityProperties.getAnonymousKey())).
                        POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.discarding()).
                thenAcceptAsync(response -> LOG.info(
                "Pushed to {} with HTTP status: {}", target, response.statusCode()));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_PUSH + "')")
    public void pushToWA(final WAConfigService.PushSubject subject, final List<String> services) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            serviceOps.list(NetworkService.Type.WA).stream().
                    filter(wa -> CollectionUtils.isEmpty(services) || services.contains(wa.getAddress())).
                    forEach(wa -> {
                        switch (subject) {
                            case clientApps:
                                registeredServices(client, wa.getAddress());
                                break;

                            case conf:
                            default:
                                refresh(client, wa.getAddress());
                        }
                    });
        } catch (KeymasterException e) {
            throw new NotFoundException("Could not find any WA instance", e);
        }
    }

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        throw new UnresolvedReferenceException();
    }
}
