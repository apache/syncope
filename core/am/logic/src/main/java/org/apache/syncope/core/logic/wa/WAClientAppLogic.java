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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.common.lib.wa.WAClientApp;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.provisioning.api.data.wa.WAClientAppDataBinder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class WAClientAppLogic {

    protected final WAClientAppDataBinder binder;

    protected final CASSPClientAppDAO casSPClientAppDAO;

    protected final OIDCRPClientAppDAO oidcRPClientAppDAO;

    protected final SAML2SPClientAppDAO saml2SPClientAppDAO;

    public WAClientAppLogic(
            final WAClientAppDataBinder binder,
            final CASSPClientAppDAO casSPClientAppDAO,
            final OIDCRPClientAppDAO oidcRPClientAppDAO,
            final SAML2SPClientAppDAO saml2SPClientAppDAO) {

        this.binder = binder;
        this.casSPClientAppDAO = casSPClientAppDAO;
        this.oidcRPClientAppDAO = oidcRPClientAppDAO;
        this.saml2SPClientAppDAO = saml2SPClientAppDAO;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public List<WAClientApp> list() {
        List<WAClientApp> clientApps = new ArrayList<>();

        Stream.of(ClientAppType.values()).forEach(type -> {
            switch (type) {
                case OIDCRP:
                    clientApps.addAll(oidcRPClientAppDAO.findAll().stream().
                            map(binder::getWAClientApp).toList());
                    break;

                case SAML2SP:
                    clientApps.addAll(saml2SPClientAppDAO.findAll().stream().
                            map(binder::getWAClientApp).toList());
                    break;

                case CASSP:
                default:
                    clientApps.addAll(casSPClientAppDAO.findAll().stream().
                            map(binder::getWAClientApp).toList());
            }
        });

        return clientApps;
    }

    protected WAClientApp doRead(final Long clientAppId, final ClientAppType type) {
        WAClientApp clientApp = null;

        switch (type) {
            case OIDCRP:
                clientApp = oidcRPClientAppDAO.findByClientAppId(clientAppId).map(binder::getWAClientApp).orElse(null);
                break;

            case SAML2SP:
                clientApp = saml2SPClientAppDAO.findByClientAppId(clientAppId).map(binder::getWAClientApp).orElse(null);
                break;

            case CASSP:
                clientApp = casSPClientAppDAO.findByClientAppId(clientAppId).map(binder::getWAClientApp).orElse(null);
                break;

            default:
        }

        return clientApp;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public WAClientApp read(final Long clientAppId, final ClientAppType type) {
        WAClientApp clientApp = null;
        if (type == null) {
            for (int i = 0; i < ClientAppType.values().length && clientApp == null; i++) {
                clientApp = doRead(clientAppId, ClientAppType.values()[i]);
            }
        } else {
            clientApp = doRead(clientAppId, type);
        }

        if (clientApp == null) {
            throw new NotFoundException(
                    "Client app with clientApp ID " + clientAppId + " and type " + type + " not found");
        }
        return clientApp;
    }

    protected WAClientApp doRead(final String name, final ClientAppType type) {
        WAClientApp clientApp = null;

        switch (type) {
            case OIDCRP:
                clientApp = oidcRPClientAppDAO.findByName(name).map(binder::getWAClientApp).orElse(null);
                break;

            case SAML2SP:
                clientApp = saml2SPClientAppDAO.findByName(name).map(binder::getWAClientApp).orElse(null);
                break;

            case CASSP:
                clientApp = casSPClientAppDAO.findByName(name).map(binder::getWAClientApp).orElse(null);
                break;

            default:
        }

        return clientApp;
    }

    @PreAuthorize("hasRole('" + IdRepoEntitlement.ANONYMOUS + "')")
    @Transactional(readOnly = true)
    public WAClientApp read(final String name, final ClientAppType type) {
        WAClientApp clientApp = null;
        if (type == null) {
            for (int i = 0; i < ClientAppType.values().length && clientApp == null; i++) {
                clientApp = doRead(name, ClientAppType.values()[i]);
            }
        } else {
            clientApp = doRead(name, type);
        }

        if (clientApp == null) {
            throw new NotFoundException("Client app with name " + name + " with type " + type + " not found");
        }
        return clientApp;
    }
}
