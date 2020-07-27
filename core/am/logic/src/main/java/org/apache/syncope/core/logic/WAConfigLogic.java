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

package org.apache.syncope.core.logic;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.syncope.common.keymaster.client.api.KeymasterException;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.api.model.NetworkService;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.AMEntitlement;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.auth.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.auth.WAConfigEntry;
import org.apache.syncope.core.provisioning.api.data.WAConfigDataBinder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WAConfigLogic extends AbstractTransactionalLogic<EntityTO> {
    @Autowired
    private ServiceOps serviceOps;

    @Autowired
    private WAConfigDataBinder binder;

    @Autowired
    private WAConfigDAO configDAO;

    @Resource(name = "anonymousUser")
    private String anonymousUser;

    @Resource(name = "anonymousKey")
    private String anonymousKey;

    @Override
    protected EntityTO resolveReference(final Method method, final Object... args)
        throws UnresolvedReferenceException {
        String key = null;
        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof Attr) {
                    key = ((Attr) args[i]).getSchema();
                }
            }
        }

        if (key != null) {
            try {
                Attr attr = binder.getAttr(configDAO.find(key));
                return new EntityTO() {
                    private static final long serialVersionUID = -2683326649597260323L;
                    @Override
                    public String getKey() {
                        return attr.getSchema();
                    }

                    @Override
                    public void setKey(final String key) {
                    }
                };
            } catch (final Throwable e) {
                LOG.debug("Unresolved reference", e);
                throw new UnresolvedReferenceException(e);
            }
        }

        throw new UnresolvedReferenceException();
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_LIST + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<Attr> list() {
        return configDAO.findAll().stream().map(binder::getAttr).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_UPDATE + "')")
    public void update(final Attr configTO) {
        WAConfigEntry entry = configDAO.find(configTO.getSchema());
        if (entry == null) {
            throw new NotFoundException("Configuration entry " + configTO.getSchema() + " not found");
        }
        binder.update(entry, configTO);
        configDAO.save(entry);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_DELETE + "')")
    public void delete(final String key) {
        configDAO.delete(key);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_READ + "') or hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public Attr read(final String key) {
        WAConfigEntry entry = configDAO.find(key);
        if (entry == null) {
            throw new NotFoundException("Configuration entry " + key + " not found");
        }
        return binder.getAttr(entry);
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_CREATE + "')")
    public Attr create(final Attr configTO) {
        return binder.getAttr(configDAO.save(binder.create(configTO)));
    }

    @PreAuthorize("hasRole('" + AMEntitlement.WA_CONFIG_PUSH + "')")
    public void pushToWA() {
        try {
            NetworkService wa = serviceOps.get(NetworkService.Type.WA);
            HttpClient.newBuilder().build().send(
                HttpRequest.newBuilder(URI.create(
                    StringUtils.appendIfMissing(wa.getAddress(), "/") + "actuator/refresh")).
                    header(HttpHeaders.AUTHORIZATION,
                        DefaultBasicAuthSupplier.getBasicAuthHeader(anonymousUser, anonymousKey)).
                    POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.discarding());
        } catch (KeymasterException e) {
            throw new NotFoundException("Could not find any WA instance", e);
        } catch (IOException | InterruptedException e) {
            throw new InternalServerErrorException("Errors while communicating with WA instance", e);
        }
    }
}
